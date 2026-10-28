
package pe.upeu.agrosierra.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pe.upeu.agrosierra.data.repository.AgroRepositorioOffline
import pe.upeu.agrosierra.domain.model.*

sealed interface AgroUiState {
    data object Listo : AgroUiState
    data object Cargando : AgroUiState
    data class Exito(val mensaje: String) : AgroUiState
    data class Error(val mensaje: String) : AgroUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class AgroViewModel(
    private val repo: AgroRepositorioOffline
) : ViewModel() {

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
            repo.buscarLotes(texto, v, abiertas)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
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
            repo.registrar(labor)
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
        ejecutar {
            val resultado = repo.sincronizar()
            "Enviados: ${resultado.enviados}, " +
                    "lotes: ${resultado.lotesRecibidos}, " +
                    "labores: ${resultado.laboresRecibidas}"
        }
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
            } catch (e: Exception) {
                _estado.value = AgroUiState.Error(
                    e.message ?: "No se pudo completar la operación"
                )
            }
        }
    }
}
