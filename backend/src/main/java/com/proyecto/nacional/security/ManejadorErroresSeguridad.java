package com.proyecto.nacional.security;

import com.proyecto.nacional.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

// Responde los rechazos del filtro de seguridad con el mismo JSON que el GlobalExceptionHandler
@Slf4j
@Component
@RequiredArgsConstructor
public class ManejadorErroresSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final JsonMapper jsonMapper;

    // Sin token, token inválido/vencido o cuenta deshabilitada
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        log.warn("{} {} -> 401: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        escribir(response, HttpStatus.UNAUTHORIZED, "Se requiere un token de acceso válido");
    }

    // Token válido pero el rol de la cuenta no tiene permiso sobre el recurso
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        log.warn("{} {} -> 403: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        escribir(response, HttpStatus.FORBIDDEN, "No tiene permisos para acceder a este recurso");
    }

    private void escribir(HttpServletResponse response, HttpStatus status, String mensaje) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        jsonMapper.writeValue(response.getOutputStream(), ErrorResponse.de(status, mensaje));
    }
}
