# Implementation Plan: Servicio gRPC de catálogo de productos

**Branch**: `arqweb-u4/grpc` (feature `001-grpc-productos`) | **Date**: 2026-09-12 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/001-grpc-productos/spec.md`

## Summary

Publicar el CRUD de `Producto` por gRPC como cuarto adaptador de entrada del hexágono, sin tocar dominio, casos de uso ni persistencia. El contrato es `src/main/proto/productos.proto` (schema-first, como `schema.graphqls`); los stubs Java los genera el plugin `com.google.protobuf` en el build; el servicio se implementa en `infrastructure/input/grpc/` con Spring gRPC 0.12.0 (última línea compatible con Spring Boot 3.5), un `GrpcExceptionHandler` traduce las excepciones del dominio a `Status` y la reflexión del servidor permite descubrir el servicio desde grpcurl o Postman. Pruebas de integración in-process con `spring-grpc-test`.

## Technical Context

**Language/Version**: Kotlin 2.2.21 sobre JDK 17 (toolchain fijado en `build.gradle.kts`)

**Primary Dependencies**: Spring Boot 3.5.16; Spring gRPC 0.12.0 (`spring-grpc-dependencies` como BOM, `spring-grpc-server-spring-boot-starter`); grpc-java 1.76.0 y protobuf-java 4.32.1 (versiones que importa el BOM); `io.grpc:grpc-services` (reflexión); plugin Gradle `com.google.protobuf` 0.9.4 con `protoc` y `protoc-gen-grpc-java` en las versiones del BOM

**Storage**: sin cambios: JPA/Hibernate sobre H2 (perfil `local`) y PostgreSQL en Neon (perfil `neon`), esquema de Flyway

**Testing**: JUnit 5 + `@SpringBootTest`; `spring-grpc-test` con `@AutoConfigureInProcessTransport` para no abrir puerto; stub bloqueante generado sobre un canal de `GrpcChannelFactory`

**Target Platform**: JVM 17 en local (demostración) y en el contenedor de Render (solo HTTP; el puerto gRPC no se publica en esta iteración)

**Project Type**: servicio backend (web-service) con varios adaptadores de entrada

**Performance Goals**: sin meta numérica en la spec; las seis operaciones deben responder de inmediato para un catálogo de decenas de productos en la demostración

**Constraints**: no subir Spring Boot a 4.x; comentarios sin tildes en `.kt`, `.proto` y `.yml`; el núcleo no importa Spring; nada de SQL manual; puerto gRPC configurable por `GRPC_PORT`, por defecto 9090

**Scale/Scope**: un servicio, seis operaciones unarias, dos enumeraciones y siete mensajes; catálogo de ejemplo de 18 productos y 8 categorías

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Regla de la constitución | Cómo la cumple este plan | Estado |
|---|---|---|
| Stack: Boot 3.5.16, no 4.x; Spring gRPC 0.12.x | BOM `spring-grpc-dependencies:0.12.0`, construido sobre Boot 3.5.6 | ✅ |
| El núcleo no conoce a Spring | Solo se agregan archivos bajo `infrastructure/input/grpc/` y `src/main/proto/` | ✅ |
| Un puerto de entrada, N adaptadores | El servicio gRPC recibe `GestionarProductosUseCase` por constructor y nada más | ✅ |
| Invariantes en el dominio, no en el adaptador | El mapeador construye `Producto` y deja que el `init` valide; solo convierte tipos | ✅ |
| Cada adaptador es dueño de su contrato | Mensajes del `.proto` distintos de los DTO REST y GraphQL; el mapeador vive en `grpc/mapper` | ✅ |
| Errores traducidos en la frontera, sin filtrar internos | Bean `GrpcExceptionHandler` que mapea las tres excepciones del dominio y devuelve null para el resto (Spring gRPC responde UNKNOWN) | ✅ |
| Comentarios sin tildes en código | Aplica a `.kt`, `.proto`, `.yml` nuevos | ✅ |
| Pruebas de integración por adaptador contra H2 | `ProductoGrpcAdapterTest` in-process, seis operaciones y tres errores | ✅ |
| ADR por tecnología nueva, enlazado desde README y CLAUDE.md | `docs/adr/0007-grpc-como-cuarto-adaptador-de-entrada.md` | ✅ |
| Cero código sombra | Sin streaming, sin TLS, sin cliente, sin despliegue: todo fuera de alcance en la spec | ✅ |

Sin violaciones; la tabla de Complexity Tracking queda vacía.

## Project Structure

### Documentation (this feature)

```text
specs/001-grpc-productos/
├── plan.md              # Este archivo
├── research.md          # Fase 0: decisiones y alternativas
├── data-model.md        # Fase 1: entidades y mapeo dominio <-> mensajes
├── quickstart.md        # Fase 1: cómo arrancar y validar de punta a punta
├── contracts/
│   └── productos.proto  # Fase 1: el contrato del servicio (copia de referencia del que va en src/main/proto)
└── tasks.md             # Fase 2: lo genera /speckit-tasks
```

### Source Code (repository root)

```text
build.gradle.kts                                  # plugin protobuf, BOM y dependencias de Spring gRPC
src/main/proto/
└── productos.proto                               # contrato del servicio (fuente de los stubs)
src/main/kotlin/co/edu/poli/productos/infrastructure/input/grpc/
├── ProductoGrpcAdapter.kt                        # @Service que extiende ProductosServiceImplBase
├── mapper/ProductoGrpcMapper.kt                  # Producto <-> mensajes; precio String <-> BigDecimal
└── error/ManejadorDeErroresGrpc.kt               # bean GrpcExceptionHandler
src/main/resources/application.yml                # spring.grpc.server.port, reflexion
src/test/kotlin/co/edu/poli/productos/infrastructure/input/grpc/
└── ProductoGrpcAdapterTest.kt                    # @SpringBootTest + @AutoConfigureInProcessTransport
docs/
├── adr/0007-grpc-como-cuarto-adaptador-de-entrada.md
└── grpc-llamadas.md                              # guia de llamadas grpcurl para el video
README.md, CLAUDE.md                              # secciones nuevas de gRPC
```

**Structure Decision**: el adaptador sigue exactamente la estructura de `rest/` y `graphql/` (adaptador, `mapper`, `error`). No hay subpaquete `dto` porque los tipos del borde los genera protoc a partir del contrato en `build/generated/source/proto/main/{java,grpc}`; ese directorio es el equivalente de `dto/` y no se versiona.

## Complexity Tracking

Sin violaciones de la constitución que justificar.
