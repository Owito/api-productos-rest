# Research: Servicio gRPC de catálogo de productos

Fase 0 del plan. Cada decisión trae su fundamento verificado en esta sesión (2026-09-12) y las alternativas descartadas.

## 1. Librería de integración gRPC con Spring Boot

- **Decision**: Spring gRPC 0.12.0 (`org.springframework.grpc`), importando el BOM `spring-grpc-dependencies:0.12.0` y usando `spring-grpc-server-spring-boot-starter`.
- **Rationale**: es el proyecto oficial de Spring para gRPC. El POM de `spring-grpc-spring-boot-autoconfigure:0.12.0` declara Spring Boot 3.5.6, así que es la última línea compatible con el 3.5.16 del proyecto (verificado en repo1.maven.org). La línea 1.x (1.0.0 a 1.1.1) exige Spring Boot 4, prohibido por la constitución. Trae autoconfiguración del servidor Netty, registro automático de los beans `BindableService`, `GrpcExceptionHandler` para traducir excepciones y `spring-grpc-test` para pruebas in-process.
- **Alternatives considered**: `net.devh:grpc-server-spring-boot-starter:3.1.0.RELEASE` (ecosistema, maduro, también compatible con Boot 3.x) se descartó porque duplica lo que hoy ofrece el proyecto oficial y su modelo de errores (`@GrpcAdvice`) es ajeno a Spring gRPC; grpc-java "a mano" (levantar `ServerBuilder` en un `@Bean`) se descartó porque obliga a reinventar ciclo de vida, puerto y pruebas que el starter ya resuelve.

## 2. Generación de los stubs

- **Decision**: plugin Gradle `com.google.protobuf` 0.9.4, con `protoc` y `protoc-gen-grpc-java` en las versiones que importa el BOM (`protobuf-java.version` 4.32.1 y `grpc.version` 1.76.0, leídas de `spring-grpc-dependencies-0.12.0.pom`). Solo generador Java (`builtins { java }` + plugin `grpc`), con la opción `@generated=omit` que usa el ejemplo oficial para no depender de `javax.annotation`.
- **Rationale**: es la configuración del ejemplo `samples/grpc-server` de Spring gRPC 0.12.0; tomar las versiones del BOM evita desalinear protoc con la librería en tiempo de ejecución. Los stubs Java se consumen desde Kotlin sin fricción (`ProductosServiceGrpc.ProductosServiceImplBase`).
- **Alternatives considered**: generar también stubs Kotlin (`grpc-kotlin` 1.4.3, en el BOM) daría corrutinas, pero agrega un segundo generador y un modelo asíncrono que la spec no pide; se descartó por cero código sombra. Versionar los stubs generados se descartó: se regeneran en cada build y el `.proto` es la única fuente de verdad.

## 3. Forma del contrato

- **Decision**: un servicio `ProductosService` con seis RPC unarias; mensajes propios (`Producto`, `ProductoInput`, `ProductoIdRequest`, `ActualizarProductoRequest`, `ListarProductosRequest/Response`, `ListarCategoriasResponse`, `CategoriaInfo`); `google.protobuf.Empty` para la baja y para la petición de categorías; enumeración `Categoria` con el valor 0 reservado como "sin especificar"; campos `optional` para descripción y para el filtro de categoría; precio como `string`.
- **Rationale**: proto3 exige que el valor 0 de una enumeración sea el por defecto, así que un cliente que no manda categoría envía "sin especificar" y el adaptador lo traduce a la excepción del dominio "La categoria es obligatoria", igual que REST. `optional` distingue "no filtrar" de "filtrar por el valor 0". El precio en `string` conserva la precisión de `BigDecimal` (proto3 no tiene decimal; `double` perdería centavos), y es la misma decisión que tomó el adaptador GraphQL al declarar `BigDecimal` detrás del escalar `Float`.
- **Alternatives considered**: `google.type.Money` (requiere `proto-google-common-protos`, en el BOM) modela moneda y unidades enteras más nanos: más fiel pero más ceremonia para un catálogo académico; `double` se descartó por precisión; `int64` en centavos se descartó porque el dominio no fija escala.

## 4. Traducción de errores

- **Decision**: un `@Component` que implementa `GrpcExceptionHandler` (`StatusException handleException(Throwable)`): `DatosDeProductoInvalidosException` → `INVALID_ARGUMENT`, `ProductoNoEncontradoException` → `NOT_FOUND`, `NombreDeProductoDuplicadoException` → `ALREADY_EXISTS`, cada uno con el mensaje del dominio como descripción; para cualquier otra excepción devuelve `null`, y Spring gRPC responde `UNKNOWN` sin descripción. Un precio no numérico se traduce en el mapeador a `DatosDeProductoInvalidosException`, así que cae en `INVALID_ARGUMENT` por el mismo camino.
- **Rationale**: es el equivalente exacto de `ManejadorGlobalDeErrores` (REST) y `ManejadorDeErroresGraphQl`: la frontera decide el código y el dominio no conoce el protocolo. Verificado en `server.adoc` de v0.12.0 ("add @Beans of type GrpcExceptionHandler... returning null for those it does not support") y en `GrpcServerIntegrationTests` del ejemplo, donde una `RuntimeException` sin manejador sale como `Code.UNKNOWN`.
- **Alternatives considered**: capturar excepciones dentro de cada método del servicio y llamar `responseObserver.onError` repite el mismo `when` seis veces; un `ServerInterceptor` global es más bajo nivel de lo necesario.

## 5. Descubrimiento del servicio

- **Decision**: dependencia `io.grpc:grpc-services` y propiedad `spring.grpc.server.reflection.enabled=true` (Spring gRPC registra `ProtoReflectionServiceV1` cuando la clase está en el classpath).
- **Rationale**: la historia P3 pide que grpcurl y Postman listen el servicio sin el `.proto`; la reflexión es el mecanismo estándar de gRPC para eso (`grpcurl -plaintext localhost:9090 list`). Es el análogo de publicar el esquema en `/graphql/schema` y OpenAPI en `/v3/api-docs`.
- **Alternatives considered**: entregar el `.proto` a mano a cada herramienta funciona pero no cumple el escenario 1 de P3 y hace la demostración más torpe.

## 6. Puerto y convivencia con HTTP

- **Decision**: servidor Netty nativo en `spring.grpc.server.port: ${GRPC_PORT:9090}`, junto al HTTP de Tomcat en 8080. No se usa `spring-grpc-server-web-spring-boot-starter` (gRPC sobre el servlet en el mismo puerto).
- **Rationale**: dos puertos separados hacen evidente en el video que son dos servicios de comunicación distintos, y el `server.adoc` de v0.12.0 confirma que el servidor nativo "will run happily inside a web application, listening on a different port". El despliegue en Render no cambia: solo publica 8080.
- **Alternatives considered**: gRPC sobre el servlet compartiría el 8080 (útil si algún día se quisiera exponer en Render, que publica un solo puerto), pero requiere HTTP/2 en Tomcat y limita la configuración del servidor; queda como opción futura documentada en el ADR.

## 7. Pruebas

- **Decision**: `ProductoGrpcAdapterTest` con `@SpringBootTest` + `@AutoConfigureInProcessTransport` (de `spring-grpc-test`), creando el stub bloqueante con `ProductosServiceGrpc.newBlockingStub(channels.createChannel("0.0.0.0:0"))` donde `channels` es el `GrpcChannelFactory` autoconfigurado; con el transporte in-process de prueba ese canal llega al servidor sin abrir puerto (verificado en `GrpcServerIntegrationTests` de v0.12.0). Limpieza de la tabla en `@BeforeEach` con `ProductoJpaRepository.deleteAll()`, como en la prueba de GraphQL.
- **Rationale**: misma capa que las demás pruebas de adaptador (Spring completo + H2 + Flyway), sin puerto de red, así que corre en CI y en paralelo con la suite existente.
- **Alternatives considered**: `spring.grpc.server.port=0` + `@LocalGrpcPort` abre un puerto real; funciona pero es más frágil y no aporta nada frente al in-process.

## 8. Documentación

- **Decision**: ADR 0007 con el patrón de los anteriores (contexto, decisión, consecuencias, alternativas); sección "gRPC" en README y en la guía del repositorio (cómo arrancar, puerto, herramientas, trampas); `docs/grpc-llamadas.md` con una llamada `grpcurl` por operación y por error, en el orden del guion del video.
- **Rationale**: FR-012 y el criterio de rúbrica "repositorio gRPC" (25 puntos).
