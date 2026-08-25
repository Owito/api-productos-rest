# ADR 0005: Flyway como dueño del esquema, en vez de la generación automática del ORM

- **Estado:** aceptada
- **Fecha:** 2026-08-24

## Contexto

Hasta ahora el esquema lo generaba Hibernate con `ddl-auto: update`: al arrancar, el ORM comparaba
el mapeo de `ProductoJpaEntity` contra la base y aplicaba las diferencias. Es cómodo mientras el
modelo cambia rápido y la base es desechable, y por eso fue la decisión correcta al empezar.

Deja de serlo en cuanto hay una base con datos que no se puede recrear. Tres razones concretas:

1. **No hay historial.** No existe forma de saber qué se aplicó, cuándo, ni en qué orden. El estado
   del esquema es lo que el ORM haya conseguido hacer, y eso no se puede revisar en un *pull
   request*.
2. **Falla en silencio.** `update` aplica lo que puede y registra lo que no. Un cambio que la base
   rechaza deja la aplicación arrancando con un esquema distinto al que el código espera, y el
   síntoma aparece después, en una petición cualquiera. Al agregar la restricción única de nombre
   nos topamos justo con esto: no había forma de confirmar desde fuera si había quedado creada.
3. **No borra ni renombra.** `update` solo agrega. Una columna que se quita del mapeo se queda en la
   tabla para siempre, y el esquema real se va separando del que describe el código.

## Decisión

El esquema pasa a declararse en migraciones versionadas de **Flyway**, y Hibernate baja a
`ddl-auto: validate`: ya no modifica nada, solo comprueba al arrancar que la tabla corresponde al
mapeo y **se niega a arrancar si no**.

Dos detalles de la implementación importan:

**Migraciones separadas por motor**, con `locations: classpath:db/migration/{vendor}`. No es
duplicación por gusto: el mismo `@Enumerated(STRING)` se traduce al tipo `ENUM` nativo en H2 y a un
`varchar` con `CHECK` en PostgreSQL. Con una sola carpeta habría que elegir un motor y romper el
otro, o escribir SQL del mínimo común denominador que ya no coincidiría con lo que el ORM valida.

**La base de producción se adopta con `baseline-on-migrate`.** La base de Neon ya existía, creada
por el ORM y con datos. Flyway la marca como si `V1` ya se hubiera aplicado y sigue desde ahí, en
vez de fallar por encontrar un esquema que no controla. La consecuencia práctica hay que tenerla
presente al agregar migraciones: **lo que se escriba en `V1` no se ejecuta en producción**. Por eso
la restricción única vive en `V2` y no en `V1`, aunque en una base nueva las dos se apliquen
seguidas.

El contenido de `V1` no se escribió a mano: es el DDL que generaba Hibernate para cada dialecto,
capturado con la generación de scripts de JPA. Copiarlo tal cual es lo que garantiza que `validate`
pase, porque el tipo de cada columna es exactamente el que el ORM espera.

## Alternativas consideradas

| Opción | A favor | En contra |
|---|---|---|
| **Flyway con migraciones por motor** | Historial versionado y revisable; el esquema deja de depender de lo que el ORM deduzca; un desajuste sale como fallo de arranque y no como corrupción silenciosa; adopta la base existente sin recrearla | Dos carpetas que mantener en paralelo, y la trampa de que `V1` no corre en producción |
| Seguir con `ddl-auto: update` | Cero trabajo | Es justo lo que motivó este ADR |
| `ddl-auto: validate` a secas, creando el esquema a mano | Quita el riesgo de que el ORM altere la base | El esquema queda sin registrar en el repositorio: nadie puede levantar el proyecto desde cero |
| Liquibase | Más expresivo, con *changesets* independientes del motor y *rollback* declarativo | Su abstracción en XML o YAML resolvería la diferencia entre motores, pero a costa de no poder copiar el DDL exacto del ORM, que es lo que hace que `validate` pase sin ajustes |

## Consecuencias

- **Un error de esquema ahora tumba el arranque**, donde antes pasaba desapercibido. Es el
  intercambio buscado: es preferible un despliegue que falla ruidoso a una aplicación viva contra
  una base que no corresponde. En el plan gratuito de Render eso significa que un fallo de migración
  deja el servicio abajo hasta corregirlo.
- **Toda modificación de esquema pasa por un archivo nuevo**, nunca editando uno ya aplicado: Flyway
  compara la suma de verificación en cada arranque y rechaza los que cambiaron.
- La restricción única del nombre queda impuesta por la base y no solo por el caso de uso, que era
  el hueco de concurrencia que quedaba abierto.
- Sigue habiendo una asimetría consciente: la unicidad de la base es sensible a mayúsculas y la
  regla completa no. Expresarla pediría un índice funcional sobre `lower(nombre)`, que PostgreSQL
  soporta y H2 no; ponerlo en un solo motor haría que local y producción se comportaran distinto,
  que es peor que la limitación.
