package pe.edu.upeu.pharmamobil.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pe.edu.upeu.pharmamobil.domain.repository.AlmacenSincronizacion
import platform.Foundation.NSUserDefaults

class AlmacenSincronizacionIos : AlmacenSincronizacion {

    private val defaults = NSUserDefaults.standardUserDefaults

    private val _ultima = MutableStateFlow(
        if (defaults.objectForKey(CLAVE) != null) defaults.integerForKey(CLAVE) else null
    )
    override val ultima: StateFlow<Long?> = _ultima.asStateFlow()

    override fun registrar(milisegundos: Long) {
        defaults.setInteger(milisegundos, forKey = CLAVE)
        _ultima.value = milisegundos
    }

    private companion object {
        const val CLAVE = "ultima_sincronizacion"
    }
}