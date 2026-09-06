package co.edu.poli.productos.infrastructure.input.graphql.dto

import java.math.BigDecimal

/**
 * Contratos del borde GraphQL.
 *
 * Son deliberadamente distintos de los DTO del adaptador REST aunque hoy lleven
 * los mismos campos: cada adaptador es dueno de su contrato. Compartirlos ataria
 * la evolucion del esquema GraphQL a la de la especificacion OpenAPI, que es
 * justo lo que la arquitectura hexagonal evita.
 *
 * Aqui no hay anotaciones de Bean Validation: la forma de la entrada la valida
 * el propio motor de GraphQL contra el esquema (tipos, obligatoriedad y valores
 * validos de la enumeracion), y las reglas de negocio las sigue defendiendo el
 * modelo de dominio.
 */

/** Producto tal como sale en la respuesta GraphQL. */
data class ProductoGql(
	val id: Long,
	val nombre: String,
	val descripcion: String?,
	val precio: BigDecimal,
	val categoria: String,
	val categoriaEtiqueta: String,
)

/** Categoria disponible, expuesta por la consulta `categorias`. */
data class CategoriaInfoGql(
	val codigo: String,
	val etiqueta: String,
)

/**
 * Datos de entrada de una mutacion.
 *
 * El precio se declara como BigDecimal aunque el escalar Float de GraphQL
 * entregue un Double: el enlace de argumentos de Spring lo convierte, y de ahi
 * en adelante el valor viaja con el tipo que el dominio espera para dinero.
 */
data class ProductoInputGql(
	val nombre: String?,
	val descripcion: String?,
	val precio: BigDecimal?,
	val categoria: String?,
)
