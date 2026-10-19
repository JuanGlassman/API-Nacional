package com.proyecto.nacional.service;

import com.proyecto.nacional.dto.reservas.ReservaLiberacionRequest;
import com.proyecto.nacional.dto.reservas.ReservaRequest;
import com.proyecto.nacional.dto.reservas.ReservaResponse;
import com.proyecto.nacional.dto.reservas.ReservaTemporalRequest;
import com.proyecto.nacional.dto.reservas.ReservaTemporalRequest.ItemReserva;
import com.proyecto.nacional.exception.ConflictoException;
import com.proyecto.nacional.exception.RecursoNoEncontradoException;
import com.proyecto.nacional.exception.SolicitudInvalidaException;
import com.proyecto.nacional.model.EstadoReserva;
import com.proyecto.nacional.model.InventarioOng;
import com.proyecto.nacional.model.Oferta;
import com.proyecto.nacional.model.OfertaAporte;
import com.proyecto.nacional.model.OfertaAporteOng;
import com.proyecto.nacional.model.Ong;
import com.proyecto.nacional.model.Recurso;
import com.proyecto.nacional.model.Reserva;
import com.proyecto.nacional.repository.InventarioOngRepository;
import com.proyecto.nacional.repository.OfertaRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReservaServiceTest {

    private static final Instant INSTANTE = Instant.parse("2026-10-19T12:00:00Z");
    private static final LocalDateTime AHORA = LocalDateTime.ofInstant(INSTANTE, ZoneOffset.UTC);

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

    private ReservaService service;
    private Ong bomberos;
    private Recurso paramedicos;
    private InventarioOng stockBomberos;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(INSTANTE, ZoneOffset.UTC);
        service = new ReservaService(ofertaRepository, ongRepository, recursoRepository,
                inventarioOngRepository, reservaRepository, clock, 24);

        bomberos = new Ong();
        bomberos.setId(3L);
        bomberos.setRazonSocial("Bomberos Voluntarios de La Plata");
        paramedicos = new Recurso();
        paramedicos.setId(2L);
        paramedicos.setNombre("Paramédicos");
        paramedicos.setUnidadMedida("personas");
        stockBomberos = new InventarioOng();
        stockBomberos.setOng(bomberos);
        stockBomberos.setRecurso(paramedicos);
        stockBomberos.setCantidadDisponible(6);

        // Oferta 1 del lote 1 de la emergencia 1: Bomberos ofrece 4 paramédicos
        when(ofertaRepository.findByEmergenciaIdAndLoteIdAndOfertaId(1L, 1L, 1L))
                .thenReturn(Optional.of(oferta(4)));
        when(recursoRepository.findByNombre("Paramédicos")).thenReturn(Optional.of(paramedicos));
        when(ongRepository.findAllById(anyCollection())).thenReturn(List.of(bomberos));
        when(inventarioOngRepository.bloquearPorOngYRecurso(anyCollection(), anyCollection()))
                .thenReturn(List.of(stockBomberos));
        when(reservaRepository.sumarActivas(anyLong(), anyLong(), any())).thenReturn(0L);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(i -> i.getArgument(0));
        when(reservaRepository.findByEmergenciaIdAndLoteIdAndOfertaIdOrderByIdAsc(1L, 1L, 1L))
                .thenReturn(List.of());
    }

    // ---------- Reserva temporal ----------

    @Test
    void reservaTemporalBloqueaTodoLoOfrecidoPorUnDia() {
        ReservaResponse respuesta = service.reservarTemporal(temporal());

        ArgumentCaptor<Reserva> guardada = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).save(guardada.capture());
        assertThat(guardada.getValue().getCantidad()).isEqualTo(4);
        assertThat(guardada.getValue().getEstado()).isEqualTo(EstadoReserva.BLOQUEADA);
        assertThat(guardada.getValue().getExpiraEn()).isEqualTo(AHORA.plusHours(24));
        assertThat(respuesta.estado()).isEqualTo("BLOQUEADA");
        assertThat(respuesta.expiraEn()).isEqualTo(AHORA.plusHours(24));
        assertThat(respuesta.items()).hasSize(1);
    }

    @Test
    void reservaTemporalConItemsReservaSoloEsaCantidad() {
        service.reservarTemporal(new ReservaTemporalRequest(1L, 1L, 1L, List.of(new ItemReserva(3L, 2L, 2))));

        ArgumentCaptor<Reserva> guardada = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).save(guardada.capture());
        assertThat(guardada.getValue().getCantidad()).isEqualTo(2);
    }

    @Test
    void reservaTemporalRechazaItemsQueSuperanLoOfrecido() {
        assertThatThrownBy(() -> service.reservarTemporal(
                new ReservaTemporalRequest(1L, 1L, 1L, List.of(new ItemReserva(3L, 2L, 5)))))
                .isInstanceOf(SolicitudInvalidaException.class);
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void reservaTemporalRechazaItemsQueLaOfertaNoIncluye() {
        assertThatThrownBy(() -> service.reservarTemporal(
                new ReservaTemporalRequest(1L, 1L, 1L, List.of(new ItemReserva(3L, 4L, 1)))))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void reservaTemporalSinStockSuficienteDevuelveConflictoYNoReservaNada() {
        // Stock 6, ya hay 3 tomados por otras ofertas: quedan 3 libres y se piden 4
        when(reservaRepository.sumarActivas(3L, 2L, AHORA)).thenReturn(3L);

        assertThatThrownBy(() -> service.reservarTemporal(temporal()))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("Paramédicos")
                .hasMessageContaining("3 libres");
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void reservaTemporalSinFilaDeInventarioCuentaComoStockCero() {
        when(inventarioOngRepository.bloquearPorOngYRecurso(anyCollection(), anyCollection()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.reservarTemporal(temporal())).isInstanceOf(ConflictoException.class);
    }

    @Test
    void reservaTemporalDeOfertaInexistenteDevuelve404() {
        when(ofertaRepository.findByEmergenciaIdAndLoteIdAndOfertaId(1L, 1L, 9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reservarTemporal(new ReservaTemporalRequest(1L, 1L, 9L, null)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void reservaTemporalConRecursoFueraDelCatalogoDevuelveConflicto() {
        when(recursoRepository.findByNombre("Paramédicos")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reservarTemporal(temporal())).isInstanceOf(ConflictoException.class);
    }

    @Test
    void revalidarReemplazaLosBloqueosAnteriores() {
        Reserva anterior = reserva(10L, 4, EstadoReserva.BLOQUEADA, AHORA.plusHours(3));
        when(reservaRepository.findByEmergenciaIdAndLoteIdAndOfertaIdOrderByIdAsc(1L, 1L, 1L))
                .thenReturn(List.of(anterior));

        service.reservarTemporal(temporal());

        assertThat(anterior.getEstado()).isEqualTo(EstadoReserva.LIBERADA);
        assertThat(anterior.getFechaLiberacion()).isEqualTo(AHORA);
        verify(reservaRepository).save(any(Reserva.class));
    }

    @Test
    void noSePuedeReservarTemporalUnaOfertaYaComprometida() {
        when(reservaRepository.findByEmergenciaIdAndLoteIdAndOfertaIdOrderByIdAsc(1L, 1L, 1L))
                .thenReturn(List.of(reserva(10L, 4, EstadoReserva.COMPROMETIDA, null)));

        assertThatThrownBy(() -> service.reservarTemporal(temporal())).isInstanceOf(ConflictoException.class);
        verify(reservaRepository, never()).save(any());
    }

    // ---------- Reserva definitiva ----------

    @Test
    void definitivaPromueveElBloqueoVigenteSinVolverAComprobarElStock() {
        Reserva bloqueo = reserva(10L, 4, EstadoReserva.BLOQUEADA, AHORA.plusHours(3));
        reservasDeLaOferta(bloqueo);

        ReservaResponse respuesta = service.reservarDefinitiva(new ReservaRequest(1L, 1L, 1L));

        assertThat(bloqueo.getEstado()).isEqualTo(EstadoReserva.COMPROMETIDA);
        assertThat(bloqueo.getExpiraEn()).isNull();
        assertThat(bloqueo.getFechaCompromiso()).isEqualTo(AHORA);
        assertThat(respuesta.estado()).isEqualTo("COMPROMETIDA");
        verify(reservaRepository, never()).sumarActivas(anyLong(), anyLong(), any());
    }

    @Test
    void definitivaConBloqueoVencidoVuelveAComprobarYPromueveSiHayStock() {
        Reserva vencida = reserva(10L, 4, EstadoReserva.BLOQUEADA, AHORA.minusHours(1));
        reservasDeLaOferta(vencida);

        service.reservarDefinitiva(new ReservaRequest(1L, 1L, 1L));

        assertThat(vencida.getEstado()).isEqualTo(EstadoReserva.COMPROMETIDA);
        verify(reservaRepository).sumarActivas(3L, 2L, AHORA);
    }

    @Test
    void definitivaConBloqueoVencidoYSinStockDevuelveConflictoYLoDejaVencido() {
        Reserva vencida = reserva(10L, 4, EstadoReserva.BLOQUEADA, AHORA.minusHours(1));
        reservasDeLaOferta(vencida);
        when(reservaRepository.sumarActivas(3L, 2L, AHORA)).thenReturn(5L);

        assertThatThrownBy(() -> service.reservarDefinitiva(new ReservaRequest(1L, 1L, 1L)))
                .isInstanceOf(ConflictoException.class);
        assertThat(vencida.getEstado()).isEqualTo(EstadoReserva.BLOQUEADA);
    }

    @Test
    void definitivaEsIdempotenteSiYaEstaComprometida() {
        Reserva comprometida = reserva(10L, 4, EstadoReserva.COMPROMETIDA, null);
        reservasDeLaOferta(comprometida);

        ReservaResponse respuesta = service.reservarDefinitiva(new ReservaRequest(1L, 1L, 1L));

        assertThat(respuesta.estado()).isEqualTo("COMPROMETIDA");
        verify(inventarioOngRepository, never()).bloquearPorOngYRecurso(anyCollection(), anyCollection());
    }

    @Test
    void definitivaSinReservasDevuelve404() {
        assertThatThrownBy(() -> service.reservarDefinitiva(new ReservaRequest(1L, 1L, 1L)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void definitivaDeUnaOfertaYaLiberadaDevuelveConflicto() {
        reservasDeLaOferta(reserva(10L, 4, EstadoReserva.LIBERADA, null));

        assertThatThrownBy(() -> service.reservarDefinitiva(new ReservaRequest(1L, 1L, 1L)))
                .isInstanceOf(ConflictoException.class);
    }

    // ---------- Liberación ----------

    @Test
    void liberacionLiberaLoComprometido() {
        Reserva comprometida = reserva(10L, 4, EstadoReserva.COMPROMETIDA, null);
        reservasDeLaOferta(comprometida);

        ReservaResponse respuesta = service.liberar(new ReservaLiberacionRequest(1L, 1L, 1L, null));

        assertThat(comprometida.getEstado()).isEqualTo(EstadoReserva.LIBERADA);
        assertThat(comprometida.getFechaLiberacion()).isEqualTo(AHORA);
        assertThat(respuesta.estado()).isEqualTo("LIBERADA");
    }

    @Test
    void liberacionTambienSueltaLosBloqueosVigentes() {
        Reserva bloqueo = reserva(10L, 4, EstadoReserva.BLOQUEADA, AHORA.plusHours(5));
        reservasDeLaOferta(bloqueo);

        service.liberar(new ReservaLiberacionRequest(1L, 1L, 1L, null));

        assertThat(bloqueo.getEstado()).isEqualTo(EstadoReserva.LIBERADA);
    }

    @Test
    void liberacionRepetidaNoCambiaNada() {
        Reserva liberada = reserva(10L, 4, EstadoReserva.LIBERADA, null);
        liberada.setFechaLiberacion(AHORA.minusDays(1));
        reservasDeLaOferta(liberada);

        ReservaResponse respuesta = service.liberar(new ReservaLiberacionRequest(1L, 1L, 1L, null));

        assertThat(liberada.getFechaLiberacion()).isEqualTo(AHORA.minusDays(1));
        assertThat(respuesta.estado()).isEqualTo("LIBERADA");
    }

    @Test
    void liberacionDejaComoEstaUnBloqueoVencido() {
        Reserva vencida = reserva(10L, 4, EstadoReserva.BLOQUEADA, AHORA.minusHours(1));
        reservasDeLaOferta(vencida);

        ReservaResponse respuesta = service.liberar(new ReservaLiberacionRequest(1L, 1L, 1L, null));

        assertThat(vencida.getEstado()).isEqualTo(EstadoReserva.BLOQUEADA);
        assertThat(respuesta.estado()).isEqualTo("EXPIRADA");
    }

    @Test
    void liberacionPorOngSoloLiberaLasDeEsaOng() {
        Ong otra = new Ong();
        otra.setId(5L);
        otra.setRazonSocial("Techo");
        Reserva deBomberos = reserva(10L, 4, EstadoReserva.COMPROMETIDA, null);
        Reserva deTecho = reserva(11L, 2, EstadoReserva.COMPROMETIDA, null);
        deTecho.setOng(otra);
        reservasDeLaOferta(deBomberos, deTecho);

        service.liberar(new ReservaLiberacionRequest(1L, 1L, 1L, 3L));

        assertThat(deBomberos.getEstado()).isEqualTo(EstadoReserva.LIBERADA);
        assertThat(deTecho.getEstado()).isEqualTo(EstadoReserva.COMPROMETIDA);
    }

    @Test
    void liberacionSinReservasDevuelve404() {
        assertThatThrownBy(() -> service.liberar(new ReservaLiberacionRequest(1L, 1L, 1L, null)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // ---------- Consulta ----------

    @Test
    void unBloqueoVencidoSeInformaComoExpirada() {
        reservasDeLaOferta(reserva(10L, 4, EstadoReserva.BLOQUEADA, AHORA.minusMinutes(1)));

        ReservaResponse respuesta = service.consultar(1L, 1L, 1L);

        assertThat(respuesta.estado()).isEqualTo("EXPIRADA");
        assertThat(respuesta.expiraEn()).isNull();
        assertThat(respuesta.items().get(0).estado()).isEqualTo("EXPIRADA");
    }

    @Test
    void elEstadoDeLaOfertaResumeElDeSusItems() {
        Reserva comprometida = reserva(10L, 4, EstadoReserva.COMPROMETIDA, null);
        Reserva liberada = reserva(11L, 2, EstadoReserva.LIBERADA, null);
        reservasDeLaOferta(liberada, comprometida);

        assertThat(service.consultar(1L, 1L, 1L).estado()).isEqualTo("COMPROMETIDA");
    }

    @Test
    void consultarSinReservasDevuelve404() {
        assertThatThrownBy(() -> service.consultar(1L, 1L, 1L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    // ---------- Helpers ----------

    private ReservaTemporalRequest temporal() {
        return new ReservaTemporalRequest(1L, 1L, 1L, null);
    }

    private void reservasDeLaOferta(Reserva... reservas) {
        when(reservaRepository.findByEmergenciaIdAndLoteIdAndOfertaIdOrderByIdAsc(1L, 1L, 1L))
                .thenReturn(new ArrayList<>(List.of(reservas)));
    }

    private Reserva reserva(Long id, int cantidad, EstadoReserva estado, LocalDateTime expiraEn) {
        Reserva reserva = new Reserva();
        reserva.setId(id);
        reserva.setOng(bomberos);
        reserva.setRecurso(paramedicos);
        reserva.setCantidad(cantidad);
        reserva.setEstado(estado);
        reserva.setEmergenciaId(1L);
        reserva.setLoteId(1L);
        reserva.setOfertaId(1L);
        reserva.setFechaCreacion(AHORA.minusHours(2));
        reserva.setExpiraEn(expiraEn);
        return reserva;
    }

    // Oferta con un aporte de paramédicos en el que Bomberos (ONG 3) ofrece la cantidad indicada
    private Oferta oferta(int cantidadOfrecida) {
        OfertaAporteOng porOng = new OfertaAporteOng();
        porOng.setOngId(3L);
        porOng.setRazonSocial("Bomberos Voluntarios de La Plata");
        porOng.setCantidadOfrecida(cantidadOfrecida);

        OfertaAporte aporte = new OfertaAporte();
        aporte.setRecursoNombre("Paramédicos");
        aporte.setPorOng(new ArrayList<>(List.of(porOng)));

        Oferta oferta = new Oferta();
        oferta.setOfertaId(1L);
        oferta.setEmergenciaId(1L);
        oferta.setLoteId(1L);
        oferta.setAportes(new ArrayList<>(List.of(aporte)));
        return oferta;
    }
}
