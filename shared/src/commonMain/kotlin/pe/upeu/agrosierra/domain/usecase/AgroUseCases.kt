package pe.upeu.agrosierra.domain.usecase

import kotlinx.coroutines.flow.Flow
import pe.upeu.agrosierra.domain.model.*
import pe.upeu.agrosierra.domain.repository.AgroRepositorio

class BuscarLotesAgroUseCase(private val repo: AgroRepositorio) {
    operator fun invoke(texto: String, valle: String, soloAbiertas: Boolean): Flow<List<Lote>> =
        repo.buscarLotes(texto, valle, soloAbiertas)
}

class RegistrarLaborAgroUseCase(private val repo: AgroRepositorio) {
    operator fun invoke(labor: Labor): Labor = repo.registrar(labor)
}

class SincronizarAgroUseCase(private val repo: AgroRepositorio) {
    suspend operator fun invoke(): ResumenSincronizacion = repo.sincronizar()
}
