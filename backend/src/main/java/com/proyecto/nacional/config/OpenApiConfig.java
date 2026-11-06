package com.proyecto.nacional.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenApiCustomizer respuestasDeError() {
        return openApi -> {
            openApi.getComponents().addSchemas("ErrorResponse", esquemaError());
            openApi.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> {
                ApiResponses responses = operation.getResponses();
                agregarError(responses, "400", "Solicitud inválida o datos de entrada incorrectos.");
                agregarError(responses, "401", "Falta un token válido o las credenciales son incorrectas.");
                agregarError(responses, "403", "El usuario autenticado no tiene permisos para esta operación.");
                agregarError(responses, "404", "No se encontró la ruta o el recurso solicitado.");
                agregarError(responses, "409", "La operación entra en conflicto con el estado actual de los recursos.");
                agregarError(responses, "500", "Error interno inesperado del servidor.");
            }));
        };
    }

    private void agregarError(ApiResponses responses, String codigo, String descripcion) {
        responses.putIfAbsent(codigo, new ApiResponse()
                .description(descripcion)
                .content(new Content().addMediaType("application/json",
                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/ErrorResponse")))));
    }

    private Schema<?> esquemaError() {
        Map<String, Schema> propiedades = new LinkedHashMap<>();
        propiedades.put("timestamp", new Schema<>().type("string").format("date-time")
                .example("2026-11-06T12:00:00"));
        propiedades.put("status", new Schema<>().type("integer").format("int32").example(400));
        propiedades.put("error", new Schema<>().type("string").example("Bad Request"));
        propiedades.put("mensaje", new Schema<>().type("string").example("Datos inválidos"));
        propiedades.put("detalles", new Schema<>().type("array").items(new Schema<>().type("string"))
                .example(java.util.List.of("nombre: no debe estar vacío")));
        return new Schema<>().type("object").properties(propiedades);
    }

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