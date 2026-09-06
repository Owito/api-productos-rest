package co.edu.poli.productos.infrastructure.input.graphql

import co.edu.poli.productos.infrastructure.output.persistence.repository.ProductoJpaRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.graphql.test.tester.ExecutionGraphQlServiceTester

import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pruebas de integracion del adaptador GraphQL: consultas, mutaciones y errores,
 * atravesando toda la aplicacion hasta la base de datos H2.
 *
 * ExecutionGraphQlServiceTester ejecuta contra el esquema real sin levantar el
 * servidor HTTP, asi que tambien valida que el esquema y los resolvers casen.
 */
@SpringBootTest
@AutoConfigureGraphQlTester
class ProductoGraphQlAdapterTest {

	@Autowired lateinit var tester: ExecutionGraphQlServiceTester
	@Autowired lateinit var repositorio: ProductoJpaRepository

	@BeforeEach
	fun limpiar() {
		repositorio.deleteAll()
	}

	private fun crear(
		nombre: String,
		precio: String = "1000.00",
		categoria: String = "PERIFERICOS",
	): String = tester.document(
		"""
		mutation {
			crearProducto(entrada: {
				nombre: "$nombre"
				descripcion: "descripcion de prueba"
				precio: $precio
				categoria: $categoria
			}) { id }
		}
		""",
	).execute().path("crearProducto.id").entity(String::class.java).get()

	@Test
	fun `la mutacion crea el producto y devuelve los campos pedidos`() {
		tester.document(
			"""
			mutation {
				crearProducto(entrada: {
					nombre: "Teclado 60"
					precio: 289900.00
					categoria: PERIFERICOS
				}) { nombre precio categoria categoriaEtiqueta }
			}
			""",
		).execute()
			.path("crearProducto.nombre").entity(String::class.java).isEqualTo("Teclado 60")
			.path("crearProducto.categoria").entity(String::class.java).isEqualTo("PERIFERICOS")
			.path("crearProducto.categoriaEtiqueta").entity(String::class.java).isEqualTo("Perifericos")
	}

	@Test
	fun `la consulta devuelve solo los campos que el cliente declara`() {
		crear("Monitor 27", "1200000.00", "PANTALLAS")

		val respuesta = tester.document("{ productos { nombre } }")
			.execute()
			.path("productos[0]")
			.entity(Map::class.java)
			.get()

		assertEquals(setOf("nombre"), respuesta.keys)
	}

	@Test
	fun `la consulta filtra por categoria`() {
		crear("Audifonos", "350000.00", "AUDIO")
		crear("Monitor 27", "1200000.00", "PANTALLAS")

		tester.document("{ productos(categoria: AUDIO) { nombre categoria } }")
			.execute()
			.path("productos").entityList(Map::class.java).hasSize(1)
			.path("productos[0].nombre").entity(String::class.java).isEqualTo("Audifonos")
	}

	@Test
	fun `un producto inexistente se responde como null y no como error`() {
		tester.document("{ producto(id: 999999) { nombre } }")
			.execute()
			.errors().verify()
			.path("producto").valueIsNull()
	}

	@Test
	fun `la mutacion de actualizacion cambia los datos y conserva el identificador`() {
		val id = crear("Mouse", "150000.00")

		tester.document(
			"""
			mutation {
				actualizarProducto(id: $id, entrada: {
					nombre: "Mouse inalambrico"
					precio: 199000.00
					categoria: PERIFERICOS
				}) { id nombre }
			}
			""",
		).execute()
			.path("actualizarProducto.id").entity(String::class.java).isEqualTo(id)
			.path("actualizarProducto.nombre").entity(String::class.java).isEqualTo("Mouse inalambrico")
	}

	@Test
	fun `la mutacion de borrado elimina el producto`() {
		val id = crear("Cable HDMI", "45000.00", "CONECTIVIDAD")

		tester.document("mutation { eliminarProducto(id: $id) }")
			.execute()
			.path("eliminarProducto").entity(Boolean::class.java).isEqualTo(true)

		tester.document("{ productos { nombre } }")
			.execute()
			.path("productos").entityList(Map::class.java).hasSize(0)
	}

	@Test
	fun `un nombre duplicado se reporta como BAD_REQUEST y no como error interno`() {
		crear("Teclado 60")

		tester.document(
			"""
			mutation {
				crearProducto(entrada: {
					nombre: "Teclado 60"
					precio: 100000.00
					categoria: PERIFERICOS
				}) { id }
			}
			""",
		).execute()
			.errors()
			.satisfy { errores ->
				assertEquals(1, errores.size)
				assertEquals("BAD_REQUEST", errores.first().errorType.toString())
				assertTrue(errores.first().message!!.contains("Teclado 60"))
			}
	}

	@Test
	fun `borrar un producto inexistente se reporta como NOT_FOUND`() {
		tester.document("mutation { eliminarProducto(id: 999999) }")
			.execute()
			.errors()
			.satisfy { errores ->
				assertEquals("NOT_FOUND", errores.first().errorType.toString())
			}
	}

	@Test
	fun `una categoria inexistente la rechaza el propio esquema antes de llegar al dominio`() {
		tester.document("{ productos(categoria: INVENTADA) { nombre } }")
			.execute()
			.errors()
			.satisfy { errores ->
				assertEquals("ValidationError", errores.first().errorType.toString())
			}
	}

	@Test
	fun `la consulta de categorias devuelve el catalogo cerrado con su etiqueta`() {
		tester.document("{ categorias { codigo etiqueta } }")
			.execute()
			.path("categorias").entityList(Map::class.java).hasSize(8)
			.path("categorias[0].codigo").entity(String::class.java).isEqualTo("AUDIO")
			.path("categorias[0].etiqueta").entity(String::class.java).isEqualTo("Audio")
	}

	@Test
	fun `una sola consulta trae el producto y el catalogo de categorias`() {
		crear("Teclado 60")

		val respuesta: Map<*, *> = tester.document(
			"{ productos { nombre } categorias { codigo } }",
		).execute().path("").entity(Map::class.java).get()

		assertEquals(setOf("productos", "categorias"), respuesta.keys)
		assertNull(respuesta["error"])
	}

}
