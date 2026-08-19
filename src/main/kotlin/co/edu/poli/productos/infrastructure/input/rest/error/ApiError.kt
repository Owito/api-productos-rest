package co.edu.poli.productos.infrastructure.input.rest.error

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

/**
 * Cuerpo unico de respuesta para todos los errores de la API.
 * Vive en el adaptador de entrada porque hablar en codigos HTTP es su trabajo,
 * no el del dominio.
 */
@Schema(description = "Estructura estandar de error")
data class ApiError(
	@Schema(example = "2026-08-18T19:30:00-05:00") val marcaDeTiempo: OffsetDateTime = OffsetDateTime.now(),
	@Schema(example = "404") val estado: Int,
	@Schema(example = "Not Found") val error: String,
	@Schema(example = "No existe un producto con id 99") val mensaje: String,
	@Schema(example = "/api/v1/productos/99") val ruta: String,
	@Schema(description = "Detalle por campo cuando el error es de validacion")
	val detalles: List<ErrorDeCampo> = emptyList(),
)

@Schema(description = "Error de validacion asociado a un campo del cuerpo de la peticion")
data class ErrorDeCampo(
	@Schema(example = "precio") val campo: String,
	@Schema(example = "El precio debe ser mayor que cero") val mensaje: String,
)
