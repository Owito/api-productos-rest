# Feature Specification: Servicio gRPC de catálogo de productos

**Feature Branch**: `001-grpc-productos`

**Created**: 2026-09-12

**Status**: Draft

**Input**: User description: "Exponer el CRUD de Producto también por gRPC como cuarto adaptador de entrada sobre el mismo núcleo hexagonal, para la Actividad de entrega 2 (Unidad 4) del módulo Arquitectura de Aplicaciones Web (TIC51372)."

## Contexto

La Unidad 4 del módulo pide construir el backend de dos aplicaciones con servicios CRUD sobre la entidad `Producto`: una con GraphQL y otra con gRPC, con framework, ORM, base de datos y repositorio en GitHub, sustentadas en un video de máximo 15 minutos. La parte GraphQL ya existe desde la Unidad 3. Esta feature cubre la parte gRPC.

El evaluador califica, para gRPC, cuatro criterios: arquitectura de la solución (25 puntos), implementación con framework, ORM y CRUD funcional (50), pruebas de cada operación con una herramienta (25) y repositorio ordenado en GitHub (25). El valor de fondo para el proyecto es demostrar que el hexágono está bien cerrado: el dominio, los casos de uso y la persistencia no cambian por publicar el catálogo en un protocolo más.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar el catálogo por gRPC (Priority: P1)

Un cliente gRPC (grpcurl, Postman o un stub generado) lista el catálogo completo o filtrado por categoría, obtiene un producto por su identificador y consulta el catálogo cerrado de categorías con su etiqueta legible.

**Why this priority**: es la operación que se muestra primero en el video y la que prueba que el servicio arranca, que el contrato se descubre y que el adaptador llega hasta la base de datos a través del ORM. Sin lectura no hay demostración posible.

**Independent Test**: con el catálogo de ejemplo sembrado, un cliente que solo implementa las tres operaciones de lectura obtiene 18 productos, un producto concreto por id y 8 categorías, sin necesitar ninguna operación de escritura.

**Acceptance Scenarios**:

1. **Given** el catálogo tiene productos de varias categorías, **When** el cliente pide listar sin categoría, **Then** recibe todos los productos ordenados por identificador ascendente.
2. **Given** el catálogo tiene productos de la categoría AUDIO y de otras, **When** el cliente pide listar con la categoría AUDIO, **Then** recibe solo los productos de AUDIO.
3. **Given** existe un producto con identificador 1, **When** el cliente pide obtener el producto 1, **Then** recibe ese producto con identificador, nombre, descripción, precio, categoría y etiqueta de categoría.
4. **Given** no existe un producto con identificador 999999, **When** el cliente pide obtenerlo, **Then** recibe el estado NOT_FOUND con un mensaje que nombra el identificador.
5. **Given** el servicio está en ejecución, **When** el cliente pide las categorías, **Then** recibe las 8 categorías del catálogo cerrado, cada una con su código y su etiqueta legible.

---

### User Story 2 - Crear, actualizar y eliminar productos por gRPC (Priority: P2)

Un cliente gRPC crea un producto, actualiza sus datos conservando el identificador y lo elimina.

**Why this priority**: completa el CRUD que exige el enunciado y la rúbrica (implementación 50 puntos y pruebas de cada operación 25 puntos). Depende de que la lectura funcione para verificar los resultados.

**Independent Test**: sobre una base vacía, un cliente crea un producto, lo actualiza y lo elimina, y cada paso se verifica con la operación de lectura correspondiente.

**Acceptance Scenarios**:

1. **Given** no existe un producto llamado "Teclado 60", **When** el cliente crea un producto con ese nombre, descripción, precio "289900.00" y categoría PERIFERICOS, **Then** recibe el producto creado con un identificador asignado y los mismos datos.
2. **Given** existe el producto creado en el escenario anterior, **When** el cliente lo actualiza con nombre "Teclado 65" y precio "310000.00", **Then** recibe el producto con el mismo identificador y los datos nuevos.
3. **Given** existe un producto, **When** el cliente lo elimina, **Then** recibe una confirmación vacía y una lectura posterior por su identificador responde NOT_FOUND.
4. **Given** ya existe un producto llamado "Teclado 60", **When** el cliente intenta crear otro llamado "teclado 60" (distinta capitalización), **Then** recibe el estado ALREADY_EXISTS con un mensaje que nombra el producto.
5. **Given** el cliente envía un precio "0" o un nombre vacío, **When** intenta crear o actualizar, **Then** recibe el estado INVALID_ARGUMENT con el mensaje de la regla incumplida.
6. **Given** no existe un producto con identificador 999999, **When** el cliente intenta actualizarlo o eliminarlo, **Then** recibe el estado NOT_FOUND.

---

### User Story 3 - Descubrir y probar el servicio con una herramienta (Priority: P3)

Quien sustenta el trabajo abre una herramienta de cliente gRPC, descubre los servicios y sus operaciones sin tener el archivo de contrato a la mano, ejecuta cada operación y muestra la respuesta en pantalla. Además, quien clona el repositorio encuentra documentado cómo arrancar el servicio y qué llamadas ejecutar.

**Why this priority**: es lo que la rúbrica evalúa en "pruebas gRPC" (25 puntos) y "repositorio gRPC" (25 puntos): que cada operación se pruebe con un software y que el repositorio esté ordenado y documentado. Depende de P1 y P2 para tener algo que mostrar.

**Independent Test**: con el servicio arrancado localmente, la herramienta lista el servicio de productos y sus seis operaciones sin que se le suministre el contrato, y la documentación del repositorio contiene una llamada de ejemplo por operación que se ejecuta tal cual.

**Acceptance Scenarios**:

1. **Given** el servicio está en ejecución en local, **When** la herramienta pide la lista de servicios disponibles, **Then** aparece el servicio de productos con sus seis operaciones.
2. **Given** la documentación del repositorio, **When** quien sustenta copia la llamada de ejemplo de cada operación, **Then** la llamada se ejecuta sin modificaciones y produce la respuesta descrita.
3. **Given** el repositorio en GitHub, **When** el evaluador lo abre, **Then** encuentra el contrato del servicio, el adaptador en su paquete propio, sus pruebas, la decisión de arquitectura documentada y la guía de uso, sin artefactos de compilación ni secretos.

---

### Edge Cases

- Un identificador negativo o cero se trata como inexistente: la operación responde NOT_FOUND, no un error interno.
- Un precio con más de dos decimales ("10.999") se acepta tal como llega; el dominio no fija escala. Un precio que no es un número ("abc") o vacío responde INVALID_ARGUMENT.
- Una categoría fuera del catálogo la rechaza el contrato antes de llegar al dominio (la enumeración del mensaje no la admite); un valor "sin especificar" en la creación o actualización responde INVALID_ARGUMENT con el mensaje del dominio.
- Un nombre con espacios alrededor se guarda recortado, igual que en REST y GraphQL.
- Una descripción vacía se guarda como ausente, no como cadena vacía.
- Al actualizar un producto con su propio nombre (misma capitalización o distinta) no hay conflicto; al ponerle el nombre de otro producto existente responde ALREADY_EXISTS.
- Cualquier fallo no previsto (por ejemplo la base de datos caída) responde el estado UNKNOWN sin exponer el mensaje interno ni la traza.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema debe exponer un servicio gRPC de productos con seis operaciones unarias: listar productos, obtener producto, crear producto, actualizar producto, eliminar producto y listar categorías.
- **FR-002**: El sistema debe responder la lista de productos ordenada por identificador ascendente cuando no se indica categoría, y solo los de esa categoría cuando se indica.
- **FR-003**: El sistema debe responder cada producto con identificador, nombre, descripción (opcional), precio, categoría del catálogo cerrado y etiqueta legible de la categoría.
- **FR-004**: El sistema debe transportar el precio como texto decimal en los mensajes, sin pérdida de precisión, y responder INVALID_ARGUMENT si el texto recibido no es un número decimal.
- **FR-005**: El sistema debe persistir las altas, cambios y bajas a través de la misma capa de persistencia (ORM) que usan los demás adaptadores, sin consultas SQL manuales.
- **FR-006**: El sistema debe aplicar las reglas del dominio ya existentes sin duplicarlas en el adaptador: nombre obligatorio de máximo 120 caracteres, único sin importar mayúsculas; descripción de máximo 500 caracteres; precio mayor que cero; categoría del catálogo cerrado.
- **FR-007**: El sistema debe traducir los errores del dominio a estados gRPC en la frontera del adaptador: datos inválidos a INVALID_ARGUMENT, producto inexistente a NOT_FOUND, nombre duplicado a ALREADY_EXISTS, cada uno con el mensaje legible del dominio.
- **FR-008**: El sistema debe responder UNKNOWN sin exponer mensaje interno ni traza ante cualquier error que no sea del dominio.
- **FR-009**: El sistema debe permitir que un cliente descubra en tiempo de ejecución el servicio y sus operaciones sin disponer del archivo de contrato.
- **FR-010**: El sistema debe escuchar gRPC en un puerto propio, distinto del HTTP, con valor por defecto 9090 y configurable por la variable de entorno GRPC_PORT.
- **FR-011**: El sistema debe conservar intactos el dominio, los casos de uso, el puerto de salida y el adaptador de persistencia: el adaptador gRPC depende únicamente del puerto de entrada existente.
- **FR-012**: El repositorio debe documentar la decisión de arquitectura (ADR), actualizar la guía del proyecto y la guía para el agente, e incluir una guía de llamadas de ejemplo por operación para la sustentación.
- **FR-013**: La suite de pruebas existente debe seguir en verde y el adaptador debe traer pruebas de integración que cubran las seis operaciones y los tres errores del dominio sin abrir un puerto de red.

### Key Entities

- **Producto**: identificador, nombre, descripción opcional, precio, categoría. Es el mismo modelo de dominio de las unidades anteriores; gRPC no le agrega atributos.
- **Categoría**: catálogo cerrado de 8 valores (AUDIO, PERIFERICOS, PANTALLAS, COMPUTO, ALMACENAMIENTO, CONECTIVIDAD, ENERGIA, MOBILIARIO), cada uno con etiqueta legible.
- **Contrato del servicio**: definición de las seis operaciones y de los mensajes que intercambian; es la fuente de verdad del borde gRPC, como el esquema lo es para GraphQL y la especificación OpenAPI para REST.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Las seis operaciones se ejecutan con éxito desde una herramienta de cliente gRPC contra el servicio en local, y cada una queda registrada en la guía de llamadas del repositorio.
- **SC-002**: Los tres errores del dominio (inexistente, duplicado, inválido) se reproducen desde la herramienta con el estado gRPC previsto y un mensaje legible.
- **SC-003**: La suite completa del proyecto pasa con las 54 pruebas previas más las nuevas del adaptador gRPC, sin modificar ninguna prueba existente.
- **SC-004**: Ningún archivo de `domain/`, `application/` ni `infrastructure/output/` cambia en esta feature (verificable con la lista de archivos del cambio).
- **SC-005**: Un evaluador que clona el repositorio arranca el servicio y ejecuta la primera llamada de ejemplo en menos de 5 minutos siguiendo solo la guía.

## Assumptions

- Modalidad de entrega: se asume grupal como en las Unidades 2 y 3 (mismos integrantes), con confirmación pendiente del tutor. No cambia el alcance técnico.
- "Dos aplicaciones" del enunciado se interpreta como dos servicios de comunicación distintos sobre el mismo backend (dos adaptadores de entrada en un solo repositorio), coherente con la arquitectura hexagonal ya calificada en la Unidad 2. Si el tutor exige dos repositorios, el mismo código se separa después sin tocar el dominio; queda como pregunta abierta para el encuentro sincrónico, no como trabajo de esta iteración.
- La demostración es local: el despliegue en Render sigue publicando solo HTTP en 8080 y no expone el puerto gRPC. El video muestra el servicio corriendo en la máquina de quien sustenta.
- La herramienta de pruebas para el video es grpcurl o Postman; la guía de llamadas se escribe para grpcurl y las mismas llamadas se replican en Postman importando el contrato o por descubrimiento.
- El identificador del producto viaja como entero de 64 bits, igual que en la base de datos.
- Los mensajes del servicio y sus comentarios van en español sin tildes, siguiendo la convención del repositorio para archivos de código.

## Out of Scope

- Streaming (de servidor, de cliente o bidireccional): el enunciado pide CRUD punto a punto.
- Autenticación, autorización o TLS en gRPC: la demo pública sigue sin autenticación por decisión previa del proyecto.
- Un cliente gRPC dentro de la aplicación o un segundo repositorio.
- Publicar gRPC en Render o en otra plataforma.
- Cualquier cambio a los adaptadores REST, web o GraphQL.
