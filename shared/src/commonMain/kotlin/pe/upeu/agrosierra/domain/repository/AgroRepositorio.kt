package pe.upeu.agrosierra.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.upeu.agrosierra.domain.model.*

/** Contrato del dominio: la presentación no conoce SQLDelight ni Ktor. */
interface AgroRepositorio {
    fun observarUltimaSincronizacion(): Flow<Long?>
    fun observarLotes(): Flow<List<Lote>>
    fun buscarLotes(texto: String, valle: String, soloAbiertas: Boolean): Flow<List<Lote>>
    fun observarLabores(loteId: Long): Flow<List<Labor>>
    fun contarPendientes(loteId: Long): Flow<Long>
    fun observarPendientesPorLotes(): Flow<Map<Long, Long>>
    fun registrar(labor: Labor): Labor
    fun corregir(labor: Labor): Labor
    suspend fun eliminar(id: Long)
    suspend fun cerrarCampania(id: Long): Lote
    suspend fun sincronizar(): ResumenSincronizacion
}
