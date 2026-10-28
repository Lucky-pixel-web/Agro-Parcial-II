package pe.edu.upeu.pharmamobil.domain.session

import kotlinx.coroutines.flow.*
import pe.edu.upeu.pharmamobil.domain.model.Sesion

class SesionManager {
    private val _sesion = MutableStateFlow<Sesion?>(null)
    val sesion: StateFlow<Sesion?> = _sesion.asStateFlow()
    private val _aviso = MutableStateFlow<String?>(null)
    val aviso: StateFlow<String?> = _aviso.asStateFlow()

    fun iniciar(s: Sesion) { _aviso.value = null; _sesion.value = s }
    fun cerrar() { _sesion.value = null }
    fun expirar() {
        if (_sesion.value != null) {
            _sesion.value = null
            _aviso.value = "Tu sesión expiró. Inicia sesión nuevamente."
        }
    }
}