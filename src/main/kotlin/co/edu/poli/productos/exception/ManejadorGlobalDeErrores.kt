package co.edu.poli.productos.exception

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.NoHandlerFoundException

/**
 * Manejo centralizado de errores. Ningun controlador atrapa excepciones:
 * todas terminan aqui y salen con el mismo contrato y el codigo HTTP correcto.
 */
@RestControllerAdvice
class ManejadorGlobalDeErrores {

	private val log = LoggerFactory.getLogger(javaClass)

	@ExceptionHandler(RecursoNoEncontradoException::class)
	fun noEncontrado(ex: RecursoNoEncontradoException, req: HttpServletRequest): ResponseEntity<ApiError> =
		responder(HttpStatus.NOT_FOUND, ex.message ?: "Recurso no encontrado", req)

	@ExceptionHandler(ReglaDeNegocioException::class)
	fun reglaDeNegocio(ex: ReglaDeNegocioException, req: HttpServletRequest): ResponseEntity<ApiError> =
		responder(HttpStatus.CONFLICT, ex.message ?: "Conflicto con una regla de negocio", req)

	/** Falla la validacion declarativa de los DTO anotados con @Valid. */
	@ExceptionHandler(MethodArgumentNotValidException::class)
	fun validacion(ex: MethodArgumentNotValidException, req: HttpServletRequest): ResponseEntity<ApiError> {
		val detalles = ex.bindingResult.fieldErrors.map {
			ErrorDeCampo(campo = it.field, mensaje = it.defaultMessage ?: "Valor invalido")
		}
		return responder(HttpStatus.BAD_REQUEST, "La peticion tiene campos invalidos", req, detalles)
	}

	/** El cuerpo no es JSON valido o un tipo no corresponde (por ejemplo precio: "abc"). */
	@ExceptionHandler(HttpMessageNotReadableException::class)
	fun cuerpoIlegible(ex: HttpMessageNotReadableException, req: HttpServletRequest): ResponseEntity<ApiError> =
		responder(HttpStatus.BAD_REQUEST, "El cuerpo de la peticion no se pudo leer como JSON valido", req)

	/** El identificador de la ruta no es del tipo esperado (por ejemplo /productos/abc). */
	@ExceptionHandler(MethodArgumentTypeMismatchException::class)
	fun tipoInvalido(ex: MethodArgumentTypeMismatchException, req: HttpServletRequest): ResponseEntity<ApiError> =
		responder(HttpStatus.BAD_REQUEST, "El parametro '${ex.name}' no tiene un formato valido", req)

	@ExceptionHandler(NoHandlerFoundException::class)
	fun rutaInexistente(ex: NoHandlerFoundException, req: HttpServletRequest): ResponseEntity<ApiError> =
		responder(HttpStatus.NOT_FOUND, "La ruta ${ex.requestURL} no existe en esta API", req)

	/** Red de seguridad: nada sale al cliente como traza de pila. */
	@ExceptionHandler(Exception::class)
	fun inesperado(ex: Exception, req: HttpServletRequest): ResponseEntity<ApiError> {
		log.error("Error no controlado en {} {}", req.method, req.requestURI, ex)
		return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrio un error inesperado en el servidor", req)
	}

	private fun responder(
		estado: HttpStatus,
		mensaje: String,
		req: HttpServletRequest,
		detalles: List<ErrorDeCampo> = emptyList(),
	): ResponseEntity<ApiError> = ResponseEntity.status(estado).body(
		ApiError(
			estado = estado.value(),
			error = estado.reasonPhrase,
			mensaje = mensaje,
			ruta = req.requestURI,
			detalles = detalles,
		),
	)
}
