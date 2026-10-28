package pe.edu.upeu.pharmamobil.domain.usecase

import kotlinx.coroutines.flow.Flow
import pe.edu.upeu.pharmamobil.domain.repository.AlmacenSincronizacion

class ObservarUltimaSincronizacionUseCase(private val almacen: AlmacenSincronizacion) {
    operator fun invoke(): Flow<Long?> = almacen.ultima
}