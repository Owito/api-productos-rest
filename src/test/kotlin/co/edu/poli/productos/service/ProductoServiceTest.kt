package co.edu.poli.productos.service

import co.edu.poli.productos.dto.ProductoRequest
import co.edu.poli.productos.exception.RecursoNoEncontradoException
import co.edu.poli.productos.exception.ReglaDeNegocioException
import co.edu.poli.productos.repository.ProductoRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

/** Pruebas de la logica de negocio contra la base de datos H2 del perfil local. */
@SpringBootTest
@Transactional
class ProductoServiceTest {

	@Autowired
	lateinit var servicio: ProductoService

	@Autowired
	lateinit var repositorio: ProductoRepository

	@BeforeEach
	fun limpiar() {
		repositorio.deleteAll()
	}

	private fun request(nombre: String, precio: String = "1000.00") =
		ProductoRequest(nombre = nombre, descripcion = "descripcion de prueba", precio = BigDecimal(precio))

	@Test
	fun `crear persiste el producto y le asigna identificador`() {
		val creado = servicio.crear(request("Mouse vertical"))

		assertTrue(creado.id > 0)
		assertEquals("Mouse vertical", creado.nombre)
		assertEquals(1, repositorio.count().toInt())
	}

	@Test
	fun `crear rechaza un nombre ya registrado`() {
		servicio.crear(request("Monitor 27 pulgadas"))

		assertThrows(ReglaDeNegocioException::class.java) {
			servicio.crear(request("monitor 27 PULGADAS"))
		}
	}

	@Test
	fun `actualizar cambia los datos del producto existente`() {
		val creado = servicio.crear(request("Silla ergonomica", "500000.00"))

		val actualizado = servicio.actualizar(
			creado.id,
			ProductoRequest("Silla ergonomica malla", "respaldo en malla", BigDecimal("650000.00")),
		)

		assertEquals(creado.id, actualizado.id)
		assertEquals("Silla ergonomica malla", actualizado.nombre)
		assertEquals(0, BigDecimal("650000.00").compareTo(actualizado.precio))
	}

	@Test
	fun `eliminar borra el producto de la base de datos`() {
		val creado = servicio.crear(request("Lampara de escritorio"))

		servicio.eliminar(creado.id)

		assertEquals(0, repositorio.count().toInt())
	}

	@Test
	fun `operar sobre un id inexistente lanza recurso no encontrado`() {
		assertThrows(RecursoNoEncontradoException::class.java) { servicio.obtener(9999L) }
		assertThrows(RecursoNoEncontradoException::class.java) { servicio.eliminar(9999L) }
	}
}
