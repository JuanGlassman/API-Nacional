package com.proyecto.nacional.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

// Formato único de los errores que devuelve la API
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String mensaje,
        List<String> detalles
) {

    public static ErrorResponse de(HttpStatus status, String mensaje) {
        return de(status, mensaje, List.of());
    }

    public static ErrorResponse de(HttpStatus status, String mensaje, List<String> detalles) {
        return new ErrorResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(), mensaje, detalles);
    }
}
