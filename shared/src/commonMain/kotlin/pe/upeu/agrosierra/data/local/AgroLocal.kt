
package pe.upeu.agrosierra.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import app.cash.sqldelight.coroutines.mapToOne
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.upeu.agrosierra.db.AgroDb
import pe.upeu.agrosierra.domain.model.*

class AgroLocal(private val db: AgroDb) : AlmacenAgroLocal {

    private val q = db.agroQueries

    override fun observarLotes(): Flow<List<Lote>> =
        q.observarLotes()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { filas ->
                filas.map {
                    Lote(
                        it.id,
                        it.codigo,
                        it.nombre,
                        it.hectareas,
                        it.cultivo,
                        it.valle,
                        it.campaniaCerrada == 1L,
                        it.dosisMaxima
                    )
                }
            }

    override fun buscarLotes(
        texto: String,
        valle: String,
        soloAbiertas: Boolean
    ): Flow<List<Lote>> =
        q.buscarLotes(
            texto = texto.trim(),
            valleFiltro = valle.trim(),
            soloAbiertas = if (soloAbiertas) 1L else 0L
        ).asFlow()
            .mapToList(Dispatchers.Default)
            .map { filas ->
                filas.map {
                    Lote(
                        it.id, it.codigo, it.nombre,
                        it.hectareas, it.cultivo, it.valle,
                        it.campaniaCerrada == 1L,
                        it.dosisMaxima
                    )
                }
            }

    override fun observarLabores(idLote: Long): Flow<List<Labor>> =
        q.observarLaboresPorLote(idLote)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { filas -> filas.map { it.aLabor() } }

    override fun contarPendientes(idLote: Long): Flow<Long> =
        q.contarPendientesPorLote(idLote)
            .asFlow()
            .mapToOne(Dispatchers.Default)

    override fun observarPendientesPorLotes(): Flow<Map<Long, Long>> =
        q.contarPendientesPorLotes()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { filas ->
                filas.associate { fila ->
                    fila.loteId to fila.totalPendientes
                }
            }

    override fun observarUltimaSincronizacion(): Flow<Long?> =
        q.observarUltimaSincronizacion()
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)

    override fun guardarUltimaSincronizacion(epochMillis: Long) {
        q.guardarUltimaSincronizacion(epochMillis)
    }

    override fun pendientes(): List<Labor> =
        q.listarPendientes()
            .executeAsList()
            .map { it.aLabor() }

    override fun guardarPendiente(labor: Labor): Labor {
        val menor = q.siguienteIdLocal().executeAsOne()
        val id = minOf(menor, 0L) - 1L
        val pendiente = labor.copy(
            id = id,
            pendiente = true
        )
        guardarLabor(pendiente)
        return pendiente
    }

    override fun guardarEdicion(labor: Labor): Labor {
        val pendiente = labor.copy(pendiente = true)
        guardarLabor(pendiente)
        return pendiente
    }

    override fun confirmar(
        original: Labor,
        definitiva: Labor
    ) {
        db.transaction {
            q.borrarLaborPorId(original.id)
            guardarLabor(definitiva.copy(pendiente = false))
        }
    }

    override fun eliminar(id: Long) {
        q.borrarLaborPorId(id)
    }

    override fun actualizarDesdeServidor(
        lotes: List<Lote>,
        labores: List<Labor>
    ) {
        db.transaction {
            q.borrarLotes()

            lotes.forEach {
                q.guardarLote(
                    it.id, it.codigo, it.nombre,
                    it.hectareas, it.cultivo, it.valle,
                    if (it.campaniaCerrada) 1L else 0L,
                    it.dosisMaximaPorHectarea
                )
            }

            // Nunca borrar filas que siguen pendientes.
            q.borrarLaboresDescargadas()

            labores.forEach {
                guardarLabor(it.copy(pendiente = false))
            }
        }
    }

    private fun guardarLabor(labor: Labor) {
        q.guardarLabor(
            labor.id,
            labor.loteId,
            labor.tipo.name,
            labor.fecha,
            labor.responsable,
            labor.cantidad,
            labor.observacion,
            if (labor.pendiente) 1L else 0L,
            labor.cantidadJornales.toLong()
        )
    }

    private fun pe.upeu.agrosierra.db.Labor.aLabor(): Labor =
        Labor(
            id = id,
            loteId = loteId,
            tipo = TipoLabor.valueOf(tipo),
            fecha = fecha,
            responsable = responsable,
            cantidad = cantidad,
            observacion = observacion,
            pendiente = pendiente == 1L,
            cantidadJornales = cantidadJornales.toInt()
        )

}
