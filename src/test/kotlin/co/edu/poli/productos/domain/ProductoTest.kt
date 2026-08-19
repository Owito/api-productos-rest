package co.edu.poli.productos.domain

import co.edu.poli.productos.domain.exception.DatosDeProductoInvalidosException
import co.edu.poli.productos.domain.exception.DatosDeProductoInvalidosException as ExcepcionDatos
import co.edu.poli.productos.domain.model.Categoria
import co.edu.poli.productos.domain.model.Producto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Pruebas del modelo de dominio. Kotlin puro, sin Spring, sin base de datos:
 * corren en milisegundos porque el nucleo no depende de nada.
 */
class ProductoTest {

	private fun producto(
		nombre: String = "Teclado",
		precio: String = "1000.00",
		categoria: Categoria = Categoria.PERIFERICOS,
	) = Producto(
		nombre = nombre,
		descripcion = "descripcion",
		precio = BigDecimal(precio),
		categoria = categoria,
	)

	@Test
	fun `un producto valido se construye sin problema`() {
		val p = producto()

		assertEquals("Teclado", p.nombre)
		assertEquals(0, BigDecimal("1000.00").compareTo(p.precio))
	}

	@Test
	fun `el nombre no puede estar vacio`() {
		val ex = assertThrows(DatosDeProductoInvalidosException::class.java) { producto(nombre = "   ") }

		assertEquals("El nombre del producto es obligatorio", ex.message)
	}

	@Test
	fun `el nombre no puede superar el largo maximo`() {
		assertThrows(DatosDeProductoInvalidosException::class.java) {
			producto(nombre = "x".repeat(Producto.LARGO_MAXIMO_NOMBRE + 1))
		}
	}

	@Test
	fun `el precio debe ser mayor que cero`() {
		assertThrows(DatosDeProductoInvalidosException::class.java) { producto(precio = "0.00") }
		assertThrows(DatosDeProductoInvalidosException::class.java) { producto(precio = "-1.00") }
	}

	@Test
	fun `actualizar conserva la identidad y cambia los datos`() {
		val original = producto().copy(id = 7L)

		val actualizado = original.actualizadoCon(producto(nombre = "Mouse", precio = "2000.00"))

		assertEquals(7L, actualizado.id)
		assertEquals("Mouse", actualizado.nombre)
		assertEquals(0, BigDecimal("2000.00").compareTo(actualizado.precio))
	}

	@Test
	fun `actualizar tambien cambia la categoria`() {
		val original = producto(categoria = Categoria.PERIFERICOS).copy(id = 3L)

		val actualizado = original.actualizadoCon(producto(categoria = Categoria.AUDIO))

		assertEquals(Categoria.AUDIO, actualizado.categoria)
		assertEquals(3L, actualizado.id)
	}

	@Test
	fun `una categoria desconocida se rechaza en el dominio`() {
		assertThrows(ExcepcionDatos::class.java) { Categoria.desde("INVENTADA") }
		assertThrows(ExcepcionDatos::class.java) { Categoria.desde("  ") }
		assertThrows(ExcepcionDatos::class.java) { Categoria.desde(null) }
	}

	@Test
	fun `la categoria se resuelve sin importar mayusculas ni espacios`() {
		assertEquals(Categoria.AUDIO, Categoria.desde("  audio  "))
		assertEquals(Categoria.MOBILIARIO, Categoria.desde("Mobiliario"))
	}

	@Test
	fun `la comparacion de nombres ignora mayusculas`() {
		assertTrue(producto(nombre = "Teclado").seLlamaIgualQue("TECLADO"))
	}
}
