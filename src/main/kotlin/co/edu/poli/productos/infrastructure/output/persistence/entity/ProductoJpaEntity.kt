package co.edu.poli.productos.infrastructure.output.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

/**
 * Entidad de persistencia. Es un detalle de infraestructura, no el modelo de
 * dominio: vive aqui para que las anotaciones del ORM no contaminen el nucleo.
 *
 * Hibernate crea y mantiene la tabla `productos` a partir de esta clase.
 */
@Entity
@Table(name = "productos")
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
)
