package co.edu.poli.productos.infrastructure.config

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * La documentacion es parte del entregable, asi que se prueba como tal: la
 * especificacion OpenAPI describe los siete endpoints, y la interfaz de Scalar
 * responde en /docs sirviendo su propio JavaScript, sin depender de una CDN.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DocumentacionApiTest {

	@Autowired lateinit var mockMvc: MockMvc

	@Test
	fun `la especificacion OpenAPI describe la API de productos`() {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.info.title").value("API de Productos"))
			.andExpect(jsonPath("$.paths['/api/v1/productos']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/productos/{id}']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/productos/categorias']").exists())
	}

	@Test
	fun `la interfaz de documentacion se sirve en la ruta docs`() {
		mockMvc.perform(get("/docs"))
			.andExpect(status().isOk)
			.andExpect(content().contentTypeCompatibleWith("text/html"))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("docs/scalar.js")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("/v3/api-docs")))
	}

	@Test
	fun `el javascript de la interfaz viaja con la aplicacion`() {
		mockMvc.perform(get("/docs/scalar.js"))
			.andExpect(status().isOk)
			.andExpect(content().contentTypeCompatibleWith("application/javascript"))
	}

	@Test
	fun `la interfaz anterior de swagger ya no se publica`() {
		mockMvc.perform(get("/swagger-ui.html")).andExpect(status().isNotFound)
	}
}
