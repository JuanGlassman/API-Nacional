package com.proyecto.nacional.exception;

// El cuerpo es válido en formato pero no tiene sentido para el negocio: se responde 400
public class SolicitudInvalidaException extends RuntimeException {

    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
