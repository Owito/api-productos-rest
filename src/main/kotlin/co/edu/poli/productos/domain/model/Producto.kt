package co.edu.poli.productos.domain.model

import co.edu.poli.productos.domain.exception.DatosDeProductoInvalidosException
import java.math.BigDecimal

/**
 * Modelo de dominio Producto.
 *
 * Es una clase de Kotlin puro: no tiene anotaciones de JPA, de Jackson ni de
 * Bean Validation. Sus invariantes se cumplen aunque se cambie la base de datos
 * o se exponga la aplicacion por un protocolo distinto a HTTP.
 */
data class Producto(
	val id: Long? = null,
	val nombre: String,
	val descripcion: String? = null,
	val precio: BigDecimal,
) {

	init {
		if (nombre.isBlank()) {
			throw DatosDeProductoInvalidosException("El nombre del producto es obligatorio")
		}
		if (nombre.length > LARGO_MAXIMO_NOMBRE) {
			throw DatosDeProductoInvalidosException(
				"El nombre no puede superar $LARGO_MAXIMO_NOMBRE caracteres",
			)
		}
		if ((descripcion?.length ?: 0) > LARGO_MAXIMO_DESCRIPCION) {
			throw DatosDeProductoInvalidosException(
				"La descripcion no puede superar $LARGO_MAXIMO_DESCRIPCION caracteres",
			)
		}
		if (precio <= BigDecimal.ZERO) {
			throw DatosDeProductoInvalidosException("El precio debe ser mayor que cero")
		}
	}

	/** Devuelve una copia con los datos de [nuevos], conservando la identidad. */
	fun actualizadoCon(nuevos: Producto): Producto = copy(
		nombre = nuevos.nombre,
		descripcion = nuevos.descripcion,
		precio = nuevos.precio,
	)

	/** Dos productos no pueden llamarse igual, sin importar mayusculas. */
	fun seLlamaIgualQue(otroNombre: String): Boolean = nombre.equals(otroNombre, ignoreCase = true)

	companion object {
		const val LARGO_MAXIMO_NOMBRE = 120
		const val LARGO_MAXIMO_DESCRIPCION = 500
	}
}
