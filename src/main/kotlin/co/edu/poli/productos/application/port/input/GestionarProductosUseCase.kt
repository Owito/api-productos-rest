package co.edu.poli.productos.application.port.input

import co.edu.poli.productos.domain.model.Categoria
import co.edu.poli.productos.domain.model.Producto

/**
 * Puerto de entrada (driving port): todo lo que la aplicacion sabe hacer.
 *
 * Cualquier adaptador de entrada (REST hoy, una CLI o un consumidor de mensajes
 * manana) depende de esta interfaz y no de su implementacion.
 */
interface GestionarProductosUseCase {

	/** Lista el catalogo completo, o solo una categoria si se indica. */
	fun listar(categoria: Categoria? = null): List<Producto>

	fun obtener(id: Long): Producto

	fun crear(producto: Producto): Producto

	fun actualizar(id: Long, datos: Producto): Producto

	fun eliminar(id: Long)
}
