package pe.edu.upeu.pharmamobil.presentation.detalle

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pe.edu.upeu.pharmamobil.data.repository.FakeProductoRepository
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import pe.edu.upeu.pharmamobil.domain.usecase.ObtenerProductoUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private class FakeCompartidor : Compartidor {
    var ultimoTexto: String? = null
    override fun compartir(texto: String) {
        ultimoTexto = texto
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DetalleProductoViewModelTest {

    private val producto = Producto(id = 1L, nombre = "Paracetamol 500 mg", precio = 4.5, stock = 120)

    @BeforeTest
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun nuevoViewModel(
        compartidor: Compartidor = FakeCompartidor(),
        productos: MutableList<Producto> = mutableListOf(producto)
    ) = DetalleProductoViewModel(
        obtenerProducto = ObtenerProductoUseCase(FakeProductoRepository(productos)),
        compartidor = compartidor
    )

    @Test
    fun cargarMuestraElProductoDelRepositorio() = runTest {
        val viewModel = nuevoViewModel()

        viewModel.cargar(1L)

        val fase = assertIs<DetalleProductoUiState.Fase.Listo>(viewModel.uiState.value.fase)
        assertEquals("Paracetamol 500 mg", fase.producto.nombre)
    }

    @Test
    fun cargarUnProductoInexistenteMuestraError() = runTest {
        val viewModel = nuevoViewModel()

        viewModel.cargar(99L)

        assertIs<DetalleProductoUiState.Fase.Error>(viewModel.uiState.value.fase)
    }

    @Test
    fun compartirEntregaAlCompartidorElTextoArmadoEnCodigoComun() = runTest {
        val compartidor = FakeCompartidor()
        val viewModel = nuevoViewModel(compartidor)

        viewModel.compartir(producto)

        val texto = assertNotNull(compartidor.ultimoTexto)
        assertTrue(texto.contains("Paracetamol 500 mg"))
        assertTrue(texto.contains("Stock: 120"))
    }
}