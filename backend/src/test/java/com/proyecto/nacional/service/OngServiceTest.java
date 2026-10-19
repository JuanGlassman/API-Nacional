package com.proyecto.nacional.service;

import com.proyecto.nacional.dto.ongs.InventarioItemRequest;
import com.proyecto.nacional.dto.ongs.InventarioRequest;
import com.proyecto.nacional.dto.ongs.OngRequest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OngServiceTest {

    @Mock
    private OngRepository ongRepository;
    @Mock
    private RecursoRepository recursoRepository;
    @Mock
    private InventarioOngRepository inventarioOngRepository;
    @Mock
    private CuentaRepository cuentaRepository;
    @Mock
    private ReservaRepository reservaRepository;
    @Spy
    private Clock clock = Clock.fixed(Instant.parse("2026-10-19T12:00:00Z"), ZoneOffset.UTC);
    @Spy
    private OngMapper ongMapper = new OngMapper();
    @InjectMocks
    private OngService ongService;

    private Ong ong1;
    private Recurso frazadas;

    @BeforeEach
    void setUp() {
        ong1 = ong(1L, "Cruz Roja", "/ONG/CruzRoja");
        frazadas = new Recurso();
        frazadas.setId(3L);
        frazadas.setNombre("Frazadas");
        frazadas.setUnidadMedida("unidades");
    }

    @Test
    void crearConRazonSocialRepetidaDevuelveConflicto() {
        when(ongRepository.existsByRazonSocial("Cruz Roja")).thenReturn(true);

        assertThatThrownBy(() -> ongService.crear(new OngRequest("Cruz Roja", "/ONG/Otra")))
                .isInstanceOf(ConflictoException.class);
        verify(ongRepository, never()).save(any());
    }

    @Test
    void ongEditaSuPropiaRazonSocial() {
        when(cuentaRepository.findOngIdByUsername("cruzroja")).thenReturn(Optional.of(1L));
        when(ongRepository.findById(1L)).thenReturn(Optional.of(ong1));

        var respuesta = ongService.actualizar(1L, new OngRequest("Cruz Roja Nueva", "/ONG/CruzRoja"),
                auth("cruzroja", "ROLE_ONG"));

        assertThat(respuesta.razonSocial()).isEqualTo("Cruz Roja Nueva");
    }

    @Test
    void ongNoPuedeEditarOtraOng() {
        when(cuentaRepository.findOngIdByUsername("caritas")).thenReturn(Optional.of(2L));

        assertThatThrownBy(() -> ongService.actualizar(1L, new OngRequest("X", "/ONG/CruzRoja"),
                auth("caritas", "ROLE_ONG")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void cuentaOngSinOngAsociadaNoPuedeEditar() {
        when(cuentaRepository.findOngIdByUsername("suelta")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ongService.guardarInventario(1L,
                new InventarioRequest(List.of(new InventarioItemRequest(3L, 5))), auth("suelta", "ROLE_ONG")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void ongNoPuedeCambiarElGrupoDeBonita() {
        when(cuentaRepository.findOngIdByUsername("cruzroja")).thenReturn(Optional.of(1L));
        when(ongRepository.findById(1L)).thenReturn(Optional.of(ong1));

        assertThatThrownBy(() -> ongService.actualizar(1L, new OngRequest("Cruz Roja", "/ONG/Otro"),
                auth("cruzroja", "ROLE_ONG")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void adminEditaCualquierOngIncluidoElGrupoDeBonita() {
        when(ongRepository.findById(1L)).thenReturn(Optional.of(ong1));

        var respuesta = ongService.actualizar(1L, new OngRequest("Cruz Roja", "/ONG/Otro"),
                auth("admin", "ROLE_ADMIN"));

        assertThat(respuesta.bonitaGroupPath()).isEqualTo("/ONG/Otro");
    }

    @Test
    void guardarInventarioActualizaItemExistenteYCreaElNuevo() {
        when(ongRepository.findById(1L)).thenReturn(Optional.of(ong1));
        when(recursoRepository.findById(3L)).thenReturn(Optional.of(frazadas));
        InventarioOng existente = new InventarioOng();
        existente.setOng(ong1);
        existente.setRecurso(frazadas);
        existente.setCantidadDisponible(200);
        when(inventarioOngRepository.bloquearPorOngYRecurso(1L, 3L)).thenReturn(Optional.of(existente));
        when(inventarioOngRepository.findByOngId(1L)).thenReturn(List.of(existente));

        var respuesta = ongService.guardarInventario(1L,
                new InventarioRequest(List.of(new InventarioItemRequest(3L, 250))), auth("admin", "ROLE_ADMIN"));

        assertThat(existente.getCantidadDisponible()).isEqualTo(250);
        assertThat(respuesta.recursos()).hasSize(1);
        verify(inventarioOngRepository).saveAndFlush(existente);
    }

    @Test
    void guardarInventarioConRecursoInexistenteDevuelve404() {
        when(ongRepository.findById(1L)).thenReturn(Optional.of(ong1));
        when(recursoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ongService.guardarInventario(1L,
                new InventarioRequest(List.of(new InventarioItemRequest(99L, 1))), auth("admin", "ROLE_ADMIN")))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void eliminarOngConCuentasAsociadasDevuelveConflicto() {
        when(ongRepository.findById(1L)).thenReturn(Optional.of(ong1));
        when(cuentaRepository.existsByOngId(1L)).thenReturn(true);

        assertThatThrownBy(() -> ongService.eliminar(1L)).isInstanceOf(ConflictoException.class);
        verify(ongRepository, never()).delete(any());
    }

    @Test
    void eliminarOngBorraPrimeroSuInventario() {
        when(ongRepository.findById(1L)).thenReturn(Optional.of(ong1));
        when(cuentaRepository.existsByOngId(1L)).thenReturn(false);

        ongService.eliminar(1L);

        verify(inventarioOngRepository).deleteByOngId(1L);
        verify(ongRepository).delete(ong1);
    }

    @Test
    void eliminarItemInexistenteDevuelve404() {
        when(ongRepository.findById(1L)).thenReturn(Optional.of(ong1));
        when(inventarioOngRepository.findByOngIdAndRecursoId(1L, 3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ongService.eliminarItemInventario(1L, 3L, auth("admin", "ROLE_ADMIN")))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void noSePuedeBajarElStockPorDebajoDeLoReservado() {
        when(ongRepository.findById(1L)).thenReturn(Optional.of(ong1));
        when(recursoRepository.findById(3L)).thenReturn(Optional.of(frazadas));
        InventarioOng existente = new InventarioOng();
        existente.setOng(ong1);
        existente.setRecurso(frazadas);
        existente.setCantidadDisponible(200);
        when(inventarioOngRepository.bloquearPorOngYRecurso(1L, 3L)).thenReturn(Optional.of(existente));
        when(reservaRepository.sumarActivas(eq(1L), eq(3L), any())).thenReturn(120L);

        assertThatThrownBy(() -> ongService.guardarInventario(1L,
                new InventarioRequest(List.of(new InventarioItemRequest(3L, 100))), auth("admin", "ROLE_ADMIN")))
                .isInstanceOf(ConflictoException.class);
        assertThat(existente.getCantidadDisponible()).isEqualTo(200);
    }

    @Test
    void noSeQuitaDelInventarioUnRecursoConCantidadesReservadas() {
        when(ongRepository.findById(1L)).thenReturn(Optional.of(ong1));
        InventarioOng existente = new InventarioOng();
        when(inventarioOngRepository.findByOngIdAndRecursoId(1L, 3L)).thenReturn(Optional.of(existente));
        when(reservaRepository.sumarActivas(eq(1L), eq(3L), any())).thenReturn(10L);

        assertThatThrownBy(() -> ongService.eliminarItemInventario(1L, 3L, auth("admin", "ROLE_ADMIN")))
                .isInstanceOf(ConflictoException.class);
        verify(inventarioOngRepository, never()).delete(any());
    }

    @Test
    void noSeEliminaUnaOngConRecursosReservados() {
        when(ongRepository.findById(1L)).thenReturn(Optional.of(ong1));
        when(cuentaRepository.existsByOngId(1L)).thenReturn(false);
        when(reservaRepository.existenActivasDeOng(eq(1L), any())).thenReturn(true);

        assertThatThrownBy(() -> ongService.eliminar(1L)).isInstanceOf(ConflictoException.class);
        verify(ongRepository, never()).delete(any());
    }

    private static Ong ong(Long id, String razonSocial, String path) {
        Ong ong = new Ong();
        ong.setId(id);
        ong.setRazonSocial(razonSocial);
        ong.setBonitaGroupPath(path);
        return ong;
    }

    private static Authentication auth(String username, String rol) {
        return new UsernamePasswordAuthenticationToken(username, null, List.of(new SimpleGrantedAuthority(rol)));
    }
}
