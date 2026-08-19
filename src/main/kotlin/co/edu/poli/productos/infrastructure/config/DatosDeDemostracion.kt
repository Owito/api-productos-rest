package co.edu.poli.productos.infrastructure.config

import co.edu.poli.productos.application.port.input.GestionarProductosUseCase
import co.edu.poli.productos.domain.model.Categoria
import co.edu.poli.productos.domain.model.Producto
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.math.BigDecimal

/**
 * Siembra un catalogo de ejemplo la primera vez que arranca la aplicacion.
 *
 * Existe para que quien clone el repositorio vea la aplicacion con contenido
 * sin tener que crear productos a mano. Esta apagado por defecto y encendido en
 * el perfil `local`; para poblar una base en la nube hay que pedirlo de forma
 * explicita con `APP_DATOS_DEMO=true`.
 *
 * Solo siembra si la tabla esta vacia, asi que reiniciar no duplica nada.
 *
 * Entra por el puerto de entrada, igual que la API y la interfaz web: los datos
 * de ejemplo pasan por las mismas reglas de negocio que cualquier otro alta.
 */
@Component
@ConditionalOnProperty(name = ["app.datos-demo"], havingValue = "true")
class DatosDeDemostracion(
	private val casoDeUso: GestionarProductosUseCase,
) : ApplicationRunner {

	private val log = LoggerFactory.getLogger(javaClass)

	override fun run(args: ApplicationArguments) {
		if (casoDeUso.listar().isNotEmpty()) {
			log.info("El catalogo ya tiene productos: no se siembran datos de demostracion")
			return
		}
		CATALOGO.forEach(casoDeUso::crear)
		log.info("Catalogo de demostracion sembrado con {} productos", CATALOGO.size)
	}

	private companion object {

		private fun producto(nombre: String, descripcion: String, precio: String, categoria: Categoria) =
			Producto(nombre = nombre, descripcion = descripcion, precio = BigDecimal(precio), categoria = categoria)

		val CATALOGO = listOf(
			producto(
				"Audifonos over-ear inalambricos",
				"Cancelacion activa de ruido, 40 horas de bateria y estuche rigido.",
				"749000.00", Categoria.AUDIO,
			),
			producto(
				"Audifonos in-ear deportivos",
				"Resistencia IPX5, aletas de sujecion y estuche de carga.",
				"219900.00", Categoria.AUDIO,
			),
			producto(
				"Interfaz de audio de 2 canales",
				"Dos preamplificadores con phantom power y monitoreo sin latencia.",
				"689000.00", Categoria.AUDIO,
			),
			producto(
				"Teclado mecanico 65 por ciento",
				"Switches lineales, cuerpo de aluminio y conexion por cable o Bluetooth.",
				"329900.00", Categoria.PERIFERICOS,
			),
			producto(
				"Mouse vertical ergonomico",
				"Seis botones programables y sensor de 4000 DPI para reducir la tension de muneca.",
				"149900.00", Categoria.PERIFERICOS,
			),
			producto(
				"Tableta grafica de 10 pulgadas",
				"Lapiz sin bateria con 8192 niveles de presion y ocho teclas rapidas.",
				"415000.00", Categoria.PERIFERICOS,
			),
			producto(
				"Monitor de 27 pulgadas 1440p",
				"Panel IPS a 165 Hz con cobertura del 99 por ciento de sRGB.",
				"1249000.00", Categoria.PANTALLAS,
			),
			producto(
				"Monitor portatil de 15 pulgadas",
				"Full HD por USB-C con funda plegable que sirve de soporte.",
				"689000.00", Categoria.PANTALLAS,
			),
			producto(
				"Portatil de 14 pulgadas para desarrollo",
				"16 GB de memoria, 512 GB de estado solido y teclado retroiluminado.",
				"4890000.00", Categoria.COMPUTO,
			),
			producto(
				"Mini PC de escritorio",
				"Procesador de bajo consumo, doble salida de video y montaje VESA.",
				"1790000.00", Categoria.COMPUTO,
			),
			producto(
				"Disco de estado solido externo de 1 TB",
				"Lectura de hasta 1050 MB por segundo, carcasa resistente a caidas.",
				"529000.00", Categoria.ALMACENAMIENTO,
			),
			producto(
				"Disco duro de escritorio de 4 TB",
				"Pensado para copias de respaldo, con software de programacion incluido.",
				"459000.00", Categoria.ALMACENAMIENTO,
			),
			producto(
				"Concentrador USB-C de 7 puertos",
				"HDMI 4K, lector de tarjetas, ethernet y carga de paso de 100 W.",
				"289000.00", Categoria.CONECTIVIDAD,
			),
			producto(
				"Router de banda dual Wi-Fi 6",
				"Cuatro antenas, red de invitados y control parental por aplicacion.",
				"569000.00", Categoria.CONECTIVIDAD,
			),
			producto(
				"Cargador GaN de 65 W",
				"Tres puertos con reparto automatico de potencia y tamano de bolsillo.",
				"179000.00", Categoria.ENERGIA,
			),
			producto(
				"Bateria externa de 20000 mAh",
				"Carga rapida de 45 W por USB-C y pantalla con porcentaje real.",
				"239000.00", Categoria.ENERGIA,
			),
			producto(
				"Silla ergonomica con soporte lumbar",
				"Malla transpirable, apoyabrazos en cuatro direcciones y reclinacion bloqueable.",
				"1290000.00", Categoria.MOBILIARIO,
			),
			producto(
				"Escritorio elevable electrico",
				"Altura de 70 a 120 centimetros con cuatro posiciones memorizadas.",
				"1950000.00", Categoria.MOBILIARIO,
			),
		)
	}
}
