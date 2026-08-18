package co.edu.poli.productos.exception

/** Se lanza cuando se opera sobre un producto que no existe en la base de datos. */
class RecursoNoEncontradoException(mensaje: String) : RuntimeException(mensaje)
