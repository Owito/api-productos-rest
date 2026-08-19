package co.edu.poli.productos.infrastructure.output.persistence.repository

import co.edu.poli.productos.domain.model.Categoria
import co.edu.poli.productos.infrastructure.output.persistence.entity.ProductoJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

/**
 * Repositorio de Spring Data JPA. Es una pieza del adaptador de persistencia,
 * no un puerto: el nucleo nunca lo ve.
 */
interface ProductoJpaRepository : JpaRepository<ProductoJpaEntity, Long> {

	fun existsByNombreIgnoreCase(nombre: String): Boolean

	fun findByCategoriaOrderByIdAsc(categoria: Categoria): List<ProductoJpaEntity>
}
