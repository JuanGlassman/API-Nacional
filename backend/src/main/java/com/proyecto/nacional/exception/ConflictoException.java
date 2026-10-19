package com.proyecto.nacional.exception;

// La operación choca con datos existentes (duplicados, dependencias): se responde 409
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
