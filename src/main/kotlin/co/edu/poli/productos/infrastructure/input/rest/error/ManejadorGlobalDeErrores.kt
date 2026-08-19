package co.edu.poli.productos.infrastructure.input.rest.error

import co.edu.poli.productos.domain.exception.DatosDeProductoInvalidosException
import co.edu.poli.productos.domain.exception.NombreDeProductoDuplicadoException
import co.edu.poli.productos.domain.exception.ProductoNoEncontradoException
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
 * Traduce las excepciones del dominio a codigos de estado HTTP.
 *
 * Esta clase es la frontera: el nucleo lanza hechos del negocio y aqui, y solo
 * aqui, se convierten en 404, 409 o 400.
 *
 * Esta acotado por paquete a proposito. Sin ese limite tambien atenderia al
 * adaptador web, que necesita responder HTML y no JSON.
 */
@RestControllerAdvice(basePackages = ["co.edu.poli.productos.infrastructure.input.rest"])
class ManejadorGlobalDeErrores {

	private val log = LoggerFactory.getLogger(javaClass)

	@ExceptionHandler(ProductoNoEncontradoException::class)
	fun noEncontrado(ex: ProductoNoEncontradoException, req: HttpServletRequest): ResponseEntity<ApiError> =
		responder(HttpStatus.NOT_FOUND, ex.message ?: "Recurso no encontrado", req)

	@ExceptionHandler(NombreDeProductoDuplicadoException::class)
	fun duplicado(ex: NombreDeProductoDuplicadoException, req: HttpServletRequest): ResponseEntity<ApiError> =
		responder(HttpStatus.CONFLICT, ex.message ?: "Conflicto con una regla de negocio", req)

	/** Invariante del dominio rota: la peticion paso la validacion del borde pero el modelo la rechaza. */
	@ExceptionHandler(DatosDeProductoInvalidosException::class)
	fun dominioInvalido(ex: DatosDeProductoInvalidosException, req: HttpServletRequest): ResponseEntity<ApiError> =
		responder(HttpStatus.BAD_REQUEST, ex.message ?: "Datos de producto invalidos", req)

	/** Falla la validacion declarativa de los DTO anotados con @Valid. */
	@ExceptionHandler(MethodArgumentNotValidException::class)
	fun validacion(ex: MethodArgumentNotValidException, req: HttpServletRequest): ResponseEntity<ApiError> {
		val detalles = ex.bindingResult.fieldErrors.map {
			ErrorDeCampo(campo = it.field, mensaje = it.defaultMessage ?: "Valor invalido")
		}
		return responder(HttpStatus.BAD_REQUEST, "La peticion tiene campos invalidos", req, detalles)
	}

	/** El cuerpo no es JSON valido o un tipo no corresponde. */
	@ExceptionHandler(HttpMessageNotReadableException::class)
	fun cuerpoIlegible(ex: HttpMessageNotReadableException, req: HttpServletRequest): ResponseEntity<ApiError> =
		responder(HttpStatus.BAD_REQUEST, "El cuerpo de la peticion no se pudo leer como JSON valido", req)

	/** El identificador de la ruta no es del tipo esperado, por ejemplo /productos/abc. */
	@ExceptionHandler(MethodArgumentTypeMismatchException::class)
	fun tipoInvalido(ex: MethodArgumentTypeMismatchException, req: HttpServletRequest): ResponseEntity<ApiError> =
		responder(HttpStatus.BAD_REQUEST, "El parametro " + ex.name + " no tiene un formato valido", req)

	@ExceptionHandler(NoHandlerFoundException::class)
	fun rutaInexistente(ex: NoHandlerFoundException, req: HttpServletRequest): ResponseEntity<ApiError> =
		responder(HttpStatus.NOT_FOUND, "La ruta " + ex.requestURL + " no existe en esta API", req)

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
