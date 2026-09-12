package co.edu.poli.productos.infrastructure.input.grpc.error

import co.edu.poli.productos.domain.exception.DatosDeProductoInvalidosException
import co.edu.poli.productos.domain.exception.NombreDeProductoDuplicadoException
import co.edu.poli.productos.domain.exception.ProductoNoEncontradoException
import io.grpc.Status
import io.grpc.StatusException
import org.springframework.grpc.server.exception.GrpcExceptionHandler
import org.springframework.stereotype.Component

/**
 * Traduce las excepciones del dominio a estados de gRPC.
 *
 * Es el equivalente del ManejadorGlobalDeErrores del adaptador REST y del
 * ManejadorDeErroresGraphQl: el dominio lanza hechos de negocio que no saben
 * nada del protocolo, y cada frontera decide como reportarlos. Spring gRPC
 * consulta todos los beans GrpcExceptionHandler; devolver null significa "no
 * es mio", y entonces la excepcion sale como UNKNOWN sin descripcion, que es
 * la forma de no filtrar el mensaje de un error interno al cliente.
 */
@Component
class ManejadorDeErroresGrpc : GrpcExceptionHandler {

	override fun handleException(excepcion: Throwable): StatusException? {
		val estado = when (excepcion) {
			is ProductoNoEncontradoException -> Status.NOT_FOUND
			is NombreDeProductoDuplicadoException -> Status.ALREADY_EXISTS
			is DatosDeProductoInvalidosException -> Status.INVALID_ARGUMENT
			else -> return null
		}
		return estado.withDescription(excepcion.message).asException()
	}
}
