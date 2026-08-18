package co.edu.poli.productos.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

/** Representacion de un producto tal como se devuelve al cliente. */
@Schema(description = "Producto devuelto por la API")
data class ProductoResponse(
	@Schema(example = "1") val id: Long,
	@Schema(example = "Teclado mecanico 60%") val nombre: String,
	@Schema(example = "Teclado mecanico inalambrico, switches lineales, layout ANSI") val descripcion: String?,
	@Schema(example = "289900.00") val precio: BigDecimal,
)
