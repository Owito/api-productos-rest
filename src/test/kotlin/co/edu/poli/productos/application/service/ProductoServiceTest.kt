package co.edu.poli.productos.application.service

import co.edu.poli.productos.application.port.output.ProductoRepositoryPort
import co.edu.poli.productos.domain.exception.NombreDeProductoDuplicadoException
import co.edu.poli.productos.domain.exception.ProductoNoEncontradoException
import co.edu.poli.productos.domain.model.Producto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Adaptador de salida falso, en memoria.
 *
 * Existe porque el nucleo depende de un puerto y no de JPA. Es la prueba de que
 * la inversion de dependencias es real y no decorativa.
 */
private class RepositorioEnMemoria : ProductoRepositoryPort {

	private val datos = linkedMapOf<Long, Producto>()
	private var secuencia = 0L

	override fun listarOrdenadosPorId(): List<Producto> = datos.values.sortedBy { it.id }

	override fun buscarPorId(id: Long): Producto? = datos[id]

	override fun guardar(producto: Producto): Producto {
		val id = producto.id ?: ++secuencia
		val guardado = producto.copy(id = id)
		datos[id] = guardado
		return guardado
	}

	override fun eliminarPorId(id: Long) {
		datos.remove(id)
	}

	override fun existeConNombre(nombre: String): Boolean =
		datos.values.any { it.seLlamaIgualQue(nombre) }

	fun cantidad(): Int = datos.size
}

/**
 * Pruebas de los casos de uso sin Spring, sin base de datos y sin HTTP.
 * Toda la suite corre en memoria contra el adaptador falso.
 */
class ProductoServiceTest {

	private lateinit var repositorio: RepositorioEnMemoria
	private lateinit var servicio: ProductoService

	@BeforeEach
	fun preparar() {
		repositorio = RepositorioEnMemoria()
		servicio = ProductoService(repositorio)
	}

	private fun producto(nombre: String, precio: String = "1000.00") =
		Producto(nombre = nombre, descripcion = "descripcion de prueba", precio = BigDecimal(precio))

	@Test
	fun `crear persiste el producto y le asigna identificador`() {
		val creado = servicio.crear(producto("Mouse vertical"))

		assertEquals(1L, creado.id)
		assertEquals("Mouse vertical", creado.nombre)
		assertEquals(1, repositorio.cantidad())
	}

	@Test
	fun `crear rechaza un nombre ya registrado sin importar mayusculas`() {
		servicio.crear(producto("Monitor 27 pulgadas"))

		assertThrows(NombreDeProductoDuplicadoException::class.java) {
			servicio.crear(producto("monitor 27 PULGADAS"))
		}
	}

	@Test
	fun `listar devuelve los productos ordenados por identificador`() {
		servicio.crear(producto("Primero"))
		servicio.crear(producto("Segundo"))

		assertEquals(listOf("Primero", "Segundo"), servicio.listar().map { it.nombre })
	}

	@Test
	fun `actualizar cambia los datos y conserva el identificador`() {
		val creado = servicio.crear(producto("Silla ergonomica", "500000.00"))

		val actualizado = servicio.actualizar(creado.id!!, producto("Silla en malla", "650000.00"))

		assertEquals(creado.id, actualizado.id)
		assertEquals("Silla en malla", actualizado.nombre)
		assertEquals(0, BigDecimal("650000.00").compareTo(actualizado.precio))
		assertEquals(1, repositorio.cantidad())
	}

	@Test
	fun `actualizar permite conservar el mismo nombre`() {
		val creado = servicio.crear(producto("Lampara", "50000.00"))

		val actualizado = servicio.actualizar(creado.id!!, producto("Lampara", "60000.00"))

		assertEquals(0, BigDecimal("60000.00").compareTo(actualizado.precio))
	}

	@Test
	fun `actualizar rechaza tomar el nombre de otro producto`() {
		servicio.crear(producto("Hub USB C"))
		val segundo = servicio.crear(producto("Cargador 65W"))

		assertThrows(NombreDeProductoDuplicadoException::class.java) {
			servicio.actualizar(segundo.id!!, producto("hub usb c"))
		}
	}

	@Test
	fun `eliminar borra el producto`() {
		val creado = servicio.crear(producto("Webcam 1080p"))

		servicio.eliminar(creado.id!!)

		assertEquals(0, repositorio.cantidad())
	}

	@Test
	fun `operar sobre un id inexistente lanza producto no encontrado`() {
		assertThrows(ProductoNoEncontradoException::class.java) { servicio.obtener(9999L) }
		assertThrows(ProductoNoEncontradoException::class.java) { servicio.eliminar(9999L) }
		assertThrows(ProductoNoEncontradoException::class.java) {
			servicio.actualizar(9999L, producto("Cualquiera"))
		}
	}
}
