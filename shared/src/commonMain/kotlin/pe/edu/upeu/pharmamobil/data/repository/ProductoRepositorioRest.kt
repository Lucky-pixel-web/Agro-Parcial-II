package pe.edu.upeu.pharmamobil.data.repository

import pe.edu.upeu.pharmamobil.data.mapper.toDomain
import pe.edu.upeu.pharmamobil.data.mapper.toRequest
import pe.edu.upeu.pharmamobil.data.remote.ProductoApi
import pe.edu.upeu.pharmamobil.data.remote.ejecutarLlamada
import pe.edu.upeu.pharmamobil.domain.model.Producto

class ProductoRepositorioRest(
    private val api: ProductoApi,
    private val categoriaPorDefecto: Long
) : ProductoRemoto {

    override suspend fun listar(): List<Producto> =
        ejecutarLlamada { api.listar().contenido.map { it.toDomain() } }.getOrThrow()

    override suspend fun obtener(id: Long): Producto =
        ejecutarLlamada { api.obtener(id).toDomain() }.getOrThrow()

    override suspend fun registrar(producto: Producto): Producto =
        ejecutarLlamada {
            api.crear(producto.toRequest(categoriaPorDefecto)).toDomain()
        }.getOrThrow()

    override suspend fun actualizar(producto: Producto): Producto =
        ejecutarLlamada {
            api.actualizar(producto.id, producto.toRequest(categoriaPorDefecto)).toDomain()
        }.getOrThrow()

    override suspend fun eliminar(id: Long) =
        ejecutarLlamada { api.eliminar(id) }.getOrThrow()
}