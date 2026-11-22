package com.proyecto.nacional.config;

import com.proyecto.nacional.model.SeedVersion;
import com.proyecto.nacional.repository.InventarioOngRepository;
import com.proyecto.nacional.repository.OfertaRepository;
import com.proyecto.nacional.repository.OngCategoriaNivelRepository;
import com.proyecto.nacional.repository.ReservaRepository;
import com.proyecto.nacional.repository.SeedVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReinicioDatosDemoTest {

    private ReservaRepository reservaRepository;
    private OfertaRepository ofertaRepository;
    private InventarioOngRepository inventarioOngRepository;
    private OngCategoriaNivelRepository nivelRepository;
    private SeedVersionRepository seedVersionRepository;

    @BeforeEach
    void setUp() {
        reservaRepository = mock(ReservaRepository.class);
        ofertaRepository = mock(OfertaRepository.class);
        inventarioOngRepository = mock(InventarioOngRepository.class);
        nivelRepository = mock(OngCategoriaNivelRepository.class);
        seedVersionRepository = mock(SeedVersionRepository.class);
    }

    @Test
    void sinVersionConfiguradaNoHaceNada() {
        reinicio("").run();

        verify(reservaRepository, never()).deleteAllInBatch();
        verify(seedVersionRepository, never()).save(any());
    }

    @Test
    void conUnaVersionNuevaBorraElEstadoYLaRecuerda() {
        when(seedVersionRepository.findById(SeedVersion.ID)).thenReturn(Optional.empty());

        reinicio("demo-1").run();

        verify(reservaRepository).deleteAllInBatch();
        verify(ofertaRepository).deleteAll();
        verify(inventarioOngRepository).deleteAllInBatch();
        verify(nivelRepository).deleteAllInBatch();
        ArgumentCaptor<SeedVersion> guardada = ArgumentCaptor.forClass(SeedVersion.class);
        verify(seedVersionRepository).save(guardada.capture());
        assertThat(guardada.getValue().getVersion()).isEqualTo("demo-1");
    }

    @Test
    void conLaMismaVersionYaAplicadaNoBorraNada() {
        when(seedVersionRepository.findById(SeedVersion.ID)).thenReturn(Optional.of(new SeedVersion("demo-1")));

        reinicio("demo-1").run();

        verify(reservaRepository, never()).deleteAllInBatch();
        verify(inventarioOngRepository, never()).deleteAllInBatch();
        verify(seedVersionRepository, never()).save(any());
    }

    @Test
    void conOtraVersionVuelveAReiniciar() {
        when(seedVersionRepository.findById(SeedVersion.ID)).thenReturn(Optional.of(new SeedVersion("demo-1")));

        reinicio("demo-2").run();

        verify(reservaRepository).deleteAllInBatch();
        verify(inventarioOngRepository).deleteAllInBatch();
    }

    private ReinicioDatosDemo reinicio(String version) {
        return new ReinicioDatosDemo(reservaRepository, ofertaRepository, inventarioOngRepository,
                nivelRepository, seedVersionRepository, version);
    }
}
