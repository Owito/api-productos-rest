package co.edu.poli.productos.infrastructure.output.persistence.entity

import co.edu.poli.productos.domain.model.Categoria
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.math.BigDecimal

/**
 * Entidad de persistencia. Es un detalle de infraestructura, no el modelo de
 * dominio: vive aqui para que las anotaciones del ORM no contaminen el nucleo.
 *
 * Hibernate crea y mantiene la tabla `productos` a partir de esta clase.
 */
@Entity
@Table(
	name = "productos",
	indexes = [Index(name = "idx_productos_categoria", columnList = "categoria")],
)
class ProductoJpaEntity(

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	var id: Long? = null,

	@Column(name = "nombre", nullable = false, length = 120)
	var nombre: String = "",

	@Column(name = "descripcion", length = 500)
	var descripcion: String? = null,

	@Column(name = "precio", nullable = false, precision = 12, scale = 2)
	var precio: BigDecimal = BigDecimal.ZERO,

	// STRING y no ORDINAL: guardar el nombre deja la columna legible y evita que
	// reordenar la enumeracion corrompa los datos ya guardados.
	// Sin valor por defecto a proposito: no existe una categoria "neutra" que
	// tenga sentido inventar. El mapeador siempre la provee.
	@Enumerated(EnumType.STRING)
	@Column(name = "categoria", nullable = false, length = 30)
	var categoria: Categoria,
)
