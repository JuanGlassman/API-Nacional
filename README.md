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

Agregar el archivo `.env` en la raíz con estas variables:

| Variable                 | Descripción                                                                  |
| ------------------------ | ---------------------------------------------------------------------------- |
| `DB_HOST`, `DB_PORT`     | Host y puerto de PostgreSQL (por defecto `localhost:5432`)                   |
| `DB_NAME`                | Nombre de la base                                                            |
| `DB_USER`, `DB_PASSWORD` | Credenciales de la base                                                      |
| `SERVER_PORT`            | Puerto de la API (por defecto `8080`)                                        |
| `JWT_SECRET`             | Secreto para firmar los JWT. **Obligatorio**, mínimo 32 caracteres           |
| `JWT_EXPIRATION_MINUTES` | Vigencia del token en minutos (por defecto `60`)                             |
| `ADMIN_PASSWORD`         | Contraseña de la cuenta `admin` (rol `ADMIN`), que se crea al arrancar       |
| `BONITA_PASSWORD`        | Contraseña de la cuenta `bonita` (rol `SISTEMA_CLIENTE`), que se crea al arrancar |

Si falta `ADMIN_PASSWORD` o `BONITA_PASSWORD`, esa cuenta no se crea (queda un aviso en el log).

URL API: https://sistema-nacional-api-wqib.onrender.com

### Autenticación

Todos los endpoints requieren un JWT, salvo `POST /login`, `/actuator/health` y Swagger.

1. Obtener el token:

   ```http
   POST /login
   Content-Type: application/json

   { "username": "bonita", "password": "..." }
   ```

   Respuesta:

   ```json
   { "token": "eyJhbGciOi...", "tipo": "Bearer", "expiraEn": 3600, "rol": "SISTEMA_CLIENTE" }
   ```

2. Enviarlo en cada solicitud: `Authorization: Bearer <token>`. En Swagger UI se carga con el botón **Authorize**.

Roles: `ADMIN` (gestión de ONGs y cuentas), `ONG` (sus datos, certificaciones y recursos) y `SISTEMA_CLIENTE` (Bonita: validación, compromiso y liberación). Los errores de seguridad devuelven `401` (sin token, token inválido o vencido, cuenta deshabilitada) o `403` (el rol no tiene permiso), con el formato de error común de la API.

### API
| Concepto                  | Función                                                                                                         |
| ------------------------- | --------------------------------------------------------------------------------------------------------------- |
| **OpenAPI**               | Especificación estándar que describe una API REST: endpoints, parámetros, esquemas, respuestas y seguridad.     |
| **Swagger UI**            | Interfaz web que presenta la documentación OpenAPI y permite ejecutar solicitudes HTTP.                         |
| **Springdoc OpenAPI**     | Biblioteca que integra OpenAPI con Spring Boot y genera la documentación a partir del código y sus anotaciones. |
| **Bearer Authentication** | Mecanismo que permite enviar un token en la cabecera HTTP `Authorization` para autenticar solicitudes.          |
