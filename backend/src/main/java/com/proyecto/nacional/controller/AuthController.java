package com.proyecto.nacional.controller;

import com.proyecto.nacional.dto.auth.LoginRequest;
import com.proyecto.nacional.dto.auth.LoginResponse;
import com.proyecto.nacional.exception.ErrorResponse;
import com.proyecto.nacional.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Obtención del JWT para consumir el resto de la API")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @SecurityRequirements // Endpoint público: Swagger no pide token para probarlo
    @Operation(summary = "Iniciar sesión",
            description = "Valida usuario y contraseña y devuelve un JWT para usar como Bearer Token.")
    @ApiResponse(responseCode = "200", description = "Login correcto")
    @ApiResponse(responseCode = "400", description = "Faltan usuario o contraseña",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Credenciales inválidas o cuenta deshabilitada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
