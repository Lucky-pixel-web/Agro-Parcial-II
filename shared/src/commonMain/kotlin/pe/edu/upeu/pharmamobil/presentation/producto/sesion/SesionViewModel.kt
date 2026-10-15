package pe.edu.upeu.pharmamobil.presentation.sesion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pe.edu.upeu.pharmamobil.domain.model.*
import pe.edu.upeu.pharmamobil.domain.session.SesionManager
import pe.edu.upeu.pharmamobil.domain.usecase.*

class SesionViewModel(
    private val iniciarSesion: IniciarSesionUseCase,
    private val recuperarSesion: RecuperarSesionUseCase,
    private val cerrarSesion: CerrarSesionUseCase,
    private val manager: SesionManager
) : ViewModel() {

    private val _ui = MutableStateFlow(SesionUiState())
    val uiState: StateFlow<SesionUiState> = _ui.asStateFlow()
    private val _eventos = MutableSharedFlow<EventoSesion>(extraBufferCapacity = 1)
    val eventos: SharedFlow<EventoSesion> = _eventos.asSharedFlow()

    init {
        viewModelScope.launch {
            manager.sesion.collect { s -> _ui.update { it.copy(sesion = s) } }
        }
        viewModelScope.launch {
            manager.aviso.collect { a -> if (a != null) _ui.update { it.copy(error = a, clave = "") } }
        }
        viewModelScope.launch {
            try {
                recuperarSesion()?.let { manager.iniciar(it) }
            } catch (e: ErrorSesionException) {
                _ui.update { it.copy(error = e.message) }
            }
            _ui.update { it.copy(verificando = false) }
        }
    }

    fun onUsuarioChange(v: String) = _ui.update { it.copy(usuario = v, error = null) }
    fun onClaveChange(v: String) = _ui.update { it.copy(clave = v, error = null) }

    fun iniciar() {
        val s = _ui.value
        if (s.usuario.isBlank() || s.clave.isBlank()) {
            _ui.update { it.copy(error = "Ingresa usuario y contraseña.") }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(cargando = true, error = null) }
            try {
                val nueva = iniciarSesion(Credenciales(s.usuario.trim(), s.clave))
                manager.iniciar(nueva)
                _ui.update { it.copy(cargando = false, clave = "") }
                _eventos.tryEmit(EventoSesion.Ingreso(nueva.nombreCompleto))
            } catch (e: ErrorSesionException) {
                _ui.update { it.copy(cargando = false, error = e.message) }
                _eventos.tryEmit(EventoSesion.Rechazado(e.message ?: "No se pudo iniciar sesión."))
            }
        }
    }

    fun cerrar() {
        viewModelScope.launch {
            cerrarSesion()
            manager.cerrar()
            _ui.update { it.copy(usuario = "", clave = "", error = null) }
            _eventos.tryEmit(EventoSesion.Cierre)
        }
    }
}