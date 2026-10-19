package com.proyecto.nacional.service;

import com.proyecto.nacional.dto.reservas.ReservaLiberacionRequest;
import com.proyecto.nacional.dto.reservas.ReservaRequest;
import com.proyecto.nacional.dto.reservas.ReservaResponse;
import com.proyecto.nacional.dto.reservas.ReservaResponse.ItemReservaResponse;
import com.proyecto.nacional.dto.reservas.ReservaTemporalRequest;
import com.proyecto.nacional.exception.ConflictoException;
import com.proyecto.nacional.exception.RecursoNoEncontradoException;
import com.proyecto.nacional.exception.SolicitudInvalidaException;
import com.proyecto.nacional.model.EstadoReserva;
import com.proyecto.nacional.model.InventarioOng;
import com.proyecto.nacional.model.Oferta;
import com.proyecto.nacional.model.Ong;
import com.proyecto.nacional.model.Recurso;
import com.proyecto.nacional.model.Reserva;
import com.proyecto.nacional.repository.InventarioOngRepository;
import com.proyecto.nacional.repository.OfertaRepository;
import com.proyecto.nacional.repository.OngRepository;
import com.proyecto.nacional.repository.RecursoRepository;
import com.proyecto.nacional.repository.ReservaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reserva de los recursos de las ONGs que ofrece una oferta, en tres pasos que llama Bonita:
 * reserva temporal (validación), reserva definitiva (oferta adjudicada) y liberación (finalización).
 *
 * El stock de cada ONG (InventarioOng.cantidadDisponible) no se modifica: lo libre es el stock menos las
 * reservas activas. Para que dos operaciones simultáneas no vean una imagen vieja de lo libre, cada una
 * corre en una transacción que primero toma con SELECT ... FOR UPDATE la oferta y las filas de inventario
 * involucradas (en orden de id) y recién después calcula y escribe.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class ReservaService {

    private record Par(Long ongId, Long recursoId) {
    }

    private final OfertaRepository ofertaRepository;
    private final OngRepository ongRepository;
    private final RecursoRepository recursoRepository;
    private final InventarioOngRepository inventarioOngRepository;
    private final ReservaRepository reservaRepository;
    private final Clock clock;
    private final long bloqueoHoras;

    public ReservaService(OfertaRepository ofertaRepository,
                          OngRepository ongRepository,
                          RecursoRepository recursoRepository,
                          InventarioOngRepository inventarioOngRepository,
                          ReservaRepository reservaRepository,
                          Clock clock,
                          @Value("${reservas.bloqueo-horas:24}") long bloqueoHoras) {
        this.ofertaRepository = ofertaRepository;
        this.ongRepository = ongRepository;
        this.recursoRepository = recursoRepository;
        this.inventarioOngRepository = inventarioOngRepository;
        this.reservaRepository = reservaRepository;
        this.clock = clock;
        this.bloqueoHoras = bloqueoHoras;
    }

    // ---------- Reserva temporal ----------

    @Transactional
    public ReservaResponse reservarTemporal(ReservaTemporalRequest request) {
        Oferta oferta = ofertaRepository
                .findByEmergenciaIdAndLoteIdAndOfertaId(request.emergenciaId(), request.loteId(), request.ofertaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la oferta " + request.ofertaId()
                        + " del lote " + request.loteId() + " de la emergencia " + request.emergenciaId()));
        LocalDateTime ahora = LocalDateTime.now(clock);

        List<Reserva> existentes = reservaRepository.findByEmergenciaIdAndLoteIdAndOfertaIdOrderByIdAsc(
                request.emergenciaId(), request.loteId(), request.ofertaId());
        if (existentes.stream().anyMatch(r -> r.getEstado() == EstadoReserva.COMPROMETIDA)) {
            throw new ConflictoException("La oferta " + request.ofertaId()
                    + " ya tiene recursos comprometidos; libérelos antes de volver a reservar");
        }

        Map<Par, Integer> ofrecido = new LinkedHashMap<>();
        Map<Long, Recurso> recursos = new HashMap<>();
        cargarOfrecido(oferta, ofrecido, recursos);
        Map<Par, Integer> pedido = elegirPedido(request, ofrecido);

        Map<Long, Ong> ongs = buscarOngs(pedido.keySet());
        Map<Par, InventarioOng> inventario = bloquearInventario(pedido.keySet());

        // Revalidar reemplaza los bloqueos anteriores de la oferta; si esto falla, la transacción los restituye
        existentes.stream()
                .filter(r -> r.getEstado() == EstadoReserva.BLOQUEADA)
                .forEach(r -> liberar(r, ahora));

        verificarDisponibilidad(pedido, inventario, ongs, recursos, ahora);

        List<Reserva> nuevas = new ArrayList<>();
        pedido.forEach((par, cantidad) -> {
            Reserva reserva = new Reserva();
            reserva.setOng(ongs.get(par.ongId()));
            reserva.setRecurso(recursos.get(par.recursoId()));
            reserva.setCantidad(cantidad);
            reserva.setEstado(EstadoReserva.BLOQUEADA);
            reserva.setEmergenciaId(request.emergenciaId());
            reserva.setLoteId(request.loteId());
            reserva.setOfertaId(request.ofertaId());
            reserva.setFechaCreacion(ahora);
            reserva.setExpiraEn(ahora.plusHours(bloqueoHoras));
            nuevas.add(reservaRepository.save(reserva));
        });
        log.info("Reserva temporal. oferta={} lote={} emergencia={} items={} expiraEn={}", request.ofertaId(),
                request.loteId(), request.emergenciaId(), nuevas.size(), ahora.plusHours(bloqueoHoras));
        return armarRespuesta(request.emergenciaId(), request.loteId(), request.ofertaId(), nuevas, ahora);
    }

    // ---------- Reserva definitiva ----------

    @Transactional
    public ReservaResponse reservarDefinitiva(ReservaRequest request) {
        bloquearOferta(request.emergenciaId(), request.loteId(), request.ofertaId());
        LocalDateTime ahora = LocalDateTime.now(clock);

        List<Reserva> reservas = reservasDeLaOferta(request.emergenciaId(), request.loteId(), request.ofertaId());
        if (reservas.isEmpty()) {
            throw new RecursoNoEncontradoException("La oferta " + request.ofertaId()
                    + " no tiene reservas; hay que hacer la reserva temporal primero");
        }
        List<Reserva> vigentes = reservas.stream()
                .filter(r -> r.getEstado() != EstadoReserva.LIBERADA)
                .toList();
        if (vigentes.isEmpty()) {
            throw new ConflictoException("Los recursos de la oferta " + request.ofertaId()
                    + " ya fueron liberados; hay que volver a reservarlos");
        }

        List<Reserva> bloqueadas = vigentes.stream()
                .filter(r -> r.getEstado() == EstadoReserva.BLOQUEADA)
                .toList();
        if (!bloqueadas.isEmpty()) {
            Set<Par> pares = new HashSet<>();
            bloqueadas.forEach(r -> pares.add(new Par(r.getOng().getId(), r.getRecurso().getId())));
            Map<Par, InventarioOng> inventario = bloquearInventario(pares);

            for (Reserva reserva : bloqueadas) {
                // Un bloqueo vencido ya no tenía tomado el stock: hay que comprobar que siga habiendo
                if (vencida(reserva, ahora)) {
                    Par par = new Par(reserva.getOng().getId(), reserva.getRecurso().getId());
                    verificarDisponibilidad(Map.of(par, reserva.getCantidad()), inventario,
                            Map.of(par.ongId(), reserva.getOng()), Map.of(par.recursoId(), reserva.getRecurso()),
                            ahora);
                }
                reserva.setEstado(EstadoReserva.COMPROMETIDA);
                reserva.setExpiraEn(null);
                reserva.setFechaCompromiso(ahora);
                // Se escribe ya para que el próximo ítem del mismo recurso lo cuente como tomado
                reservaRepository.saveAndFlush(reserva);
            }
            log.info("Reserva definitiva. oferta={} items={}", request.ofertaId(), bloqueadas.size());
        }
        return armarRespuesta(request.emergenciaId(), request.loteId(), request.ofertaId(), vigentes, ahora);
    }

    // ---------- Liberación ----------

    @Transactional
    public ReservaResponse liberar(ReservaLiberacionRequest request) {
        bloquearOferta(request.emergenciaId(), request.loteId(), request.ofertaId());
        LocalDateTime ahora = LocalDateTime.now(clock);

        List<Reserva> reservas = reservasDeLaOferta(request.emergenciaId(), request.loteId(), request.ofertaId())
                .stream()
                .filter(r -> request.ongId() == null || r.getOng().getId().equals(request.ongId()))
                .toList();
        if (reservas.isEmpty()) {
            throw new RecursoNoEncontradoException("No hay reservas de la oferta " + request.ofertaId()
                    + (request.ongId() == null ? "" : " para la ONG " + request.ongId()));
        }

        // Las ya liberadas y los bloqueos vencidos se dejan como están, así repetir la liberación no cambia nada
        reservas.stream()
                .filter(r -> activa(r, ahora))
                .forEach(r -> liberar(r, ahora));
        log.info("Liberación. oferta={} ong={} items={}", request.ofertaId(), request.ongId(), reservas.size());
        return armarRespuesta(request.emergenciaId(), request.loteId(), request.ofertaId(), reservas, ahora);
    }

    // ---------- Consulta ----------

    public ReservaResponse consultar(Long emergenciaId, Long loteId, Long ofertaId) {
        List<Reserva> reservas = reservasDeLaOferta(emergenciaId, loteId, ofertaId);
        if (reservas.isEmpty()) {
            throw new RecursoNoEncontradoException("No hay reservas de la oferta " + ofertaId);
        }
        return armarRespuesta(emergenciaId, loteId, ofertaId, reservas, LocalDateTime.now(clock));
    }

    // ---------- Helpers ----------

    // Toma el lock de la oferta si está guardada; liberar o comprometer no exige que siga guardada
    private void bloquearOferta(Long emergenciaId, Long loteId, Long ofertaId) {
        ofertaRepository.findByEmergenciaIdAndLoteIdAndOfertaId(emergenciaId, loteId, ofertaId);
    }

    private List<Reserva> reservasDeLaOferta(Long emergenciaId, Long loteId, Long ofertaId) {
        return reservaRepository.findByEmergenciaIdAndLoteIdAndOfertaIdOrderByIdAsc(emergenciaId, loteId, ofertaId);
    }

    // Lo que ofrece cada ONG de cada recurso, sumando los aportes de la oferta que repiten recurso
    private void cargarOfrecido(Oferta oferta, Map<Par, Integer> ofrecido, Map<Long, Recurso> recursos) {
        Map<String, Recurso> porNombre = new HashMap<>();
        oferta.getAportes().forEach(aporte -> {
            Recurso recurso = porNombre.computeIfAbsent(aporte.getRecursoNombre(), nombre ->
                    recursoRepository.findByNombre(nombre).orElseThrow(() -> new ConflictoException(
                            "El recurso '" + nombre + "' de la oferta no está en el catálogo")));
            recursos.put(recurso.getId(), recurso);
            aporte.getPorOng().stream()
                    .filter(porOng -> porOng.getCantidadOfrecida() != null && porOng.getCantidadOfrecida() > 0)
                    .forEach(porOng -> ofrecido.merge(new Par(porOng.getOngId(), recurso.getId()),
                            porOng.getCantidadOfrecida(), Integer::sum));
        });
        if (ofrecido.isEmpty()) {
            throw new ConflictoException("La oferta no tiene aportes de ninguna ONG para reservar");
        }
    }

    // Todo lo ofrecido, o solo los ítems pedidos (que deben estar en la oferta y no superarla)
    private Map<Par, Integer> elegirPedido(ReservaTemporalRequest request, Map<Par, Integer> ofrecido) {
        if (request.items() == null || request.items().isEmpty()) {
            return ofrecido;
        }
        Map<Par, Integer> pedido = new LinkedHashMap<>();
        for (ReservaTemporalRequest.ItemReserva item : request.items()) {
            Par par = new Par(item.ongId(), item.recursoId());
            Integer maximo = ofrecido.get(par);
            if (maximo == null) {
                throw new SolicitudInvalidaException("La oferta no incluye el recurso " + item.recursoId()
                        + " de la ONG " + item.ongId());
            }
            if (item.cantidad() > maximo) {
                throw new SolicitudInvalidaException("La ONG " + item.ongId() + " ofrece " + maximo
                        + " del recurso " + item.recursoId() + " y se pidieron " + item.cantidad());
            }
            if (pedido.put(par, item.cantidad()) != null) {
                throw new SolicitudInvalidaException("El recurso " + item.recursoId() + " de la ONG "
                        + item.ongId() + " está repetido en los ítems");
            }
        }
        return pedido;
    }

    private Map<Long, Ong> buscarOngs(Set<Par> pares) {
        Set<Long> ids = new HashSet<>();
        pares.forEach(par -> ids.add(par.ongId()));
        Map<Long, Ong> ongs = new HashMap<>();
        ongRepository.findAllById(ids).forEach(ong -> ongs.put(ong.getId(), ong));
        ids.stream().filter(id -> !ongs.containsKey(id)).findFirst().ifPresent(id -> {
            throw new ConflictoException("La ONG " + id + " de la oferta no está registrada en el Sistema Nacional");
        });
        return ongs;
    }

    // SELECT ... FOR UPDATE de las filas de stock de estos pares; un par sin fila queda fuera (stock 0)
    private Map<Par, InventarioOng> bloquearInventario(Set<Par> pares) {
        Set<Long> ongIds = new HashSet<>();
        Set<Long> recursoIds = new HashSet<>();
        pares.forEach(par -> {
            ongIds.add(par.ongId());
            recursoIds.add(par.recursoId());
        });
        Map<Par, InventarioOng> inventario = new HashMap<>();
        for (InventarioOng fila : inventarioOngRepository.bloquearPorOngYRecurso(ongIds, recursoIds)) {
            inventario.put(new Par(fila.getOng().getId(), fila.getRecurso().getId()), fila);
        }
        return inventario;
    }

    // Todo o nada: si algún recurso no alcanza se informan todos los faltantes y no se reserva nada
    private void verificarDisponibilidad(Map<Par, Integer> pedido, Map<Par, InventarioOng> inventario,
                                         Map<Long, Ong> ongs, Map<Long, Recurso> recursos, LocalDateTime ahora) {
        List<String> faltantes = new ArrayList<>();
        pedido.forEach((par, cantidad) -> {
            InventarioOng fila = inventario.get(par);
            long stock = fila == null ? 0 : fila.getCantidadDisponible();
            long libre = stock - reservaRepository.sumarActivas(par.ongId(), par.recursoId(), ahora);
            if (cantidad > libre) {
                faltantes.add(ongs.get(par.ongId()).getRazonSocial() + " - "
                        + recursos.get(par.recursoId()).getNombre() + ": se piden " + cantidad
                        + " y hay " + Math.max(libre, 0) + " libres");
            }
        });
        if (!faltantes.isEmpty()) {
            throw new ConflictoException("Recursos insuficientes: " + String.join("; ", faltantes));
        }
    }

    private void liberar(Reserva reserva, LocalDateTime ahora) {
        reserva.setEstado(EstadoReserva.LIBERADA);
        reserva.setExpiraEn(null);
        reserva.setFechaLiberacion(ahora);
    }

    private boolean vencida(Reserva reserva, LocalDateTime ahora) {
        return reserva.getEstado() == EstadoReserva.BLOQUEADA && !reserva.getExpiraEn().isAfter(ahora);
    }

    // Comprometida, o bloqueada y todavía vigente
    private boolean activa(Reserva reserva, LocalDateTime ahora) {
        return reserva.getEstado() == EstadoReserva.COMPROMETIDA
                || (reserva.getEstado() == EstadoReserva.BLOQUEADA && !vencida(reserva, ahora));
    }

    private String estadoVisible(Reserva reserva, LocalDateTime ahora) {
        return vencida(reserva, ahora) ? "EXPIRADA" : reserva.getEstado().name();
    }

    private ReservaResponse armarRespuesta(Long emergenciaId, Long loteId, Long ofertaId,
                                           List<Reserva> reservas, LocalDateTime ahora) {
        List<ItemReservaResponse> items = reservas.stream()
                .sorted(Comparator.comparing(Reserva::getId))
                .map(r -> new ItemReservaResponse(
                        r.getOng().getId(), r.getOng().getRazonSocial(),
                        r.getRecurso().getId(), r.getRecurso().getNombre(), r.getRecurso().getUnidadMedida(),
                        r.getCantidad(), estadoVisible(r, ahora), r.getExpiraEn()))
                .toList();

        String estado = resumir(items);
        LocalDateTime expiraEn = items.stream()
                .filter(i -> "BLOQUEADA".equals(i.estado()))
                .map(ItemReservaResponse::expiraEn)
                .max(Comparator.naturalOrder())
                .orElse(null);
        return new ReservaResponse(emergenciaId, loteId, ofertaId, estado, expiraEn, items);
    }

    private String resumir(List<ItemReservaResponse> items) {
        for (String estado : List.of("COMPROMETIDA", "BLOQUEADA", "EXPIRADA")) {
            if (items.stream().anyMatch(i -> estado.equals(i.estado()))) {
                return estado;
            }
        }
        return "LIBERADA";
    }
}
