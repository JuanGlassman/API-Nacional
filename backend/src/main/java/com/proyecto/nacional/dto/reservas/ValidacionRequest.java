package com.proyecto.nacional.dto.reservas;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ValidacionRequest(
        @NotNull @Schema(example = "1") Long emergenciaId,
        @NotNull @Schema(example = "1") Long loteId
) {
}
