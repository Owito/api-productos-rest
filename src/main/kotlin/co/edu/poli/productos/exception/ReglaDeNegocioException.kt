package co.edu.poli.productos.exception

/** Se lanza cuando la peticion es sintacticamente valida pero viola una regla del dominio. */
class ReglaDeNegocioException(mensaje: String) : RuntimeException(mensaje)
