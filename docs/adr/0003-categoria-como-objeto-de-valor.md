# ADR 0003: Categoría como objeto de valor, no como entidad

- **Estado:** aceptada
- **Fecha:** 2026-08-18

## Contexto

El catálogo necesita clasificar los productos. La decisión no es si hay categorías, sino qué son
dentro del modelo: un texto libre, una entidad con su propia tabla y su propio CRUD, o un conjunto
cerrado definido en el dominio.

## Decisión

`Categoria` es una **enumeración del dominio**, en `domain/model/Categoria.kt`, persistida como
texto en una columna de la tabla `productos`.

Cada constante lleva dos cosas: el **nombre**, que es el contrato estable hacia afuera (lo que
viaja en el JSON y lo que se guarda en la base de datos), y la **etiqueta**, que es solo
presentación. Separarlas permite corregir un texto en pantalla sin migrar datos ni romper clientes.

La conversión desde texto vive en el dominio (`Categoria.desde`) y lanza
`DatosDeProductoInvalidosException` si el valor no existe. Los dos adaptadores de entrada la usan,
y cada uno decide cómo reportarlo: la API responde 400 con el contrato de error, y la interfaz web
repinta el formulario con el mensaje junto al campo.

## Alternativas consideradas

| Opción | A favor | En contra |
|---|---|---|
| **Enumeración en el dominio** | Imposible guardar una categoría que no existe; el conjunto válido es explícito y se puede listar sin consultar la base | Agregar una categoría exige recompilar y desplegar |
| Texto libre en el producto | Cero ceremonia, flexibilidad total | Aparecen "Audio", "audio" y "Audios" como tres categorías distintas, y nadie puede confiar en un filtro |
| Entidad `Categoria` con su tabla y su CRUD | Se administran en tiempo de ejecución, admite atributos propios | Duplica la superficie del proyecto por una necesidad que hoy no existe, y el enunciado pide el CRUD de una entidad, `Producto` |

## Consecuencias

- El filtro por categoría es exacto por construcción: `GET /api/v1/productos?categoria=AUDIO` y
  `/productos?categoria=AUDIO` no pueden devolver basura porque un valor inválido nunca llega a la
  consulta.
- La columna se persiste con `@Enumerated(EnumType.STRING)` y no `ORDINAL`. Guardar el número de
  posición dejaría la tabla ilegible y, peor, reordenar la enumeración corrompería los datos
  existentes en silencio. Hay un índice sobre la columna porque el filtro es la consulta más
  frecuente de la interfaz.
- Agregar una categoría es editar una línea de la enumeración. Es el costo aceptado a cambio de la
  garantía de integridad.
- **Si el negocio llega a necesitar categorías administrables**, el cambio está contenido: se
  convierte en entidad con su propio puerto de salida, y ni el adaptador REST ni el web se enteran,
  porque hoy ya hablan con el dominio y no con la base de datos. Esa contención es precisamente lo
  que compra la arquitectura hexagonal del ADR 0002.
