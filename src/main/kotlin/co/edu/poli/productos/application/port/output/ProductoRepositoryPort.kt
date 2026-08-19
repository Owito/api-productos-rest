package co.edu.poli.productos.application.port.output

import co.edu.poli.productos.domain.model.Categoria
import co.edu.poli.productos.domain.model.Producto

/**
 * Puerto de salida (driven port): lo que la aplicacion necesita del mundo exterior
 * para persistir productos.
 *
 * Aqui esta la inversion de dependencias del patron: la interfaz la define el
 * nucleo, y es la infraestructura la que se adapta a ella. Cambiar PostgreSQL por
 * MongoDB o por una API externa significa escribir otro adaptador, no tocar el
 * caso de uso.
 */
interface ProductoRepositoryPort {

	fun listarOrdenadosPorId(): List<Producto>

	fun listarPorCategoria(categoria: Categoria): List<Producto>

	fun buscarPorId(id: Long): Producto?

	fun guardar(producto: Producto): Producto

	fun eliminarPorId(id: Long)

	fun existeConNombre(nombre: String): Boolean
}
