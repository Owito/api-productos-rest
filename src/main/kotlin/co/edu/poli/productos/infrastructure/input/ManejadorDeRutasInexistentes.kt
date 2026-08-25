package co.edu.poli.productos.infrastructure.input

import co.edu.poli.productos.infrastructure.input.rest.error.ApiError
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.NoHandlerFoundException
import org.springframework.web.servlet.resource.NoResourceFoundException

/**
 * Ruta que no existe en ningun adaptador de entrada.
 *
 * Este manejador vive fuera de `input/rest` y de `input/web`, y no lleva
 * selectores, por una razon concreta: los advice acotados por paquete o por
 * tipo solo se consultan cuando la peticion llego a un controlador. Aqui no
 * llego a ninguno, asi que un advice acotado nunca se entera. Por eso el
 * manejador de la API no podia cubrir este caso, aunque declarara el
 * `NoHandlerFoundException`.
 *
 * Se atienden dos excepciones distintas porque el contenedor produce una u otra
 * segun quien reclame la ruta. Con recursos estaticos publicados (`/css`, `/js`)
 * el mapeo de recursos responde por cualquier ruta, encuentra "handler" y lanza
 * `NoResourceFoundException`; sin ese mapeo el DispatcherServlet no encuentra
 * nada y lanza `NoHandlerFoundException`. Declarar solo una de las dos deja el
 * hueco abierto.
 *
 * El reparto de formato es el mismo que sostiene el resto del proyecto: bajo
 * `/api` manda el contrato de error de la API, y en cualquier otra ruta manda la
 * pagina de error, porque una persona en el navegador no lee JSON.
 */
@ControllerAdvice
class ManejadorDeRutasInexistentes {

	/**
	 * El tipo de retorno es `Any` a proposito: Spring elige como escribir la
	 * respuesta segun el objeto devuelto, y aqui una misma excepcion sale como
	 * JSON o como HTML. Devolver un tipo concreto obligaria a partir esto en dos
	 * manejadores que no pueden coexistir, porque atenderian la misma excepcion.
	 */
	@ExceptionHandler(NoHandlerFoundException::class, NoResourceFoundException::class)
	fun rutaInexistente(req: HttpServletRequest): Any =
		if (esRutaDeApi(req)) comoJson(req) else comoPagina(req)

	private fun esRutaDeApi(req: HttpServletRequest) = req.requestURI.startsWith(PREFIJO_DE_API)

	private fun comoJson(req: HttpServletRequest): ResponseEntity<ApiError> =
		ResponseEntity.status(HttpStatus.NOT_FOUND).body(
			ApiError(
				estado = HttpStatus.NOT_FOUND.value(),
				error = HttpStatus.NOT_FOUND.reasonPhrase,
				mensaje = "La ruta " + req.requestURI + " no existe en esta API",
				ruta = req.requestURI,
			),
		)

	private fun comoPagina(req: HttpServletRequest): ModelAndView = ModelAndView(
		"error/no-encontrado",
		mapOf(
			"codigo" to "Error 404",
			"titulo" to "Pagina no encontrada",
			"mensaje" to "La direccion " + req.requestURI + " no corresponde a ninguna pagina del sitio.",
			"pista" to "Revisa la direccion, o vuelve al catalogo desde el menu de arriba.",
		),
		HttpStatus.NOT_FOUND,
	)

	private companion object {
		const val PREFIJO_DE_API = "/api/"
	}
}
