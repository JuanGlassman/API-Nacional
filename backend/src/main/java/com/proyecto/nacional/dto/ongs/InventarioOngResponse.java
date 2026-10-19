package com.proyecto.nacional.dto.ongs;

import java.util.List;

public record InventarioOngResponse(
        Long ongId,
        String razonSocial,
        List<DisponibilidadRecursoResponse> recursos
) {
}
