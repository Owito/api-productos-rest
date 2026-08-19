package co.edu.poli.productos.infrastructure.input.web.form

import co.edu.poli.productos.domain.model.Producto
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal

/**
 * Objeto de enlace del formulario HTML.
 *
 * Es el equivalente de ProductoRequest pero para el adaptador web: Spring MVC
 * necesita una clase mutable con constructor sin argumentos para poblarla desde
 * los campos del formulario y para repintarla cuando la validacion falla.
 */
class ProductoForm(

	var id: Long? = null,

	@field:NotBlank(message = "El nombre es obligatorio")
	@field:Size(max = 120, message = "El nombre no puede superar 120 caracteres")
	var nombre: String? = null,

	@field:Size(max = 500, message = "La descripcion no puede superar 500 caracteres")
	var descripcion: String? = null,

	@field:NotNull(message = "El precio es obligatorio")
	@field:DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor que cero")
	@field:Digits(integer = 10, fraction = 2, message = "El precio admite maximo 10 enteros y 2 decimales")
	var precio: BigDecimal? = null,
) {

	fun aDominio(): Producto = Producto(
		id = id,
		nombre = nombre.orEmpty().trim(),
		descripcion = descripcion?.trim()?.ifBlank { null },
		precio = precio ?: BigDecimal.ZERO,
	)

	companion object {
		fun desdeDominio(producto: Producto) = ProductoForm(
			id = producto.id,
			nombre = producto.nombre,
			descripcion = producto.descripcion,
			precio = producto.precio,
		)
	}
}
