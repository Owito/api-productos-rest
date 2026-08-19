package co.edu.poli.productos.infrastructure.input.rest.dto

import io.swagger.v3.oas.annotations.media.Schema

/** Categoria tal como la publica la API, con su nombre estable y su etiqueta legible. */
@Schema(description = "Categoria disponible para clasificar productos")
data class CategoriaResponse(
	@Schema(example = "PERIFERICOS") val codigo: String,
	@Schema(example = "Perifericos") val etiqueta: String,
)
