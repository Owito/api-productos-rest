# ADR 0002: Organización en capas con DTO en la frontera

- **Estado:** aceptada
- **Fecha:** 2026-08-18

## Contexto

El proyecto expone una única entidad. La tentación es resolverlo en un solo archivo con el
controlador llamando directamente al repositorio, que es lo que muestran la mayoría de los
tutoriales de CRUD.

## Decisión

Se organiza el código en capas con dependencia en una sola dirección:

```
controller  ->  service  ->  repository  ->  model
     |                                         ^
     v                                         |
    dto        ------ mapper ------------------
```

Reglas que se hacen cumplir:

1. La entidad `Producto` no cruza la frontera HTTP. El controlador solo conoce
   `ProductoRequest` y `ProductoResponse`.
2. El controlador no inyecta el repositorio.
3. Ningún componente atrapa excepciones para convertirlas en respuestas HTTP. Esa traducción
   ocurre en un único lugar, `ManejadorGlobalDeErrores`.
4. Las transacciones se declaran en el servicio, no en el controlador ni en el repositorio.

## Alternativas consideradas

- **Controlador que usa la entidad directamente.** Menos archivos, pero el esquema de base de
  datos queda publicado como contrato de la API: cualquier renombrado de columna rompe a los
  clientes, y cualquier campo interno queda expuesto.
- **Arquitectura hexagonal con puertos y adaptadores.** Aísla mejor el dominio, pero para una
  entidad sin reglas de negocio complejas la indirección adicional cuesta más de lo que aporta.

## Consecuencias

- Agregar un campo a `Producto` implica tocar entidad, DTO y mapper. Es el precio explícito de
  poder evolucionar el esquema sin romper el contrato público.
- La estructura escala a nuevas entidades por repetición del mismo patrón, sin rediseño.
- El costo de las pruebas baja: el servicio se prueba sin levantar la capa HTTP y el controlador
  se prueba con MockMvc sin depender de una base de datos real.
