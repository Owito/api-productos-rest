package co.edu.poli.productos.infrastructure.input.web

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

/** Manda la raiz del sitio al listado, para que la aplicacion abra en algo util. */
@Controller
class RaizWebAdapter {

	@GetMapping("/")
	fun raiz(): String = "redirect:/productos"
}
