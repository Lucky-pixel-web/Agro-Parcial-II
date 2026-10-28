
package pe.upeu.agrosierra.data.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.upeu.agrosierra.data.local.AlmacenAgroLocal
import pe.upeu.agrosierra.data.remote.ServicioAgroRemoto
import pe.upeu.agrosierra.domain.repository.AgroRepositorio
import pe.upeu.agrosierra.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock

class AgroRepositorioOffline(
    private val local: AlmacenAgroLocal,
    private val remoto: ServicioAgroRemoto
) : AgroRepositorio {

    private val mutex = Mutex()

    override fun observarUltimaSincronizacion(): Flow<Long?> =
        local.observarUltimaSincronizacion()

    override fun observarLotes(): Flow<List<Lote>> =
        local.observarLotes()

    override fun buscarLotes(
        texto: String,
        valle: String,
        soloAbiertas: Boolean
    ): Flow<List<Lote>> =
        local.buscarLotes(texto, valle, soloAbiertas)

    override fun observarLabores(loteId: Long): Flow<List<Labor>> =
        local.observarLabores(loteId)

    override fun contarPendientes(loteId: Long): Flow<Long> =
        local.contarPendientes(loteId)

    override fun observarPendientesPorLotes(): Flow<Map<Long, Long>> =
        local.observarPendientesPorLotes()

    override fun registrar(labor: Labor): Labor =
        local.guardarPendiente(labor)

    override fun corregir(labor: Labor): Labor =
        local.guardarEdicion(labor)

    override suspend fun eliminar(id: Long) {
        if (id > 0L) {
            remoto.eliminarLabor(id)
        }
        local.eliminar(id)
    }

    override suspend fun cerrarCampania(id: Long): Lote =
        remoto.cerrarCampania(id)

    override suspend fun sincronizar(): ResumenSincronizacion =
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
            local.guardarUltimaSincronizacion(Clock.System.now().toEpochMilliseconds())

            ResumenSincronizacion(
                enviados = enviados,
                lotesRecibidos = lotes.size,
                laboresRecibidas = labores.size
            )
        }
}
