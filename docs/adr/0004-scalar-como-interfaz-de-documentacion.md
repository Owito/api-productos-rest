# ADR 0004: Scalar como interfaz de documentación, en vez de Swagger UI

- **Estado:** aceptada
- **Fecha:** 2026-08-20

## Contexto

La API tiene que documentarse sola y dejar probar los endpoints desde el navegador, sin pedirle a
quien la revisa que instale Postman ni que arme peticiones a mano.

Conviene separar dos cosas que suelen confundirse en un solo nombre:

1. **La especificación.** El documento OpenAPI que describe rutas, esquemas y códigos de respuesta.
   Aquí lo genera **springdoc** leyendo los controladores y los DTO, y lo publica en `/v3/api-docs`.
2. **La interfaz.** La página que lee esa especificación y la vuelve navegable y ejecutable.

La primera es el contrato y no se toca. La segunda es reemplazable: es un consumidor más del
documento. El proyecto arrancó con Swagger UI porque viene empaquetada en el artefacto
`springdoc-openapi-starter-webmvc-ui`, que es el camino por defecto.

## Decisión

La especificación se sigue generando con springdoc, ahora desde el artefacto
`springdoc-openapi-starter-webmvc-api`, que **produce el documento y no trae interfaz**.

La interfaz es **Scalar** (`com.scalar.maven:scalar-webmvc`), configurada en `application.yml` y
servida en **`/docs`**, apuntando a `/v3/api-docs`.

La ruta es `/docs` y no `/scalar` a propósito: nombra la función, no la herramienta. Si mañana
entra otra interfaz, cambia una línea de configuración y la URL que está en el README, en los
créditos y en el navegador del catálogo sigue siendo válida.

## Alternativas consideradas

| Opción | A favor | En contra |
|---|---|---|
| **Scalar sobre el documento de springdoc** | Interfaz moderna con cliente HTTP integrado y ejemplos de código en varios lenguajes; el paquete trae su propio JavaScript, así que la página no depende de una CDN; se configura por propiedades, sin escribir código | Una dependencia más que mantener, y es un proyecto más joven que Swagger UI |
| Swagger UI (lo que había) | Es el camino por defecto de springdoc y lo que todo el mundo reconoce | Interfaz anticuada y densa; se sirve por *webjars* a través del manejador de recursos estáticos, lo que ya había obligado a una restricción en la configuración de Spring MVC |
| Redoc | Lectura muy limpia | Es solo lectura: no permite ejecutar peticiones, que es justo lo que se quiere mostrar |
| Página propia con el JavaScript de Scalar por CDN | Control total del HTML | Agrega una dependencia externa en tiempo de ejecución y código que mantener, para lograr lo mismo que ya resuelve el arranque automático |

## Consecuencias

- El documento OpenAPI queda expuesto en `/v3/api-docs` y **cualquier herramienta puede
  consumirlo**: Scalar, Postman al importarlo, o un generador de clientes. La interfaz dejó de ser
  el producto; el producto es la especificación.
- Desaparecen los *webjars* de Swagger UI del `.jar` final. Scalar sirve su propio bundle desde dos
  rutas de controlador (`/docs` y `/docs/scalar.js`), no desde el manejador de recursos estáticos.
- La página **no llama a ningún servicio externo**: la telemetría y el asistente de IA que Scalar
  trae encendidos por defecto quedan apagados en `application.yml`. Solo habla con esta aplicación.
- El estilo de la documentación se alinea con el de la interfaz web mediante `custom-css`, con la
  misma base oscura y el mismo verde de acento, para que las dos caras del proyecto se lean como un
  mismo producto.
- El orden de las operaciones por método HTTP, que ya se usaba en Swagger UI, se conserva con
  `operation-sorter: method`.
