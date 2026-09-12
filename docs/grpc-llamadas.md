# Guía de llamadas gRPC

Llamadas listas para ejecutar contra el servicio en local, en el orden del video de sustentación.
Usan [grpcurl](https://github.com/fullstorydev/grpcurl) (`winget install fullstorydev.grpcurl`
en Windows, `brew install grpcurl` en macOS). En Postman: **New → gRPC Request**, servidor
`localhost:9090`, y **Use server reflection** para que liste el servicio y sus operaciones.

Antes de empezar:

```bash
./gradlew bootRun
```

El perfil `local` siembra 18 productos en H2. En el log deben aparecer Tomcat en 8080 y el
servidor gRPC en 9090. El puerto se cambia con la variable `GRPC_PORT`.

Todas las llamadas usan `-plaintext` porque la demostración local no tiene TLS.

## 1. Descubrir el servicio (reflexión)

```bash
grpcurl -plaintext localhost:9090 list
grpcurl -plaintext localhost:9090 describe productos.v1.ProductosService
grpcurl -plaintext localhost:9090 describe productos.v1.ProductoInput
```

Respuesta esperada: `productos.v1.ProductosService` con seis RPC; luego la definición de cada
mensaje tal como está en `src/main/proto/productos.proto`.

## 2. Lectura

Catálogo completo, ordenado por identificador:

```bash
grpcurl -plaintext localhost:9090 productos.v1.ProductosService/ListarProductos
```

Solo una categoría:

```bash
grpcurl -plaintext -d '{"categoria":"AUDIO"}' localhost:9090 productos.v1.ProductosService/ListarProductos
```

Un producto por identificador:

```bash
grpcurl -plaintext -d '{"id":1}' localhost:9090 productos.v1.ProductosService/ObtenerProducto
```

Respuesta esperada:

```json
{
  "id": "1",
  "nombre": "...",
  "descripcion": "...",
  "precio": "749000.00",
  "categoria": "AUDIO",
  "categoria_etiqueta": "Audio"
}
```

Dos detalles de representación de grpcurl, no del contrato: muestra `id` como texto porque los
enteros de 64 bits viajan así en la representación JSON de protobuf (en el mensaje binario es un
`int64`), y nombra los campos como en el `.proto` (`categoria_etiqueta`); Postman y los stubs
generados usan `categoriaEtiqueta`.

Catálogo de categorías:

```bash
grpcurl -plaintext localhost:9090 productos.v1.ProductosService/ListarCategorias
```

Respuesta esperada: 8 categorías con `codigo` y `etiqueta`.

## 3. Escritura

Crear:

```bash
grpcurl -plaintext -d '{"nombre":"Teclado 60","descripcion":"Compacto, 60 por ciento","precio":"289900.00","categoria":"PERIFERICOS"}' localhost:9090 productos.v1.ProductosService/CrearProducto
```

Respuesta esperada: el producto con `id` asignado (19 si la base es la recién sembrada).

Actualizar (conserva el identificador):

```bash
grpcurl -plaintext -d '{"id":19,"datos":{"nombre":"Teclado 65","descripcion":"Compacto, 65 por ciento","precio":"310000.00","categoria":"PERIFERICOS"}}' localhost:9090 productos.v1.ProductosService/ActualizarProducto
```

Eliminar (responde vacío):

```bash
grpcurl -plaintext -d '{"id":19}' localhost:9090 productos.v1.ProductosService/EliminarProducto
```

Respuesta esperada: `{}`. Una lectura posterior del 19 responde `NotFound`.

## 4. Errores del dominio, traducidos en la frontera

Producto inexistente:

```bash
grpcurl -plaintext -d '{"id":999999}' localhost:9090 productos.v1.ProductosService/ObtenerProducto
```

```text
ERROR:
  Code: NotFound
  Message: No existe un producto con id 999999
```

Nombre duplicado (sin importar mayúsculas):

```bash
grpcurl -plaintext -d '{"nombre":"Teclado 60","precio":"1","categoria":"PERIFERICOS"}' localhost:9090 productos.v1.ProductosService/CrearProducto
grpcurl -plaintext -d '{"nombre":"teclado 60","precio":"1","categoria":"PERIFERICOS"}' localhost:9090 productos.v1.ProductosService/CrearProducto
```

La segunda responde `Code: AlreadyExists`.

Datos inválidos (nombre vacío, precio no numérico, categoría ausente):

```bash
grpcurl -plaintext -d '{"nombre":"","precio":"100","categoria":"AUDIO"}' localhost:9090 productos.v1.ProductosService/CrearProducto
grpcurl -plaintext -d '{"nombre":"Cable","precio":"abc","categoria":"AUDIO"}' localhost:9090 productos.v1.ProductosService/CrearProducto
grpcurl -plaintext -d '{"nombre":"Cable","precio":"10"}' localhost:9090 productos.v1.ProductosService/CrearProducto
```

Las tres responden `Code: InvalidArgument` con el mensaje de la regla incumplida: "El nombre del
producto es obligatorio", "El precio debe ser un numero decimal" y "La categoria es obligatoria".

Una categoría que no existe en el contrato la rechaza el propio grpcurl antes de enviar la
petición, igual que el motor de GraphQL rechaza un valor fuera de la enumeración:

```bash
grpcurl -plaintext -d '{"nombre":"Cable","precio":"10","categoria":"INVENTADA"}' localhost:9090 productos.v1.ProductosService/CrearProducto
```

## 5. Las mismas operaciones en los otros adaptadores

Para el bloque comparativo del video:

| Operación | REST | GraphQL | gRPC |
|---|---|---|---|
| Listar | `GET /api/v1/productos?categoria=AUDIO` | `{ productos(categoria: AUDIO) { nombre } }` | `ListarProductos {"categoria":"AUDIO"}` |
| Obtener | `GET /api/v1/productos/1` | `{ producto(id: 1) { nombre } }` | `ObtenerProducto {"id":1}` |
| Crear | `POST /api/v1/productos` | `mutation { crearProducto(entrada: {...}) { id } }` | `CrearProducto {...}` |
| Inexistente | `404` | `null` (campo anulable) | `NotFound` |
| Duplicado | `409` | `BAD_REQUEST` | `AlreadyExists` |
| Inválido | `400` | `BAD_REQUEST` | `InvalidArgument` |
