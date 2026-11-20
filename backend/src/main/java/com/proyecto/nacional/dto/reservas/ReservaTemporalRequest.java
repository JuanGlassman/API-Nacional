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
        @Valid List<ItemReserva> items,
        @Schema(description = "Opcional, por defecto false. Con true, si algún recurso no alcanza se reserva lo que "
                + "haya libre de cada uno (se omiten los que no tienen nada) y solo responde 409 si no se puede "
                + "reservar nada. Con false o sin indicar es todo o nada", example = "false")
        Boolean parcial
) {
    public ReservaTemporalRequest(Long emergenciaId, Long loteId, Long ofertaId, List<ItemReserva> items) {
        this(emergenciaId, loteId, ofertaId, items, null);
    }

    public boolean esParcial() {
        return Boolean.TRUE.equals(parcial);
    }

    public record ItemReserva(
            @NotNull @Schema(example = "57f6bace-9f0e-4c1b-9ac8-0e3b09069e29") String ongCodigo,
            @NotNull @Schema(example = "1") Long recursoId,
            @NotNull @Min(1) @Schema(example = "1") Integer cantidad
    ) {
    }
}
