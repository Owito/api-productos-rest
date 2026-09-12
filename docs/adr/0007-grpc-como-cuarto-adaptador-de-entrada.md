# ADR 0007: gRPC como cuarto adaptador de entrada, en un puerto propio

- **Estado:** aceptada
- **Fecha:** 2026-09-12

## Contexto

La Unidad 4 del módulo pide construir servicios backend con GraphQL y con gRPC para hacer CRUD
sobre `Producto`, con framework, ORM y repositorio en GitHub, y compararlos en un video frente al
enfoque REST. El proyecto ya publica el catálogo por tres adaptadores de entrada (REST, web y
GraphQL, ADR 0002 y 0006) sobre un único puerto de aplicación, `GestionarProductosUseCase`.

Tres preguntas de diseño, en orden de importancia:

1. **¿Dónde se enchufa gRPC?** Igual que con GraphQL, la tentación es tratarlo como "otra
   aplicación". El enunciado habla de "dos aplicaciones", pero lo que evalúa la rúbrica es la
   arquitectura de la solución, la implementación con ORM, las pruebas y el repositorio.
2. **¿Con qué librería?** Spring Boot 3.5 no trae gRPC de fábrica. Hay dos integraciones vivas:
   el proyecto oficial Spring gRPC (`org.springframework.grpc`) y el starter del ecosistema
   `net.devh:grpc-server-spring-boot-starter`.
3. **¿En qué puerto?** gRPC corre sobre HTTP/2. Puede vivir en un servidor Netty propio o dentro
   del contenedor de servlets compartiendo el puerto de Tomcat.

## Decisión

gRPC entra como **cuarto adaptador de entrada**, en `infrastructure/input/grpc`, con la misma
estructura que los otros tres: el contrato es `src/main/proto/productos.proto`, los mensajes los
genera protoc en el build (son el equivalente de los DTO), el mapeador vive en `grpc/mapper` y el
traductor de errores en `grpc/error`. El núcleo no cambió ni una línea: dominio, casos de uso,
puerto de salida y persistencia son los mismos que sirven a REST, web y GraphQL. Esa es la prueba
de que el hexágono está bien cerrado, y es lo que se muestra en el video.

**Librería: Spring gRPC 0.12.0**, importando su BOM `spring-grpc-dependencies`. Es el proyecto
oficial de Spring, y la línea 0.12 es la última construida sobre Spring Boot 3.5 (su
autoconfiguración declara Boot 3.5.6). La línea 1.x exige Spring Boot 4, que este proyecto no
adopta (ver "Trampas conocidas" en la guía del repositorio). El BOM fija también grpc-java 1.76 y
protobuf-java 4.32, y el plugin de Gradle `com.google.protobuf` 0.9.4 toma `protoc` y
`protoc-gen-grpc-java` en esas mismas versiones para que el código generado y la librería no se
desalineen.

**Puerto propio: 9090** (variable `GRPC_PORT`), servidor Netty nativo junto al Tomcat de 8080.
En la demostración queda claro que son dos servicios de comunicación distintos sobre el mismo
núcleo. El despliegue en Render no cambia: sigue publicando solo el puerto HTTP, y gRPC se
demuestra en local.

Piezas concretas:

| Pieza | Ruta | Papel |
|---|---|---|
| `spring-grpc-server-spring-boot-starter` | `build.gradle.kts` | Servidor Netty, registro automático de los servicios, `GrpcExceptionHandler` |
| `com.google.protobuf` (plugin) | `build.gradle.kts` | Compila el `.proto` y genera los stubs en `build/generated/source/proto/main`, que no se versionan |
| `productos.proto` | `src/main/proto/` | El contrato: seis operaciones unarias, dos enumeraciones y siete mensajes |
| `ProductoGrpcAdapter` | `infrastructure/input/grpc` | `@Service` que extiende el esqueleto generado y depende solo del puerto |
| `ProductoGrpcMapper` | `.../grpc/mapper` | Mensajes ↔ dominio; precio texto ↔ `BigDecimal`; categoría 0 ↔ "obligatoria" |
| `ManejadorDeErroresGrpc` | `.../grpc/error` | `GrpcExceptionHandler`: `NOT_FOUND`, `ALREADY_EXISTS`, `INVALID_ARGUMENT`; el resto sale `UNKNOWN` sin mensaje |
| `io.grpc:grpc-services` + `spring.grpc.server.reflection.enabled` | `build.gradle.kts`, `application.yml` | Reflexión: grpcurl y Postman descubren el servicio sin el `.proto`, el análogo de `/graphql/schema` |
| `spring-grpc-test` | pruebas | Transporte in-process: cliente y servidor en memoria, sin puerto |

Dos decisiones de contrato que conviene conocer:

- **El precio viaja como texto decimal.** proto3 no tiene tipo decimal y `double` perdería
  precisión con dinero. Es la misma decisión que tomó el adaptador GraphQL al declarar
  `BigDecimal` detrás del escalar `Float`. Un texto que no es un número se reporta como
  `INVALID_ARGUMENT` por el mismo camino que un precio negativo.
- **El valor 0 de la enumeración `Categoria` está reservado.** En proto3 es lo que recibe el
  servidor cuando el cliente no manda el campo, así que se llama `CATEGORIA_SIN_ESPECIFICAR` y el
  mapeador lo traduce a la regla del dominio "la categoria es obligatoria", igual que REST cuando
  la categoría viene vacía.

## Consecuencias

- Cuatro adaptadores, un núcleo. Agregar una operación sigue siendo tocar el puerto y el
  servicio, y cada adaptador la expone a su manera.
- El build ahora genera código: `./gradlew compileKotlin` compila primero el `.proto`. En el IDE
  hay que marcar `build/generated/source/proto/main/{java,grpc}` como raíces de fuentes generadas
  si no lo detecta solo.
- La suite crece con las pruebas in-process del adaptador, que atraviesan Spring, Flyway y H2
  como las demás pruebas de adaptador, sin abrir puertos.
- Render sigue sin exponer gRPC. Si algún día hiciera falta, la vía es
  `spring-grpc-server-web-spring-boot-starter` (gRPC sobre el servlet, mismo puerto que HTTP),
  documentada como alternativa abajo.
- Spring Boot queda un poco más atado a la 3.5: subir a 4.x obligaría a saltar a Spring gRPC 1.x.

## Alternativas descartadas

- **`net.devh:grpc-server-spring-boot-starter` 3.1.0.** Maduro y compatible con Boot 3.x, pero
  duplica lo que ya ofrece el proyecto oficial y su modelo de errores (`@GrpcAdvice`) es ajeno a
  Spring gRPC. Si Spring gRPC no existiera, sería la elección.
- **grpc-java a mano** (levantar `ServerBuilder` en un `@Bean`). Obliga a reinventar ciclo de
  vida, puerto, manejo de errores y pruebas que el starter ya resuelve.
- **gRPC sobre el servlet** (mismo puerto 8080). Serviría para exponerlo en Render, que publica
  un solo puerto, pero exige HTTP/2 en Tomcat y limita la configuración del servidor. Para una
  demostración local, dos puertos son más claros.
- **Un segundo repositorio con una aplicación aparte.** Cumpliría la lectura literal de "dos
  aplicaciones", pero duplicaría dominio, persistencia y migraciones, y contradiría el argumento
  central de la arquitectura ya calificada en la Unidad 2. Si el tutor lo exige, el adaptador se
  puede mover a un repositorio propio sin tocar el núcleo; la decisión queda registrada como
  pregunta abierta en la spec de la feature.
- **Stubs Kotlin con corrutinas (`grpc-kotlin`).** Están en el BOM, pero agregan un segundo
  generador y un modelo asíncrono que la spec no pide.
