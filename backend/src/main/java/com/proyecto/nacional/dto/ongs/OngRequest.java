package com.proyecto.nacional.dto.ongs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OngRequest(
        @NotBlank @Size(max = 200) String razonSocial,
        @NotBlank @Size(max = 200) String bonitaGroupPath
) {
}
