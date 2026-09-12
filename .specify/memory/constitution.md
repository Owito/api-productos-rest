# Constitución del Proyecto: API de Productos

Fuente de cada regla: `CLAUDE.md` del repositorio, los ADR de `docs/adr/`, `build.gradle.kts` y
`src/main/resources/application.yml`. Nada de lo que sigue se inventó para este documento: si una
regla no tiene origen en el código o en su documentación, va marcada `[POR DEFINIR]`.

## 1. Naturaleza del proyecto

Backend académico de catálogo de productos para el módulo Arquitectura de Aplicaciones Web
(TIC51372) de la Maestría en Arquitectura de Software del Politécnico Grancolombiano. Un solo
núcleo de dominio (`Producto`, `Categoria`) expuesto por varios adaptadores de entrada, cada uno
correspondiente a una unidad del módulo: REST y web (Unidad 2), GraphQL (Unidad 3) y gRPC
(Unidad 4). Es a la vez una aplicación desplegada de verdad en Render contra PostgreSQL en Neon.
Fuente: `CLAUDE.md` líneas 5-9, `README.md` "En línea".

## 2. Stack tecnológico (reglas de implementación)

- **Lenguaje:** Kotlin 2.2 sobre JDK 17 (`build.gradle.kts`: `kotlin("jvm") version "2.2.21"`,
  toolchain 17). Código, comentarios, nombres y mensajes de commit en español. Comentarios
  dentro de `.kt`, `.yml`, `.proto`, `Dockerfile` y `render.yaml` **sin tildes**; los Markdown sí
  las llevan (`CLAUDE.md` líneas 10-12).
- **Framework backend:** Spring Boot **3.5.16, fijado a mano; no subir a 4.x** (renombra los
  starters y cambia a Jackson 3; `CLAUDE.md` "Trampas conocidas").
- **ORM y base de datos:** Spring Data JPA sobre Hibernate. El esquema **lo declara Flyway, no el
  ORM** (`ddl-auto: validate`); scripts por motor en `db/migration/{h2,postgresql}` y nunca se
  edita una migración aplicada (ADR 0005). H2 en memoria en el perfil `local`, PostgreSQL en Neon
  en el perfil `neon`.
- **Adaptadores de entrada existentes:** REST (`/api/v1/productos`, springdoc + Scalar en
  `/docs`, ADR 0004), web Thymeleaf (`/productos`), GraphQL (`/graphql`, GraphiQL en
  `/graphiql`, esquema como contrato, ADR 0006).
- **gRPC (Unidad 4):** Spring gRPC (`org.springframework.grpc`) en la línea **0.12.x**, la última
  construida sobre Spring Boot 3.5 (verificado en el POM de
  `spring-grpc-spring-boot-autoconfigure:0.12.0`, que declara Boot 3.5.6). La línea 1.x exige
  Boot 4 y por tanto queda prohibida por la regla anterior. Stubs Java generados con el plugin
  `com.google.protobuf` desde `src/main/proto`; el `.proto` es el contrato, igual que
  `schema.graphqls` lo es para GraphQL.
- **Infra y despliegue:** Gradle Wrapper (Kotlin DSL), Docker multietapa, Render plan gratuito
  desde `render.yaml` con despliegue manual. El chequeo de salud de la plataforma apunta a
  `/actuator/health/liveness` y **nunca consulta la base de datos** (`CLAUDE.md` "Trampas").

## 3. Reglas de dominio y lógica de negocio (inmutables)

- **El núcleo no conoce a Spring.** `domain/` y `application/` son Kotlin puro; el caso de uso se
  registra como `@Bean` en `infrastructure/config/ConfiguracionDeCasosDeUso` y usa
  `jakarta.transaction.Transactional`, declarada en el bloque `allOpen` (ADR 0002, `CLAUDE.md`
  "Detalles del cableado").
- **Un solo puerto de entrada, N adaptadores encima.** Todo adaptador de entrada depende de
  `GestionarProductosUseCase` y nunca del repositorio ni de otro adaptador. Cualquier lógica de
  negocio dentro de un adaptador está en el lugar equivocado.
- **Invariantes de `Producto`:** nombre obligatorio de máximo 120 caracteres, descripción de
  máximo 500, precio estrictamente mayor que cero, categoría del catálogo cerrado
  (`domain/model/Producto.kt`, `Categoria.kt`). Las valida el propio modelo en `init`; los
  adaptadores no las duplican, las traducen.
- **Nombre único sin importar mayúsculas**, defendido en dos capas: el caso de uso responde un
  conflicto legible y la restricción `uk_productos_nombre` resiste la concurrencia
  (`CLAUDE.md` "Trampas").
- **Cada adaptador es dueño de su contrato:** modelo de dominio, entidad JPA y DTO de cada borde
  son tres cosas distintas y no se colapsan (`CLAUDE.md` "Hay tres modelos distintos").
- **Autenticación y roles:** no hay. La demo pública queda sin autenticación por decisión del
  18 de agosto de 2026 (ficha del módulo); no se agrega en ninguna unidad sin una spec propia.

## 4. Estructura y estilo de código

- **Estructura:** hexagonal por paquetes `domain/`, `application/{port,service}`,
  `infrastructure/{input/<adaptador>,output/persistence,config}`. Un adaptador de entrada nuevo
  vive en `infrastructure/input/<nombre>/` con sus subpaquetes `dto`, `mapper` y `error`, como
  `rest/` y `graphql/`.
- **Nomenclatura:** identificadores en español y camelCase; clases de adaptador terminan en
  `Adapter`; los manejadores de error en `ManejadorDe...`; los mapeadores son `object`.
- **Decisiones de arquitectura:** cada tecnología nueva lleva su ADR numerado en `docs/adr/`
  (0001 a 0006 existentes) y se enlaza desde el README y desde `CLAUDE.md`.
- **Pruebas:** la suite completa debe seguir en verde (54 al inicio de esta feature, verificado
  con `./gradlew test`). Todo adaptador de entrada trae pruebas de integración `@SpringBootTest`
  contra H2 que cubren los cuatro verbos y sus errores (`CLAUDE.md` "Pruebas"); las del núcleo
  no levantan Spring.

## 5. Manejo de errores y validaciones

- **Las excepciones del dominio no conocen el protocolo.** `ProductoNoEncontradoException`,
  `NombreDeProductoDuplicadoException` y `DatosDeProductoInvalidosException` se traducen en la
  frontera de cada adaptador, con un manejador acotado a ese adaptador (`CLAUDE.md` "Tres
  manejadores de errores"): REST → 404, 409, 400; GraphQL → `NOT_FOUND`, `BAD_REQUEST`;
  gRPC → códigos `Status` equivalentes (`NOT_FOUND`, `ALREADY_EXISTS`, `INVALID_ARGUMENT`).
- **Nunca se filtra la traza de pila ni el mensaje de un error interno** al cliente
  (`server.error.include-stacktrace: never`; el manejador GraphQL deja pasar lo que no es del
  dominio para que salga como error interno genérico). Un manejador gRPC sigue la misma regla.
- **Los secretos nunca entran al repositorio:** `.env` está ignorado, `render.yaml` marca la
  contraseña con `sync: false`.

## 6. Comportamiento del agente de IA (reglas SDD)

- **Cero código sombra:** se construye estrictamente lo documentado en `specs/<feature>/spec.md`.
  Nada "por si acaso", ni streaming, ni autenticación, ni un cliente gRPC si la spec no lo pide.
- **Fuente de la verdad:** si una instrucción contradice esta constitución o la spec, o se detecta
  una falla lógica, se detiene el trabajo, se advierte y se pide actualizar el artefacto antes de
  tocar código.
- **Un cambio nunca entra por el código:** entra por el artefacto más alto que toca (constitución,
  spec o plan) y se regenera hacia abajo.
- **Evidencia:** todo eval reportado como "pasa" se ejecutó y se vio el resultado; lo no ejecutado
  se marca como no verificado con el comando que lo resolvería.
- **Documentación viva:** `CLAUDE.md`, `README.md` y los ADR se actualizan en la misma feature que
  cambia el comportamiento que describen.

## Gobernanza

Esta constitución prevalece sobre cualquier otra práctica del repositorio. Las enmiendas se
documentan aquí, se versionan y se propagan a specs, plan y tareas.
**Versión:** 1.0.0 · **Ratificada:** 2026-09-12 · **Última enmienda:** 2026-09-12
