package co.edu.poli.productos.infrastructure.input.web

import co.edu.poli.productos.infrastructure.output.persistence.repository.ProductoJpaRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.model
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.view

/**
 * Pruebas del segundo adaptador de entrada.
 *
 * Verifican que la interfaz web opera sobre el mismo nucleo que la API REST y
 * que los formularios HTML emiten PUT y DELETE mediante el campo _method.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProductoWebAdapterTest {

	@Autowired lateinit var mockMvc: MockMvc
	@Autowired lateinit var repositorio: ProductoJpaRepository

	@BeforeEach
	fun limpiar() {
		repositorio.deleteAll()
	}

	private fun crear(nombre: String, precio: String = "1000.00", categoria: String = "PERIFERICOS") =
		mockMvc.perform(
			post("/productos")
				.param("nombre", nombre)
				.param("descripcion", "descripcion de prueba")
				.param("precio", precio)
				.param("categoria", categoria),
		)

	@Test
	fun `la raiz redirige al listado`() {
		mockMvc.perform(get("/"))
			.andExpect(status().is3xxRedirection)
			.andExpect(redirectedUrl("/productos"))
	}

	@Test
	fun `el listado vacio muestra el estado vacio`() {
		mockMvc.perform(get("/productos"))
			.andExpect(status().isOk)
			.andExpect(view().name("productos/lista"))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Todavia no hay productos")))
	}

	@Test
	fun `el formulario crea el producto y redirige con mensaje`() {
		crear("Teclado mecanico")
			.andExpect(status().is3xxRedirection)
			.andExpect(redirectedUrl("/productos"))
			.andExpect(flash().attributeExists("mensaje"))

		mockMvc.perform(get("/productos"))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Teclado mecanico")))
	}

	@Test
	fun `un formulario invalido se repinta con el error y no redirige`() {
		mockMvc.perform(post("/productos").param("nombre", "").param("precio", "-5").param("categoria", ""))
			.andExpect(status().isOk)
			.andExpect(view().name("productos/formulario"))
			.andExpect(model().attributeHasFieldErrors("producto", "nombre", "precio", "categoria"))
	}

	@Test
	fun `un nombre duplicado se muestra como error del campo, no como pagina de error`() {
		crear("Mouse vertical")

		mockMvc.perform(
			post("/productos").param("nombre", "MOUSE VERTICAL").param("precio", "100.00")
				.param("categoria", "PERIFERICOS"),
		)
			.andExpect(status().isOk)
			.andExpect(view().name("productos/formulario"))
			.andExpect(model().attributeHasFieldErrors("producto", "nombre"))
	}

	@Test
	fun `el formulario de edicion llega poblado con los datos del producto`() {
		crear("Base para portatil", "120000.00")
		val id = repositorio.findAll().first().id

		mockMvc.perform(get("/productos/$id/editar"))
			.andExpect(status().isOk)
			.andExpect(view().name("productos/formulario"))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Base para portatil")))
	}

	@Test
	fun `el formulario de edicion emite PUT mediante el campo _method`() {
		crear("Hub USB C")
		val id = repositorio.findAll().first().id

		mockMvc.perform(
			post("/productos/$id")
				.param("_method", "put")
				.param("id", id.toString())
				.param("nombre", "Hub USB C de 7 puertos")
				.param("precio", "180000.00")
				.param("categoria", "CONECTIVIDAD"),
		)
			.andExpect(status().is3xxRedirection)
			.andExpect(redirectedUrl("/productos"))

		mockMvc.perform(get("/productos"))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Hub USB C de 7 puertos")))
	}

	@Test
	fun `el boton de eliminar emite DELETE mediante el campo _method`() {
		crear("Cargador 65W")
		val id = repositorio.findAll().first().id

		mockMvc.perform(post("/productos/$id").param("_method", "delete"))
			.andExpect(status().is3xxRedirection)
			.andExpect(redirectedUrl("/productos"))

		mockMvc.perform(get("/productos"))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Todavia no hay productos")))
	}

	@Test
	fun `el listado filtra por categoria desde la URL`() {
		crear("Audifonos over-ear", categoria = "AUDIO")
		crear("Teclado mecanico", categoria = "PERIFERICOS")

		mockMvc.perform(get("/productos?categoria=AUDIO"))
			.andExpect(status().isOk)
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Audifonos over-ear")))
			.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Teclado mecanico"))))
	}

	@Test
	fun `una categoria inexistente en la URL muestra una pagina HTML de error`() {
		mockMvc.perform(get("/productos?categoria=INVENTADA"))
			.andExpect(status().isBadRequest)
			.andExpect(view().name("error/no-encontrado"))
	}

	@Test
	fun `un producto inexistente muestra la pagina HTML de no encontrado, no JSON`() {
		mockMvc.perform(get("/productos/9999/editar"))
			.andExpect(status().isNotFound)
			.andExpect(view().name("error/no-encontrado"))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("No existe un producto con id 9999")))
	}
}
