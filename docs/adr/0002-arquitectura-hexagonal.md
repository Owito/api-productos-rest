# ADR 0002: Arquitectura hexagonal (puertos y adaptadores)

- **Estado:** aceptada
- **Fecha:** 2026-08-18
- **Reemplaza a:** la organización en capas planas de la primera versión del proyecto

## Contexto

El proyecto expone una sola entidad. La tentación es resolverlo en un archivo con el controlador
llamando directamente al repositorio, que es lo que muestran casi todos los tutoriales de CRUD.
La primera versión de este proyecto ya evitaba eso con capas planas
(`controller / service / repository / model`), pero conservaba un problema de fondo: el modelo de
dominio era la entidad de JPA, y el servicio importaba el repositorio de Spring Data. La
dirección de la dependencia iba del negocio hacia la infraestructura.

## Decisión

Se adopta **arquitectura hexagonal**, también llamada puertos y adaptadores.

```
                   ┌──────────────── infrastructure ────────────────┐
                   │                                                │
  HTTP  ──────▶  ProductoRestAdapter                                │
                   │        │                                       │
                   │        ▼                                       │
                   │  ┌── GestionarProductosUseCase ◀── puerto de entrada
                   │  │                                             │
                   │  │   application: ProductoService              │
                   │  │                                             │
                   │  │   domain: Producto, excepciones             │
                   │  │                                             │
                   │  └── ProductoRepositoryPort ◀──── puerto de salida
                   │              ▲                                 │
                   │              │                                 │
                   │   ProductoPersistenceAdapter ──▶ PostgreSQL / H2
                   └────────────────────────────────────────────────┘
```

Las tres reglas que sostienen la estructura:

1. **El dominio no importa nada.** `Producto` es Kotlin puro: sin JPA, sin Jackson, sin Bean
   Validation. Sus invariantes (nombre obligatorio, precio mayor que cero, largos máximos) se
   cumplen aunque se cambie de base de datos o de protocolo.
2. **Los puertos los define el núcleo.** `ProductoRepositoryPort` es una interfaz de la capa de
   aplicación; la infraestructura se adapta a ella. Esa es la inversión de dependencias: las
   flechas apuntan hacia adentro.
3. **La aplicación no conoce Spring.** `ProductoService` no lleva `@Service`; se registra como
   bean en `infrastructure/config/ConfiguracionDeCasosDeUso`. Su única anotación es
   `jakarta.transaction.Transactional`, que es un estándar de Jakarta EE y no ata el núcleo a un
   framework concreto.

Hay tres modelos deliberadamente distintos, cada uno con un dueño:

| Modelo | Capa | Para qué existe |
|---|---|---|
| `Producto` | dominio | Reglas de negocio e invariantes |
| `ProductoJpaEntity` | infraestructura de salida | Mapeo a la tabla `productos` |
| `ProductoRequest` / `ProductoResponse` | infraestructura de entrada | Contrato público de la API |

## Alternativas consideradas

- **Capas planas (la versión anterior).** Menos archivos y menos traducción, pero el modelo de
  dominio queda atado al ORM: cambiar de motor o de esquema obliga a tocar la lógica de negocio,
  y probar un caso de uso exige levantar Spring.
- **Controlador que usa la entidad directamente.** El esquema de la base de datos queda publicado
  como contrato de la API: cualquier renombrado de columna rompe a los clientes.
- **Arquitectura por características (vertical slices).** Escala mejor con muchas entidades, pero
  con una sola no hay rebanadas que separar y la ventaja no se materializa.

## Consecuencias

**A favor:**

- El núcleo se prueba sin Spring, sin base de datos y sin HTTP. `ProductoServiceTest` corre contra
  un adaptador falso en memoria de veinte líneas, y `ProductoTest` prueba las invariantes en
  milisegundos. Eso no es un detalle de comodidad: es la evidencia de que la inversión de
  dependencias es real y no decorativa.
- Cambiar PostgreSQL por MongoDB, o agregar una CLI junto a la API REST, significa escribir otro
  adaptador sin tocar el caso de uso.
- Las reglas de negocio están en un solo lugar y no se pueden saltar desde otro punto de entrada.

**En contra, y hay que decirlo:**

- Hay dos mappers y tres representaciones del mismo concepto. Para una entidad con cuatro campos
  es más código del que un CRUD necesita. El costo se paga una vez y se amortiza cuando aparecen
  la segunda y la tercera entidad; en un proyecto que nunca crezca, sería sobreingeniería.
- Agregar un campo a `Producto` obliga a tocar el modelo, la entidad, los dos DTO y los dos
  mappers.
- El cableado explícito en `ConfiguracionDeCasosDeUso` es un archivo más que mantener, a cambio de
  que la capa de aplicación no importe Spring.
