package com.proyecto.nacional.exception;

// El recurso pedido (ONG, ítem de catálogo o de inventario) no existe: se responde 404
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
