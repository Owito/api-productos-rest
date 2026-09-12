# Tasks: Servicio gRPC de catálogo de productos

**Input**: Design documents from `/specs/001-grpc-productos/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/productos.proto, quickstart.md

**Tests**: la spec los exige (FR-013): pruebas de integración in-process que cubran las seis operaciones y los tres errores del dominio, con la suite previa intacta.

**Organization**: tareas agrupadas por historia de usuario; cada historia es demostrable sola una vez completada la fase 2.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede ir en paralelo (archivos distintos, sin dependencias)
- **[Story]**: historia a la que pertenece (US1, US2, US3)

## Path Conventions

Proyecto único: `src/main/kotlin/co/edu/poli/productos/`, `src/main/proto/`, `src/test/kotlin/...`, `docs/`.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: dependencias y generación de código

- [x] T001 Agregar a `build.gradle.kts` el plugin `com.google.protobuf` 0.9.4, el BOM `org.springframework.grpc:spring-grpc-dependencies:0.12.0` vía `dependencyManagement`, `spring-grpc-server-spring-boot-starter`, `io.grpc:grpc-services` y `spring-grpc-test` (test), y el bloque `protobuf {}` con `protoc` y `protoc-gen-grpc-java` en las versiones importadas del BOM y `@generated=omit`
- [x] T002 [P] Crear el contrato `src/main/proto/productos.proto` idéntico a `specs/001-grpc-productos/contracts/productos.proto`
- [x] T003 [P] Agregar en `src/main/resources/application.yml` el bloque `spring.grpc.server` con `port: ${GRPC_PORT:9090}` y `reflection.enabled: true`, comentado sin tildes
- [x] T004 Verificar que `./gradlew compileKotlin` genera los stubs en `build/generated/source/proto/main/{java,grpc}` y compila

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: el mapeo y la traducción de errores, que usan las tres historias

**⚠️ CRITICAL**: ninguna historia se implementa antes de esta fase

- [x] T005 [P] Crear `infrastructure/input/grpc/mapper/ProductoGrpcMapper.kt` con las reglas de data-model.md: `aDominio(ProductoInput)`, `aMensaje(Producto)`, `categoriaDe(ListarProductosRequest)`, `aCategoriaInfo(Categoria)`; precio texto → `BigDecimal` con `DatosDeProductoInvalidosException("El precio debe ser un numero decimal")` si no parsea
- [x] T006 [P] Crear `infrastructure/input/grpc/error/ManejadorDeErroresGrpc.kt`: `@Component` que implementa `GrpcExceptionHandler` mapeando las tres excepciones del dominio a `INVALID_ARGUMENT`, `NOT_FOUND`, `ALREADY_EXISTS` con el mensaje como descripción y devolviendo null para el resto
- [x] T007 Crear `infrastructure/input/grpc/ProductoGrpcAdapter.kt`: `@Service` que extiende `ProductosServiceGrpc.ProductosServiceImplBase`, recibe `GestionarProductosUseCase` por constructor, con los seis métodos delegando en el mapeador y el caso de uso (sin lógica de negocio)

**Checkpoint**: la aplicación arranca con el servidor gRPC en 9090 y `grpcurl list` muestra el servicio

---

## Phase 3: User Story 1 - Consultar el catálogo por gRPC (Priority: P1) 🎯 MVP

**Goal**: listar (con y sin categoría), obtener por id y listar categorías

**Independent Test**: con el catálogo sembrado, las tres operaciones de lectura devuelven 18 productos, un producto por id y 8 categorías

### Tests for User Story 1

- [x] T008 [US1] Crear `src/test/kotlin/co/edu/poli/productos/infrastructure/input/grpc/ProductoGrpcAdapterTest.kt` con `@SpringBootTest` + `@AutoConfigureInProcessTransport`, stub bloqueante sobre `GrpcChannelFactory.createChannel("0.0.0.0:0")`, limpieza de tabla en `@BeforeEach` y las pruebas: listar ordenado por id, filtrar por categoría, obtener existente con sus seis campos, obtener inexistente → `NOT_FOUND` con el id en el mensaje, listar 8 categorías con etiqueta

### Implementation for User Story 1

- [x] T009 [US1] Implementar en `ProductoGrpcAdapter` `listarProductos`, `obtenerProducto` y `listarCategorias` (ya cubierto por T007; verificar contra T008)

**Checkpoint**: T008 en verde

---

## Phase 4: User Story 2 - Crear, actualizar y eliminar productos por gRPC (Priority: P2)

**Goal**: alta, cambio y baja con los tres errores del dominio

**Independent Test**: sobre base vacía, crear, actualizar y eliminar verificando cada paso por lectura

### Tests for User Story 2

- [x] T010 [US2] Agregar a `ProductoGrpcAdapterTest`: crear devuelve id y datos; actualizar conserva id y cambia datos; eliminar responde vacío y luego `NOT_FOUND`; duplicado con distinta capitalización → `ALREADY_EXISTS`; nombre vacío → `INVALID_ARGUMENT`; precio "abc" → `INVALID_ARGUMENT` con "numero decimal"; categoría sin especificar → `INVALID_ARGUMENT` "La categoria es obligatoria"; actualizar y eliminar inexistente → `NOT_FOUND`

### Implementation for User Story 2

- [x] T011 [US2] Implementar en `ProductoGrpcAdapter` `crearProducto`, `actualizarProducto` y `eliminarProducto` (ya cubierto por T007; verificar contra T010)

**Checkpoint**: T008 y T010 en verde; `./gradlew test` completo en verde con las 54 previas

---

## Phase 5: User Story 3 - Descubrir y probar el servicio con una herramienta (Priority: P3)

**Goal**: reflexión activa, guía de llamadas y repositorio ordenado

**Independent Test**: `grpcurl -plaintext localhost:9090 list` muestra el servicio y cada llamada de la guía corre tal cual

### Implementation for User Story 3

- [x] T012 [P] [US3] Escribir `docs/grpc-llamadas.md` con las llamadas de quickstart.md secciones 3 a 6 en el orden del video, con la respuesta esperada de cada una
- [x] T013 [P] [US3] Escribir `docs/adr/0007-grpc-como-cuarto-adaptador-de-entrada.md` con el patrón de los ADR anteriores (contexto, decisión, consecuencias, alternativas: net.devh, grpc-java a mano, gRPC sobre servlet, dos repositorios)
- [x] T014 [US3] Verificar con `git status` que `build/generated` no se versiona (ya cubierto por `build/` en `.gitignore`) y que no entran secretos ni artefactos

**Checkpoint**: la guía se ejecuta contra la aplicación arrancada en local

---

## Phase N: Polish & Cross-Cutting Concerns

- [x] T015 [P] Actualizar `README.md`: descripción con cuatro adaptadores, fila de gRPC en la tabla de stack, sección de uso con grpcurl y Postman, enlace al ADR 0007 y a la guía
- [x] T016 [P] Actualizar `CLAUDE.md`: comando de arranque con el puerto gRPC, estructura `infrastructure/input/grpc`, dónde viven los stubs generados, el manejador de errores gRPC en la lista de manejadores, conteo de pruebas y trampas nuevas
- [x] T017 Ejecutar la validación de `quickstart.md` de punta a punta (bootRun + grpcurl) y registrar los resultados como evals en `specs/001-grpc-productos/evals.md`
- [x] T018 Confirmar SC-004 con `git diff --stat main -- src/main/kotlin/.../domain .../application .../infrastructure/output` sin salida

---

## Dependencies & Execution Order

- **Setup (Phase 1)**: T001 primero; T002 y T003 en paralelo; T004 al final.
- **Foundational (Phase 2)**: depende de T004 (los stubs deben existir). T005 y T006 en paralelo; T007 después.
- **US1 (Phase 3)** y **US2 (Phase 4)**: dependen de la fase 2; sus pruebas van en el mismo archivo, así que se escriben en orden (T008, luego T010).
- **US3 (Phase 5)**: T012 y T013 en paralelo desde el inicio de la fase 2; T014 al final.
- **Polish**: después de todas las historias. T017 exige la aplicación corriendo.

## Notes

- Los stubs generados viven en `build/generated/source/proto/main` y no se versionan: el `.proto` es la fuente.
- Cero código sombra: nada de streaming, TLS, cliente gRPC ni despliegue.
- Un commit por fase, en español e imperativo.
