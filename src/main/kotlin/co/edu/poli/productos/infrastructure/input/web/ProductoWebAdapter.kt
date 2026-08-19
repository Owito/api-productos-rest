package co.edu.poli.productos.infrastructure.input.web

import co.edu.poli.productos.application.port.input.GestionarProductosUseCase
import co.edu.poli.productos.domain.exception.DatosDeProductoInvalidosException
import co.edu.poli.productos.domain.model.Categoria
import co.edu.poli.productos.domain.exception.NombreDeProductoDuplicadoException
import co.edu.poli.productos.infrastructure.input.web.form.ProductoForm
import jakarta.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.mvc.support.RedirectAttributes

/**
 * Segundo adaptador de entrada: interfaz web renderizada en el servidor.
 *
 * Es la demostracion practica de la arquitectura hexagonal. Consume exactamente
 * el mismo puerto que el adaptador REST, `GestionarProductosUseCase`, y para
 * agregarlo no hubo que tocar ni una linea del dominio, de la capa de
 * aplicacion ni del adaptador de persistencia.
 *
 * A diferencia del adaptador REST, aqui si se atrapan las excepciones de
 * negocio: una interfaz de usuario no responde con un 409, repinta el
 * formulario con el error al lado del campo que lo causo.
 */
@Controller
@RequestMapping("/productos")
class ProductoWebAdapter(
	private val casoDeUso: GestionarProductosUseCase,
) {

	/** Todas las vistas ofrecen el catalogo de categorias para pintar filtros y selectores. */
	@ModelAttribute("categorias")
	fun categorias(): List<Categoria> = Categoria.entries

	@GetMapping
	fun listar(@RequestParam(required = false) categoria: String?, model: Model): String {
		val filtro = categoria?.takeIf { it.isNotBlank() }?.let(Categoria::desde)
		model.addAttribute("productos", casoDeUso.listar(filtro))
		model.addAttribute("filtro", filtro)
		return "productos/lista"
	}

	@GetMapping("/nuevo")
	fun formularioDeCreacion(model: Model): String {
		model.addAttribute("producto", ProductoForm())
		model.addAttribute("modoEdicion", false)
		return "productos/formulario"
	}

	@PostMapping
	fun crear(
		@Valid @ModelAttribute("producto") form: ProductoForm,
		errores: BindingResult,
		model: Model,
		redirect: RedirectAttributes,
	): String {
		if (errores.hasErrors()) {
			return volverAlFormulario(model, false)
		}
		return try {
			val creado = casoDeUso.crear(form.aDominio())
			redirect.addFlashAttribute("mensaje", "Se creo el producto \"${creado.nombre}\".")
			"redirect:/productos"
		} catch (ex: NombreDeProductoDuplicadoException) {
			errores.rejectValue("nombre", "duplicado", ex.message ?: "Ese nombre ya esta registrado")
			volverAlFormulario(model, false)
		} catch (ex: DatosDeProductoInvalidosException) {
			errores.reject("dominio", ex.message ?: "Los datos del producto no son validos")
			volverAlFormulario(model, false)
		}
	}

	@GetMapping("/{id}/editar")
	fun formularioDeEdicion(@PathVariable id: Long, model: Model): String {
		model.addAttribute("producto", ProductoForm.desdeDominio(casoDeUso.obtener(id)))
		model.addAttribute("modoEdicion", true)
		return "productos/formulario"
	}

	@PutMapping("/{id}")
	fun actualizar(
		@PathVariable id: Long,
		@Valid @ModelAttribute("producto") form: ProductoForm,
		errores: BindingResult,
		model: Model,
		redirect: RedirectAttributes,
	): String {
		if (errores.hasErrors()) {
			return volverAlFormulario(model, true)
		}
		return try {
			val actualizado = casoDeUso.actualizar(id, form.aDominio())
			redirect.addFlashAttribute("mensaje", "Se actualizo el producto \"${actualizado.nombre}\".")
			"redirect:/productos"
		} catch (ex: NombreDeProductoDuplicadoException) {
			errores.rejectValue("nombre", "duplicado", ex.message ?: "Ese nombre ya esta registrado")
			volverAlFormulario(model, true)
		} catch (ex: DatosDeProductoInvalidosException) {
			errores.reject("dominio", ex.message ?: "Los datos del producto no son validos")
			volverAlFormulario(model, true)
		}
	}

	@DeleteMapping("/{id}")
	fun eliminar(@PathVariable id: Long, redirect: RedirectAttributes): String {
		val producto = casoDeUso.obtener(id)
		casoDeUso.eliminar(id)
		redirect.addFlashAttribute("mensaje", "Se elimino el producto \"${producto.nombre}\".")
		return "redirect:/productos"
	}

	private fun volverAlFormulario(model: Model, modoEdicion: Boolean): String {
		model.addAttribute("modoEdicion", modoEdicion)
		return "productos/formulario"
	}
}
