package co.edu.poli.productos.infrastructure.input

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.view

/**
 * Una ruta inexistente no llega a ningun controlador, asi que ningun advice
 * acotado por paquete o por tipo la ve. Estas pruebas fijan que aun asi cada
 * adaptador responde en su propio formato, que es lo que antes no ocurria: la
 * API devolvia el error generico del contenedor en vez de su contrato.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ManejadorDeRutasInexistentesTest {

	@Autowired lateinit var mockMvc: MockMvc

	@Test
	fun `una ruta inexistente bajo api responde con el contrato de error de la API`() {
		mockMvc.perform(get("/api/v1/inventado"))
			.andExpect(status().isNotFound)
			.andExpect(content().contentTypeCompatibleWith("application/json"))
			.andExpect(jsonPath("$.estado").value(404))
			.andExpect(jsonPath("$.error").value("Not Found"))
			.andExpect(jsonPath("$.ruta").value("/api/v1/inventado"))
			.andExpect(jsonPath("$.mensaje").value("La ruta /api/v1/inventado no existe en esta API"))
			.andExpect(jsonPath("$.marcaDeTiempo").exists())
	}

	@Test
	fun `el contrato tambien aplica a una version de la API que no existe`() {
		mockMvc.perform(get("/api/v9/productos"))
			.andExpect(status().isNotFound)
			.andExpect(jsonPath("$.estado").value(404))
			.andExpect(jsonPath("$.ruta").value("/api/v9/productos"))
	}

	@Test
	fun `una pagina inexistente responde HTML y no JSON`() {
		mockMvc.perform(get("/pagina-que-no-existe"))
			.andExpect(status().isNotFound)
			.andExpect(view().name("error/no-encontrado"))
			.andExpect(content().contentTypeCompatibleWith("text/html"))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Pagina no encontrada")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Revisa la direccion")))
			// El remate del caso "producto borrado" no aplica a una direccion que
			// nunca existio, y la plantilla es la misma para los dos.
			.andExpect(content().string(org.hamcrest.Matchers.not(
				org.hamcrest.Matchers.containsString("se haya eliminado desde otra pestana"),
			)))
	}
}
