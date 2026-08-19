package co.edu.poli.productos.infrastructure.input.web

import co.edu.poli.productos.infrastructure.config.CreditosProperties
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

/** Pagina de creditos academicos del trabajo. */
@Controller
class CreditosWebAdapter(
	private val creditos: CreditosProperties,
) {

	@GetMapping("/creditos")
	fun creditos(model: Model): String {
		model.addAttribute("creditos", creditos)
		return "creditos"
	}
}
