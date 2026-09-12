# Quickstart: validar el servicio gRPC de punta a punta

Guía de validación de la feature `001-grpc-productos`. Prueba el comportamiento descrito en [spec.md](spec.md) contra el contrato de [contracts/productos.proto](contracts/productos.proto).

## Prerrequisitos

- JDK 17 (el wrapper de Gradle trae el resto).
- `grpcurl` para las llamadas manuales: `winget install fullstorydev.grpcurl` en Windows o `brew install grpcurl` en macOS. Postman también sirve (Nuevo → gRPC Request → `localhost:9090` → "Use server reflection").

## 1. Pruebas automáticas (no abren puerto)

```bash
./gradlew test
```

Esperado: la suite completa en verde, con las 54 pruebas previas más las del adaptador gRPC (`ProductoGrpcAdapterTest`). Cubre SC-003 y FR-013.

Solo el adaptador gRPC:

```bash
./gradlew test --tests "co.edu.poli.productos.infrastructure.input.grpc.ProductoGrpcAdapterTest"
```

## 2. Arrancar en local

```bash
./gradlew bootRun
```

Esperado en el log: Tomcat en 8080 y `gRPC Server started, listening on address: *, port: 9090`. El perfil `local` siembra 18 productos en H2.

## 3. Descubrir el servicio (historia P3, FR-009)

```bash
grpcurl -plaintext localhost:9090 list
grpcurl -plaintext localhost:9090 describe productos.v1.ProductosService
```

Esperado: aparece `productos.v1.ProductosService` con seis RPC.

## 4. Lectura (historia P1)

```bash
grpcurl -plaintext localhost:9090 productos.v1.ProductosService/ListarProductos
grpcurl -plaintext -d '{"categoria":"AUDIO"}' localhost:9090 productos.v1.ProductosService/ListarProductos
grpcurl -plaintext -d '{"id":1}' localhost:9090 productos.v1.ProductosService/ObtenerProducto
grpcurl -plaintext -d '{"id":999999}' localhost:9090 productos.v1.ProductosService/ObtenerProducto
grpcurl -plaintext localhost:9090 productos.v1.ProductosService/ListarCategorias
```

Esperado: 18 productos ordenados por id; solo los de AUDIO; el producto 1 con sus seis campos; `Code: NotFound` con "No existe un producto con id 999999"; 8 categorías con etiqueta.

## 5. Escritura (historia P2)

```bash
grpcurl -plaintext -d '{"nombre":"Teclado 60","descripcion":"Compacto","precio":"289900.00","categoria":"PERIFERICOS"}' localhost:9090 productos.v1.ProductosService/CrearProducto
grpcurl -plaintext -d '{"id":19,"datos":{"nombre":"Teclado 65","precio":"310000.00","categoria":"PERIFERICOS"}}' localhost:9090 productos.v1.ProductosService/ActualizarProducto
grpcurl -plaintext -d '{"id":19}' localhost:9090 productos.v1.ProductosService/EliminarProducto
grpcurl -plaintext -d '{"id":19}' localhost:9090 productos.v1.ProductosService/ObtenerProducto
```

Esperado: el producto creado con `id` 19 (si la base es la sembrada); el mismo id con los datos nuevos; respuesta vacía `{}`; y luego `Code: NotFound`.

## 6. Errores del dominio (FR-007, FR-008)

```bash
grpcurl -plaintext -d '{"nombre":"teclado 60","precio":"1","categoria":"PERIFERICOS"}' localhost:9090 productos.v1.ProductosService/CrearProducto
grpcurl -plaintext -d '{"nombre":"","precio":"100","categoria":"AUDIO"}' localhost:9090 productos.v1.ProductosService/CrearProducto
grpcurl -plaintext -d '{"nombre":"Cable","precio":"abc","categoria":"AUDIO"}' localhost:9090 productos.v1.ProductosService/CrearProducto
grpcurl -plaintext -d '{"nombre":"Cable","precio":"10"}' localhost:9090 productos.v1.ProductosService/CrearProducto
```

Esperado, en orden: `AlreadyExists` (si "Teclado 60" sigue existiendo), `InvalidArgument` "El nombre del producto es obligatorio", `InvalidArgument` "El precio debe ser un numero decimal", `InvalidArgument` "La categoria es obligatoria".

## 7. Comprobar que el núcleo no cambió (SC-004)

```bash
git diff --stat main -- src/main/kotlin/co/edu/poli/productos/domain src/main/kotlin/co/edu/poli/productos/application src/main/kotlin/co/edu/poli/productos/infrastructure/output
```

Esperado: sin salida.
