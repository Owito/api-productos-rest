package co.edu.poli.productos.infrastructure.output.persistence

import co.edu.poli.productos.application.port.output.ProductoRepositoryPort
import co.edu.poli.productos.domain.model.Categoria
import co.edu.poli.productos.domain.model.Producto
import co.edu.poli.productos.infrastructure.output.persistence.mapper.ProductoPersistenceMapper
import co.edu.poli.productos.infrastructure.output.persistence.repository.ProductoJpaRepository
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

	override fun guardar(producto: Producto): Producto =
		ProductoPersistenceMapper.aDominio(repositorio.save(ProductoPersistenceMapper.aEntidad(producto)))

	override fun eliminarPorId(id: Long) = repositorio.deleteById(id)

	override fun existeConNombre(nombre: String): Boolean =
		repositorio.existsByNombreIgnoreCase(nombre)
}
