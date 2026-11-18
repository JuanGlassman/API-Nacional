package com.proyecto.nacional.service;

import com.proyecto.nacional.service.AsignadorRecursos.Asignacion;
import com.proyecto.nacional.service.AsignadorRecursos.Candidato;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AsignadorRecursosTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 10, 19, 10, 0);

    private static Candidato candidato(long oferta, int minutos, String ong, int prioridad, int ofrecido) {
        return new Candidato(oferta, T0.plusMinutes(minutos), ong, prioridad, ofrecido);
    }

    private static Map<String, Integer> libre(Object... pares) {
        Map<String, Integer> libre = new HashMap<>();
        for (int i = 0; i < pares.length; i += 2) {
            libre.put((String) pares[i], (Integer) pares[i + 1]);
        }
        return libre;
    }

    @Test
    void elEspecialistaVaAntesQueElApoyoAunqueSuOfertaSeaMasNueva() {
        var asignaciones = AsignadorRecursos.asignar(5, List.of(
                candidato(1, 0, "apoyo", 2, 4),
                candidato(2, 30, "especialista", 1, 4)), libre("apoyo", 10, "especialista", 10));

        assertThat(asignaciones).containsExactly(
                new Asignacion(2L, "especialista", 4), new Asignacion(1L, "apoyo", 1));
    }

    @Test
    void conElMismoNivelGanaLaOfertaMasAntigua() {
        var asignaciones = AsignadorRecursos.asignar(5, List.of(
                candidato(2, 30, "b", 1, 4),
                candidato(1, 0, "a", 1, 4)), libre("a", 10, "b", 10));

        assertThat(asignaciones).containsExactly(new Asignacion(1L, "a", 4), new Asignacion(2L, "b", 1));
    }

    @Test
    void noAsignaMasDeLoQuePideElLote() {
        var asignaciones = AsignadorRecursos.asignar(3, List.of(candidato(1, 0, "a", 1, 10)), libre("a", 10));

        assertThat(asignaciones).containsExactly(new Asignacion(1L, "a", 3));
    }

    @Test
    void noAsignaMasDelStockLibreNiDeLoOfrecido() {
        var asignaciones = AsignadorRecursos.asignar(100, List.of(
                candidato(1, 0, "a", 1, 10),
                candidato(2, 5, "b", 1, 4)), libre("a", 6, "b", 50));

        assertThat(asignaciones).containsExactly(new Asignacion(1L, "a", 6), new Asignacion(2L, "b", 4));
    }

    @Test
    void elStockLibreDeUnaOngSeComparteEntreSusOfertas() {
        var asignaciones = AsignadorRecursos.asignar(100, List.of(
                candidato(1, 0, "a", 1, 4),
                candidato(2, 5, "a", 1, 4)), libre("a", 6));

        assertThat(asignaciones).containsExactly(new Asignacion(1L, "a", 4), new Asignacion(2L, "a", 2));
    }

    @Test
    void sinCandidatosNiStockNoAsignaNada() {
        assertThat(AsignadorRecursos.asignar(5, List.of(), libre())).isEmpty();
        assertThat(AsignadorRecursos.asignar(5, List.of(candidato(1, 0, "a", 1, 4)), libre("a", 0))).isEmpty();
    }
}
