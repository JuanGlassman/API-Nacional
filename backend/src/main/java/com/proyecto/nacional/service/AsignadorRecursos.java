package com.proyecto.nacional.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Reparte lo que pide un lote de un recurso entre los aportes de las ONGs de todas las ofertas.
 * No toca la base: recibe lo ofrecido, la prioridad de cada ONG y su stock libre, y devuelve las cantidades
 * a bloquear. Orden: primero la ONG de mejor nivel en la categoría del recurso, después la oferta más
 * antigua y por último el id de la oferta. El stock libre de una ONG se comparte entre todas sus ofertas.
 */
final class AsignadorRecursos {

    /** Lo que una ONG ofrece de un recurso dentro de una oferta. prioridad: 1 = especialista, 2 = apoyo. */
    record Candidato(Long ofertaId, LocalDateTime fechaOferta, String ongCodigo, int prioridad, int ofrecido) {
    }

    record Asignacion(Long ofertaId, String ongCodigo, int cantidad) {
    }

    private static final Comparator<Candidato> ORDEN = Comparator
            .comparingInt(Candidato::prioridad)
            .thenComparing(Candidato::fechaOferta, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Candidato::ofertaId)
            .thenComparing(Candidato::ongCodigo);

    private AsignadorRecursos() {
    }

    /**
     * @param necesidad   cantidad que pide el lote del recurso
     * @param candidatos  aportes de las ONGs habilitadas (los no habilitados no deben venir)
     * @param librePorOng stock libre de cada ONG para este recurso; se descuenta a medida que se asigna
     */
    static List<Asignacion> asignar(int necesidad, Collection<Candidato> candidatos, Map<String, Integer> librePorOng) {
        List<Asignacion> asignaciones = new ArrayList<>();
        int restante = necesidad;
        for (Candidato candidato : candidatos.stream().sorted(ORDEN).toList()) {
            if (restante <= 0) {
                break;
            }
            int libre = librePorOng.getOrDefault(candidato.ongCodigo(), 0);
            int cantidad = Math.min(Math.min(candidato.ofrecido(), libre), restante);
            if (cantidad <= 0) {
                continue;
            }
            librePorOng.put(candidato.ongCodigo(), libre - cantidad);
            restante -= cantidad;
            asignaciones.add(new Asignacion(candidato.ofertaId(), candidato.ongCodigo(), cantidad));
        }
        return asignaciones;
    }
}
