package co.edu.poli.productos.mapper

import co.edu.poli.productos.dto.ProductoRequest
import co.edu.poli.productos.dto.ProductoResponse
import co.edu.poli.productos.model.Producto

/**
 * Traduccion entre la frontera HTTP (DTO) y el modelo persistente (entidad).
 * Aisla el esquema de la base de datos del contrato publico de la API.
 */
object ProductoMapper {

	fun aEntidad(request: ProductoRequest): Producto = Producto(
		nombre = request.nombre!!.trim(),
		descripcion = request.descripcion?.trim(),
		precio = request.precio!!,
	)

	fun copiarSobre(destino: Producto, request: ProductoRequest): Producto = destino.apply {
		nombre = request.nombre!!.trim()
		descripcion = request.descripcion?.trim()
		precio = request.precio!!
	}

	fun aRespuesta(producto: Producto): ProductoResponse = ProductoResponse(
		id = requireNonNullId(producto),
		nombre = producto.nombre,
		descripcion = producto.descripcion,
		precio = producto.precio,
	)

	private fun requireNonNullId(producto: Producto): Long =
		producto.id ?: error("El producto persistido debe tener identificador")
}
