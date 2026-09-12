package co.edu.poli.productos.infrastructure.input.grpc

import co.edu.poli.productos.application.port.input.GestionarProductosUseCase
import co.edu.poli.productos.domain.model.Categoria
import co.edu.poli.productos.infrastructure.input.grpc.mapper.ProductoGrpcMapper
import co.edu.poli.productos.infrastructure.input.grpc.proto.ActualizarProductoRequest
import co.edu.poli.productos.infrastructure.input.grpc.proto.ListarCategoriasResponse
import co.edu.poli.productos.infrastructure.input.grpc.proto.ListarProductosRequest
import co.edu.poli.productos.infrastructure.input.grpc.proto.ListarProductosResponse
import co.edu.poli.productos.infrastructure.input.grpc.proto.ProductoIdRequest
import co.edu.poli.productos.infrastructure.input.grpc.proto.ProductoInput
import co.edu.poli.productos.infrastructure.input.grpc.proto.ProductosServiceGrpc
import com.google.protobuf.Empty
import io.grpc.stub.StreamObserver
import org.springframework.stereotype.Service
import co.edu.poli.productos.infrastructure.input.grpc.proto.Producto as ProductoMensaje

/**
 * Cuarto adaptador de entrada: gRPC.
 *
 * Mismo patron que REST, web y GraphQL: depende solo del puerto
 * [GestionarProductosUseCase], asi que el nucleo no cambia ni una linea por
 * publicar el catalogo en un protocolo mas. Spring gRPC registra como servicio
 * cualquier bean que extienda el esqueleto generado por protoc y lo publica en
 * el puerto de spring.grpc.server.port.
 *
 * La diferencia visible con REST y GraphQL esta en el transporte, no en el
 * dominio: el contrato lo fija productos.proto, los mensajes viajan en binario
 * sobre HTTP/2 y el cliente trabaja con un stub generado en vez de armar
 * peticiones a mano. Las seis operaciones son unarias: una peticion, una
 * respuesta.
 */
@Service
class ProductoGrpcAdapter(
	private val casoDeUso: GestionarProductosUseCase,
) : ProductosServiceGrpc.ProductosServiceImplBase() {

	override fun listarProductos(
		peticion: ListarProductosRequest,
		respuesta: StreamObserver<ListarProductosResponse>,
	) {
		val productos = casoDeUso.listar(ProductoGrpcMapper.categoriaDe(peticion))
			.map(ProductoGrpcMapper::aMensaje)
		respuesta.responder(ListarProductosResponse.newBuilder().addAllProductos(productos).build())
	}

	/**
	 * A diferencia de GraphQL, donde un producto inexistente es un null valido,
	 * aqui es un estado NOT_FOUND: la excepcion del dominio sube tal cual y la
	 * traduce ManejadorDeErroresGrpc.
	 */
	override fun obtenerProducto(peticion: ProductoIdRequest, respuesta: StreamObserver<ProductoMensaje>) {
		respuesta.responder(ProductoGrpcMapper.aMensaje(casoDeUso.obtener(peticion.id)))
	}

	override fun crearProducto(peticion: ProductoInput, respuesta: StreamObserver<ProductoMensaje>) {
		val creado = casoDeUso.crear(ProductoGrpcMapper.aDominio(peticion))
		respuesta.responder(ProductoGrpcMapper.aMensaje(creado))
	}

	override fun actualizarProducto(
		peticion: ActualizarProductoRequest,
		respuesta: StreamObserver<ProductoMensaje>,
	) {
		val actualizado = casoDeUso.actualizar(peticion.id, ProductoGrpcMapper.aDominio(peticion.datos))
		respuesta.responder(ProductoGrpcMapper.aMensaje(actualizado))
	}

	override fun eliminarProducto(peticion: ProductoIdRequest, respuesta: StreamObserver<Empty>) {
		casoDeUso.eliminar(peticion.id)
		respuesta.responder(Empty.getDefaultInstance())
	}

	override fun listarCategorias(peticion: Empty, respuesta: StreamObserver<ListarCategoriasResponse>) {
		val categorias = Categoria.entries.map(ProductoGrpcMapper::aCategoriaInfo)
		respuesta.responder(ListarCategoriasResponse.newBuilder().addAllCategorias(categorias).build())
	}

	/** Una operacion unaria responde exactamente un mensaje y cierra. */
	private fun <T> StreamObserver<T>.responder(mensaje: T) {
		onNext(mensaje)
		onCompleted()
	}
}
