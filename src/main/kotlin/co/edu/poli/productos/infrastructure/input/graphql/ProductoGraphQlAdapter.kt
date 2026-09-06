package co.edu.poli.productos.infrastructure.input.graphql

import co.edu.poli.productos.application.port.input.GestionarProductosUseCase
import co.edu.poli.productos.domain.exception.ProductoNoEncontradoException
import co.edu.poli.productos.domain.model.Categoria
import co.edu.poli.productos.infrastructure.input.graphql.dto.CategoriaInfoGql
import co.edu.poli.productos.infrastructure.input.graphql.dto.ProductoGql
import co.edu.poli.productos.infrastructure.input.graphql.dto.ProductoInputGql
import co.edu.poli.productos.infrastructure.input.graphql.mapper.ProductoGraphQlMapper
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

/**
 * Tercer adaptador de entrada: GraphQL.
 *
 * Es el mismo patron del adaptador REST y del adaptador web con Thymeleaf.
 * Depende solo del puerto [GestionarProductosUseCase], asi que el nucleo de la
 * aplicacion no cambia ni una linea por publicar el catalogo en un protocolo
 * mas. Esa es la prueba practica de que el hexagono esta bien cerrado.
 *
 * La diferencia visible con REST esta en la forma de consultar, no en el
 * dominio: aqui el cliente declara en la consulta que campos necesita y recibe
 * exactamente esos, en vez de escoger entre las representaciones fijas que el
 * servidor decidio publicar en cada ruta.
 */
@Controller
class ProductoGraphQlAdapter(
	private val casoDeUso: GestionarProductosUseCase,
) {

	/**
	 * El argumento `categoria` es opcional en el esquema. Cuando no viene, llega
	 * como null y el caso de uso devuelve el catalogo completo.
	 */
	@QueryMapping
	fun productos(@Argument categoria: String?): List<ProductoGql> =
		casoDeUso.listar(categoria?.let(Categoria::desde))
			.map(ProductoGraphQlMapper::aRespuesta)

	/**
	 * Un producto inexistente no es un error del protocolo sino una ausencia:
	 * el campo se declaro anulable en el esquema, asi que se responde null y la
	 * consulta sigue siendo valida. Es la convencion habitual en GraphQL, y la
	 * diferencia con el 404 que devuelve el adaptador REST para el mismo caso.
	 */
	@QueryMapping
	fun producto(@Argument id: Long): ProductoGql? = try {
		ProductoGraphQlMapper.aRespuesta(casoDeUso.obtener(id))
	} catch (_: ProductoNoEncontradoException) {
		null
	}

	@QueryMapping
	fun categorias(): List<CategoriaInfoGql> =
		Categoria.entries.map { CategoriaInfoGql(it.name, it.etiqueta) }

	@MutationMapping
	fun crearProducto(@Argument entrada: ProductoInputGql): ProductoGql =
		ProductoGraphQlMapper.aRespuesta(casoDeUso.crear(ProductoGraphQlMapper.aDominio(entrada)))

	@MutationMapping
	fun actualizarProducto(@Argument id: Long, @Argument entrada: ProductoInputGql): ProductoGql =
		ProductoGraphQlMapper.aRespuesta(
			casoDeUso.actualizar(id, ProductoGraphQlMapper.aDominio(entrada)),
		)

	@MutationMapping
	fun eliminarProducto(@Argument id: Long): Boolean {
		casoDeUso.eliminar(id)
		return true
	}
}
