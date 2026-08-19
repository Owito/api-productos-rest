package co.edu.poli.productos.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Datos academicos del trabajo, mostrados en la pagina de creditos.
 *
 * Viven en `application.yml` bajo `app.creditos` y no en el HTML, para que
 * corregir un dato sea editar una linea de configuracion y no buscarlo en una
 * plantilla.
 */
@ConfigurationProperties(prefix = "app.creditos")
data class CreditosProperties(
	val integrantes: List<String> = emptyList(),
	val programa: String = "",
	val asignatura: String = "",
	val codigoAsignatura: String = "",
	val unidad: String = "",
	val tutor: String = "",
	val institucion: String = "",
	val periodo: String = "",
	val fechaDeEntrega: String = "",
	val repositorio: String = "",
)
