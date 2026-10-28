package pe.upeu.agrosierra.data.remote

import pe.upeu.agrosierra.domain.model.*

/** Puerto remoto para probar la sincronización con respuestas controladas. */
interface ServicioAgroRemoto {
    suspend fun crearLabor(labor: Labor): Labor
    suspend fun actualizarLabor(labor: Labor): Labor
    suspend fun eliminarLabor(id: Long)
    suspend fun cerrarCampania(id: Long): Lote
    suspend fun descargarTodosLosLotes(): List<Lote>
    suspend fun descargarTodasLasLabores(loteId: Long): List<Labor>
}
