package pe.edu.upeu.pharmamobil.presentation.producto

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pe.edu.upeu.pharmamobil.data.repository.FakeProductoRepository
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.AlmacenSincronizacionEnMemoria
import pe.edu.upeu.pharmamobil.domain.usecase.ActualizarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.EliminarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ObtenerProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ObservarProductosUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ObservarUltimaSincronizacionUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.SincronizarProductosUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProductoViewModelTest {

    @BeforeTest
    fun instalarMain() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun restaurarMain() {
        Dispatchers.resetMain()
    }

    private fun nuevoViewModel(
        repositorio: FakeProductoRepository = FakeProductoRepository()
    ) = ProductoViewModel(
        registrarProducto = RegistrarProductoUseCase(repositorio),
        observarProductos = ObservarProductosUseCase(repositorio),
        sincronizarProductos = SincronizarProductosUseCase(repositorio),
        actualizarProducto = ActualizarProductoUseCase(repositorio),
        eliminarProducto = EliminarProductoUseCase(repositorio),
        obtenerProducto = ObtenerProductoUseCase(repositorio),
        observarUltimaSincronizacion = ObservarUltimaSincronizacionUseCase(AlmacenSincronizacionEnMemoria()
        )
    )

    @Test
    fun repositorioVacioProduceFaseSinProductos() = runTest {

        val viewModel = nuevoViewModel(FakeProductoRepository())

        assertEquals(
            ProductoUiState.Fase.SinProductos,
            viewModel.uiState.value.fase
        )
    }

    @Test
    fun repositorioConProductosProduceFaseConProductos() = runTest {

        val repositorio = FakeProductoRepository(
            mutableListOf(
                Producto(id = 1L, nombre = "Paracetamol", precio = 8.5, stock = 100),
                Producto(id = 2L, nombre = "Ibuprofeno", precio = 12.0, stock = 50),
                Producto(id = 3L, nombre = "Amoxicilina", precio = 18.5, stock = 20)
            )
        )

        val fase = assertIs<ProductoUiState.Fase.ConProductos>(
            nuevoViewModel(repositorio).uiState.value.fase
        )

        assertEquals(3, fase.productos.size)
    }

    @Test
    fun sinConexionMuestraAvisoYConservaLosDatos() = runTest {
        val repositorio = FakeProductoRepository(
            mutableListOf(Producto(id = 1L, nombre = "Paracetamol", precio = 8.5, stock = 100))
        ).apply { fallaAlListar = ErrorApiException(ErrorApi.SinConexion) }
        val estado = nuevoViewModel(repositorio).uiState.value
        assertEquals("Sin conexión: mostrando datos guardados", estado.avisoRed)
        assertIs<ProductoUiState.Fase.ConProductos>(estado.fase)
        assertFalse(estado.sincronizando)
    }

    @Test
    fun precioCeroDejaErrorEnFormularioSinLlamarAlRepositorio() = runTest {

        val repositorio = FakeProductoRepository()
        val viewModel = nuevoViewModel(repositorio)

        viewModel.onNombreChange("Paracetamol")
        viewModel.onPrecioChange("0")
        viewModel.onStockChange("100")
        viewModel.registrar()

        val estado = viewModel.uiState.value

        assertEquals("El precio debe ser mayor que cero", estado.formulario.precioError)
        assertNull(estado.formulario.nombreError)
        assertNull(estado.formulario.stockError)
        assertEquals(0, repositorio.vecesQueSeLlamoRegistrar)
    }

    @Test
    fun eliminarDejaElProductoInactivoYLaListaVisible() = runTest {

        val repositorio = FakeProductoRepository(
            mutableListOf(Producto(id = 1L, nombre = "Paracetamol", precio = 8.5, stock = 100))
        )
        val viewModel = nuevoViewModel(repositorio)

        viewModel.eliminar(1L)

        val estado = viewModel.uiState.value
        val fase = assertIs<ProductoUiState.Fase.ConProductos>(estado.fase)

        assertEquals(false, fase.productos.first().activo)
        assertEquals(ProductoUiState.Operacion.Inactiva, estado.operacion)
        assertNotNull(estado.mensajeExito)
    }

    @Test
    fun actualizarCambiaElProductoYLimpiaElFormulario() = runTest {

        val repositorio = FakeProductoRepository(
            mutableListOf(Producto(id = 1L, nombre = "Paracetamol", precio = 8.5, stock = 100))
        )
        val viewModel = nuevoViewModel(repositorio)

        val productoUi = assertIs<ProductoUiState.Fase.ConProductos>(
            viewModel.uiState.value.fase
        ).productos.first()

        viewModel.editar(productoUi)
        viewModel.onNombreChange("Paracetamol 500 mg")
        viewModel.guardar()

        val estado = viewModel.uiState.value
        val fase = assertIs<ProductoUiState.Fase.ConProductos>(estado.fase)

        assertEquals("Paracetamol 500 mg", fase.productos.first().nombre)
        assertEquals(ProductoUiState.Operacion.Inactiva, estado.operacion)
        assertNull(estado.formulario.idEnEdicion)
    }

    @Test
    fun validacionDelServidorSeMuestraDebajoDelCampo() = runTest {

        val mensaje = "El nombre debe tener entre 3 y 150 caracteres"
        val repositorio = FakeProductoRepository().apply {
            fallaAlRegistrar = ErrorApiException(
                ErrorApi.Validacion(mapOf("nombre" to mensaje))
            )
        }
        val viewModel = nuevoViewModel(repositorio)

        viewModel.onNombreChange("ab")
        viewModel.onPrecioChange("5")
        viewModel.onStockChange("10")
        viewModel.registrar()

        val estado = viewModel.uiState.value

        assertEquals(mensaje, estado.formulario.nombreError)
        assertNull(estado.formulario.precioError)
        assertNull(estado.formulario.stockError)
        assertEquals(ProductoUiState.Operacion.Inactiva, estado.operacion)
        assertEquals(ProductoUiState.Fase.SinProductos, estado.fase)
    }

    @Test
    fun validacionSinCampoMuestraAvisoGeneral() = runTest {

        val repositorio = FakeProductoRepository().apply {
            fallaAlRegistrar = ErrorApiException(
                ErrorApi.Validacion(emptyMap(), "El cuerpo de la petición no es válido")
            )
        }
        val viewModel = nuevoViewModel(repositorio)

        viewModel.onNombreChange("Aspirina")
        viewModel.onPrecioChange("5")
        viewModel.onStockChange("10")
        viewModel.registrar()

        val estado = viewModel.uiState.value

        assertIs<ProductoUiState.Operacion.Fallida>(estado.operacion)
        assertNull(estado.formulario.nombreError)
    }

    @Test
    fun laListaSeMuestraDesdeLocalMientrasSincroniza() = runTest {
        val repositorio = FakeProductoRepository(
            mutableListOf(
                Producto(id = 1L, nombre = "Paracetamol", precio = 8.5, stock = 100),
                Producto(id = 2L, nombre = "Ibuprofeno", precio = 12.0, stock = 50)
            )
        ).apply { compuertaListar = CompletableDeferred() }
        val viewModel = nuevoViewModel(repositorio)
        assertTrue(viewModel.uiState.value.sincronizando)
        val fase = assertIs<ProductoUiState.Fase.ConProductos>(viewModel.uiState.value.fase)
        assertEquals(listOf("Paracetamol", "Ibuprofeno"), fase.productos.map { it.nombre })
        repositorio.compuertaListar!!.complete(Unit)
        assertFalse(viewModel.uiState.value.sincronizando)
        assertNull(viewModel.uiState.value.avisoRed)
    }

    @Test
    fun eliminarPasaPorEnCursoYVuelveAInactivaRecargandoLaLista() = runTest {

        val repositorio = FakeProductoRepository(
            mutableListOf(Producto(id = 1L, nombre = "Paracetamol", precio = 8.5, stock = 100))
        ).apply { compuertaEliminar = CompletableDeferred() }

        val viewModel = nuevoViewModel(repositorio)

        viewModel.eliminar(1L)

        assertEquals(
            ProductoUiState.Operacion.EnCurso(ProductoUiState.Operacion.Tipo.Eliminar),
            viewModel.uiState.value.operacion
        )
        assertIs<ProductoUiState.Fase.ConProductos>(viewModel.uiState.value.fase)

        repositorio.compuertaEliminar!!.complete(Unit)

        val estado = viewModel.uiState.value
        assertEquals(ProductoUiState.Operacion.Inactiva, estado.operacion)
        assertEquals(1, repositorio.vecesQueSeLlamoSincronizar)

        val fase = assertIs<ProductoUiState.Fase.ConProductos>(estado.fase)
        assertEquals(false, fase.productos.first().activo)
    }

    @Test
    fun cancelarLaCorrutinaNoMuestraError() = runTest {

        val repositorio = FakeProductoRepository(
            mutableListOf(Producto(id = 1L, nombre = "Paracetamol", precio = 8.5, stock = 100))
        ).apply { compuertaEliminar = CompletableDeferred() }

        val viewModel = nuevoViewModel(repositorio)

        viewModel.eliminar(1L)
        assertIs<ProductoUiState.Operacion.EnCurso>(viewModel.uiState.value.operacion)

        viewModel.viewModelScope.cancel()

        val estado = viewModel.uiState.value
        assertTrue(estado.operacion !is ProductoUiState.Operacion.Fallida)
        assertTrue(estado.fase !is ProductoUiState.Fase.Error)
        assertNull(estado.mensajeExito)
    }
}