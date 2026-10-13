# Swagger y OpenAPI

## Documentación técnica

## 1. Introducción

OpenAPI es una especificación estándar para describir, documentar y definir la interfaz de una API HTTP. Permite representar de manera estructurada sus operaciones, parámetros, esquemas de datos, respuestas y mecanismos de seguridad.

Swagger es un conjunto de herramientas que permite trabajar con la especificación OpenAPI. Entre sus herramientas se encuentra Swagger UI, una interfaz gráfica que presenta la documentación de una API y permite ejecutar solicitudes HTTP desde el navegador.

En aplicaciones desarrolladas con Spring Boot, Springdoc OpenAPI permite generar automáticamente una descripción OpenAPI a partir de los controladores, los modelos y las anotaciones presentes en el código fuente.

La combinación de estas tecnologías facilita la comprensión, el mantenimiento, la integración y la prueba de los servicios REST.

## 2. OpenAPI

### 2.1. Definición

OpenAPI es una especificación independiente del lenguaje de programación y del framework utilizado para implementar una API.

Describe el contrato de comunicación entre un cliente y un servidor. Este contrato establece qué operaciones están disponibles, cómo deben invocarse y qué respuestas pueden obtenerse.

La especificación puede representarse mediante documentos JSON o YAML.

### 2.2. Información que permite documentar

Un documento OpenAPI puede incluir:

* **Información general:** nombre, descripción, versión y datos de contacto de la API.
* **Servidores:** direcciones base desde las que se accede a la API.
* **Endpoints:** rutas disponibles y métodos HTTP admitidos.
* **Parámetros:** valores enviados mediante la ruta, la consulta o las cabeceras HTTP.
* **Request bodies:** estructuras de datos que recibe el servidor.
* **Responses:** códigos HTTP, descripciones y estructuras de las respuestas.
* **Schemas:** definición de los objetos y tipos de datos utilizados.
* **Security schemes:** mecanismos de autenticación y seguridad.
* **Examples:** ejemplos de solicitudes, respuestas y valores de los campos.

### 2.3. Estructura de un documento OpenAPI

Un documento OpenAPI contiene distintas secciones que describen el contrato de la API.

| Sección      | Descripción                                                     |
| ------------ | --------------------------------------------------------------- |
| `openapi`    | Versión de la especificación OpenAPI utilizada.                 |
| `info`       | Metadatos generales de la API.                                  |
| `servers`    | Servidores disponibles.                                         |
| `paths`      | Rutas y operaciones de la API.                                  |
| `components` | Esquemas reutilizables, parámetros y definiciones de seguridad. |
| `security`   | Requisitos de autenticación globales o específicos.             |

Por ejemplo, una definición simplificada podría ser:

```yaml
openapi: 3.1.0

info:
  title: API de ejemplo
  version: 1.0.0
  description: API REST de ejemplo.

paths:
  /api/recursos:
    get:
      summary: Listar recursos
      responses:
        '200':
          description: Lista obtenida correctamente.
```

Este documento describe una operación HTTP `GET` sobre `/api/recursos` y establece que puede responder con el código `200`.

El ejemplo es ilustrativo y no implica que ese endpoint exista en una aplicación concreta.

## 3. Swagger

Swagger es un conjunto de herramientas destinadas a facilitar el desarrollo y el uso de APIs descritas mediante OpenAPI.

Entre sus principales herramientas se encuentran:

### 3.1. Swagger UI

Swagger UI transforma una descripción OpenAPI en una interfaz web interactiva.

Sus funcionalidades incluyen:

* Mostrar las operaciones agrupadas por rutas o etiquetas.
* Consultar los parámetros de entrada y salida.
* Visualizar los esquemas de datos.
* Introducir valores para los parámetros.
* Construir solicitudes HTTP.
* Ejecutar operaciones contra un servidor.
* Consultar los códigos HTTP y los cuerpos de las respuestas.
* Proporcionar credenciales cuando existe un esquema de autenticación configurado.

Swagger UI permite probar una API sin tener que construir manualmente cada solicitud en herramientas externas.

### 3.2. Swagger Editor

Swagger Editor es una herramienta que permite escribir, validar y visualizar documentos OpenAPI.

Es útil cuando se necesita diseñar el contrato de una API antes de implementar sus endpoints.

A diferencia de Springdoc, que puede generar documentación a partir de una aplicación Spring Boot, Swagger Editor está orientado a la edición directa de la especificación.

### 3.3. Diferencia entre Swagger y OpenAPI

OpenAPI es la especificación que define cómo describir una API. Swagger es el nombre de un conjunto de herramientas que permite trabajar con esa especificación.

Por ejemplo, un documento OpenAPI describe las operaciones de una API, mientras que Swagger UI utiliza ese documento para mostrar una interfaz interactiva.

## 4. Springdoc OpenAPI

### 4.1. Definición

Springdoc OpenAPI es una biblioteca que integra OpenAPI con aplicaciones Spring Boot.

Su función principal es analizar los componentes de la aplicación y generar automáticamente la documentación de los servicios HTTP.

Puede obtener información de:

* Controladores anotados con `@RestController`.
* Métodos anotados con `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping` y `@DeleteMapping`.
* Parámetros de los métodos.
* DTO utilizados en las solicitudes y respuestas.
* Anotaciones de validación.
* Anotaciones específicas de OpenAPI.
* Configuraciones de seguridad declaradas en la documentación.

La generación automática reduce la necesidad de mantener manualmente un documento independiente de la implementación.

### 4.2. Integración con Spring Boot

Para utilizar Springdoc en una aplicación Spring Boot basada en Spring MVC, se incorpora la dependencia correspondiente en Maven:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>${springdoc.version}</version>
</dependency>
```

La propiedad `springdoc.version` representa la versión de Springdoc elegida para el proyecto. Debe definirse con una versión compatible con la versión de Spring Boot utilizada.

La dependencia integra la generación de documentación y la interfaz Swagger UI.

### 4.3. Endpoints de documentación

Con la configuración predeterminada, Springdoc expone los siguientes recursos:

| Endpoint            | Función                                        |
| ------------------- | ---------------------------------------------- |
| `/v3/api-docs`      | Devuelve el documento OpenAPI en formato JSON. |
| `/v3/api-docs.yaml` | Devuelve la documentación en formato YAML.     |
| `/swagger-ui.html`  | Permite acceder a Swagger UI.                  |

La interfaz consume el documento OpenAPI y utiliza su contenido para presentar las operaciones disponibles.

Las rutas pueden modificarse mediante las propiedades de configuración de Springdoc.

## 5. Documentación de endpoints

Un endpoint representa una operación accesible mediante una ruta y un método HTTP.

Springdoc detecta las operaciones definidas en los controladores de Spring Boot y genera su descripción inicial.

Por ejemplo:

```java
@RestController
@RequestMapping("/api/recursos")
public class RecursoController {

    @GetMapping
    public List<String> listar() {
        return List.of("Recurso A", "Recurso B");
    }
}
```

A partir de este controlador, Springdoc puede documentar la operación `GET /api/recursos`.

Para mejorar la documentación se pueden utilizar anotaciones de OpenAPI.

### 5.1. Anotación `@Tag`

Permite agrupar operaciones relacionadas.

```java
@Tag(
    name = "Recursos",
    description = "Operaciones relacionadas con los recursos."
)
@RestController
@RequestMapping("/api/recursos")
public class RecursoController {
}
```

En Swagger UI, las operaciones del controlador se pueden presentar bajo el grupo «Recursos».

### 5.2. Anotación `@Operation`

Describe una operación individual.

```java
@Operation(
    summary = "Listar recursos",
    description = "Obtiene los recursos disponibles en el sistema."
)
@GetMapping
public List<String> listar() {
    return List.of("Recurso A", "Recurso B");
}
```

Los campos `summary` y `description` permiten indicar, respectivamente, el propósito breve y la explicación detallada de la operación.

### 5.3. Anotación `@ApiResponse`

Documenta las posibles respuestas de una operación.

```java
@ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Solicitud procesada correctamente."
    ),
    @ApiResponse(
        responseCode = "500",
        description = "Error interno del servidor."
    )
})
```

Los códigos documentados deben corresponder con los que la implementación puede devolver. Declarar una respuesta en OpenAPI no hace que el servidor genere automáticamente ese código.

## 6. Esquemas y DTO

### 6.1. Definición de esquemas

Los esquemas describen la estructura de los datos utilizados en las solicitudes y respuestas.

Pueden definir:

* Propiedades y tipos de datos.
* Campos obligatorios.
* Restricciones de validación.
* Formatos, como fechas y direcciones de correo electrónico.
* Valores permitidos.
* Relaciones con otros esquemas.
* Ejemplos de datos.

### 6.2. Documentación automática de DTO

En Spring Boot, los DTO representan los objetos que se reciben o devuelven a través de la API.

Springdoc puede utilizar la estructura de las clases y sus anotaciones para generar los esquemas correspondientes.

Por ejemplo:

```java
public class RecursoRequestDTO {

    @NotBlank
    private String nombre;

    @NotNull
    @Min(1)
    private Integer cantidad;

    // Getters y setters
}
```

La documentación puede representar `nombre` como un campo de texto obligatorio y `cantidad` como un número entero sujeto a una restricción mínima.

Las anotaciones de validación describen restricciones para la aplicación, mientras que Springdoc las utiliza para enriquecer la documentación cuando reconoce las anotaciones correspondientes.

### 6.3. Anotación `@Schema`

La anotación `@Schema` permite proporcionar información adicional sobre una clase o una propiedad.

```java
@Schema(description = "Información de un recurso.")
public class RecursoResponseDTO {

    @Schema(
        description = "Identificador único del recurso.",
        example = "15"
    )
    private Long id;

    @Schema(
        description = "Nombre del recurso.",
        example = "Agua potable"
    )
    private String nombre;
}
```

Esto permite que Swagger UI muestre descripciones y ejemplos más claros para cada campo.

## 7. Ejemplos de solicitudes y respuestas

OpenAPI permite documentar ejemplos para mostrar cómo deben construirse las solicitudes y cómo pueden ser las respuestas.

Un ejemplo de solicitud JSON podría ser:

```json
{
  "nombre": "Agua potable",
  "cantidad": 100
}
```

Una respuesta ilustrativa podría ser:

```json
{
  "id": 15,
  "nombre": "Agua potable",
  "cantidad": 100
}
```

Estos ejemplos son representativos de las estructuras de datos y no constituyen respuestas reales de un servidor.

Los ejemplos pueden declararse mediante anotaciones de OpenAPI o directamente en el documento de especificación. Deben mantenerse consistentes con los DTO y las reglas de negocio de la API.

## 8. Autenticación y autorización

La seguridad de una API comprende mecanismos para identificar a los usuarios y determinar qué operaciones pueden ejecutar.

Autenticación y autorización son conceptos diferentes:

* **Autenticación:** determina la identidad o validez de las credenciales presentadas.
* **Autorización:** determina si la identidad autenticada tiene permisos para ejecutar una operación.

OpenAPI permite describir los requisitos de seguridad de una API, pero no implementa por sí mismo estos mecanismos en el servidor.

### 8.1. JWT

JSON Web Token (JWT) es un formato compacto para representar información mediante un conjunto de declaraciones o *claims*.

Un JWT firmado suele contener tres partes codificadas en Base64URL, separadas por puntos:

1. Encabezado.
2. Cuerpo o *payload*.
3. Firma.

La firma permite comprobar la integridad del token y, según el algoritmo y las claves utilizados, verificar su autenticidad.

Un JWT firmado no necesariamente cifra su contenido. Por lo tanto, no deben incluirse datos secretos suponiendo que estarán ocultos.

El servidor debe validar el token según la configuración de seguridad establecida, incluyendo su firma y las condiciones pertinentes de vigencia.

### 8.2. Bearer Authentication

Bearer es un esquema de autenticación HTTP en el que el cliente presenta un token de acceso en la cabecera `Authorization`.

Su formato es:

```http
Authorization: Bearer <token>
```

En este esquema, quien posea un token válido puede utilizarlo conforme a los permisos asociados. Por eso es importante proteger su confidencialidad y transmitirlo mediante HTTPS en entornos que no sean locales de desarrollo.

### 8.3. Configuración de Bearer en OpenAPI

OpenAPI permite declarar un esquema HTTP Bearer mediante `components.securitySchemes`.

En Spring Boot puede configurarse con el modelo de OpenAPI:

```java
@Bean
public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                )
        )
        .addSecurityItem(
            new SecurityRequirement()
                .addList("bearerAuth")
        );
}
```

Esta configuración declara el mecanismo de seguridad y lo aplica globalmente a las operaciones documentadas.

Para excluir una operación pública, puede definirse una política de seguridad específica para ella en la documentación OpenAPI.

La declaración de seguridad solo describe el contrato. La aplicación debe implementar la validación real del token y las reglas de acceso.

### 8.4. Uso de Bearer en Swagger UI

Cuando la especificación OpenAPI incluye el esquema Bearer, Swagger UI puede mostrar el botón **Authorize**.

El usuario introduce el token de acceso y la interfaz lo envía en las solicitudes que requieren autenticación.

El encabezado resultante tiene este formato:

```http
Authorization: Bearer eyJhbGciOi...
```

Normalmente, el usuario introduce solamente el token en el cuadro de autorización. Swagger UI incorpora el prefijo `Bearer`.

El resultado depende de la configuración del esquema y de las operaciones documentadas. Que el botón aparezca no significa que el servidor esté validando correctamente el token.

## 9. Flujo de ejecución de una solicitud autenticada

El proceso de prueba de una operación protegida puede resumirse así:

1. El cliente obtiene un JWT mediante el mecanismo de autenticación del sistema.
2. El cliente selecciona la operación en Swagger UI.
3. Introduce el token mediante el botón Authorize.
4. Swagger UI construye la solicitud HTTP y agrega la cabecera `Authorization`.
5. El servidor recibe la solicitud.
6. Los componentes de seguridad del servidor validan el token.
7. Si la solicitud está autenticada y autorizada, se ejecuta la operación.
8. El servidor devuelve una respuesta HTTP.
9. Swagger UI muestra el código y el contenido de la respuesta.

Si el token falta, es inválido o no cumple los requisitos de seguridad, el servidor puede rechazar la solicitud. Los códigos concretos dependen de la configuración y del motivo del rechazo; habitualmente se utilizan `401 Unauthorized` para problemas de autenticación y `403 Forbidden` para accesos no permitidos.

## 10. Pruebas mediante Swagger UI

Swagger UI permite ejecutar solicitudes HTTP desde la interfaz gráfica.

Una prueba puede incluir:

1. Seleccionar un endpoint.
2. Completar los parámetros.
3. Introducir el cuerpo JSON, cuando corresponda.
4. Autorizar la solicitud con un token si es necesario.
5. Ejecutar la operación.
6. Revisar el código HTTP, las cabeceras y el cuerpo de la respuesta.

La herramienta facilita las pruebas manuales y la exploración de los servicios.

Sin embargo, Swagger UI no reemplaza las pruebas unitarias, las pruebas de integración ni las verificaciones específicas de seguridad.

## 11. Actualización de la documentación

Cuando se incorporan nuevos controladores, DTO y anotaciones, Springdoc puede actualizar automáticamente el documento OpenAPI.

La generación automática depende de la información disponible en el código. Las reglas de negocio complejas, los casos de error específicos y los ejemplos representativos pueden requerir documentación adicional.

Por ello, la documentación debe mantenerse junto con el código y revisarse cuando cambian los contratos de los endpoints.
