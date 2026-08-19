# API de Productos

Backend con servicios RESTful que expone operaciones CRUD sobre la entidad `Producto`,
construido con **Spring Boot + Kotlin** bajo **arquitectura hexagonal**, persistencia mediante
**ORM (Spring Data JPA sobre Hibernate)** y base de datos **PostgreSQL** alojada en Neon.

Módulo **Arquitectura de Aplicaciones Web (TIC51372)**, Unidad 2, actividad sumativa.

---

## Stack

| Componente | Elección | Motivo |
|---|---|---|
| Lenguaje | Kotlin 2.2 sobre JDK 17 | Tipos no nulos por defecto, menos código ceremonial que Java |
| Framework backend | Spring Boot 3.5 | Framework de referencia del módulo, convención clara de capas |
| ORM | Spring Data JPA (Hibernate) | Genera el esquema y resuelve el acceso a datos sin SQL manual |
| Base de datos | PostgreSQL (Neon, capa gratuita) | Relacional, gestionada, con conexión TLS |
| Base de datos local | H2 en memoria | El proyecto se clona y se ejecuta sin configurar credenciales |
| Documentación y pruebas | springdoc OpenAPI + Swagger UI | Cliente HTTP embebido para probar los endpoints |
| Construcción | Gradle Wrapper (Kotlin DSL) | No exige Gradle instalado en la máquina |

## Arquitectura

**Hexagonal (puertos y adaptadores).** El dominio y los casos de uso no dependen de ningun
framework; la infraestructura se adapta a las interfaces que define el nucleo.

```
src/main/kotlin/co/edu/poli/productos/
|
+-- domain/                                  NUCLEO. Kotlin puro, cero dependencias
|   +-- model/Producto.kt                    modelo con sus invariantes
|   +-- exception/                           hechos del negocio, no codigos HTTP
|
+-- application/                             CASOS DE USO. No conoce Spring
|   +-- port/input/GestionarProductosUseCase   puerto de entrada  (driving)
|   +-- port/output/ProductoRepositoryPort     puerto de salida   (driven)
|   +-- service/ProductoService.kt             implementa el caso de uso
|
+-- infrastructure/                          ADAPTADORES. Aqui vive la tecnologia
    +-- input/rest/                          adaptador de entrada
    |   +-- ProductoRestAdapter.kt             @RestController
    |   +-- dto/                               contrato publico de la API
    |   +-- mapper/                            DTO  <->  dominio
    |   +-- error/                             traduce excepciones a HTTP
    +-- output/persistence/                  adaptador de salida
    |   +-- ProductoPersistenceAdapter.kt      implementa el puerto de salida
    |   +-- entity/ProductoJpaEntity.kt        entidad JPA, detalle de infraestructura
    |   +-- repository/                        Spring Data JPA
    |   +-- mapper/                            entidad  <->  dominio
    +-- config/                              cableado y OpenAPI
```

Las tres reglas que sostienen la estructura:

1. **El dominio no importa nada.** `Producto` no tiene anotaciones de JPA, Jackson ni Bean
   Validation. Sus invariantes se cumplen aunque cambie la base de datos o el protocolo.
2. **Los puertos los define el nucleo.** `ProductoRepositoryPort` es una interfaz de la capa de
   aplicacion y la infraestructura se adapta a ella. Las dependencias apuntan hacia adentro.
3. **La aplicacion no conoce Spring.** `ProductoService` no lleva `@Service`: se registra como
   bean en `infrastructure/config/ConfiguracionDeCasosDeUso`.

La consecuencia practica es que el nucleo se prueba sin levantar Spring, sin base de datos y sin
HTTP, contra un adaptador falso en memoria. El razonamiento completo, con las alternativas
descartadas y el costo que tiene esta decision, esta en [`docs/adr/`](docs/adr).

Hay tres representaciones distintas del mismo concepto, cada una con un dueno:

| Modelo | Capa | Para que existe |
|---|---|---|
| `Producto` | dominio | Reglas de negocio e invariantes |
| `ProductoJpaEntity` | infraestructura de salida | Mapeo a la tabla `productos` |
| `ProductoRequest` / `ProductoResponse` | infraestructura de entrada | Contrato publico de la API |

## Modelo de datos

Hibernate crea y mantiene la tabla `productos` a partir de la entidad.

| Campo | Tipo | Restricciones |
|---|---|---|
| `id` | `BIGINT` autoincremental | Llave primaria |
| `nombre` | `VARCHAR(120)` | Obligatorio, único en la práctica (validado en el servicio) |
| `descripcion` | `VARCHAR(500)` | Opcional |
| `precio` | `NUMERIC(12,2)` | Obligatorio, mayor que cero |

## Endpoints

Base: `/api/v1/productos`

| Verbo | Ruta | Descripción | Éxito | Errores |
|---|---|---|---|---|
| `GET` | `/api/v1/productos` | Lista todos los productos | `200` | |
| `GET` | `/api/v1/productos/{id}` | Consulta un producto | `200` | `400` id no numérico, `404` no existe |
| `POST` | `/api/v1/productos` | Crea un producto | `201` + `Location` | `400` datos inválidos, `409` nombre repetido |
| `PUT` | `/api/v1/productos/{id}` | Actualiza un producto | `200` | `400`, `404`, `409` |
| `DELETE` | `/api/v1/productos/{id}` | Elimina un producto | `204` | `400`, `404` |

Documentación interactiva: `http://localhost:8080/swagger-ui.html`
Especificación OpenAPI: `http://localhost:8080/v3/api-docs`

### Contrato de error

Todos los fallos responden con la misma estructura:

```json
{
  "marcaDeTiempo": "2026-08-18T18:38:00.725-05:00",
  "estado": 400,
  "error": "Bad Request",
  "mensaje": "La peticion tiene campos invalidos",
  "ruta": "/api/v1/productos",
  "detalles": [
    { "campo": "precio", "mensaje": "El precio debe ser mayor que cero" },
    { "campo": "nombre", "mensaje": "El nombre es obligatorio" }
  ]
}
```

## Cómo ejecutar

Requisito único: **JDK 17** o superior. Gradle lo aporta el wrapper.

### Perfil local (H2 en memoria, sin configuración)

```bash
./gradlew bootRun
```

La aplicación queda en `http://localhost:8080`. La consola de H2 está en `/h2-console`
(JDBC URL `jdbc:h2:mem:productos`, usuario `sa`, sin contraseña).

### Perfil de nube (PostgreSQL en Neon)

1. Crear una base de datos gratuita en [Neon](https://neon.tech) y copiar la cadena de conexión.
2. Copiar `.env.example` como `.env` y completar `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`.
   El archivo `.env` está ignorado por git: **las credenciales no se versionan**.
3. Ejecutar con el perfil activo:

```bash
SPRING_PROFILES_ACTIVE=neon DB_URL=... DB_USERNAME=... DB_PASSWORD=... ./gradlew bootRun
```

En Windows PowerShell basta con el script incluido, que lee el `.env`:

```powershell
.\run-neon.ps1
```

Neon suspende el cómputo por inactividad en la capa gratuita: la primera conexión despierta la
base y puede tardar unos segundos.

## Pruebas

### Automatizadas

```bash
./gradlew test
```

23 pruebas repartidas segun la arquitectura:

| Suite | Pruebas | Levanta Spring |
|---|---|---|
| `ProductoTest` (dominio) | 6 | no |
| `ProductoServiceTest` (casos de uso, adaptador falso en memoria) | 8 | no |
| `ProductoRestAdapterTest` (integracion, los 4 verbos y los errores) | 8 | si |
| `ProductosApplicationTests` (carga de contexto) | 1 | si |

Las 14 pruebas del nucleo corren sin contenedor de dependencias ni base de datos. Eso es lo que
compra la arquitectura hexagonal.

### Manuales con Postman

Importar [`docs/Productos-API.postman_collection.json`](docs/Productos-API.postman_collection.json).
La colección se ejecuta en orden con el Collection Runner: guarda el `id` del producto creado en
una variable y lo reutiliza en `GET`, `PUT` y `DELETE`. Incluye aserciones automáticas de código
de estado y de cuerpo de respuesta.

### Manuales con curl

```bash
API=http://localhost:8080/api/v1/productos

curl -i -X POST $API -H "Content-Type: application/json" \
  -d '{"nombre":"Teclado mecanico 60%","descripcion":"Switches lineales","precio":289900.00}'

curl $API
curl $API/1

curl -X PUT $API/1 -H "Content-Type: application/json" \
  -d '{"nombre":"Teclado mecanico 65%","descripcion":"Layout compacto","precio":329900.00}'

curl -i -X DELETE $API/1
```

## Licencia

Trabajo académico. Politécnico Grancolombiano, Maestría en Arquitectura de Software.
