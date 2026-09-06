# ADR 0006: GraphQL como tercer adaptador de entrada, no como reemplazo de REST

- **Estado:** aceptada
- **Fecha:** 2026-09-06

## Contexto

La unidad 3 del módulo pide integrar la librería GraphQL en un framework de desarrollo web. El
proyecto ya publica el catálogo de productos por dos frentes: una API REST en `/api/v1/productos`
y una interfaz web renderizada en el servidor con Thymeleaf. Ambos entran por el mismo puerto de
aplicación, `GestionarProductosUseCase`, según el ADR 0002.

La pregunta de diseño no es "cómo se instala GraphQL", que es una línea en `build.gradle.kts`,
sino **dónde se enchufa**. Hay dos lecturas posibles:

1. GraphQL es una forma distinta de exponer lo mismo, así que es otro adaptador de entrada.
2. GraphQL sustituye a REST, porque resuelve el mismo problema mejor.

La segunda lectura es la que circula con más frecuencia y es la que este ADR rechaza.

## Decisión

GraphQL entra como **tercer adaptador de entrada**, en
`infrastructure/input/graphql`, con la misma estructura que ya tienen los otros dos: sus propios
DTO, su propio mapeador hacia el dominio y su propio traductor de errores.

**REST se queda.** Los dos protocolos conviven sobre el mismo núcleo, que no cambió ni una línea
para que esto funcionara.

Piezas concretas:

| Pieza | Ruta | Papel |
|---|---|---|
| `spring-boot-starter-graphql` | `build.gradle.kts` | Empaqueta GraphQL Java (el motor), Spring for GraphQL (el puente con el contenedor) y el transporte HTTP |
| `schema.graphqls` | `src/main/resources/graphql/` | El contrato. Spring falla al arrancar si un campo declarado aquí no tiene resolver |
| `ProductoGraphQlAdapter` | `infrastructure/input/graphql` | `@Controller` con `@QueryMapping` y `@MutationMapping`, dependiendo solo del puerto |
| `ManejadorDeErroresGraphQl` | `.../graphql/error` | Traduce las excepciones de dominio a `NOT_FOUND` y `BAD_REQUEST` tipados |
| Endpoint | `/graphql` | Punto único de entrada |
| GraphiQL | `/graphiql` | Consola interactiva, el análogo de `/docs` para el lado REST |
| Esquema publicado | `/graphql/schema` | El análogo de `/v3/api-docs` |

Dos decisiones menores que se derivan de esta:

- **Las categorías viajan como `enum` del esquema, no como texto libre.** El catálogo es cerrado
  (ADR 0003), así que una categoría inexistente la rechaza el motor de GraphQL antes de llegar al
  dominio. En REST el mismo caso llega hasta `Categoria.desde()` y sale como 400.
- **`producto(id:)` devuelve `null` cuando no existe, en vez de un error.** Es la convención de
  GraphQL para una ausencia, y contrasta a propósito con el 404 del adaptador REST. Que dos
  adaptadores reporten distinto el mismo hecho del dominio es exactamente lo que la arquitectura
  hexagonal permite: la política de reporte pertenece a la frontera, no al núcleo.

## Alternativas consideradas

| Opción | A favor | En contra |
|---|---|---|
| **GraphQL como tercer adaptador, junto a REST** | Demuestra en la práctica que el hexágono está cerrado: cero cambios en dominio y aplicación; permite comparar los dos estilos sobre el mismo caso | Dos contratos que mantener, y dos superficies que asegurar |
| Reemplazar REST por GraphQL | Un solo contrato | Se pierde la evidencia de la unidad 2, se rompen la especificación OpenAPI y los clientes existentes, y no hay ninguna necesidad del caso que lo justifique |
| GraphQL sobre un servicio nuevo, aparte | Aísla el riesgo | Duplicaría el dominio o obligaría a una llamada de red interna para llegar al mismo caso de uso |
| Exponer los repositorios de JPA directamente al esquema | Menos código | Rompe el ADR 0002: el adaptador de entrada quedaría hablando con la persistencia y saltándose las invariantes del dominio |

## Consecuencias

- El núcleo quedó verificado por segunda vez: publicar un protocolo nuevo costó un adaptador y
  ningún cambio en el dominio, en el servicio ni en el repositorio.
- El filtro `springdoc.paths-to-match: /api/v1/**` sigue vigente, así que el endpoint de GraphQL
  **no** aparece en la especificación OpenAPI. Cada contrato se documenta en su propio formato.
- Las 11 pruebas nuevas de `ProductoGraphQlAdapterTest` ejecutan contra el esquema real, así que
  un desajuste entre el `.graphqls` y los resolvers rompe la construcción.
- **Deuda conocida:** el precio se declara como `Float` en el esquema y viaja como `Double`, que
  no es el tipo correcto para dinero. En este proyecto el efecto es nulo porque los precios tienen
  dos decimales y se guardan como `NUMERIC` en la base, pero un catálogo con más precisión
  necesitaría un escalar `BigDecimal` propio, que hoy exigiría la dependencia
  `graphql-java-extended-scalars`. Se deja anotado y no se resuelve aquí.
- **Superficie nueva:** `/graphql` acepta consultas anidadas arbitrarias. El esquema actual es
  plano (no hay relaciones que recorrer), así que hoy no hay riesgo de consulta abusiva, pero en
  cuanto se agregue una relación habrá que limitar la profundidad y la complejidad.
