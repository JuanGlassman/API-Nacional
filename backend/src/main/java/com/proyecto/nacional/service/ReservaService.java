package com.proyecto.nacional.service;

import com.proyecto.nacional.dto.reservas.ReservaLiberacionRequest;
import com.proyecto.nacional.dto.reservas.ReservaRequest;
import com.proyecto.nacional.dto.reservas.ReservaResponse;
import com.proyecto.nacional.dto.reservas.ReservaResponse.ItemReservaResponse;
import com.proyecto.nacional.dto.reservas.ReservaTemporalRequest;
import com.proyecto.nacional.dto.reservas.ValidacionRequest;
import com.proyecto.nacional.dto.reservas.ValidacionResponse;
import com.proyecto.nacional.exception.ConflictoException;
import com.proyecto.nacional.exception.RecursoNoEncontradoException;
import com.proyecto.nacional.exception.SolicitudInvalidaException;
import com.proyecto.nacional.model.CategoriaRecurso;
import com.proyecto.nacional.model.EstadoReserva;
import com.proyecto.nacional.model.InventarioOng;
import com.proyecto.nacional.model.NivelCompetencia;
import com.proyecto.nacional.model.Oferta;
import com.proyecto.nacional.model.OfertaAporte;
import com.proyecto.nacional.model.Ong;
import com.proyecto.nacional.model.OngCategoriaNivel;
import com.proyecto.nacional.model.Recurso;
import com.proyecto.nacional.model.Reserva;
import com.proyecto.nacional.repository.InventarioOngRepository;
import com.proyecto.nacional.repository.OfertaRepository;
import com.proyecto.nacional.repository.OngCategoriaNivelRepository;
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
import java.util.TreeSet;

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

    // La ONG se identifica por su código común, no por el id de esta base
    private record Par(String ongCodigo, Long recursoId) {
    }

    private final OfertaRepository ofertaRepository;
    private final OngRepository ongRepository;
    private final RecursoRepository recursoRepository;
    private final InventarioOngRepository inventarioOngRepository;
    private final ReservaRepository reservaRepository;
    private final OngCategoriaNivelRepository nivelRepository;
    private final Clock clock;
    private final long bloqueoHoras;

    public ReservaService(OfertaRepository ofertaRepository,
                          OngRepository ongRepository,
                          RecursoRepository recursoRepository,
                          InventarioOngRepository inventarioOngRepository,
                          ReservaRepository reservaRepository,
                          OngCategoriaNivelRepository nivelRepository,
                          Clock clock,
                          @Value("${reservas.bloqueo-horas:24}") long bloqueoHoras) {
        this.ofertaRepository = ofertaRepository;
        this.ongRepository = ongRepository;
        this.recursoRepository = recursoRepository;
        this.inventarioOngRepository = inventarioOngRepository;
        this.reservaRepository = reservaRepository;
        this.nivelRepository = nivelRepository;
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
        Map<Par, Integer> solicitado = elegirPedido(request, ofrecido);

        Map<String, Ong> ongs = buscarOngs(solicitado.keySet());
        Map<Par, InventarioOng> inventario = bloquearInventario(solicitado.keySet());

        // Revalidar reemplaza los bloqueos anteriores de la oferta; si esto falla, la transacción los restituye
        existentes.stream()
                .filter(r -> r.getEstado() == EstadoReserva.BLOQUEADA)
                .forEach(r -> liberar(r, ahora));

        Map<Par, Integer> pedido = solicitado;
        if (request.esParcial()) {
            pedido = ajustarAlLibre(solicitado, inventario, ongs, recursos, ahora);
        } else {
            verificarDisponibilidad(solicitado, inventario, ongs, recursos, ahora);
        }
        Map<Par, Integer> aReservar = pedido;

        List<Reserva> nuevas = new ArrayList<>();
        aReservar.forEach((par, cantidad) -> nuevas.add(bloquear(request.emergenciaId(), request.loteId(),
                request.ofertaId(), ongs.get(par.ongCodigo()), recursos.get(par.recursoId()), cantidad, ahora)));
        log.info("Reserva temporal. oferta={} lote={} emergencia={} items={} expiraEn={}", request.ofertaId(),
                request.loteId(), request.emergenciaId(), nuevas.size(), ahora.plusHours(bloqueoHoras));
        return armarRespuesta(request.emergenciaId(), request.loteId(), request.ofertaId(), nuevas, ahora);
    }

    // ---------- Validación del lote ----------

    /**
     * Valida de una vez todas las ofertas de un lote y deja bloqueados los recursos que se les asignan.
     * Por cada recurso reparte lo que pide el lote entre los aportes de las ONGs: primero las ONGs de mejor
     * nivel en la categoría del recurso, después la oferta más antigua, hasta cubrir lo pedido o agotar el
     * stock libre. Una ONG sin nivel en la categoría no está habilitada y no recibe nada.
     * Repetirla con bloqueos vigentes devuelve esos mismos bloqueos sin recalcular.
     */
    @Transactional
    public ValidacionResponse validar(ValidacionRequest request) {
        List<Oferta> ofertas = ofertaRepository.bloquearPorLote(request.emergenciaId(), request.loteId());
        if (ofertas.isEmpty()) {
            throw new RecursoNoEncontradoException("No hay ofertas guardadas del lote " + request.loteId()
                    + " de la emergencia " + request.emergenciaId());
        }
        LocalDateTime ahora = LocalDateTime.now(clock);

        List<Reserva> existentes = reservaRepository.findByEmergenciaIdAndLoteIdOrderByIdAsc(
                request.emergenciaId(), request.loteId());
        if (existentes.stream().anyMatch(r -> r.getEstado() == EstadoReserva.COMPROMETIDA)) {
            throw new ConflictoException("El lote " + request.loteId()
                    + " ya tiene recursos comprometidos; libérelos antes de volver a validar");
        }
        if (existentes.stream().anyMatch(r -> activa(r, ahora))) {
            return armarValidacion(request, ofertas, existentes, ahora);
        }
        // Lo que quede son bloqueos vencidos: dejan de contar
        existentes.stream().filter(r -> r.getEstado() == EstadoReserva.BLOQUEADA).forEach(r -> liberar(r, ahora));

        Map<Long, Recurso> recursos = new HashMap<>();
        Map<Long, Integer> requerido = new HashMap<>();
        Map<Long, Map<Par, Integer>> ofrecidoPorOferta = new LinkedHashMap<>();
        Set<Par> pares = new HashSet<>();
        for (Oferta oferta : ofertas) {
            Map<Par, Integer> ofrecido = new LinkedHashMap<>();
            sumarOfrecido(oferta, ofrecido, recursos, requerido);
            ofrecidoPorOferta.put(oferta.getOfertaId(), ofrecido);
            pares.addAll(ofrecido.keySet());
        }

        List<Reserva> nuevas = new ArrayList<>();
        if (!pares.isEmpty()) {
            Map<String, Ong> ongs = buscarOngs(pares);
            Map<Par, InventarioOng> inventario = bloquearInventario(pares);
            Map<String, Map<CategoriaRecurso, NivelCompetencia>> niveles = nivelesDe(ongs.keySet());
            Map<Long, LocalDateTime> fechas = new HashMap<>();
            ofertas.forEach(o -> fechas.put(o.getOfertaId(), o.getFechaOferta()));

            for (Long recursoId : new TreeSet<>(recursos.keySet())) {
                Recurso recurso = recursos.get(recursoId);
                List<AsignadorRecursos.Candidato> candidatos = new ArrayList<>();
                Map<String, Integer> libre = new HashMap<>();
                ofrecidoPorOferta.forEach((ofertaId, ofrecido) -> ofrecido.forEach((par, cantidad) -> {
                    if (!par.recursoId().equals(recursoId)) {
                        return;
                    }
                    int prioridad = prioridad(recurso, niveles.get(par.ongCodigo()));
                    if (prioridad == 0) {
                        return; // ONG no habilitada en la categoría
                    }
                    candidatos.add(new AsignadorRecursos.Candidato(ofertaId, fechas.get(ofertaId),
                            par.ongCodigo(), prioridad, cantidad));
                    libre.computeIfAbsent(par.ongCodigo(), codigo -> libre(ongs.get(codigo), recurso,
                            inventario.get(par), ahora));
                }));
                for (AsignadorRecursos.Asignacion a : AsignadorRecursos.asignar(
                        requerido.getOrDefault(recursoId, Integer.MAX_VALUE), candidatos, libre)) {
                    nuevas.add(bloquear(request.emergenciaId(), request.loteId(), a.ofertaId(),
                            ongs.get(a.ongCodigo()), recurso, a.cantidad(), ahora));
                }
            }
        }
        log.info("Validación del lote. lote={} emergencia={} ofertas={} bloqueos={}", request.loteId(),
                request.emergenciaId(), ofertas.size(), nuevas.size());
        return armarValidacion(request, ofertas, nuevas, ahora);
    }

    // Sin categoría el recurso no distingue ONGs (todas con la misma prioridad); con categoría, 0 = no habilitada
    private int prioridad(Recurso recurso, Map<CategoriaRecurso, NivelCompetencia> nivelesOng) {
        if (recurso.getCategoria() == null) {
            return NivelCompetencia.ESPECIALISTA.getPrioridad();
        }
        NivelCompetencia nivel = nivelesOng == null ? null : nivelesOng.get(recurso.getCategoria());
        return nivel == null ? 0 : nivel.getPrioridad();
    }

    private Map<String, Map<CategoriaRecurso, NivelCompetencia>> nivelesDe(Set<String> ongCodigos) {
        Map<String, Map<CategoriaRecurso, NivelCompetencia>> niveles = new HashMap<>();
        for (OngCategoriaNivel fila : nivelRepository.findByOngCodigoIn(ongCodigos)) {
            niveles.computeIfAbsent(fila.getOng().getCodigo(), codigo -> new HashMap<>())
                    .put(fila.getCategoria(), fila.getNivel());
        }
        return niveles;
    }

    private int libre(Ong ong, Recurso recurso, InventarioOng fila, LocalDateTime ahora) {
        long stock = fila == null ? 0 : fila.getCantidadDisponible();
        long libre = stock - reservaRepository.sumarActivas(ong.getId(), recurso.getId(), ahora);
        return (int) Math.max(libre, 0);
    }

    private ValidacionResponse armarValidacion(ValidacionRequest request, List<Oferta> ofertas,
                                               List<Reserva> reservas, LocalDateTime ahora) {
        List<ValidacionResponse.ResultadoOferta> resultados = ofertas.stream().map(oferta -> {
            List<ItemReservaResponse> items = reservas.stream()
                    .filter(r -> r.getOfertaId().equals(oferta.getOfertaId()) && activa(r, ahora))
                    .sorted(Comparator.comparing(Reserva::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(r -> itemRespuesta(r, ahora))
                    .toList();
            return new ValidacionResponse.ResultadoOferta(oferta.getOfertaId(), !items.isEmpty(), items);
        }).toList();
        return new ValidacionResponse(request.emergenciaId(), request.loteId(),
                resultados.stream().anyMatch(ValidacionResponse.ResultadoOferta::aprobada), resultados);
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
            bloqueadas.forEach(r -> pares.add(new Par(r.getOng().getCodigo(), r.getRecurso().getId())));
            Map<Par, InventarioOng> inventario = bloquearInventario(pares);

            for (Reserva reserva : bloqueadas) {
                // Un bloqueo vencido ya no tenía tomado el stock: hay que comprobar que siga habiendo
                if (vencida(reserva, ahora)) {
                    Par par = new Par(reserva.getOng().getCodigo(), reserva.getRecurso().getId());
                    verificarDisponibilidad(Map.of(par, reserva.getCantidad()), inventario,
                            Map.of(par.ongCodigo(), reserva.getOng()), Map.of(par.recursoId(), reserva.getRecurso()),
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
                .filter(r -> request.ongCodigo() == null || r.getOng().getCodigo().equals(request.ongCodigo()))
                .toList();
        if (reservas.isEmpty()) {
            throw new RecursoNoEncontradoException("No hay reservas de la oferta " + request.ofertaId()
                    + (request.ongCodigo() == null ? "" : " para la ONG " + request.ongCodigo()));
        }

        // Las ya liberadas y los bloqueos vencidos se dejan como están, así repetir la liberación no cambia nada
        reservas.stream()
                .filter(r -> activa(r, ahora))
                .forEach(r -> liberar(r, ahora));
        log.info("Liberación. oferta={} ong={} items={}", request.ofertaId(), request.ongCodigo(), reservas.size());
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
        sumarOfrecido(oferta, ofrecido, recursos, null);
        if (ofrecido.isEmpty()) {
            throw new ConflictoException("La oferta no tiene aportes de ninguna ONG para reservar");
        }
    }

    // Igual que cargarOfrecido pero sin exigir aportes; si se pasa requerido, acumula lo que pide el lote de cada recurso
    private void sumarOfrecido(Oferta oferta, Map<Par, Integer> ofrecido, Map<Long, Recurso> recursos,
                               Map<Long, Integer> requerido) {
        Map<String, Recurso> porNombre = new HashMap<>();
        for (OfertaAporte aporte : oferta.getAportes()) {
            Recurso recurso = porNombre.computeIfAbsent(aporte.getRecursoNombre(), nombre ->
                    recursoRepository.findByNombre(nombre).orElseThrow(() -> new ConflictoException(
                            "El recurso '" + nombre + "' de la oferta no está en el catálogo")));
            recursos.put(recurso.getId(), recurso);
            if (requerido != null) {
                // Sin cantidad requerida el lote no pone tope
                requerido.merge(recurso.getId(), aporte.getCantidadRequerida() == null
                        ? Integer.MAX_VALUE : aporte.getCantidadRequerida(), Math::max);
            }
            aporte.getPorOng().stream()
                    .filter(porOng -> porOng.getCantidadOfrecida() != null && porOng.getCantidadOfrecida() > 0)
                    .forEach(porOng -> ofrecido.merge(new Par(porOng.getOngCodigo(), recurso.getId()),
                            porOng.getCantidadOfrecida(), Integer::sum));
        }
    }

    private Reserva bloquear(Long emergenciaId, Long loteId, Long ofertaId, Ong ong, Recurso recurso,
                             int cantidad, LocalDateTime ahora) {
        Reserva reserva = new Reserva();
        reserva.setOng(ong);
        reserva.setRecurso(recurso);
        reserva.setCantidad(cantidad);
        reserva.setEstado(EstadoReserva.BLOQUEADA);
        reserva.setEmergenciaId(emergenciaId);
        reserva.setLoteId(loteId);
        reserva.setOfertaId(ofertaId);
        reserva.setFechaCreacion(ahora);
        reserva.setExpiraEn(ahora.plusHours(bloqueoHoras));
        return reservaRepository.save(reserva);
    }

    // Todo lo ofrecido, o solo los ítems pedidos (que deben estar en la oferta y no superarla)
    private Map<Par, Integer> elegirPedido(ReservaTemporalRequest request, Map<Par, Integer> ofrecido) {
        if (request.items() == null || request.items().isEmpty()) {
            return ofrecido;
        }
        Map<Par, Integer> pedido = new LinkedHashMap<>();
        for (ReservaTemporalRequest.ItemReserva item : request.items()) {
            Par par = new Par(item.ongCodigo(), item.recursoId());
            Integer maximo = ofrecido.get(par);
            if (maximo == null) {
                throw new SolicitudInvalidaException("La oferta no incluye el recurso " + item.recursoId()
                        + " de la ONG " + item.ongCodigo());
            }
            if (item.cantidad() > maximo) {
                throw new SolicitudInvalidaException("La ONG " + item.ongCodigo() + " ofrece " + maximo
                        + " del recurso " + item.recursoId() + " y se pidieron " + item.cantidad());
            }
            if (pedido.put(par, item.cantidad()) != null) {
                throw new SolicitudInvalidaException("El recurso " + item.recursoId() + " de la ONG "
                        + item.ongCodigo() + " está repetido en los ítems");
            }
        }
        return pedido;
    }

    private Map<String, Ong> buscarOngs(Set<Par> pares) {
        Set<String> codigos = new HashSet<>();
        pares.forEach(par -> codigos.add(par.ongCodigo()));
        Map<String, Ong> ongs = new HashMap<>();
        ongRepository.findAllByCodigoIn(codigos).forEach(ong -> ongs.put(ong.getCodigo(), ong));
        codigos.stream().filter(codigo -> !ongs.containsKey(codigo)).findFirst().ifPresent(codigo -> {
            throw new ConflictoException("La ONG con código " + codigo
                    + " de la oferta no está registrada en el Sistema Nacional");
        });
        return ongs;
    }

    // SELECT ... FOR UPDATE de las filas de stock de estos pares; un par sin fila queda fuera (stock 0)
    private Map<Par, InventarioOng> bloquearInventario(Set<Par> pares) {
        Set<String> ongCodigos = new HashSet<>();
        Set<Long> recursoIds = new HashSet<>();
        pares.forEach(par -> {
            ongCodigos.add(par.ongCodigo());
            recursoIds.add(par.recursoId());
        });
        Map<Par, InventarioOng> inventario = new HashMap<>();
        for (InventarioOng fila : inventarioOngRepository.bloquearPorOngCodigoYRecurso(ongCodigos, recursoIds)) {
            inventario.put(new Par(fila.getOng().getCodigo(), fila.getRecurso().getId()), fila);
        }
        return inventario;
    }

    // Todo o nada: si algún recurso no alcanza se informan todos los faltantes y no se reserva nada
    private void verificarDisponibilidad(Map<Par, Integer> pedido, Map<Par, InventarioOng> inventario,
                                         Map<String, Ong> ongs, Map<Long, Recurso> recursos, LocalDateTime ahora) {
        List<String> faltantes = new ArrayList<>();
        pedido.forEach((par, cantidad) -> {
            InventarioOng fila = inventario.get(par);
            long stock = fila == null ? 0 : fila.getCantidadDisponible();
            long libre = stock - reservaRepository.sumarActivas(ongs.get(par.ongCodigo()).getId(), par.recursoId(), ahora);
            if (cantidad > libre) {
                faltantes.add(ongs.get(par.ongCodigo()).getRazonSocial() + " - "
                        + recursos.get(par.recursoId()).getNombre() + ": se piden " + cantidad
                        + " y hay " + Math.max(libre, 0) + " libres");
            }
        });
        if (!faltantes.isEmpty()) {
            throw new ConflictoException("Recursos insuficientes: " + String.join("; ", faltantes));
        }
    }

    // Reserva parcial: de cada par se toma lo que haya libre, hasta lo pedido, y se omiten los que no tienen nada.
    // Si no queda ningún par con libre se informa igual que en el todo o nada (409 con los faltantes).
    private Map<Par, Integer> ajustarAlLibre(Map<Par, Integer> pedido, Map<Par, InventarioOng> inventario,
                                             Map<String, Ong> ongs, Map<Long, Recurso> recursos,
                                             LocalDateTime ahora) {
        Map<Par, Integer> ajustado = new LinkedHashMap<>();
        pedido.forEach((par, cantidad) -> {
            InventarioOng fila = inventario.get(par);
            long stock = fila == null ? 0 : fila.getCantidadDisponible();
            long libre = stock - reservaRepository.sumarActivas(ongs.get(par.ongCodigo()).getId(), par.recursoId(), ahora);
            int reservable = (int) Math.min(cantidad, Math.max(libre, 0));
            if (reservable > 0) {
                ajustado.put(par, reservable);
            }
        });
        if (ajustado.isEmpty()) {
            verificarDisponibilidad(pedido, inventario, ongs, recursos, ahora);
        }
        return ajustado;
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
                .map(r -> itemRespuesta(r, ahora))
                .toList();

        String estado = resumir(items);
        LocalDateTime expiraEn = items.stream()
                .filter(i -> "BLOQUEADA".equals(i.estado()))
                .map(ItemReservaResponse::expiraEn)
                .max(Comparator.naturalOrder())
                .orElse(null);
        return new ReservaResponse(emergenciaId, loteId, ofertaId, estado, expiraEn, items);
    }

    private ItemReservaResponse itemRespuesta(Reserva r, LocalDateTime ahora) {
        return new ItemReservaResponse(
                r.getOng().getCodigo(), r.getOng().getRazonSocial(),
                r.getRecurso().getId(), r.getRecurso().getNombre(), r.getRecurso().getUnidadMedida(),
                r.getCantidad(), estadoVisible(r, ahora), r.getExpiraEn());
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
