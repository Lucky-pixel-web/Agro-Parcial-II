package pe.edu.upeu.pharmamobil.presentation.detalle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import pe.edu.upeu.pharmamobil.domain.usecase.ObtenerProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.comoTextoParaCompartir
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoUi
import pe.edu.upeu.pharmamobil.presentation.producto.aUi

data class DetalleProductoUiState(
    val fase: Fase = Fase.Cargando
) {
    sealed interface Fase {
        data object Cargando : Fase
        data class Listo(val producto: Producto, val detalle: ProductoUi) : Fase
        data class Error(val mensaje: String) : Fase
    }
}

class DetalleProductoViewModel(
    private val obtenerProducto: ObtenerProductoUseCase,
    private val compartidor: Compartidor
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetalleProductoUiState())
    val uiState: StateFlow<DetalleProductoUiState> = _uiState.asStateFlow()

    fun cargar(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(fase = DetalleProductoUiState.Fase.Cargando) }
            obtenerProducto(id)
                .onSuccess { producto ->
                    _uiState.update {
                        it.copy(fase = DetalleProductoUiState.Fase.Listo(producto, producto.aUi()))
                    }
                }
                .onFailure { fallo ->
                    _uiState.update {
                        it.copy(fase = DetalleProductoUiState.Fase.Error(fallo.message ?: "No se pudo cargar el producto"))
                    }
                }
        }
    }

    fun compartir(producto: Producto) {
        compartidor.compartir(producto.comoTextoParaCompartir())
    }
}