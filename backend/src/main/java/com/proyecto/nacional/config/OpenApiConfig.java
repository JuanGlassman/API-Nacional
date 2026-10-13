package com.proyecto.nacional.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI sistemaNacionalOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sistema Nacional API")
                        .description(
                                "Documentación de los servicios REST "
                                        + "del Sistema Nacional."
                        )
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Equipo de desarrollo")
                        )
                )
                .components(new Components()
                        .addSecuritySchemes(
                                SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description(
                                                "Ingresar el token de acceso. "
                                                        + "Swagger agregará automáticamente "
                                                        + "el prefijo Bearer."
                                        )
                        )
                )
                .addSecurityItem(
                        new SecurityRequirement()
                                .addList(SECURITY_SCHEME_NAME)
                );
    }
}