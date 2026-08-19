package co.edu.poli.productos.infrastructure.input.rest.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal

/**
 * Contrato de entrada del adaptador REST.
 *
 * Las anotaciones de validacion son una comodidad del borde HTTP, que permite
 * responder 400 con el detalle por campo. No sustituyen a las invariantes del
 * dominio: el modelo `Producto` se sigue defendiendo solo.
 */
@Schema(description = "Datos de entrada de un producto")
data class ProductoRequest(

	@field:NotBlank(message = "El nombre es obligatorio")
	@field:Size(max = 120, message = "El nombre no puede superar 120 caracteres")
	@Schema(example = "Teclado mecanico 60%")
	val nombre: String?,

	@field:Size(max = 500, message = "La descripcion no puede superar 500 caracteres")
	@Schema(example = "Teclado mecanico inalambrico, switches lineales, layout ANSI")
	val descripcion: String?,

	@field:NotNull(message = "El precio es obligatorio")
	@field:DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor que cero")
	@field:Digits(integer = 10, fraction = 2, message = "El precio admite maximo 10 enteros y 2 decimales")
	@Schema(example = "289900.00")
	val precio: BigDecimal?,
)
