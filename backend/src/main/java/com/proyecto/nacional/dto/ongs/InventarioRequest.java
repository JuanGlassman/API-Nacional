package com.proyecto.nacional.dto.ongs;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record InventarioRequest(
        @NotEmpty List<@Valid InventarioItemRequest> recursos
) {
}
