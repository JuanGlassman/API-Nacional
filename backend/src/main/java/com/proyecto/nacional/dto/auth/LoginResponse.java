package com.proyecto.nacional.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

// Respuesta plana para que los clientes (por ejemplo, el conector REST de Bonita) lean el token fácilmente
@Getter
@AllArgsConstructor
public class LoginResponse {

    @Schema(description = "JWT a enviar en el header Authorization: Bearer <token>")
    private String token;

    @Schema(example = "Bearer")
    private String tipo;

    @Schema(description = "Segundos de vigencia del token", example = "3600")
    private long expiraEn;

    @Schema(example = "SISTEMA_CLIENTE")
    private String rol;

}
