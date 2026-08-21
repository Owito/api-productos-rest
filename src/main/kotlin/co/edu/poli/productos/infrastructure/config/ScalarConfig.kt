package co.edu.poli.productos.infrastructure.config

import com.scalar.maven.webmvc.ScalarWebMvcAutoConfiguration
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

/**
 * Registra la interfaz de documentacion de Scalar, que se sirve en /docs y lee
 * el documento OpenAPI de /v3/api-docs.
 *
 * La libreria trae su propia clase de configuracion y la declara en
 * META-INF/spring/...AutoConfiguration.imports, pero la anota solo con
 * @Configuration y no con @AutoConfiguration, asi que Spring Boot 3.5 no la
 * carga por si sola. Se importa aqui a mano.
 *
 * Se importa la clase de la libreria en vez de copiar sus @Bean para que, si
 * una version futura agrega o cambia beans, esta aplicacion los reciba sin
 * tocar nada. Las propiedades siguen viviendo en application.yml.
 */
@Configuration
@Import(ScalarWebMvcAutoConfiguration::class)
class ScalarConfig
