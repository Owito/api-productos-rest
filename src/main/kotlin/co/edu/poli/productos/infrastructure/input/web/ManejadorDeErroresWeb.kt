package co.edu.poli.productos.infrastructure.input.web

import co.edu.poli.productos.domain.exception.ProductoNoEncontradoException
import org.springframework.http.HttpStatus
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus

/**
 * Manejo de errores del adaptador web.
 *
 * El advice del adaptador REST responde JSON, asi que esta acotado a su propio
 * paquete. Aqui pasa lo mismo al reves: este solo atiende a la interfaz web y
 * devuelve una pagina HTML.
 */
@ControllerAdvice(assignableTypes = [ProductoWebAdapter::class])
class ManejadorDeErroresWeb {

	@ExceptionHandler(ProductoNoEncontradoException::class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	fun noEncontrado(ex: ProductoNoEncontradoException, model: Model): String {
		model.addAttribute("mensaje", ex.message)
		return "error/no-encontrado"
	}
}
