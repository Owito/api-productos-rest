# API de Productos

Backend con servicios RESTful que expone operaciones CRUD sobre la entidad `Producto`,
construido con **Spring Boot + Kotlin**, persistencia mediante **ORM (Spring Data JPA sobre
Hibernate)** y base de datos **PostgreSQL** alojada en Neon.

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

Separación por responsabilidad en capas. Cada paquete tiene una única razón para cambiar y
depende solo de la capa inmediatamente inferior.

```
src/main/kotlin/co/edu/poli/productos/
├── ProductosApplication.kt        Punto de entrada
├── config/                        Configuración transversal (OpenAPI)
├── controller/                    Capa web: mapea HTTP al caso de uso, no contiene lógica
├── dto/                           Contrato público de entrada y salida, con validaciones
├── mapper/                        Traducción DTO <-> entidad
├── service/                       Lógica de negocio y control transaccional
├── repository/                    Acceso a datos mediante Spring Data JPA
├── model/                         Entidad de dominio persistente
└── exception/                     Excepciones propias y manejo centralizado de errores
```

Reglas que sostienen la estructura:

1. **La entidad nunca sale por HTTP.** El controlador solo habla en DTO, así el esquema de la
   base de datos puede cambiar sin romper a los clientes.
2. **El controlador no conoce el repositorio.** Toda coordinación pasa por el servicio.
3. **Ningún controlador atrapa excepciones.** Todas terminan en `ManejadorGlobalDeErrores`,
   que las traduce a un mismo contrato de error y al código HTTP correcto.

Las decisiones y sus alternativas descartadas están registradas en [`docs/adr/`](docs/adr).

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

En Windows PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE="neon"; $env:DB_URL="jdbc:postgresql://...?sslmode=require"
$env:DB_USERNAME="..."; $env:DB_PASSWORD="..."; .\gradlew.bat bootRun
```

## Pruebas

### Automatizadas

```bash
./gradlew test
```

13 pruebas: carga de contexto, 5 de lógica de negocio y 7 de la capa web que cubren los cuatro
verbos HTTP y los casos de error.

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
