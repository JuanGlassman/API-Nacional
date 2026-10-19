package com.proyecto.nacional.service;

import com.proyecto.nacional.dto.ongs.InventarioItemRequest;
import com.proyecto.nacional.dto.ongs.InventarioOngResponse;
import com.proyecto.nacional.dto.ongs.InventarioRequest;
import com.proyecto.nacional.dto.ongs.OngRequest;
import com.proyecto.nacional.dto.ongs.OngResponse;
import com.proyecto.nacional.dto.ongs.RecursoResponse;
import com.proyecto.nacional.exception.ConflictoException;
import com.proyecto.nacional.exception.RecursoNoEncontradoException;
import com.proyecto.nacional.mapper.OngMapper;
import com.proyecto.nacional.model.InventarioOng;
import com.proyecto.nacional.model.Ong;
import com.proyecto.nacional.model.Recurso;
import com.proyecto.nacional.repository.CuentaRepository;
import com.proyecto.nacional.repository.InventarioOngRepository;
import com.proyecto.nacional.repository.OngRepository;
import com.proyecto.nacional.repository.RecursoRepository;
import com.proyecto.nacional.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class OngService {

    private static final String ROL_ADMIN = "ROLE_ADMIN";

    private final OngRepository ongRepository;
    private final RecursoRepository recursoRepository;
    private final InventarioOngRepository inventarioOngRepository;
    private final CuentaRepository cuentaRepository;
    private final ReservaRepository reservaRepository;
    private final Clock clock;
    private final OngMapper ongMapper;

    // ---------- ONGs ----------

    public List<OngResponse> listar() {
        return ongRepository.findAllByOrderByRazonSocialAsc().stream()
                .map(ongMapper::toResponse)
                .toList();
    }

    public OngResponse obtener(Long id) {
        return ongMapper.toResponse(buscarOng(id));
    }

    @Transactional
    public OngResponse crear(OngRequest request) {
        if (ongRepository.existsByRazonSocial(request.razonSocial())) {
            throw new ConflictoException("Ya existe una ONG con la razón social " + request.razonSocial());
        }
        if (ongRepository.existsByBonitaGroupPath(request.bonitaGroupPath())) {
            throw new ConflictoException("Ya existe una ONG con el grupo de Bonita " + request.bonitaGroupPath());
        }
        Ong ong = new Ong();
        ong.setRazonSocial(request.razonSocial());
        ong.setBonitaGroupPath(request.bonitaGroupPath());
        Ong guardada = ongRepository.save(ong);
        log.info("ONG creada. id={} razonSocial={}", guardada.getId(), guardada.getRazonSocial());
        return ongMapper.toResponse(guardada);
    }

    @Transactional
    public OngResponse actualizar(Long id, OngRequest request, Authentication auth) {
        verificarPermiso(id, auth);
        Ong ong = buscarOng(id);

        // El grupo de Bonita vincula a la ONG con sus usuarios: solo lo cambia el administrador
        if (!esAdmin(auth) && !ong.getBonitaGroupPath().equals(request.bonitaGroupPath())) {
            throw new AccessDeniedException("Solo un administrador puede cambiar el grupo de Bonita");
        }
        if (ongRepository.existsByRazonSocialAndIdNot(request.razonSocial(), id)) {
            throw new ConflictoException("Ya existe una ONG con la razón social " + request.razonSocial());
        }
        if (ongRepository.existsByBonitaGroupPathAndIdNot(request.bonitaGroupPath(), id)) {
            throw new ConflictoException("Ya existe una ONG con el grupo de Bonita " + request.bonitaGroupPath());
        }
        ong.setRazonSocial(request.razonSocial());
        ong.setBonitaGroupPath(request.bonitaGroupPath());
        log.info("ONG actualizada. id={}", id);
        return ongMapper.toResponse(ong);
    }

    @Transactional
    public void eliminar(Long id) {
        Ong ong = buscarOng(id);
        if (cuentaRepository.existsByOngId(id)) {
            throw new ConflictoException("La ONG tiene cuentas asociadas; deshabilítelas o desvincúlelas antes");
        }
        if (reservaRepository.existenActivasDeOng(id, LocalDateTime.now(clock))) {
            throw new ConflictoException("La ONG tiene recursos reservados; libérelos antes de darla de baja");
        }
        // Lo que queda son reservas liberadas o vencidas: solo historial
        reservaRepository.deleteByOngId(id);
        inventarioOngRepository.deleteByOngId(id);
        ongRepository.delete(ong);
        log.info("ONG eliminada junto con su inventario. id={}", id);
    }

    // ---------- Recursos e inventario ----------

    public List<RecursoResponse> listarRecursos() {
        return recursoRepository.findAllByOrderByIdAsc().stream()
                .map(ongMapper::toResponse)
                .toList();
    }

    public InventarioOngResponse inventario(Long ongId) {
        Ong ong = buscarOng(ongId);
        return ongMapper.toInventarioResponse(ong, inventarioOngRepository.findByOngId(ongId));
    }

    public List<InventarioOngResponse> inventarioPorOng(Set<Long> ongIds) {
        List<Ong> ongs = ongRepository.findAllById(ongIds);
        if (ongs.size() != ongIds.size()) {
            log.warn("Inventario solicitado para ONGs inexistentes. ongIds={}", ongIds);
            throw new RecursoNoEncontradoException("Alguna de las ONGs solicitadas no existe");
        }

        Map<Long, List<InventarioOng>> inventarioPorOngId = new LinkedHashMap<>();
        for (InventarioOng item : inventarioOngRepository.findByOngIdIn(ongIds)) {
            inventarioPorOngId
                    .computeIfAbsent(item.getOng().getId(), id -> new ArrayList<>())
                    .add(item);
        }

        return ongs.stream()
                .sorted((a, b) -> a.getRazonSocial().compareToIgnoreCase(b.getRazonSocial()))
                .map(ong -> ongMapper.toInventarioResponse(
                        ong, inventarioPorOngId.getOrDefault(ong.getId(), List.of())))
                .toList();
    }

    // Alta o modificación: cada ítem crea la fila del recurso en el inventario o pisa su cantidad
    @Transactional
    public InventarioOngResponse guardarInventario(Long ongId, InventarioRequest request, Authentication auth) {
        verificarPermiso(ongId, auth);
        Ong ong = buscarOng(ongId);

        LocalDateTime ahora = LocalDateTime.now(clock);
        for (InventarioItemRequest item : request.recursos()) {
            Recurso recurso = recursoRepository.findById(item.recursoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "No existe el recurso con id " + item.recursoId()));
            // Con lock: una reserva en curso sobre esta fila espera a que termine este cambio (y al revés)
            InventarioOng inventario = inventarioOngRepository
                    .bloquearPorOngYRecurso(ongId, recurso.getId())
                    .orElseGet(() -> {
                        InventarioOng nuevo = new InventarioOng();
                        nuevo.setOng(ong);
                        nuevo.setRecurso(recurso);
                        return nuevo;
                    });
            long reservado = reservaRepository.sumarActivas(ongId, recurso.getId(), ahora);
            if (item.cantidadDisponible() < reservado) {
                throw new ConflictoException("No se puede dejar el recurso " + recurso.getNombre() + " en "
                        + item.cantidadDisponible() + ": hay " + reservado + " reservados");
            }
            inventario.setCantidadDisponible(item.cantidadDisponible());
            inventarioOngRepository.saveAndFlush(inventario);
        }
        log.info("Inventario actualizado. ongId={} items={}", ongId, request.recursos().size());
        return ongMapper.toInventarioResponse(ong, inventarioOngRepository.findByOngId(ongId));
    }

    @Transactional
    public void eliminarItemInventario(Long ongId, Long recursoId, Authentication auth) {
        verificarPermiso(ongId, auth);
        buscarOng(ongId);
        InventarioOng inventario = inventarioOngRepository.findByOngIdAndRecursoId(ongId, recursoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "La ONG " + ongId + " no tiene el recurso " + recursoId + " en su inventario"));
        if (reservaRepository.sumarActivas(ongId, recursoId, LocalDateTime.now(clock)) > 0) {
            throw new ConflictoException("El recurso " + recursoId + " de la ONG " + ongId
                    + " tiene cantidades reservadas; libérelas antes de quitarlo del inventario");
        }
        inventarioOngRepository.delete(inventario);
        log.info("Ítem de inventario eliminado. ongId={} recursoId={}", ongId, recursoId);
    }

    // ---------- Helpers ----------

    private Ong buscarOng(Long id) {
        return ongRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la ONG con id " + id));
    }

    private boolean esAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> ROL_ADMIN.equals(a.getAuthority()));
    }

    // El administrador edita cualquier ONG; una cuenta ONG solo la suya
    private void verificarPermiso(Long ongId, Authentication auth) {
        if (esAdmin(auth)) {
            return;
        }
        boolean esSuya = cuentaRepository.findOngIdByUsername(auth.getName())
                .map(ongId::equals)
                .orElse(false);
        if (!esSuya) {
            log.warn("Intento de editar una ONG ajena. usuario={} ongId={}", auth.getName(), ongId);
            throw new AccessDeniedException("La cuenta no pertenece a la ONG " + ongId);
        }
    }
}
