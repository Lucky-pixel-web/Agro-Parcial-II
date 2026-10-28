package pe.edu.upeu.pharmamobil.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pe.edu.upeu.pharmamobil.domain.repository.AlmacenSincronizacion

class AlmacenSincronizacionAndroid(contexto: Context) : AlmacenSincronizacion {

    private val preferencias = contexto.getSharedPreferences("pharma_sync", Context.MODE_PRIVATE)

    private val _ultima = MutableStateFlow(
        if (preferencias.contains(CLAVE)) preferencias.getLong(CLAVE, 0L) else null
    )
    override val ultima: StateFlow<Long?> = _ultima.asStateFlow()

    override fun registrar(milisegundos: Long) {
        preferencias.edit().putLong(CLAVE, milisegundos).apply()
        _ultima.value = milisegundos
    }

    private companion object {
        const val CLAVE = "ultima_sincronizacion"
    }
}