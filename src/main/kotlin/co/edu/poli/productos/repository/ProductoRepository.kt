package co.edu.poli.productos.repository

import co.edu.poli.productos.model.Producto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Capa de acceso a datos. Spring Data JPA genera la implementacion en tiempo
 * de ejecucion: el CRUD completo queda resuelto por el ORM.
 */
@Repository
interface ProductoRepository : JpaRepository<Producto, Long> {

	fun existsByNombreIgnoreCase(nombre: String): Boolean
}
