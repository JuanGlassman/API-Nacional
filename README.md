# Sistema Nacional de Gestión de Recursos y Riesgos

API REST del Sistema Nacional de Gestión de Recursos y Riesgos.

## Tecnologías

- Java 21
- Spring Boot 4.1.1
- Maven
- PostgreSQL 15
- Docker
- Docker Compose

## Ejecución local

### Requisitos

- Docker
- Docker Compose

### Configuración

Agregar el archivo `.env` en la raíz.

URL API: https://sistema-nacional-api-wqib.onrender.com

### API
| Concepto                  | Función                                                                                                         |
| ------------------------- | --------------------------------------------------------------------------------------------------------------- |
| **OpenAPI**               | Especificación estándar que describe una API REST: endpoints, parámetros, esquemas, respuestas y seguridad.     |
| **Swagger UI**            | Interfaz web que presenta la documentación OpenAPI y permite ejecutar solicitudes HTTP.                         |
| **Springdoc OpenAPI**     | Biblioteca que integra OpenAPI con Spring Boot y genera la documentación a partir del código y sus anotaciones. |
| **Bearer Authentication** | Mecanismo que permite enviar un token en la cabecera HTTP `Authorization` para autenticar solicitudes.          |
