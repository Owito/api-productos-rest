package co.edu.poli.productos.infrastructure.config

import co.edu.poli.productos.application.port.input.GestionarProductosUseCase
import co.edu.poli.productos.application.port.output.ProductoRepositoryPort
import co.edu.poli.productos.application.service.ProductoService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Cableado de la aplicacion.
 *
 * El caso de uso se registra aqui, en infraestructura, para que la capa de
 * aplicacion no tenga que importar Spring. Esto es lo que permite probar el
 * nucleo sin levantar el contenedor.
 */
@Configuration
class ConfiguracionDeCasosDeUso {

	@Bean
	fun gestionarProductosUseCase(repositorio: ProductoRepositoryPort): GestionarProductosUseCase =
		ProductoService(repositorio)
}
