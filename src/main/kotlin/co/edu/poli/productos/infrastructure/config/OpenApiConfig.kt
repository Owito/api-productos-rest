package co.edu.poli.productos.infrastructure.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** Metadatos de la documentacion OpenAPI expuesta en /swagger-ui.html. */
@Configuration
class OpenApiConfig {

	@Bean
	fun apiDeProductos(): OpenAPI = OpenAPI().info(
		Info()
			.title("API de Productos")
			.version("1.0.0")
			.description(
				"Servicios RESTful CRUD sobre la entidad Producto, con arquitectura hexagonal. " +
					"Arquitectura de Aplicaciones Web (TIC51372), Unidad 2.",
			),
	)
}
