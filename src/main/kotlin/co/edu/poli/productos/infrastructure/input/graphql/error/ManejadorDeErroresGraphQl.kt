package co.edu.poli.productos.infrastructure.input.graphql.error

import co.edu.poli.productos.domain.exception.DatosDeProductoInvalidosException
import co.edu.poli.productos.domain.exception.NombreDeProductoDuplicadoException
import co.edu.poli.productos.domain.exception.ProductoNoEncontradoException
import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.schema.DataFetchingEnvironment
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter
import org.springframework.graphql.execution.ErrorType
import org.springframework.stereotype.Component

/**
 * Traduce las excepciones del dominio a errores de GraphQL.
 *
 * Es el equivalente del ManejadorGlobalDeErrores del adaptador REST, y existe
 * por la misma razon: el dominio lanza hechos de negocio que no saben nada del
 * protocolo, y cada frontera decide como reportarlos. Sin esto, GraphQL
 * responderia un INTERNAL_ERROR generico con el mensaje oculto, que es su
 * comportamiento por defecto para no filtrar detalles del servidor.
 */
@Component
class ManejadorDeErroresGraphQl : DataFetcherExceptionResolverAdapter() {

	override fun resolveToSingleError(
		excepcion: Throwable,
		entorno: DataFetchingEnvironment,
	): GraphQLError? {
		val tipo = when (excepcion) {
			is ProductoNoEncontradoException -> ErrorType.NOT_FOUND
			is NombreDeProductoDuplicadoException -> ErrorType.BAD_REQUEST
			is DatosDeProductoInvalidosException -> ErrorType.BAD_REQUEST
			// Cualquier otra cosa no es del dominio: se deja pasar para que
			// Spring la trate como error interno y no revele su mensaje.
			else -> return null
		}
		return GraphqlErrorBuilder.newError(entorno)
			.errorType(tipo)
			.message(excepcion.message)
			.build()
	}
}
