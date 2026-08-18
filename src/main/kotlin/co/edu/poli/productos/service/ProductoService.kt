package co.edu.poli.productos.service

import co.edu.poli.productos.dto.ProductoRequest
import co.edu.poli.productos.dto.ProductoResponse
import co.edu.poli.productos.exception.RecursoNoEncontradoException
import co.edu.poli.productos.exception.ReglaDeNegocioException
import co.edu.poli.productos.mapper.ProductoMapper
import co.edu.poli.productos.model.Producto
import co.edu.poli.productos.repository.ProductoRepository
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Capa de logica de negocio. El controlador no conoce el repositorio y el
 * repositorio no conoce HTTP: toda la coordinacion vive aqui.
 */
@Service
@Transactional
class ProductoService(
	private val repositorio: ProductoRepository,
) {

	@Transactional(readOnly = true)
	fun listar(): List<ProductoResponse> =
		repositorio.findAll(Sort.by(Sort.Direction.ASC, "id")).map(ProductoMapper::aRespuesta)

	@Transactional(readOnly = true)
	fun obtener(id: Long): ProductoResponse =
		ProductoMapper.aRespuesta(buscarOFallar(id))

	fun crear(request: ProductoRequest): ProductoResponse {
		val nombre = request.nombre!!.trim()
		if (repositorio.existsByNombreIgnoreCase(nombre)) {
			throw ReglaDeNegocioException("Ya existe un producto registrado con el nombre '$nombre'")
		}
		val guardado = repositorio.save(ProductoMapper.aEntidad(request))
		return ProductoMapper.aRespuesta(guardado)
	}

	fun actualizar(id: Long, request: ProductoRequest): ProductoResponse {
		val existente = buscarOFallar(id)
		val nombre = request.nombre!!.trim()
		if (!nombre.equals(existente.nombre, ignoreCase = true) && repositorio.existsByNombreIgnoreCase(nombre)) {
			throw ReglaDeNegocioException("Ya existe otro producto registrado con el nombre '$nombre'")
		}
		val actualizado = repositorio.save(ProductoMapper.copiarSobre(existente, request))
		return ProductoMapper.aRespuesta(actualizado)
	}

	fun eliminar(id: Long) {
		repositorio.delete(buscarOFallar(id))
	}

	private fun buscarOFallar(id: Long): Producto = repositorio.findById(id)
		.orElseThrow { RecursoNoEncontradoException("No existe un producto con id $id") }
}
