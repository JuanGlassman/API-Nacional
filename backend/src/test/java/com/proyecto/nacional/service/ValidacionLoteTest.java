package com.proyecto.nacional.service;

import com.proyecto.nacional.dto.reservas.ValidacionRequest;
import com.proyecto.nacional.dto.reservas.ValidacionResponse;
import com.proyecto.nacional.exception.ConflictoException;
import com.proyecto.nacional.exception.RecursoNoEncontradoException;
import com.proyecto.nacional.model.CategoriaRecurso;
import com.proyecto.nacional.model.EstadoReserva;
import com.proyecto.nacional.model.InventarioOng;
import com.proyecto.nacional.model.NivelCompetencia;
import com.proyecto.nacional.model.Oferta;
import com.proyecto.nacional.model.OfertaAporte;
import com.proyecto.nacional.model.OfertaAporteOng;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Validación de un lote: reparto por prioridad de ONG y categoría, tope del lote y stock libre
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ValidacionLoteTest {

    private static final Instant INSTANTE = Instant.parse("2026-10-19T12:00:00Z");
    private static final LocalDateTime AHORA = LocalDateTime.ofInstant(INSTANTE, ZoneOffset.UTC);
    private static final ValidacionRequest PEDIDO = new ValidacionRequest(1L, 1L);

    @Mock
    private OfertaRepository ofertaRepository;
    @Mock
    private OngRepository ongRepository;
    @Mock
    private RecursoRepository recursoRepository;
    @Mock
    private InventarioOngRepository inventarioOngRepository;
    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private OngCategoriaNivelRepository nivelRepository;

    private ReservaService service;
    private Ong cruzRoja;
    private Ong bomberos;
    private Recurso paramedicos;

    @BeforeEach
    void setUp() {
        service = new ReservaService(ofertaRepository, ongRepository, recursoRepository, inventarioOngRepository,
                reservaRepository, nivelRepository, Clock.fixed(INSTANTE, ZoneOffset.UTC), 24);

        cruzRoja = ong(1L, "cod-cruzroja", "Cruz Roja");
        bomberos = ong(3L, "cod-bomberos", "Bomberos");
        paramedicos = new Recurso();
        paramedicos.setId(2L);
        paramedicos.setNombre("Paramédicos");
        paramedicos.setUnidadMedida("personas");
        paramedicos.setCategoria(CategoriaRecurso.SALUD);

        when(recursoRepository.findByNombre("Paramédicos")).thenReturn(Optional.of(paramedicos));
        when(ongRepository.findAllByCodigoIn(anyCollection())).thenReturn(List.of(cruzRoja, bomberos));
        when(inventarioOngRepository.bloquearPorOngCodigoYRecurso(anyCollection(), anyCollection()))
                .thenReturn(List.of(stock(cruzRoja, 10), stock(bomberos, 6)));
        when(reservaRepository.sumarActivas(anyLong(), anyLong(), any())).thenReturn(0L);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(i -> i.getArgument(0));
        when(reservaRepository.findByEmergenciaIdAndLoteIdOrderByIdAsc(1L, 1L)).thenReturn(List.of());
        // Cruz Roja es especialista en salud y Bomberos de apoyo
        when(nivelRepository.findByOngCodigoIn(anyCollection())).thenReturn(List.of(
                nivel(cruzRoja, NivelCompetencia.ESPECIALISTA), nivel(bomberos, NivelCompetencia.APOYO)));
    }

    @Test
    void elEspecialistaSeAsignaAntesQueElApoyoYSeCortaEnLoQuePideElLote() {
        // El lote pide 6. Bomberos (apoyo) ofreció primero 4 y Cruz Roja (especialista) después 4
        ofertas(oferta(1L, 0, 6, "cod-bomberos", 4), oferta(2L, 30, 6, "cod-cruzroja", 4));

        ValidacionResponse respuesta = service.validar(PEDIDO);

        assertThat(respuesta.hayOfertasHabilitadas()).isTrue();
        assertThat(respuesta.resultados()).hasSize(2);
        var deBomberos = respuesta.resultados().get(0);
        var deCruzRoja = respuesta.resultados().get(1);
        assertThat(deCruzRoja.aprobada()).isTrue();
        assertThat(deCruzRoja.items()).extracting(i -> i.cantidad()).containsExactly(4);
        assertThat(deBomberos.aprobada()).isTrue();
        assertThat(deBomberos.items()).extracting(i -> i.cantidad()).containsExactly(2);
        assertThat(deBomberos.items().get(0).estado()).isEqualTo("BLOQUEADA");
    }

    @Test
    void unaOfertaSinRecursosAsignadosVuelveSinAprobar() {
        // El lote pide 4 y la especialista los cubre toda: la oferta de apoyo queda afuera
        ofertas(oferta(1L, 0, 4, "cod-bomberos", 4), oferta(2L, 30, 4, "cod-cruzroja", 4));

        ValidacionResponse respuesta = service.validar(PEDIDO);

        assertThat(respuesta.hayOfertasHabilitadas()).isTrue();
        assertThat(respuesta.resultados().get(0).aprobada()).isFalse();
        assertThat(respuesta.resultados().get(0).items()).isEmpty();
        assertThat(respuesta.resultados().get(1).aprobada()).isTrue();
    }

    @Test
    void unaOngSinNivelEnLaCategoriaNoRecibeNada() {
        when(nivelRepository.findByOngCodigoIn(anyCollection()))
                .thenReturn(List.of(nivel(cruzRoja, NivelCompetencia.ESPECIALISTA)));
        ofertas(oferta(1L, 0, 6, "cod-bomberos", 4));

        ValidacionResponse respuesta = service.validar(PEDIDO);

        assertThat(respuesta.hayOfertasHabilitadas()).isFalse();
        assertThat(respuesta.resultados().get(0).aprobada()).isFalse();
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void noSeReservaMasDelStockLibre() {
        // Cruz Roja tiene 10 y 8 ya tomados por otro lote: libres 2
        when(reservaRepository.sumarActivas(1L, 2L, AHORA)).thenReturn(8L);
        ofertas(oferta(1L, 0, 6, "cod-cruzroja", 5));

        ValidacionResponse respuesta = service.validar(PEDIDO);

        assertThat(respuesta.resultados().get(0).items()).extracting(i -> i.cantidad()).containsExactly(2);
    }

    @Test
    void guardaLasReservasBloqueadasConVencimiento() {
        ofertas(oferta(2L, 30, 6, "cod-cruzroja", 4));

        service.validar(PEDIDO);

        ArgumentCaptor<Reserva> guardada = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository, times(1)).save(guardada.capture());
        assertThat(guardada.getValue().getEstado()).isEqualTo(EstadoReserva.BLOQUEADA);
        assertThat(guardada.getValue().getOfertaId()).isEqualTo(2L);
        assertThat(guardada.getValue().getCantidad()).isEqualTo(4);
        assertThat(guardada.getValue().getExpiraEn()).isEqualTo(AHORA.plusHours(24));
    }

    @Test
    void repetirConBloqueosVigentesDevuelveLosMismosSinRecalcular() {
        ofertas(oferta(1L, 0, 6, "cod-bomberos", 4), oferta(2L, 30, 6, "cod-cruzroja", 4));
        Reserva vigente = reserva(2L, cruzRoja, 4, EstadoReserva.BLOQUEADA, AHORA.plusHours(5));
        when(reservaRepository.findByEmergenciaIdAndLoteIdOrderByIdAsc(1L, 1L)).thenReturn(List.of(vigente));

        ValidacionResponse respuesta = service.validar(PEDIDO);

        assertThat(respuesta.resultados().get(0).aprobada()).isFalse();
        assertThat(respuesta.resultados().get(1).aprobada()).isTrue();
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void siHayRecursosComprometidosResponde409() {
        ofertas(oferta(2L, 30, 6, "cod-cruzroja", 4));
        when(reservaRepository.findByEmergenciaIdAndLoteIdOrderByIdAsc(1L, 1L))
                .thenReturn(List.of(reserva(2L, cruzRoja, 4, EstadoReserva.COMPROMETIDA, null)));

        assertThatThrownBy(() -> service.validar(PEDIDO)).isInstanceOf(ConflictoException.class);
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void sinOfertasGuardadasResponde404() {
        when(ofertaRepository.bloquearPorLote(1L, 1L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.validar(PEDIDO)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void losBloqueosVencidosSeLiberanYSeRecalcula() {
        ofertas(oferta(2L, 30, 6, "cod-cruzroja", 4));
        Reserva vencida = reserva(7L, cruzRoja, 4, EstadoReserva.BLOQUEADA, AHORA.minusHours(1));
        when(reservaRepository.findByEmergenciaIdAndLoteIdOrderByIdAsc(1L, 1L)).thenReturn(List.of(vencida));

        ValidacionResponse respuesta = service.validar(PEDIDO);

        assertThat(vencida.getEstado()).isEqualTo(EstadoReserva.LIBERADA);
        assertThat(respuesta.resultados().get(0).aprobada()).isTrue();
    }

    // ---------- Helpers ----------

    private void ofertas(Oferta... ofertas) {
        when(ofertaRepository.bloquearPorLote(1L, 1L)).thenReturn(List.of(ofertas));
    }

    private Ong ong(Long id, String codigo, String razonSocial) {
        Ong ong = new Ong();
        ong.setId(id);
        ong.setCodigo(codigo);
        ong.setRazonSocial(razonSocial);
        return ong;
    }

    private InventarioOng stock(Ong ong, int cantidad) {
        InventarioOng inventario = new InventarioOng();
        inventario.setOng(ong);
        inventario.setRecurso(paramedicos);
        inventario.setCantidadDisponible(cantidad);
        return inventario;
    }

    private OngCategoriaNivel nivel(Ong ong, NivelCompetencia nivel) {
        OngCategoriaNivel fila = new OngCategoriaNivel();
        fila.setOng(ong);
        fila.setCategoria(CategoriaRecurso.SALUD);
        fila.setNivel(nivel);
        return fila;
    }

    private Reserva reserva(Long id, Ong ong, int cantidad, EstadoReserva estado, LocalDateTime expiraEn) {
        Reserva reserva = new Reserva();
        reserva.setId(id);
        reserva.setOng(ong);
        reserva.setRecurso(paramedicos);
        reserva.setCantidad(cantidad);
        reserva.setEstado(estado);
        reserva.setEmergenciaId(1L);
        reserva.setLoteId(1L);
        reserva.setOfertaId(id);
        reserva.setExpiraEn(expiraEn);
        return reserva;
    }

    // Oferta de un aporte de paramédicos de una sola ONG; el lote pide `requerido`
    private Oferta oferta(Long ofertaId, int minutos, int requerido, String ongCodigo, int ofrecido) {
        OfertaAporteOng porOng = new OfertaAporteOng();
        porOng.setOngCodigo(ongCodigo);
        porOng.setCantidadOfrecida(ofrecido);

        OfertaAporte aporte = new OfertaAporte();
        aporte.setRecursoNombre("Paramédicos");
        aporte.setCantidadRequerida(requerido);
        aporte.setPorOng(new ArrayList<>(List.of(porOng)));

        Oferta oferta = new Oferta();
        oferta.setOfertaId(ofertaId);
        oferta.setEmergenciaId(1L);
        oferta.setLoteId(1L);
        oferta.setFechaOferta(AHORA.minusDays(1).plusMinutes(minutos));
        oferta.setAportes(new ArrayList<>(List.of(aporte)));
        return oferta;
    }
}
