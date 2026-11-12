package com.proyecto.nacional.dto.ongs;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OngRequest(
        @Schema(description = "Opcional. Identificador común con la app local (UUID); si no viene se genera. "
                + "No se puede cambiar una vez creada la ONG", example = "57f6bace-9f0e-4c1b-9ac8-0e3b09069e29")
        @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                message = "El código debe ser un UUID")
        String codigo,
        @NotBlank @Size(max = 200) String razonSocial,
        @NotBlank @Size(max = 200) String bonitaGroupPath
) {
}
