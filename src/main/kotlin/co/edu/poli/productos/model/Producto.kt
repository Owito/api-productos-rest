package co.edu.poli.productos.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

/**
 * Entidad de dominio Producto.
 *
 * El ORM (Hibernate, a traves de Spring Data JPA) se encarga de crear la tabla
 * `productos` y de resolver el acceso a datos sin escribir SQL manual.
 */
@Entity
@Table(name = "productos")
class Producto(

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
