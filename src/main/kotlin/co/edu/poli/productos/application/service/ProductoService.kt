package co.edu.poli.productos.application.service

import co.edu.poli.productos.application.port.input.GestionarProductosUseCase
import co.edu.poli.productos.application.port.output.ProductoRepositoryPort
import co.edu.poli.productos.domain.exception.NombreDeProductoDuplicadoException
import co.edu.poli.productos.domain.exception.ProductoNoEncontradoException
import co.edu.poli.productos.domain.model.Producto
import jakarta.transaction.Transactional

/**
 * Implementacion de los casos de uso.
 *
 * No lleva anotaciones de Spring: se registra como bean en
 * `infrastructure.config.ConfiguracionDeCasosDeUso`. La unica dependencia externa
 * es `jakarta.transaction.Transactional`, que es un estandar de Jakarta EE y no
 * ata la aplicacion a un framework concreto.
 */
@Transactional
class ProductoService(
	private val repositorio: ProductoRepositoryPort,
) : GestionarProductosUseCase {

	override fun listar(): List<Producto> = repositorio.listarOrdenadosPorId()

	override fun obtener(id: Long): Producto = buscarOFallar(id)

	override fun crear(producto: Producto): Producto {
		if (repositorio.existeConNombre(producto.nombre)) {
			throw NombreDeProductoDuplicadoException(producto.nombre)
		}
		return repositorio.guardar(producto)
	}

	override fun actualizar(id: Long, datos: Producto): Producto {
		val existente = buscarOFallar(id)
		if (!existente.seLlamaIgualQue(datos.nombre) && repositorio.existeConNombre(datos.nombre)) {
			throw NombreDeProductoDuplicadoException(datos.nombre)
		}
		return repositorio.guardar(existente.actualizadoCon(datos))
	}

	override fun eliminar(id: Long) {
		buscarOFallar(id)
		repositorio.eliminarPorId(id)
	}

	private fun buscarOFallar(id: Long): Producto =
		repositorio.buscarPorId(id) ?: throw ProductoNoEncontradoException(id)
}
