package com.proyecto.nacional.dto.ofertas;

import java.time.LocalDateTime;
import java.util.List;

public record OfertasConsolidadasResponse(Long emergenciaId, Long loteId, List<OfertaResponse> ofertas) {
    public record OfertaResponse(Long id, String estado, LocalDateTime fechaOferta,
                                 LocalDateTime fechaModificacion, Integer numeroVersion,
                                 List<OngResponse> ongs, List<AporteResponse> aportes) {}
    public record OngResponse(Long id, String razonSocial) {}
    public record AporteResponse(Long itemLoteId, String recursoNombre, Integer cantidadRequerida,
                                 Integer totalOfrecido, List<AporteOngResponse> porOng) {}
    public record AporteOngResponse(Long ongId, String razonSocial, Integer cantidadOfrecida) {}
}
