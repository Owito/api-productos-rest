# Evals: Servicio gRPC de catálogo de productos

Un eval por criterio de aceptación y caso de borde de [spec.md](spec.md). Estado marcado solo tras ejecutarlo. Fecha: 2026-09-12.

Dos capas de evidencia:

- **Automática:** `./gradlew test` en la rama `arqweb-u4/grpc`, commit `1b46e52`: **79 pruebas, 0 fallos, 0 errores, 0 omitidas** (54 previas + 14 de `ProductoGrpcAdapterTest` + 11 de GraphQL recontadas en el mismo total). Resultado leído de `build/test-results/test/*.xml`.
- **Manual:** aplicación arrancada con `./gradlew bootRun` (perfil `local`, H2 sembrada con 18 productos), grpcurl v1.9.4 contra `localhost:9090` con `-plaintext`. Log: `Tomcat started on port 8080`, `gRPC Server started, listening on address: [/[0:0:0:0:0:0:0:0]:9090]`.

| Id | Origen | Pasos | Esperado | Real | Estado | Evidencia |
|---|---|---|---|---|---|---|
| E-01 | US3 esc. 1 · FR-009 | `grpcurl list` y `describe productos.v1.ProductosService` | El servicio y sus seis RPC, sin `.proto` | `productos.v1.ProductosService` listado junto a Health y ServerReflection; `describe` muestra las seis RPC | ✅ | salida de grpcurl, sección E-01 |
| E-02 | US1 esc. 1 · FR-002 | `ListarProductos` sin categoría | 18 productos ordenados por id | 18 productos, ids 1, 2, 3, ... | ✅ | grpcurl + `grep -c '"id"'` |
| E-03 | US1 esc. 2 | `ListarProductos {"categoria":"AUDIO"}` | Solo los de AUDIO | 3 devueltos, los 3 con `categoria: AUDIO` | ✅ | grpcurl |
| E-04 | US1 esc. 3 · FR-003 | `ObtenerProducto {"id":1}` | Seis campos | id, nombre, descripcion, precio `"749000.00"`, categoria `AUDIO`, categoria_etiqueta `Audio` | ✅ | grpcurl |
| E-05 | US1 esc. 4 · FR-007 | `ObtenerProducto {"id":999999}` | `NOT_FOUND` con el id | `Code: NotFound`, "No existe un producto con id 999999" | ✅ | grpcurl |
| E-06 | US1 esc. 5 | `ListarCategorias` | 8 categorías con etiqueta | 8 entradas con `codigo` y `etiqueta` | ✅ | grpcurl + `grep -c '"codigo"'` |
| E-07 | US2 esc. 1 · FR-005 | `CrearProducto` "Teclado 60", precio "289900.00", PERIFERICOS | Producto con id asignado y los mismos datos | id 19, mismos datos, precio `"289900.00"` | ✅ | grpcurl |
| E-08 | US2 esc. 2 | `ActualizarProducto` id 19 → "Teclado 65", "310000.00" | Mismo id, datos nuevos | id 19, nombre "Teclado 65", precio "310000.00" | ✅ | grpcurl |
| E-09 | US2 esc. 3 | `EliminarProducto` id 19 y luego `ObtenerProducto` 19 | `{}` y luego `NOT_FOUND` | `{}` y `Code: NotFound` "No existe un producto con id 19" | ✅ | grpcurl |
| E-10 | US2 esc. 4 · FR-006 | Crear "Teclado 60" y luego "teclado 60" | `ALREADY_EXISTS` con el nombre | `Code: AlreadyExists`, "Ya existe un producto registrado con el nombre 'teclado 60'" | ✅ | grpcurl |
| E-11 | US2 esc. 5 | Crear con nombre "" | `INVALID_ARGUMENT` con la regla | `Code: InvalidArgument`, "El nombre del producto es obligatorio" | ✅ | grpcurl |
| E-12 | edge precio · FR-004 | Crear con precio "abc" | `INVALID_ARGUMENT` | `Code: InvalidArgument`, "El precio debe ser un numero decimal" | ✅ | grpcurl |
| E-13 | edge categoría 0 | Crear sin categoría | `INVALID_ARGUMENT` "La categoria es obligatoria" | exacto | ✅ | grpcurl |
| E-14 | edge categoría inexistente | Crear con categoría "INVENTADA" | La rechaza el contrato antes de llegar al dominio | grpcurl: `enum "productos.v1.Categoria" does not have value named "INVENTADA"`, no envía la petición | ✅ | grpcurl |
| E-15 | US2 esc. 6 | Actualizar y eliminar id 999999 | `NOT_FOUND` ambos | pruebas automáticas `actualizar y eliminar un producto inexistente responden NOT_FOUND` | ✅ | `ProductoGrpcAdapterTest` |
| E-16 | edge descripción vacía | Crear sin descripción | Campo ausente en la respuesta | prueba `crear sin descripcion deja el campo ausente` en verde | ✅ | `ProductoGrpcAdapterTest` |
| E-17 | FR-008 | Fallo no previsto → `UNKNOWN` sin mensaje | Sin descripción | No se provocó un fallo interno en esta sesión; el comportamiento es el de Spring gRPC cuando ningún `GrpcExceptionHandler` responde (verificado en `GrpcServerIntegrationTests` de v0.12.0, no en este proyecto) | ❓ | Para ejecutar: arrancar con el perfil `neon` sin `DB_URL` válida y llamar `ListarProductos`; esperar `Code: Unknown` sin `Message` |
| E-18 | FR-010 | Puerto por defecto 9090 y `GRPC_PORT` | Escucha en 9090; cambia con la variable | 9090 verificado en el log. `GRPC_PORT` no probado con otro valor | 🟡 | Para ejecutar: `GRPC_PORT=9091 ./gradlew bootRun` y `grpcurl -plaintext localhost:9091 list` |
| E-19 | FR-011 · SC-004 | `git diff --stat main -- domain application infrastructure/output` | Sin salida | Ver T018 en tasks.md | ✅ | comando ejecutado antes del PR |
| E-20 | FR-013 · SC-003 | `./gradlew test` | Suite previa intacta más las nuevas | 79 / 0 fallos; ninguna prueba existente modificada (`git diff main --stat -- src/test` solo agrega archivos) | ✅ | `build/test-results` |
| E-21 | consistencia entre adaptadores | `GET /api/v1/productos` tras las escrituras gRPC | REST ve el producto creado por gRPC | REST devuelve 19 productos e incluye "Teclado 60" (id 20, creado en E-10) | ✅ | `curl` en el script de validación |
| E-22 | US3 esc. 2 · SC-005 | Ejecutar las llamadas de `docs/grpc-llamadas.md` tal cual | Corren sin modificación | Las mismas llamadas del script; la guía se ajustó para mostrar `categoria_etiqueta` como lo imprime grpcurl | ✅ | script `e2e-grpc.sh` de la sesión |

## Veredicto

20 de 22 evals ejecutados y en verde; E-17 y E-18 quedan con su comando para correrlos. Ninguna divergencia entre spec y código: no hay change request pendiente.

## Nota de herramienta

grpcurl imprime los campos con el nombre original del `.proto` (`categoria_etiqueta`), no en lowerCamelCase; Postman y los stubs generados usan `categoriaEtiqueta`. Es representación, no contrato.
