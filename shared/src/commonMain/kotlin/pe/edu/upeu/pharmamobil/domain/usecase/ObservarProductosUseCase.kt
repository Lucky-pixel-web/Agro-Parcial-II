package pe.edu.upeu.pharmamobil.domain.usecase

import kotlinx.coroutines.flow.Flow
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

class ObservarProductosUseCase(private val repositorio: ProductoRepository) {
    operator fun invoke(): Flow<List<Producto>> = repositorio.observar()
}