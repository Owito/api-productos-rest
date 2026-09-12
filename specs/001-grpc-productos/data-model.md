# Data Model: Servicio gRPC de catálogo de productos

Fase 1 del plan. El modelo de dominio no cambia; esta feature agrega una representación más en la frontera (los mensajes del contrato) y su mapeo.

## Entidades del dominio (sin cambios)

| Entidad | Atributos | Reglas (las valida el dominio en `init`) | Fuente |
|---|---|---|---|
| `Producto` | `id: Long?`, `nombre: String`, `descripcion: String?`, `precio: BigDecimal`, `categoria: Categoria` | nombre no vacío y ≤ 120; descripción ≤ 500; precio > 0 | `domain/model/Producto.kt` |
| `Categoria` | enumeración de 8 valores con `etiqueta` | catálogo cerrado; `Categoria.desde(texto)` lanza `DatosDeProductoInvalidosException` si no existe o viene vacío | `domain/model/Categoria.kt` |

Unicidad del nombre sin importar mayúsculas: la defiende `ProductoService.crear/actualizar` (excepción legible) y la restricción `uk_productos_nombre` (concurrencia). El adaptador gRPC no la toca.

## Mensajes del contrato (nuevos, en `productos.proto`)

| Mensaje | Campos | Equivale a |
|---|---|---|
| `Producto` | `int64 id`, `string nombre`, `optional string descripcion`, `string precio`, `Categoria categoria`, `string categoria_etiqueta` | `ProductoResponse` (REST) / `ProductoGql` |
| `ProductoInput` | `string nombre`, `optional string descripcion`, `string precio`, `Categoria categoria` | `ProductoRequest` (REST) / `ProductoInputGql` |
| `ProductoIdRequest` | `int64 id` | el `{id}` de la ruta REST |
| `ActualizarProductoRequest` | `int64 id`, `ProductoInput datos` | `PUT /api/v1/productos/{id}` con cuerpo |
| `ListarProductosRequest` | `optional Categoria categoria` | `?categoria=` de REST |
| `ListarProductosResponse` | `repeated Producto productos` | lista JSON |
| `CategoriaInfo` | `Categoria codigo`, `string etiqueta` | `CategoriaResponse` / `CategoriaInfoGql` |
| `ListarCategoriasResponse` | `repeated CategoriaInfo categorias` | lista JSON |
| `google.protobuf.Empty` | ninguno | respuesta de la baja y petición de categorías |

Enumeración `Categoria` del contrato: `CATEGORIA_SIN_ESPECIFICAR = 0` (obligatorio en proto3) y luego `AUDIO = 1`, `PERIFERICOS = 2`, `PANTALLAS = 3`, `COMPUTO = 4`, `ALMACENAMIENTO = 5`, `CONECTIVIDAD = 6`, `ENERGIA = 7`, `MOBILIARIO = 8`, en el mismo orden que la enumeración del dominio.

## Mapeo (en `ProductoGrpcMapper`)

| Dirección | Regla |
|---|---|
| `ProductoInput` → `Producto` | `nombre.trim()`; `descripcion` presente y no vacía → `trim()`, ausente o vacía → `null`; `precio` → `BigDecimal(texto)`, si `NumberFormatException` o vacío → `DatosDeProductoInvalidosException("El precio debe ser un numero decimal")`; `categoria` → `CATEGORIA_SIN_ESPECIFICAR`/`UNRECOGNIZED` → `Categoria.desde(null)` (lanza "La categoria es obligatoria"), cualquier otro → `Categoria.desde(nombreDelValor)` |
| `Producto` → mensaje `Producto` | `id` obligatorio (un producto persistido siempre lo tiene); `precio.toPlainString()`; `categoria` por nombre; `categoria_etiqueta = categoria.etiqueta`; `descripcion` solo se asigna si no es `null` |
| `ListarProductosRequest` → argumento de `listar` | si `hasCategoria()` y no es `CATEGORIA_SIN_ESPECIFICAR` → `Categoria.desde(nombre)`, si no → `null` (catálogo completo) |
| `Categoria` (dominio) → `CategoriaInfo` | `codigo = Categoria.valueOf(nombre)`, `etiqueta` |

## Estados y transiciones

No hay máquina de estados: un producto existe o no existe. Las transiciones son crear (nuevo id), actualizar (mismo id, datos nuevos) y eliminar (deja de existir; una lectura posterior responde `NOT_FOUND`).

## Errores (contrato de la frontera)

| Excepción del dominio | `Status` gRPC | Descripción del estado |
|---|---|---|
| `DatosDeProductoInvalidosException` | `INVALID_ARGUMENT` | mensaje del dominio |
| `ProductoNoEncontradoException` | `NOT_FOUND` | "No existe un producto con id N" |
| `NombreDeProductoDuplicadoException` | `ALREADY_EXISTS` | "Ya existe un producto registrado con el nombre 'X'" |
| cualquier otra | `UNKNOWN` | sin descripción |
