package com.proyecto.nacional.dto.ofertas;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record OfertasConsolidadasRequest(
        @NotNull Long emergenciaId,
        @NotNull Long loteId,
        @NotEmpty @Valid List<OfertaRequest> ofertas
) {
    public record OfertaRequest(
            @NotNull Long id,
            @NotNull String estado,
            @NotNull LocalDateTime fechaOferta,
            LocalDateTime fechaModificacion,
            @NotNull Integer numeroVersion,
            @Valid List<OngRequest> ongs,
            @NotEmpty @Valid List<AporteRequest> aportes
    ) {}

    public record OngRequest(Long id, String razonSocial) {}

    public record AporteRequest(
            @NotNull Long itemLoteId,
            @NotNull String recursoNombre,
            @NotNull Integer cantidadRequerida,
            @NotNull Integer totalOfrecido,
            @Valid List<AporteOngRequest> porOng
    ) {}

    public record AporteOngRequest(
            @NotNull Long ongId,
            @NotNull String razonSocial,
            @NotNull Integer cantidadOfrecida
    ) {}
}
