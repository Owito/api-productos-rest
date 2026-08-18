package co.edu.poli.productos

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Punto de entrada de la aplicacion.
 *
 * Modulo: Arquitectura de Aplicaciones Web (TIC51372)
 * Unidad 2 - Actividad sumativa: backend con servicios RESTful CRUD sobre base de datos.
 */
@SpringBootApplication
class ProductosApplication

fun main(args: Array<String>) {
	runApplication<ProductosApplication>(*args)
}
