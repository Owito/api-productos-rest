# CR-01: la entrega son dos repositorios, uno por aplicación

**Pedido por:** Carlos Guerra · **Fecha:** 2026-09-12

**Qué cambia:** el enunciado de la Unidad 4 pide "el backend de dos aplicaciones", y esta feature lo había interpretado como dos adaptadores de entrada sobre un mismo repositorio, dejando la duda registrada como Assumption. La decisión queda tomada: **la entrega son dos repositorios de GitHub**, uno por aplicación, y la actividad es **grupal** (Carlos Guerra, Rafael Gutiérrez Correales, Paulo Reyes Rodríguez).

| Aplicación | Repositorio | Contenido |
|---|---|---|
| GraphQL | `Owito/api-productos-rest` | El proyecto de las Unidades 2 y 3: núcleo hexagonal con adaptadores REST, web Thymeleaf y GraphQL. Es el entregable de la mitad GraphQL. |
| gRPC | `Owito/api-productos-grpc` | Aplicación propia: el mismo núcleo hexagonal con un único adaptador de entrada, el servicio gRPC. Es el entregable de la mitad gRPC. |

**Artefacto de entrada:** spec (toca una Assumption y un No objetivo, no la constitución ni el stack).

**Historias y requerimientos afectados:**

- Assumptions: la lectura de "dos aplicaciones" deja de ser supuesto y pasa a decisión.
- Out of Scope: "NO habrá un segundo repositorio" se elimina; ahora es parte del alcance.
- US3, escenario 3 y FR-012: el repositorio a revisar pasa a ser el de la aplicación gRPC.
- SC-004 y SC-005 se mantienen: el núcleo sigue sin cambiar y la guía sigue arrancando en menos de 5 minutos, ahora en el repositorio nuevo.

**Regeneración:** spec → plan (decisión de estructura) → ADR 0007 (la alternativa "dos repositorios", antes descartada, queda aceptada) → código (extracción a `api-productos-grpc`) → evals.

**Evals que cambian de resultado:**

- E-19 / SC-004: antes se comprobaba con `git diff` contra `main` en un solo repositorio; ahora, además, comparando el núcleo de los dos repositorios archivo por archivo (deben ser idénticos).
- E-22: la guía de llamadas se ejecuta contra la aplicación del repositorio gRPC.

**Lo que NO cambia:** el contrato `productos.proto`, el adaptador, el mapeador, el manejador de errores, las 14 pruebas y las decisiones técnicas de Spring gRPC 0.12, puerto 9090 y reflexión. El código es el mismo; cambia dónde vive.
