package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.edu.upeu.pharmamobil.data.local.ProductoLocal
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

class ProductoRepositorioOffline(
    private val local: ProductoLocal,
    private val remoto: ProductoRemoto
) : ProductoRepository {

    private val candado = Mutex()

    override fun observar(): Flow<List<Producto>> = local.observar()

    override suspend fun registrar(producto: Producto): Producto {
        val guardado = local.guardarPendiente(producto)
        try {
            intentar { sincronizar() }
        } catch (e: ErrorApiException) {
            if (e.error is ErrorApi.Validacion) local.eliminarLocal(guardado.id)
            throw e
        }
        return guardado
    }

    override suspend fun sincronizar() = candado.withLock {
        subirPendientes()
        local.reemplazarSincronizados(remoto.listar())
    }

    override suspend fun listar(): List<Producto> {
        intentar { sincronizar() }
        return local.observar().first()
    }

    override suspend fun obtener(id: Long): Producto = remoto.obtener(id)

    override suspend fun actualizar(producto: Producto): Producto {
        val actualizado = remoto.actualizar(producto)
        intentar { sincronizar() }
        return actualizado
    }

    override suspend fun eliminar(id: Long) {
        remoto.eliminar(id)
        intentar { sincronizar() }
    }

    private suspend fun subirPendientes() {
        local.pendientes().forEach { pendiente ->
            remoto.registrar(pendiente)
            local.eliminarLocal(pendiente.id)
        }
    }

    private suspend fun intentar(bloque: suspend () -> Unit) {
        try {
            bloque()
        } catch (e: ErrorApiException) {
            val sinRed = e.error == ErrorApi.SinConexion || e.error == ErrorApi.TiempoAgotado
            if (!sinRed) throw e
        }
    }
}