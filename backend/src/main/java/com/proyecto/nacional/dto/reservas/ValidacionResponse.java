package com.proyecto.nacional.dto.reservas;

import com.proyecto.nacional.dto.reservas.ReservaResponse.ItemReservaResponse;

import java.util.List;

// hayOfertasHabilitadas: alguna oferta quedó con recursos bloqueados. Cada resultado trae solo los
// ítems BLOQUEADOS; una oferta sin ítems no fue aprobada (aprobada = false).
public record ValidacionResponse(
        Long emergenciaId,
        Long loteId,
        boolean hayOfertasHabilitadas,
        List<ResultadoOferta> resultados
) {
    public record ResultadoOferta(
            Long ofertaId,
            boolean aprobada,
            List<ItemReservaResponse> items
    ) {
    }
}
