package com.proyecto.nacional.dto.ongs;

public record DisponibilidadRecursoResponse(
        Long recursoId,
        String nombre,
        String unidadMedida,
        Integer cantidadDisponible
) {
}
