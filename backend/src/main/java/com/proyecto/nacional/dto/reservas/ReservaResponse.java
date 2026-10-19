package com.proyecto.nacional.dto.reservas;

import java.time.LocalDateTime;
import java.util.List;

// estado: BLOQUEADA, EXPIRADA (bloqueo vencido), COMPROMETIDA o LIBERADA. El de la oferta resume el de
// sus ítems: COMPROMETIDA si hay alguno comprometido, si no BLOQUEADA, si no EXPIRADA, si no LIBERADA.
public record ReservaResponse(
        Long emergenciaId,
        Long loteId,
        Long ofertaId,
        String estado,
        LocalDateTime expiraEn,
        List<ItemReservaResponse> items
) {
    public record ItemReservaResponse(
            Long ongId,
            String razonSocial,
            Long recursoId,
            String recursoNombre,
            String unidadMedida,
            Integer cantidad,
            String estado,
            LocalDateTime expiraEn
    ) {
    }
}
