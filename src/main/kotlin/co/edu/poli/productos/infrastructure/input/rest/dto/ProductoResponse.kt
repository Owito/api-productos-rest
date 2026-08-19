package co.edu.poli.productos.infrastructure.input.rest.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

/** Contrato de salida del adaptador REST. */
@Schema(description = "Producto devuelto por la API")
data class ProductoResponse(
	@Schema(example = "1") val id: Long,
	@Schema(example = "Teclado mecanico 60%") val nombre: String,
	@Schema(example = "Teclado mecanico inalambrico, switches lineales, layout ANSI") val descripcion: String?,
	@Schema(example = "289900.00") val precio: BigDecimal,
)
