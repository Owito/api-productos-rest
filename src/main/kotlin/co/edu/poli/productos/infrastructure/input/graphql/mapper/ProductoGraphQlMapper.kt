package co.edu.poli.productos.infrastructure.input.graphql.mapper

import co.edu.poli.productos.domain.model.Categoria
import co.edu.poli.productos.domain.model.Producto
import co.edu.poli.productos.infrastructure.input.graphql.dto.ProductoGql
import co.edu.poli.productos.infrastructure.input.graphql.dto.ProductoInputGql
import java.math.BigDecimal

/** Traduce entre los tipos del esquema GraphQL y el modelo de dominio. */
object ProductoGraphQlMapper {

	fun aDominio(entrada: ProductoInputGql): Producto = Producto(
		nombre = entrada.nombre.orEmpty().trim(),
		descripcion = entrada.descripcion?.trim(),
		precio = entrada.precio ?: BigDecimal.ZERO,
		categoria = Categoria.desde(entrada.categoria),
	)

	fun aRespuesta(producto: Producto): ProductoGql = ProductoGql(
		id = producto.id ?: error("Un producto persistido siempre tiene identificador"),
		nombre = producto.nombre,
		descripcion = producto.descripcion,
		precio = producto.precio,
		categoria = producto.categoria.name,
		categoriaEtiqueta = producto.categoria.etiqueta,
	)
}
