package com.proyecto.nacional.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Login fallido: usuario inexistente, contraseña incorrecta o cuenta deshabilitada.
    // El mensaje es el mismo en todos los casos para no revelar qué cuentas existen.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAutenticacion(AuthenticationException ex, HttpServletRequest request) {
        log.warn("{} {} -> {}: {}", request.getMethod(), request.getRequestURI(),
                ex.getClass().getSimpleName(), ex.getMessage());
        return construir(HttpStatus.UNAUTHORIZED, "Credenciales inválidas", List.of());
    }

    // Lo lanza @PreAuthorize cuando el rol no alcanza; sin este handler caería en el genérico (500)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccesoDenegado(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("{} {} -> AccessDeniedException: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return construir(HttpStatus.FORBIDDEN, "No tiene permisos para acceder a este recurso", List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidacion(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        log.warn("{} {} -> Datos inválidos: {}", request.getMethod(), request.getRequestURI(), detalles);
        return construir(HttpStatus.BAD_REQUEST, "Datos inválidos", detalles);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleCuerpoIlegible(HttpMessageNotReadableException ex,
                                                              HttpServletRequest request) {
        log.warn("{} {} -> Cuerpo ilegible: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return construir(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud falta o no es un JSON válido", List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleRutaInexistente(NoResourceFoundException ex, HttpServletRequest request) {
        log.warn("{} {} -> Ruta inexistente", request.getMethod(), request.getRequestURI());
        return construir(HttpStatus.NOT_FOUND, "Recurso no encontrado", List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenerica(Exception ex, HttpServletRequest request) {
        log.error("{} {} -> Error no controlado: {}", request.getMethod(), request.getRequestURI(),
                ex.getMessage(), ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", List.of());
    }

    private ResponseEntity<ErrorResponse> construir(HttpStatus status, String mensaje, List<String> detalles) {
        return ResponseEntity.status(status).body(ErrorResponse.de(status, mensaje, detalles));
    }
}
