package co.edu.poli.productos.infrastructure.input.rest

import co.edu.poli.productos.infrastructure.input.rest.dto.ProductoRequest
import co.edu.poli.productos.infrastructure.output.persistence.repository.ProductoJpaRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal

/**
 * Pruebas de integracion del adaptador de entrada: los cuatro verbos HTTP y sus
 * errores, atravesando toda la aplicacion hasta la base de datos H2.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProductoRestAdapterTest {

	@Autowired lateinit var mockMvc: MockMvc
	@Autowired lateinit var mapper: ObjectMapper
	@Autowired lateinit var repositorio: ProductoJpaRepository

	private val ruta = "/api/v1/productos"

	@BeforeEach
	fun limpiar() {
		repositorio.deleteAll()
	}

	private fun json(nombre: String, precio: String, categoria: String = "PERIFERICOS") =
		mapper.writeValueAsString(
			ProductoRequest(nombre, "descripcion de prueba", BigDecimal(precio), categoria),
		)

	private fun crearProducto(nombre: String, precio: String = "1000.00", categoria: String = "PERIFERICOS"): Long {
		val respuesta = mockMvc.perform(
			post(ruta).contentType(MediaType.APPLICATION_JSON).content(json(nombre, precio, categoria)),
		).andExpect(status().isCreated).andReturn().response.contentAsString
		return mapper.readTree(respuesta).get("id").asLong()
	}

	@Test
	fun `POST crea el producto y responde 201 con la cabecera Location`() {
		mockMvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(json("Teclado 60", "289900.00")))
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.id").isNumber)
			.andExpect(jsonPath("$.nombre").value("Teclado 60"))
			.andExpect(jsonPath("$.categoria").value("PERIFERICOS"))
			.andExpect(jsonPath("$.categoriaEtiqueta").value("Perifericos"))
	}

	@Test
	fun `GET con categoria filtra el listado`() {
		crearProducto("Audifonos", categoria = "AUDIO")
		crearProducto("Teclado", categoria = "PERIFERICOS")
		crearProducto("Parlante", categoria = "AUDIO")

		mockMvc.perform(get(ruta)).andExpect(jsonPath("$.length()").value(3))
		mockMvc.perform(get("$ruta?categoria=AUDIO"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.length()").value(2))
		mockMvc.perform(get("$ruta?categoria=mobiliario"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.length()").value(0))
	}

	@Test
	fun `GET con una categoria inexistente responde 400`() {
		mockMvc.perform(get("$ruta?categoria=INVENTADA"))
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.estado").value(400))
	}

	@Test
	fun `GET categorias devuelve el catalogo completo`() {
		mockMvc.perform(get("$ruta/categorias"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.length()").value(8))
			.andExpect(jsonPath("$[0].codigo").value("AUDIO"))
			.andExpect(jsonPath("$[0].etiqueta").value("Audio"))
	}

	@Test
	fun `POST sin categoria responde 400`() {
		val cuerpo = """{"nombre":"Sin categoria","precio":1000}"""

		mockMvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.detalles[0].campo").value("categoria"))
	}

	@Test
	fun `GET lista y GET por id devuelven los productos registrados`() {
		val id = crearProducto("Webcam 1080p")

		mockMvc.perform(get(ruta)).andExpect(status().isOk).andExpect(jsonPath("$.length()").value(1))
		mockMvc.perform(get("$ruta/$id")).andExpect(status().isOk).andExpect(jsonPath("$.nombre").value("Webcam 1080p"))
	}

	@Test
	fun `PUT actualiza el producto existente`() {
		val id = crearProducto("Base para portatil")

		mockMvc.perform(
			put("$ruta/$id").contentType(MediaType.APPLICATION_JSON).content(json("Base en aluminio", "120000.00")),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.nombre").value("Base en aluminio"))
	}

	@Test
	fun `DELETE elimina el producto y responde 204`() {
		val id = crearProducto("Hub USB C")

		mockMvc.perform(delete("$ruta/$id")).andExpect(status().isNoContent)
		mockMvc.perform(get("$ruta/$id")).andExpect(status().isNotFound)
	}

	@Test
	fun `un id inexistente responde 404 con el contrato de error`() {
		mockMvc.perform(get("$ruta/9999"))
			.andExpect(status().isNotFound)
			.andExpect(jsonPath("$.estado").value(404))
			.andExpect(jsonPath("$.mensaje").value("No existe un producto con id 9999"))
	}

	@Test
	fun `un cuerpo invalido responde 400 con el detalle por campo`() {
		val cuerpo = """{"nombre":"","descripcion":"sin precio valido","precio":-5,"categoria":"AUDIO"}"""

		mockMvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.estado").value(400))
			.andExpect(jsonPath("$.detalles.length()").value(2))
	}

	@Test
	fun `un id no numerico responde 400`() {
		mockMvc.perform(get("$ruta/abc")).andExpect(status().isBadRequest)
	}

	@Test
	fun `un nombre duplicado responde 409`() {
		crearProducto("Cargador 65W")

		mockMvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(json("cargador 65w", "150000.00")))
			.andExpect(status().isConflict)
	}
}
