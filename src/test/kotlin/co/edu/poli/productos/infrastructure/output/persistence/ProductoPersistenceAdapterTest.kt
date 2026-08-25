package co.edu.poli.productos.infrastructure.output.persistence

import co.edu.poli.productos.application.port.output.ProductoRepositoryPort
import co.edu.poli.productos.domain.exception.NombreDeProductoDuplicadoException
import co.edu.poli.productos.domain.model.Categoria
import co.edu.poli.productos.domain.model.Producto
import co.edu.poli.productos.infrastructure.output.persistence.repository.ProductoJpaRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal

/**
 * El caso de uso comprueba que el nombre no se repita antes de guardar, pero esa
 * comprobacion es leer-y-despues-escribir y no resiste dos peticiones a la vez.
 *
 * Aqui se entra por el puerto de salida directamente, saltandose esa
 * comprobacion a proposito, para verificar lo unico que si resiste: la
 * restriccion de la base de datos. Y se verifica ademas que el fallo no sale
 * como excepcion de infraestructura, sino traducido al hecho de negocio que el
 * adaptador REST ya sabe convertir en 409.
 */
@SpringBootTest
class ProductoPersistenceAdapterTest {

	@Autowired lateinit var puerto: ProductoRepositoryPort
	@Autowired lateinit var repositorio: ProductoJpaRepository

	@BeforeEach
	fun limpiar() {
		repositorio.deleteAll()
	}

	private fun producto(nombre: String) = Producto(
		nombre = nombre,
		descripcion = "producto de prueba",
		precio = BigDecimal("1000.00"),
		categoria = Categoria.PERIFERICOS,
	)

	@Test
	fun `la base rechaza un nombre repetido aunque no pase por el caso de uso`() {
		puerto.guardar(producto("Teclado mecanico"))

		val ex = assertThrows(NombreDeProductoDuplicadoException::class.java) {
			puerto.guardar(producto("Teclado mecanico"))
		}

		assertEquals("Ya existe un producto registrado con el nombre 'Teclado mecanico'", ex.message)
		assertEquals(1, repositorio.count())
	}

	@Test
	fun `renombrar un producto sobre un nombre ya tomado tampoco pasa`() {
		puerto.guardar(producto("Monitor de 27 pulgadas"))
		val segundo = puerto.guardar(producto("Monitor portatil"))

		assertThrows(NombreDeProductoDuplicadoException::class.java) {
			puerto.guardar(segundo.copy(nombre = "Monitor de 27 pulgadas"))
		}
	}

	@Test
	fun `guardar un nombre libre sigue funcionando`() {
		val guardado = puerto.guardar(producto("Mouse vertical"))

		assertEquals("Mouse vertical", guardado.nombre)
		assertEquals(1, repositorio.count())
	}
}
