package co.edu.poli.productos.infrastructure.input.grpc.mapper

import co.edu.poli.productos.domain.exception.DatosDeProductoInvalidosException
import co.edu.poli.productos.domain.model.Categoria
import co.edu.poli.productos.domain.model.Producto
import co.edu.poli.productos.infrastructure.input.grpc.proto.CategoriaInfo
import co.edu.poli.productos.infrastructure.input.grpc.proto.ListarProductosRequest
import co.edu.poli.productos.infrastructure.input.grpc.proto.ProductoInput
import java.math.BigDecimal
import co.edu.poli.productos.infrastructure.input.grpc.proto.Categoria as CategoriaMensaje
import co.edu.poli.productos.infrastructure.input.grpc.proto.Producto as ProductoMensaje

/**
 * Traduce entre los mensajes del contrato gRPC y el modelo de dominio.
 *
 * Los mensajes los genera protoc a partir de productos.proto, asi que este es
 * el unico lugar del adaptador que conoce las dos representaciones. Igual que
 * en REST y GraphQL, aqui no se validan reglas de negocio: se construye el
 * Producto y es su bloque init el que las defiende. Lo unico que este mapeador
 * decide son conversiones de tipo que el contrato no puede expresar.
 */
object ProductoGrpcMapper {

	fun aDominio(entrada: ProductoInput): Producto = Producto(
		nombre = entrada.nombre.trim(),
		// Un campo optional ausente y una cadena vacia significan lo mismo
		// para el dominio: no hay descripcion.
		descripcion = if (entrada.hasDescripcion()) entrada.descripcion.trim().ifEmpty { null } else null,
		precio = precioDesde(entrada.precio),
		categoria = categoriaDesde(entrada.categoria),
	)

	fun aMensaje(producto: Producto): ProductoMensaje {
		val constructor = ProductoMensaje.newBuilder()
			.setId(producto.id ?: error("Un producto persistido siempre tiene identificador"))
			.setNombre(producto.nombre)
			// toPlainString evita la notacion cientifica que toString podria
			// producir con ciertos valores; el cliente recibe "289900.00".
			.setPrecio(producto.precio.toPlainString())
			.setCategoria(CategoriaMensaje.valueOf(producto.categoria.name))
			.setCategoriaEtiqueta(producto.categoria.etiqueta)
		producto.descripcion?.let(constructor::setDescripcion)
		return constructor.build()
	}

	/**
	 * Filtro de la consulta. Ausente, o presente con el valor reservado 0,
	 * significa catalogo completo.
	 */
	fun categoriaDe(peticion: ListarProductosRequest): Categoria? =
		if (peticion.hasCategoria() && peticion.categoria != CategoriaMensaje.CATEGORIA_SIN_ESPECIFICAR) {
			categoriaDesde(peticion.categoria)
		} else {
			null
		}

	fun aCategoriaInfo(categoria: Categoria): CategoriaInfo = CategoriaInfo.newBuilder()
		.setCodigo(CategoriaMensaje.valueOf(categoria.name))
		.setEtiqueta(categoria.etiqueta)
		.build()

	/**
	 * En proto3 el valor 0 de una enumeracion es el que llega cuando el
	 * cliente no manda el campo, por eso se reserva y se traduce a la misma
	 * regla que aplica REST cuando la categoria viene vacia. UNRECOGNIZED es
	 * un numero que este servidor no conoce; recibe el mismo trato.
	 */
	private fun categoriaDesde(valor: CategoriaMensaje): Categoria = when (valor) {
		CategoriaMensaje.CATEGORIA_SIN_ESPECIFICAR, CategoriaMensaje.UNRECOGNIZED -> Categoria.desde(null)
		else -> Categoria.desde(valor.name)
	}

	/**
	 * El precio viaja como texto porque proto3 no tiene tipo decimal y double
	 * perderia precision con dinero. Un texto que no es un numero es un dato
	 * invalido del mismo rango que un precio negativo, asi que se reporta con
	 * la excepcion del dominio y sale como INVALID_ARGUMENT.
	 */
	private fun precioDesde(texto: String): BigDecimal = try {
		BigDecimal(texto.trim())
	} catch (_: NumberFormatException) {
		throw DatosDeProductoInvalidosException("El precio debe ser un numero decimal")
	}
}
