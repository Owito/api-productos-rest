package co.edu.poli.productos.infrastructure.input.rest

import co.edu.poli.productos.application.port.input.GestionarProductosUseCase
import co.edu.poli.productos.domain.model.Categoria
import co.edu.poli.productos.infrastructure.input.rest.dto.ProductoRequest
import co.edu.poli.productos.infrastructure.input.rest.dto.ProductoResponse
import co.edu.poli.productos.infrastructure.input.rest.error.ApiError
import co.edu.poli.productos.infrastructure.input.rest.mapper.ProductoRestMapper
import co.edu.poli.productos.infrastructure.input.rest.dto.CategoriaResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

/**
 * Adaptador de entrada REST.
 *
 * Depende del puerto GestionarProductosUseCase, nunca de su implementacion ni
 * del repositorio. Su unica responsabilidad es traducir HTTP a llamadas al
 * nucleo y devolver el codigo de estado correcto.
 */
@RestController
@RequestMapping("/api/v1/productos")
@Tag(name = "Productos", description = "Operaciones CRUD sobre el recurso Producto")
class ProductoRestAdapter(
	private val casoDeUso: GestionarProductosUseCase,
) {

	@GetMapping
	@Operation(
		summary = "Lista los productos",
		description = "Sin parametros devuelve el catalogo completo. Con 'categoria' filtra por una sola.",
	)
	@ApiResponses(
		ApiResponse(responseCode = "200", description = "Listado obtenido"),
		ApiResponse(
			responseCode = "400", description = "La categoria indicada no existe",
			content = [Content(schema = Schema(implementation = ApiError::class))],
		),
	)
	fun listar(
		@RequestParam(required = false)
		@Parameter(description = "Filtra por categoria", example = "PERIFERICOS")
		categoria: String?,
	): List<ProductoResponse> =
		casoDeUso.listar(categoria?.takeIf { it.isNotBlank() }?.let(Categoria::desde))
			.map(ProductoRestMapper::aRespuesta)

	@GetMapping("/categorias")
	@Operation(summary = "Lista las categorias disponibles")
	@ApiResponse(responseCode = "200", description = "Catalogo de categorias")
	fun categorias(): List<CategoriaResponse> =
		Categoria.entries.map { CategoriaResponse(it.name, it.etiqueta) }

	@GetMapping("/{id}")
	@Operation(summary = "Consulta un producto por su identificador")
	@ApiResponses(
		ApiResponse(responseCode = "200", description = "Producto encontrado"),
		ApiResponse(
			responseCode = "404", description = "No existe el producto",
			content = [Content(schema = Schema(implementation = ApiError::class))],
		),
	)
	fun obtener(@PathVariable id: Long): ProductoResponse =
		ProductoRestMapper.aRespuesta(casoDeUso.obtener(id))

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
		val creado = ProductoRestMapper.aRespuesta(casoDeUso.crear(ProductoRestMapper.aDominio(request)))
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
		ProductoRestMapper.aRespuesta(casoDeUso.actualizar(id, ProductoRestMapper.aDominio(request)))

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
	fun eliminar(@PathVariable id: Long) = casoDeUso.eliminar(id)
}
