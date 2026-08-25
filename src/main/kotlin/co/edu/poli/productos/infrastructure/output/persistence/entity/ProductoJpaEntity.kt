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
import jakarta.persistence.UniqueConstraint
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
	// La regla "dos productos no se llaman igual" la comprueba el caso de uso
	// antes de guardar, pero esa comprobacion es leer-y-despues-escribir: entre
	// las dos operaciones cabe otra peticion, y con mas de una instancia de la
	// aplicacion cabe con holgura. La restriccion en la base es la que no se
	// puede burlar por concurrencia.
	//
	// Cubre la coincidencia exacta. La regla completa ignora mayusculas y esa
	// sigue viviendo en el caso de uso: expresarla en la base pediria un indice
	// funcional sobre lower(nombre), que no es portable entre H2 y PostgreSQL
	// con el esquema generado por el ORM.
	uniqueConstraints = [UniqueConstraint(name = "uk_productos_nombre", columnNames = ["nombre"])],
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
