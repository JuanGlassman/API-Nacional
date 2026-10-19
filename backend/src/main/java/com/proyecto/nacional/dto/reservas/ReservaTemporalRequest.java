package com.proyecto.nacional.dto.reservas;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReservaTemporalRequest(
        @NotNull @Schema(example = "1") Long emergenciaId,
        @NotNull @Schema(example = "1") Long loteId,
        @NotNull @Schema(example = "1") Long ofertaId,
        @Schema(description = "Opcional. Sin ítems se reserva todo lo que ofrece la oferta; con ítems, solo "
                + "esas cantidades (que no pueden superar lo ofrecido)")
        @Valid List<ItemReserva> items
) {
    public record ItemReserva(
            @NotNull @Schema(example = "1") Long ongId,
            @NotNull @Schema(example = "1") Long recursoId,
            @NotNull @Min(1) @Schema(example = "1") Integer cantidad
    ) {
    }
}
