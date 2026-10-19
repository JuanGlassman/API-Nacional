package com.proyecto.nacional.dto.reservas;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ReservaLiberacionRequest(
        @NotNull @Schema(example = "1") Long emergenciaId,
        @NotNull @Schema(example = "1") Long loteId,
        @NotNull @Schema(example = "1") Long ofertaId,
        @Schema(description = "Opcional. Si viene, solo se liberan los recursos de esa ONG", example = "1")
        Long ongId
) {
}
