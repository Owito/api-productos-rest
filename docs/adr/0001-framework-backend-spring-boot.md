# ADR 0001: Spring Boot como framework backend

- **Estado:** aceptada
- **Fecha:** 2026-08-18

## Contexto

La actividad exige construir servicios RESTful CRUD sobre una base de datos usando un framework
de desarrollo backend que fomente buenas prácticas y una estructura de código organizada. La
lectura fundamental del módulo delimita el universo de referencia a cuatro frameworks: Laravel,
Express.js, Django y Spring.

## Alternativas consideradas

| Opción | A favor | En contra |
|---|---|---|
| **Spring Boot + Kotlin** | Convención de capas explícita, JPA/Hibernate integrado, validación declarativa, ecosistema maduro | Arranque más lento y mayor consumo de memoria que las alternativas |
| Express.js | Curva mínima, arranque inmediato | El ORM y la estructura de paquetes son decisiones externas al framework, no convenciones |
| Django | ORM y panel de administración incluidos | El acoplamiento al patrón MTV pesa cuando solo se necesita una API |
| Ktor | Ligero, corrutinas nativas, mismo lenguaje | Fuera del universo de referencia del módulo y sin capa de persistencia integrada |

## Decisión

Se adopta **Spring Boot 3.5 con Kotlin sobre JDK 17**.

Spring Boot resuelve dentro del propio framework las tres exigencias de la actividad: la
integración del ORM (Spring Data JPA), el manejo centralizado de errores
(`@RestControllerAdvice`) y una convención de organización en paquetes que no hay que inventar.
Kotlin agrega tipos no nulos por defecto, lo que traslada a tiempo de compilación una clase de
errores que en Java aparecería en tiempo de ejecución.

## Consecuencias

- El proyecto hereda la convención de capas de Spring, que es también el criterio con el que se
  evalúa la organización del código.
- El tiempo de arranque en frío es mayor, lo que sería relevante en un despliegue serverless.
  No lo es para este alcance.
- Kotlin exige el plugin `kotlin-jpa` para generar el constructor sin argumentos que Hibernate
  necesita en las entidades. Ya está declarado en `build.gradle.kts`.
