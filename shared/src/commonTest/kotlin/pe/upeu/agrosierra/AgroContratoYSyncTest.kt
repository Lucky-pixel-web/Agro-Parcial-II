package pe.upeu.agrosierra

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import pe.upeu.agrosierra.data.local.AlmacenAgroLocal
import pe.upeu.agrosierra.data.remote.LaborDto
import pe.upeu.agrosierra.data.remote.LoteDto
import pe.upeu.agrosierra.data.remote.ServicioAgroRemoto
import pe.upeu.agrosierra.data.repository.AgroRepositorioOffline
import pe.upeu.agrosierra.domain.model.*

class AgroContratoYSyncTest {
    private val lote = Lote(1L, "L001", "Lote prueba", 5.0, "Papa", "Norte", false, 2.0)
    private val labor = Labor(-1L, 1L, TipoLabor.RIEGO, "2026-10-28", "tecnico1", 2.0, "Prueba", true, 3)

    @Test
    fun dtoSeConvierteADominioIncluyendoJornales() {
        val dto = LaborDto(14L, 1L, "RIEGO", "2026-10-28", "tecnico1", 2.0, "Prueba", false, 3)
        val resultado = dto.aDominio()
        assertEquals(14L, resultado.id)
        assertEquals(3, resultado.cantidadJornales)
        assertFalse(resultado.pendiente)
        val loteDto = LoteDto(1L, "L001", "Lote prueba", 5.0, "Papa", "Norte", false, 2.0)
        assertEquals("Norte", loteDto.aDominio().valle)
    }

    @Test
    fun registroSinRedQuedaPendiente() {
        val local = LocalFalso()
        val remoto = RemotoFalso()
        val repo = AgroRepositorioOffline(local, remoto)
        val creado = repo.registrar(labor)
        assertTrue(creado.pendiente)
        assertTrue(creado.id < 0)
        assertEquals(1, local.pendientes().size)
        assertTrue(remoto.eventos.isEmpty())
    }

    @Test
    fun sincronizacionPrimeroEnviaYLuegoDescarga() = runBlocking {
        val local = LocalFalso()
        val remoto = RemotoFalso()
        val repo = AgroRepositorioOffline(local, remoto)
        repo.registrar(labor)
        val resultado = repo.sincronizar()
        assertEquals(1, resultado.enviados)
        assertEquals(listOf("crear", "lotes", "labores"), remoto.eventos)
        assertTrue(local.pendientes().isEmpty())
        assertTrue(local.observarUltimaSincronizacion().first() != null)
    }

    private class LocalFalso : AlmacenAgroLocal {
        private val labores = mutableListOf<Labor>()
        private val fecha = MutableStateFlow<Long?>(null)
        override fun observarUltimaSincronizacion(): Flow<Long?> = fecha
        override fun observarLotes(): Flow<List<Lote>> = MutableStateFlow(emptyList())
        override fun buscarLotes(texto: String, valle: String, soloAbiertas: Boolean): Flow<List<Lote>> = observarLotes()
        override fun observarLabores(idLote: Long): Flow<List<Labor>> = MutableStateFlow(labores.filter { it.loteId == idLote })
        override fun contarPendientes(idLote: Long): Flow<Long> = MutableStateFlow(labores.count { it.loteId == idLote && it.pendiente }.toLong())
        override fun observarPendientesPorLotes(): Flow<Map<Long, Long>> = MutableStateFlow(emptyMap())
        override fun pendientes(): List<Labor> = labores.filter { it.pendiente }
        override fun guardarPendiente(labor: Labor): Labor {
            val nuevo = labor.copy(id = -1L - labores.size, pendiente = true)
            labores.add(nuevo)
            return nuevo
        }
        override fun guardarEdicion(labor: Labor): Labor {
            labores.removeAll { it.id == labor.id }
            return labor.copy(pendiente = true).also { labores.add(it) }
        }
        override fun confirmar(original: Labor, definitiva: Labor) {
            labores.removeAll { it.id == original.id }
            labores.add(definitiva.copy(pendiente = false))
        }
        override fun eliminar(id: Long) { labores.removeAll { it.id == id } }
        override fun actualizarDesdeServidor(lotes: List<Lote>, labores: List<Labor>) { }
        override fun guardarUltimaSincronizacion(epochMillis: Long) { fecha.value = epochMillis }
    }

    private class RemotoFalso : ServicioAgroRemoto {
        val eventos = mutableListOf<String>()
        override suspend fun crearLabor(labor: Labor): Labor {
            eventos.add("crear")
            return labor.copy(id = 42L, pendiente = false)
        }
        override suspend fun actualizarLabor(labor: Labor): Labor = crearLabor(labor)
        override suspend fun eliminarLabor(id: Long) { eventos.add("eliminar") }
        override suspend fun cerrarCampania(id: Long): Lote = error("No se usa")
        override suspend fun descargarTodosLosLotes(): List<Lote> {
            eventos.add("lotes")
            return listOf(Lote(1, "L001", "Lote", 5.0, "Papa", "Norte", false, 2.0))
        }
        override suspend fun descargarTodasLasLabores(loteId: Long): List<Labor> {
            eventos.add("labores")
            return emptyList()
        }
    }
}
