package co.edu.poli.productos.controller

import co.edu.poli.productos.dto.ProductoRequest
import co.edu.poli.productos.dto.ProductoResponse
import co.edu.poli.productos.exception.ApiError
import co.edu.poli.productos.service.ProductoService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

/**
 * Servicios RESTful sobre el recurso Producto.
 *
 * Un recurso, una URI base y el verbo HTTP como operacion:
 * GET (leer), POST (crear), PUT (actualizar), DELETE (eliminar).
 */
@RestController
@RequestMapping("/api/v1/productos")
@Tag(name = "Productos", description = "Operaciones CRUD sobre el recurso Producto")
class ProductoController(
	private val servicio: ProductoService,
) {

	@GetMapping
	@Operation(summary = "Lista todos los productos")
	@ApiResponse(responseCode = "200", description = "Listado obtenido")
	fun listar(): List<ProductoResponse> = servicio.listar()

	@GetMapping("/{id}")
	@Operation(summary = "Consulta un producto por su identificador")
	@ApiResponses(
		ApiResponse(responseCode = "200", description = "Producto encontrado"),
		ApiResponse(
			responseCode = "404", description = "No existe el producto",
			content = [Content(schema = Schema(implementation = ApiError::class))],
		),
	)
	fun obtener(@PathVariable id: Long): ProductoResponse = servicio.obtener(id)

	@PostMapping
	@Operation(summary = "Crea un producto")
	@ApiResponses(
		ApiResponse(responseCode = "201", description = "Producto creado"),
		ApiResponse(
			responseCode = "400", description = "Datos invalidos",
			content = [Content(schema = Schema(implementation = ApiError::class))],
		),
		ApiResponse(
			responseCode = "409", description = "Ya existe un producto con ese nombre",
			content = [Content(schema = Schema(implementation = ApiError::class))],
		),
	)
	fun crear(@Valid @RequestBody request: ProductoRequest): ResponseEntity<ProductoResponse> {
		val creado = servicio.crear(request)
		val ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(creado.id)
			.toUri()
		return ResponseEntity.created(ubicacion).body(creado)
	}

	@PutMapping("/{id}")
	@Operation(summary = "Actualiza por completo un producto existente")
	@ApiResponses(
		ApiResponse(responseCode = "200", description = "Producto actualizado"),
		ApiResponse(
			responseCode = "404", description = "No existe el producto",
			content = [Content(schema = Schema(implementation = ApiError::class))],
		),
	)
	fun actualizar(@PathVariable id: Long, @Valid @RequestBody request: ProductoRequest): ProductoResponse =
		servicio.actualizar(id, request)

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Elimina un producto")
	@ApiResponses(
		ApiResponse(responseCode = "204", description = "Producto eliminado"),
		ApiResponse(
			responseCode = "404", description = "No existe el producto",
			content = [Content(schema = Schema(implementation = ApiError::class))],
		),
	)
	fun eliminar(@PathVariable id: Long) = servicio.eliminar(id)
}
