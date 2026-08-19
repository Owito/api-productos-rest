package co.edu.poli.productos.infrastructure.output.persistence.mapper

import co.edu.poli.productos.domain.model.Producto
import co.edu.poli.productos.infrastructure.output.persistence.entity.ProductoJpaEntity

/** Traduce entre el modelo de dominio y la entidad de persistencia. */
object ProductoPersistenceMapper {

	fun aDominio(entidad: ProductoJpaEntity): Producto = Producto(
		id = entidad.id,
		nombre = entidad.nombre,
		descripcion = entidad.descripcion,
		precio = entidad.precio,
	)

	fun aEntidad(producto: Producto): ProductoJpaEntity = ProductoJpaEntity(
		id = producto.id,
		nombre = producto.nombre,
		descripcion = producto.descripcion,
		precio = producto.precio,
	)
}
