package pe.edu.upeu.pharmamobil.domain.usecase

import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository
import kotlin.coroutines.cancellation.CancellationException

class SincronizarProductosUseCase(private val repositorio: ProductoRepository) {
    suspend operator fun invoke(): Result<Unit> =
        try {
            repositorio.sincronizar()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}