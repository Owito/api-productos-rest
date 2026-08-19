package co.edu.poli.productos.domain.model

import co.edu.poli.productos.domain.exception.DatosDeProductoInvalidosException

/**
 * Categoria a la que pertenece un producto.
 *
 * Es un objeto de valor del dominio, no una entidad: el catalogo de categorias
 * es cerrado y no se administra en tiempo de ejecucion, asi que modelarlo como
 * enumeracion hace imposible guardar una categoria que no existe. Si algun dia
 * el negocio necesita crear categorias, se convierte en entidad con su propio
 * puerto, y el cambio queda contenido en el dominio.
 *
 * El nombre de la constante es el contrato estable hacia afuera (lo que viaja
 * en el JSON y se guarda en la base de datos). La etiqueta es solo presentacion.
 */
enum class Categoria(val etiqueta: String) {

	AUDIO("Audio"),
	PERIFERICOS("Perifericos"),
	PANTALLAS("Pantallas"),
	COMPUTO("Computo"),
	ALMACENAMIENTO("Almacenamiento"),
	CONECTIVIDAD("Conectividad"),
	ENERGIA("Energia"),
	MOBILIARIO("Mobiliario"),
	;

	companion object {

		/**
		 * Convierte texto libre en una categoria, aceptando el nombre de la
		 * constante sin importar mayusculas ni espacios sobrantes.
		 *
		 * @throws DatosDeProductoInvalidosException si el valor no corresponde a
		 *   ninguna categoria conocida. La frontera decide como reportarlo.
		 */
		fun desde(valor: String?): Categoria {
			val limpio = valor?.trim().orEmpty()
			if (limpio.isEmpty()) {
				throw DatosDeProductoInvalidosException("La categoria es obligatoria")
			}
			return entries.firstOrNull { it.name.equals(limpio, ignoreCase = true) }
				?: throw DatosDeProductoInvalidosException(
					"La categoria '$limpio' no existe. Validas: " + entries.joinToString { it.name },
				)
		}
	}
}
