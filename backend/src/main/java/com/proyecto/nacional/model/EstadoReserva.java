package com.proyecto.nacional.model;

// Estados que se guardan en base. "Expirada" no está acá: es una BLOQUEADA cuyo vencimiento ya pasó
// y se informa calculándola contra el reloj (ver ReservaService).
public enum EstadoReserva {
    // Reserva temporal hecha en la validación; vence a las horas configuradas si no se compromete
    BLOQUEADA,
    // Reserva definitiva (oferta adjudicada); dura hasta la liberación explícita
    COMPROMETIDA,
    LIBERADA
}
