package com.proyecto.nacional.dto.reservas;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

// Identifica la oferta (ya guardada con POST /ofertas) sobre la que se opera
public record ReservaRequest(
        @NotNull @Schema(example = "1") Long emergenciaId,
        @NotNull @Schema(example = "1") Long loteId,
        @NotNull @Schema(example = "1") Long ofertaId
) {
}
