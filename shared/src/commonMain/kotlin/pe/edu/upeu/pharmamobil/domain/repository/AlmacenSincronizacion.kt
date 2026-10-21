package pe.edu.upeu.pharmamobil.domain.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
interface AlmacenSincronizacion {
    val ultima: StateFlow<Long?>
    fun registrar(milisegundos: Long)
}

class AlmacenSincronizacionEnMemoria(inicial: Long? = null) : AlmacenSincronizacion {
    private val _ultima = MutableStateFlow(inicial)
    override val ultima: StateFlow<Long?> = _ultima.asStateFlow()
    override fun registrar(milisegundos: Long) { _ultima.value = milisegundos }
}