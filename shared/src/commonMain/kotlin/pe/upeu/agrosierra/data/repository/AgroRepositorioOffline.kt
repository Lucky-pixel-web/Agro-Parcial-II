
package pe.upeu.agrosierra.data.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.upeu.agrosierra.data.local.AgroLocal
import pe.upeu.agrosierra.data.remote.AgroApi
import pe.upeu.agrosierra.domain.model.*
import kotlinx.coroutines.flow.Flow

data class ResumenSincronizacion(
    val enviados: Int,
    val lotesRecibidos: Int,
    val laboresRecibidas: Int
)

class AgroRepositorioOffline(
    private val local: AgroLocal,
    private val remoto: AgroApi
) {

    private val mutex = Mutex()

    fun observarLotes(): Flow<List<Lote>> =
        local.observarLotes()

    fun buscarLotes(
        texto: String,
        valle: String,
        soloAbiertas: Boolean
    ): Flow<List<Lote>> =
        local.buscarLotes(texto, valle, soloAbiertas)

    fun observarLabores(loteId: Long): Flow<List<Labor>> =
        local.observarLabores(loteId)

    fun contarPendientes(loteId: Long): Flow<Long> =
        local.contarPendientes(loteId)

    fun observarPendientesPorLotes(): Flow<Map<Long, Long>> =
        local.observarPendientesPorLotes()

    fun registrar(labor: Labor): Labor =
        local.guardarPendiente(labor)

    fun corregir(labor: Labor): Labor =
        local.guardarEdicion(labor)

    suspend fun eliminar(id: Long) {
        if (id > 0L) {
            remoto.eliminarLabor(id)
        }
        local.eliminar(id)
    }

    suspend fun cerrarCampania(id: Long): Lote =
        remoto.cerrarCampania(id)

    suspend fun sincronizar(): ResumenSincronizacion =
        mutex.withLock {

            var enviados = 0

            // Primero enviar los pendientes.
            local.pendientes().forEach { pendiente ->

                val definitiva =
                    if (pendiente.id < 0L) {
                        remoto.crearLabor(pendiente)
                    } else {
                        remoto.actualizarLabor(pendiente)
                    }

                local.confirmar(pendiente, definitiva)
                enviados++
            }

            // Después descargar los datos actualizados.
            val lotes = remoto.descargarTodosLosLotes()

            val labores = lotes.flatMap { lote ->
                remoto.descargarTodasLasLabores(lote.id)
            }

            // Reemplazo atómico: conserva las filas pendientes.
            local.actualizarDesdeServidor(lotes, labores)

            ResumenSincronizacion(
                enviados = enviados,
                lotesRecibidos = lotes.size,
                laboresRecibidas = labores.size
            )
        }
}
