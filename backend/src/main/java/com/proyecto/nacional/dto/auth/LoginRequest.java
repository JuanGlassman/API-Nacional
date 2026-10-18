package com.proyecto.nacional.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank
    @Schema(example = "bonita")
    private String username;

    @NotBlank
    private String password;

}
