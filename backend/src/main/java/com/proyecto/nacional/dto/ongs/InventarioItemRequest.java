package com.proyecto.nacional.dto.ongs;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InventarioItemRequest(
        @NotNull Long recursoId,
        @NotNull @Min(0) Integer cantidadDisponible
) {
}
