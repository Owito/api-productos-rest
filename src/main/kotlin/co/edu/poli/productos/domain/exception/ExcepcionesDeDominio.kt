package co.edu.poli.productos.domain.exception

/**
 * Excepciones del dominio. No conocen HTTP ni JPA: expresan hechos del negocio.
 * Traducirlas a codigos de estado es responsabilidad del adaptador de entrada.
 */
sealed class ExcepcionDeDominio(mensaje: String) : RuntimeException(mensaje)

/** Se opero sobre un producto que no existe. */
class ProductoNoEncontradoException(id: Long) :
	ExcepcionDeDominio("No existe un producto con id $id")

/** Dos productos no pueden compartir el mismo nombre. */
class NombreDeProductoDuplicadoException(nombre: String) :
	ExcepcionDeDominio("Ya existe un producto registrado con el nombre '$nombre'")

/** Se intento construir un producto que viola sus propias invariantes. */
class DatosDeProductoInvalidosException(mensaje: String) :
	ExcepcionDeDominio(mensaje)
