# API de Productos

Backend con servicios RESTful que expone operaciones CRUD sobre la entidad `Producto`,
construido con **Spring Boot + Kotlin** bajo **arquitectura hexagonal**, persistencia mediante
**ORM (Spring Data JPA sobre Hibernate)** y base de datos **PostgreSQL** alojada en Neon.

El mismo nucleo se expone por **dos adaptadores de entrada**: una API REST en `/api/v1/productos`
y una interfaz web renderizada en el servidor en `/productos`.

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
| Interfaz web | Thymeleaf | Segundo adaptador de entrada, renderizado en el servidor, sin build de front |
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
    +-- input/rest/                          adaptador de entrada 1: API REST
    |   +-- ProductoRestAdapter.kt             @RestController
    |   +-- dto/                               contrato publico de la API
    |   +-- mapper/                            DTO  <->  dominio
    |   +-- error/                             traduce excepciones a HTTP
    +-- input/web/                           adaptador de entrada 2: interfaz web
    |   +-- ProductoWebAdapter.kt              @Controller sobre el MISMO puerto
    |   +-- form/                              enlace de los formularios HTML
    |   +-- ManejadorDeErroresWeb.kt           errores como paginas, no como JSON
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

Los dos adaptadores de entrada son la prueba de que el patron funciona: agregar la interfaz web
no cambio ni una linea del dominio, de la capa de aplicacion ni del adaptador de persistencia.
Un producto creado desde el formulario aparece en la API, y al reves.

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
| `categoria` | `VARCHAR(30)` | Obligatorio, uno de los ocho valores del catálogo, con índice |

Las categorías son un conjunto cerrado definido en el dominio, no una tabla: `AUDIO`,
`PERIFERICOS`, `PANTALLAS`, `COMPUTO`, `ALMACENAMIENTO`, `CONECTIVIDAD`, `ENERGIA` y `MOBILIARIO`.
El porqué está en [`docs/adr/0003`](docs/adr/0003-categoria-como-objeto-de-valor.md).

## Endpoints

Base: `/api/v1/productos`

| Verbo | Ruta | Descripción | Éxito | Errores |
|---|---|---|---|---|
| `GET` | `/api/v1/productos` | Lista los productos | `200` | `400` categoría inexistente |
| `GET` | `/api/v1/productos?categoria=AUDIO` | Filtra por categoría | `200` | `400` categoría inexistente |
| `GET` | `/api/v1/productos/categorias` | Lista las categorías disponibles | `200` | |
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

## Interfaz web

Segundo adaptador de entrada, renderizado en el servidor con Thymeleaf. Consume el mismo puerto
`GestionarProductosUseCase` que la API REST.

| Ruta | Metodo | Que hace |
|---|---|---|
| `/` | `GET` | Redirige al listado |
| `/productos` | `GET` | Catálogo completo |
| `/productos?categoria=AUDIO` | `GET` | Catálogo filtrado por categoría |
| `/productos/nuevo` | `GET` | Formulario de creacion |
| `/productos` | `POST` | Crea el producto |
| `/productos/{id}/editar` | `GET` | Formulario con los datos cargados |
| `/productos/{id}` | `PUT` | Actualiza el producto |
| `/productos/{id}` | `DELETE` | Elimina el producto |
| `/creditos` | `GET` | Creditos academicos del trabajo |

Los formularios HTML solo soportan `GET` y `POST`, asi que `PUT` y `DELETE` viajan en un campo
oculto `_method` que Spring traduce con `HiddenHttpMethodFilter`. La interfaz usa asi los mismos
verbos que la API.

Diferencia deliberada entre los dos adaptadores: el REST deja que las excepciones de dominio
lleguen al manejador global y se conviertan en 404 o 409. El web las atrapa y repinta el
formulario con el error junto al campo que lo causo, porque una persona frente a un formulario no
necesita un codigo de estado, necesita saber que corregir.

### Sistema de diseno

La interfaz aplica el lenguaje visual de las aplicaciones de streaming musical: base oscura por
defecto, un unico verde de acento de alta saturacion, controles en forma de pildora que crecen al
pasar el cursor, rejilla de tarjetas con portada cuadrada, y titulos muy apretados. No emplea
marcas, logotipos ni tipografias propietarias de terceros, solo los patrones de interfaz.

Los tokens estan en `static/css/estilos.css` y se resuelven en tres niveles:

| Nivel | Cuando manda |
|---|---|
| `:root` | tema oscuro, el valor por defecto |
| `@media (prefers-color-scheme: light)` | el sistema pide claro y el usuario no ha elegido |
| `:root[data-tema="claro"]` / `[data-tema="oscuro"]` | el usuario eligio en el selector |

**Modo oscuro y selector de tema.** El boton del encabezado cicla entre sistema, claro y oscuro.
La eleccion se guarda en `localStorage` y la reaplica un script sincrono en el `<head>`, para que
la pagina no parpadee al cargar. Si el navegador bloquea el almacenamiento, el selector sigue
funcionando durante la sesion.

Sin CSS ni JavaScript de terceros, sin build de front y sin peticiones a la red: una hoja de
estilos y un archivo de 50 lineas.

### Creditos

`/creditos` muestra los datos academicos del trabajo. No estan escritos en el HTML sino en
`app.creditos` dentro de `application.yml`, asi que corregir un integrante o una fecha es editar
una linea de configuracion.

## Cómo ejecutar

Requisito único: **JDK 17** o superior. Gradle lo aporta el wrapper.

### Perfil local (H2 en memoria, sin configuración)

```bash
./gradlew bootRun
```

La aplicación queda en `http://localhost:8080`: la interfaz web en `/productos` y la API en
`/api/v1/productos`. La consola de H2 está en `/h2-console`
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

## Catálogo de demostración

Al arrancar, si la tabla está vacía, la aplicación siembra 18 productos repartidos en las ocho
categorías. Sirve para que quien clone el repositorio vea la aplicación con contenido sin tener
que crear nada a mano.

Los datos entran por el mismo puerto que usan la API y la interfaz web, así que pasan por las
mismas reglas de negocio que cualquier otro alta.

| Perfil | Siembra |
|---|---|
| `local` | sí, siempre (H2 arranca vacío en cada ejecución) |
| `neon` y cualquier otro | no, salvo que se pida con `APP_DATOS_DEMO=true` |

Nunca duplica: si ya hay productos, no hace nada.

## Pruebas

### Automatizadas

```bash
./gradlew test
```

42 pruebas repartidas según la arquitectura:

| Suite | Pruebas | Levanta Spring |
|---|---|---|
| `ProductoTest` (dominio, invariantes y categorías) | 9 | no |
| `ProductoServiceTest` (casos de uso, adaptador falso en memoria) | 9 | no |
| `ProductoRestAdapterTest` (integración REST, los 4 verbos, filtros y errores) | 12 | sí |
| `ProductoWebAdapterTest` (integración web, formularios, filtros y _method) | 11 | sí |
| `ProductosApplicationTests` (carga de contexto) | 1 | sí |

Las 18 pruebas del núcleo corren sin contenedor de dependencias ni base de datos. Eso es lo que
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
  -d '{"nombre":"Teclado mecanico 60%","descripcion":"Switches lineales","precio":289900.00,"categoria":"PERIFERICOS"}'

curl $API
curl "$API?categoria=AUDIO"
curl $API/categorias
curl $API/1

curl -X PUT $API/1 -H "Content-Type: application/json" \
  -d '{"nombre":"Teclado mecanico 65%","descripcion":"Layout compacto","precio":329900.00,"categoria":"PERIFERICOS"}'

curl -i -X DELETE $API/1
```

## Créditos

Trabajo académico de la **Maestría en Arquitectura de Software** del **Politécnico
Grancolombiano**, asignatura **Arquitectura de Aplicaciones Web (TIC51372)**, Unidad 2, actividad
sumativa. Periodo 2026-2, bloque 1. Tutor: Wilson Soto.

Integrantes:

- Carlos Guerra
- Rafael Gutiérrez Correales
- Paulo Reyes Rodríguez

Los mismos datos se muestran en la aplicación en `/creditos` y se configuran en `app.creditos`
dentro de `application.yml`.
