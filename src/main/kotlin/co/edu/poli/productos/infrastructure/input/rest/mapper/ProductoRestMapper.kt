package co.edu.poli.productos.infrastructure.input.rest.mapper

import co.edu.poli.productos.domain.model.Producto
import co.edu.poli.productos.infrastructure.input.rest.dto.ProductoRequest
import co.edu.poli.productos.infrastructure.input.rest.dto.ProductoResponse

/** Traduce entre los DTO del borde HTTP y el modelo de dominio. */
object ProductoRestMapper {

	fun aDominio(request: ProductoRequest): Producto = Producto(
		nombre = request.nombre.orEmpty().trim(),
		descripcion = request.descripcion?.trim(),
		precio = request.precio ?: java.math.BigDecimal.ZERO,
	)

	fun aRespuesta(producto: Producto): ProductoResponse = ProductoResponse(
		id = producto.id ?: error("Un producto persistido siempre tiene identificador"),
		nombre = producto.nombre,
		descripcion = producto.descripcion,
		precio = producto.precio,
	)
}
