
package pe.upeu.agrosierra.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pe.upeu.agrosierra.domain.repository.AgroRepositorio
import pe.upeu.agrosierra.domain.usecase.BuscarLotesAgroUseCase
import pe.upeu.agrosierra.domain.usecase.RegistrarLaborAgroUseCase
import pe.upeu.agrosierra.domain.usecase.SincronizarAgroUseCase
import pe.upeu.agrosierra.domain.platform.MonitorConectividad
import pe.upeu.agrosierra.domain.model.*
import pe.upeu.agrosierra.data.remote.ErrorAgroException
import pe.upeu.agrosierra.data.remote.mensajeParaUsuario

sealed interface AgroUiState {
    data object Listo : AgroUiState
    data object Cargando : AgroUiState
    data class Exito(val mensaje: String) : AgroUiState
    data class Error(val mensaje: String, val campos: Map<String, String> = emptyMap()) : AgroUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class AgroViewModel(
    private val repo: AgroRepositorio,
    private val conectividad: MonitorConectividad,
    private val buscarLotes: BuscarLotesAgroUseCase,
    private val registrarLabor: RegistrarLaborAgroUseCase,
    private val sincronizarAgro: SincronizarAgroUseCase
) : ViewModel() {

    // La primera conexion carga datos; volver desde sin red sincroniza pendientes.
    init {
        viewModelScope.launch {
            conectividad.observar()
                .distinctUntilChanged()
                .filter { it }
                .collect { sincronizarInternamente() }
        }
    }

    private suspend fun sincronizarInternamente() {
        _estado.value = AgroUiState.Cargando
        try {
            val resultado = sincronizarAgro()
            _estado.value = AgroUiState.Exito(
                "Enviados: ${resultado.enviados}, lotes: ${resultado.lotesRecibidos}, " +
                    "labores: ${resultado.laboresRecibidas}"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: ErrorAgroException) {
            val campos = (e.detalle as? ErrorApi.Validacion)?.errores.orEmpty()
            _estado.value = AgroUiState.Error(e.detalle.mensajeParaUsuario(), campos)
        } catch (e: Exception) {
            _estado.value = AgroUiState.Error(
                e.message ?: "No se pudo sincronizar; los datos locales siguen disponibles"
            )
        }
    }

    private val busqueda = MutableStateFlow("")
    private val valle = MutableStateFlow("")
    private val soloAbiertas = MutableStateFlow(false)

    private val _estado =
        MutableStateFlow<AgroUiState>(AgroUiState.Listo)

    val estado: StateFlow<AgroUiState> =
        _estado.asStateFlow()

    val lotes: StateFlow<List<Lote>> =
        combine(busqueda, valle, soloAbiertas) {
                texto, v, abiertas ->
            Triple(texto, v, abiertas)
        }.flatMapLatest { (texto, v, abiertas) ->
            buscarLotes(texto, v, abiertas)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val ultimaSincronizacion: StateFlow<Long?> =
        repo.observarUltimaSincronizacion()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                null
            )

    val pendientesPorLote: StateFlow<Map<Long, Long>> =
        repo.observarPendientesPorLotes()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyMap()
            )

    fun cambiarBusqueda(texto: String) {
        busqueda.value = texto
    }

    fun cambiarValle(texto: String) {
        valle.value = texto
    }

    fun cambiarSoloAbiertas(valor: Boolean) {
        soloAbiertas.value = valor
    }

    fun observarLabores(loteId: Long): Flow<List<Labor>> =
        repo.observarLabores(loteId)

    fun contarPendientes(loteId: Long): Flow<Long> =
        repo.contarPendientes(loteId)

    fun registrar(labor: Labor) {
        ejecutar {
            registrarLabor(labor)
            "Labor guardada como pendiente"
        }
    }

    fun corregir(labor: Labor) {
        ejecutar {
            repo.corregir(labor)
            "Corrección guardada como pendiente"
        }
    }

    fun eliminar(id: Long) {
        ejecutar {
            repo.eliminar(id)
            "Labor eliminada"
        }
    }

    fun cerrarCampania(id: Long) {
        ejecutar {
            repo.cerrarCampania(id)
            repo.sincronizar()
            "Campaña cerrada correctamente"
        }
    }

    fun sincronizar() {
        viewModelScope.launch { sincronizarInternamente() }
    }

    fun limpiarMensaje() {
        _estado.value = AgroUiState.Listo
    }

    private fun ejecutar(
        operacion: suspend () -> String
    ) {
        viewModelScope.launch {
            _estado.value = AgroUiState.Cargando

            try {
                _estado.value =
                    AgroUiState.Exito(operacion())
            } catch (e: CancellationException) {
                throw e
            } catch (e: ErrorAgroException) {
                val campos = (e.detalle as? ErrorApi.Validacion)?.errores.orEmpty()
                _estado.value = AgroUiState.Error(e.detalle.mensajeParaUsuario(), campos)
            } catch (e: Exception) {
                _estado.value = AgroUiState.Error(
                    e.message ?: "No se pudo completar la operación"
                )
            }
        }
    }
}
