package pe.upeu.agrosierra.data.local

import kotlinx.coroutines.flow.Flow
import pe.upeu.agrosierra.domain.model.*

/** Puerto local para que el repositorio pueda probarse sin SQLite real. */
interface AlmacenAgroLocal {
    fun observarUltimaSincronizacion(): Flow<Long?>
    fun observarLotes(): Flow<List<Lote>>
    fun buscarLotes(texto: String, valle: String, soloAbiertas: Boolean): Flow<List<Lote>>
    fun observarLabores(idLote: Long): Flow<List<Labor>>
    fun contarPendientes(idLote: Long): Flow<Long>
    fun observarPendientesPorLotes(): Flow<Map<Long, Long>>
    fun pendientes(): List<Labor>
    fun guardarPendiente(labor: Labor): Labor
    fun guardarEdicion(labor: Labor): Labor
    fun confirmar(original: Labor, definitiva: Labor)
    fun eliminar(id: Long)
    fun actualizarDesdeServidor(lotes: List<Lote>, labores: List<Labor>)
    fun guardarUltimaSincronizacion(epochMillis: Long)
}
