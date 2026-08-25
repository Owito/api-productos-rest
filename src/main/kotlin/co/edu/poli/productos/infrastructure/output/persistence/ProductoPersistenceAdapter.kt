package co.edu.poli.productos.infrastructure.output.persistence

import co.edu.poli.productos.application.port.output.ProductoRepositoryPort
import co.edu.poli.productos.domain.exception.NombreDeProductoDuplicadoException
import co.edu.poli.productos.domain.model.Categoria
import co.edu.poli.productos.domain.model.Producto
import co.edu.poli.productos.infrastructure.output.persistence.mapper.ProductoPersistenceMapper
import co.edu.poli.productos.infrastructure.output.persistence.repository.ProductoJpaRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component

/**
 * Adaptador de salida: implementa el puerto que definio el nucleo usando
 * Spring Data JPA. Es el unico punto del proyecto que sabe que la persistencia
 * es una base de datos relacional.
 */
@Component
class ProductoPersistenceAdapter(
	private val repositorio: ProductoJpaRepository,
) : ProductoRepositoryPort {

	override fun listarOrdenadosPorId(): List<Producto> =
		repositorio.findAll(Sort.by(Sort.Direction.ASC, "id")).map(ProductoPersistenceMapper::aDominio)

	override fun listarPorCategoria(categoria: Categoria): List<Producto> =
		repositorio.findByCategoriaOrderByIdAsc(categoria).map(ProductoPersistenceMapper::aDominio)

	override fun buscarPorId(id: Long): Producto? =
		repositorio.findById(id).map(ProductoPersistenceMapper::aDominio).orElse(null)

	/**
	 * Se usa `saveAndFlush` y no `save` para que la sentencia viaje a la base
	 * dentro de este metodo. Con `save`, una actualizacion se queda en el
	 * contexto de persistencia y la base solo la rechazaria al confirmar la
	 * transaccion, fuera de este `try`, donde ya no hay quien traduzca el fallo.
	 */
	override fun guardar(producto: Producto): Producto = try {
		ProductoPersistenceMapper.aDominio(
			repositorio.saveAndFlush(ProductoPersistenceMapper.aEntidad(producto)),
		)
	} catch (ex: DataIntegrityViolationException) {
		// Traducir aqui es lo que mantiene limpio el reparto: la base reporta una
		// restriccion violada, que es vocabulario de infraestructura, y hacia
		// adentro sale el hecho de negocio que el resto del sistema ya sabe leer.
		// El caso de uso no se entera de que existe una restriccion, y el
		// adaptador REST sigue respondiendo 409 sin cambiar una linea.
		throw NombreDeProductoDuplicadoException(producto.nombre)
	}

	override fun eliminarPorId(id: Long) = repositorio.deleteById(id)

	override fun existeConNombre(nombre: String): Boolean =
		repositorio.existsByNombreIgnoreCase(nombre)
}
