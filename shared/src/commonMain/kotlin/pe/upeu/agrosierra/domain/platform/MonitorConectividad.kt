package pe.upeu.agrosierra.domain.platform

import kotlinx.coroutines.flow.Flow

/** Observa si hay una conexion a Internet validada por el sistema. */
interface MonitorConectividad {
    fun observar(): Flow<Boolean>
}
