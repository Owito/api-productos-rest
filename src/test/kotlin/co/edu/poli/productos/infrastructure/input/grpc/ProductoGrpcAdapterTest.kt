package co.edu.poli.productos.infrastructure.input.grpc

import co.edu.poli.productos.infrastructure.input.grpc.proto.ActualizarProductoRequest
import co.edu.poli.productos.infrastructure.input.grpc.proto.ListarProductosRequest
import co.edu.poli.productos.infrastructure.input.grpc.proto.ProductoIdRequest
import co.edu.poli.productos.infrastructure.input.grpc.proto.ProductoInput
import co.edu.poli.productos.infrastructure.input.grpc.proto.ProductosServiceGrpc
import co.edu.poli.productos.infrastructure.output.persistence.repository.ProductoJpaRepository
import com.google.protobuf.Empty
import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.grpc.client.GrpcChannelFactory
import org.springframework.grpc.test.AutoConfigureInProcessTransport
import co.edu.poli.productos.infrastructure.input.grpc.proto.Categoria as CategoriaMensaje
import co.edu.poli.productos.infrastructure.input.grpc.proto.Producto as ProductoMensaje
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pruebas de integracion del adaptador gRPC: las seis operaciones y los tres
 * errores del dominio, atravesando toda la aplicacion hasta la base de datos H2.
 *
 * El transporte in-process de spring-grpc-test reemplaza al servidor Netty:
 * cliente y servidor hablan en memoria y no se abre ningun puerto, asi que la
 * prueba corre en paralelo con el resto de la suite. El stub bloqueante es el
 * mismo que usaria un cliente real.
 */
@SpringBootTest
@AutoConfigureInProcessTransport
class ProductoGrpcAdapterTest {

	@Autowired lateinit var canales: GrpcChannelFactory
	@Autowired lateinit var repositorio: ProductoJpaRepository

	private val cliente: ProductosServiceGrpc.ProductosServiceBlockingStub by lazy {
		ProductosServiceGrpc.newBlockingStub(canales.createChannel("0.0.0.0:0"))
	}

	@BeforeEach
	fun limpiar() {
		repositorio.deleteAll()
	}

	private fun entrada(
		nombre: String,
		precio: String = "1000.00",
		categoria: CategoriaMensaje = CategoriaMensaje.PERIFERICOS,
		descripcion: String? = "descripcion de prueba",
	): ProductoInput {
		val constructor = ProductoInput.newBuilder()
			.setNombre(nombre)
			.setPrecio(precio)
			.setCategoria(categoria)
		descripcion?.let(constructor::setDescripcion)
		return constructor.build()
	}

	private fun crear(
		nombre: String,
		precio: String = "1000.00",
		categoria: CategoriaMensaje = CategoriaMensaje.PERIFERICOS,
	): ProductoMensaje = cliente.crearProducto(entrada(nombre, precio, categoria))

	private fun porId(id: Long): ProductoIdRequest = ProductoIdRequest.newBuilder().setId(id).build()

	private fun estadoDe(bloque: () -> Unit): Status =
		assertThrows<StatusRuntimeException>(bloque).status

	// ---- Historia 1: lectura ----

	@Test
	fun `listar sin categoria devuelve el catalogo ordenado por identificador`() {
		val primero = crear("Audifonos", "350000.00", CategoriaMensaje.AUDIO)
		val segundo = crear("Monitor 27", "1200000.00", CategoriaMensaje.PANTALLAS)

		val respuesta = cliente.listarProductos(ListarProductosRequest.getDefaultInstance())

		assertEquals(listOf(primero.id, segundo.id), respuesta.productosList.map { it.id })
	}

	@Test
	fun `listar con categoria devuelve solo los productos de esa categoria`() {
		crear("Audifonos", "350000.00", CategoriaMensaje.AUDIO)
		crear("Monitor 27", "1200000.00", CategoriaMensaje.PANTALLAS)

		val respuesta = cliente.listarProductos(
			ListarProductosRequest.newBuilder().setCategoria(CategoriaMensaje.AUDIO).build(),
		)

		assertEquals(listOf("Audifonos"), respuesta.productosList.map { it.nombre })
	}

	@Test
	fun `obtener devuelve el producto con sus seis campos`() {
		val creado = crear("Teclado 60", "289900.00")

		val producto = cliente.obtenerProducto(porId(creado.id))

		assertEquals(creado.id, producto.id)
		assertEquals("Teclado 60", producto.nombre)
		assertEquals("descripcion de prueba", producto.descripcion)
		assertEquals("289900.00", producto.precio)
		assertEquals(CategoriaMensaje.PERIFERICOS, producto.categoria)
		assertEquals("Perifericos", producto.categoriaEtiqueta)
	}

	@Test
	fun `obtener un producto inexistente responde NOT_FOUND con el identificador en el mensaje`() {
		val estado = estadoDe { cliente.obtenerProducto(porId(999999)) }

		assertEquals(Status.Code.NOT_FOUND, estado.code)
		assertTrue(estado.description!!.contains("999999"))
	}

	@Test
	fun `listar categorias devuelve el catalogo cerrado con su etiqueta`() {
		val respuesta = cliente.listarCategorias(Empty.getDefaultInstance())

		assertEquals(8, respuesta.categoriasCount)
		assertEquals(CategoriaMensaje.AUDIO, respuesta.getCategorias(0).codigo)
		assertEquals("Audio", respuesta.getCategorias(0).etiqueta)
	}

	// ---- Historia 2: escritura ----

	@Test
	fun `crear devuelve el producto con identificador asignado y los mismos datos`() {
		val creado = cliente.crearProducto(entrada("Teclado 60", "289900.00", descripcion = "Compacto"))

		assertTrue(creado.id > 0)
		assertEquals("Teclado 60", creado.nombre)
		assertEquals("Compacto", creado.descripcion)
		assertEquals("289900.00", creado.precio)
		assertEquals(CategoriaMensaje.PERIFERICOS, creado.categoria)
	}

	@Test
	fun `crear sin descripcion deja el campo ausente en la respuesta`() {
		val creado = cliente.crearProducto(entrada("Cable HDMI", "45000.00", CategoriaMensaje.CONECTIVIDAD, descripcion = null))

		assertFalse(creado.hasDescripcion())
	}

	@Test
	fun `actualizar cambia los datos y conserva el identificador`() {
		val creado = crear("Mouse", "150000.00")

		val actualizado = cliente.actualizarProducto(
			ActualizarProductoRequest.newBuilder()
				.setId(creado.id)
				.setDatos(entrada("Mouse inalambrico", "199000.00"))
				.build(),
		)

		assertEquals(creado.id, actualizado.id)
		assertEquals("Mouse inalambrico", actualizado.nombre)
		assertEquals("199000.00", actualizado.precio)
	}

	@Test
	fun `eliminar responde vacio y una lectura posterior responde NOT_FOUND`() {
		val creado = crear("Cable HDMI", "45000.00", CategoriaMensaje.CONECTIVIDAD)

		val respuesta = cliente.eliminarProducto(porId(creado.id))

		assertEquals(Empty.getDefaultInstance(), respuesta)
		assertEquals(Status.Code.NOT_FOUND, estadoDe { cliente.obtenerProducto(porId(creado.id)) }.code)
	}

	@Test
	fun `un nombre duplicado con distinta capitalizacion responde ALREADY_EXISTS`() {
		crear("Teclado 60")

		val estado = estadoDe { crear("teclado 60") }

		assertEquals(Status.Code.ALREADY_EXISTS, estado.code)
		assertTrue(estado.description!!.contains("teclado 60"))
	}

	@Test
	fun `un nombre vacio responde INVALID_ARGUMENT con la regla del dominio`() {
		val estado = estadoDe { crear("   ") }

		assertEquals(Status.Code.INVALID_ARGUMENT, estado.code)
		assertEquals("El nombre del producto es obligatorio", estado.description)
	}

	@Test
	fun `un precio que no es un numero responde INVALID_ARGUMENT`() {
		val estado = estadoDe { crear("Cable", "abc") }

		assertEquals(Status.Code.INVALID_ARGUMENT, estado.code)
		assertEquals("El precio debe ser un numero decimal", estado.description)
	}

	@Test
	fun `una categoria sin especificar responde INVALID_ARGUMENT`() {
		val estado = estadoDe { crear("Cable", "10", CategoriaMensaje.CATEGORIA_SIN_ESPECIFICAR) }

		assertEquals(Status.Code.INVALID_ARGUMENT, estado.code)
		assertEquals("La categoria es obligatoria", estado.description)
	}

	@Test
	fun `actualizar y eliminar un producto inexistente responden NOT_FOUND`() {
		val actualizar = estadoDe {
			cliente.actualizarProducto(
				ActualizarProductoRequest.newBuilder().setId(999999).setDatos(entrada("Nada")).build(),
			)
		}
		val eliminar = estadoDe { cliente.eliminarProducto(porId(999999)) }

		assertEquals(Status.Code.NOT_FOUND, actualizar.code)
		assertEquals(Status.Code.NOT_FOUND, eliminar.code)
	}
}
