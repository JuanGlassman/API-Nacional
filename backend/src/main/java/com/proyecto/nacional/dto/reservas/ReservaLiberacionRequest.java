package com.proyecto.nacional.dto.reservas;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ReservaLiberacionRequest(
        @NotNull @Schema(example = "1") Long emergenciaId,
        @NotNull @Schema(example = "1") Long loteId,
        @NotNull @Schema(example = "1") Long ofertaId,
        @Schema(description = "Opcional. Código común de la ONG; si viene, solo se liberan sus recursos",
                example = "57f6bace-9f0e-4c1b-9ac8-0e3b09069e29")
        String ongCodigo
) {
}
