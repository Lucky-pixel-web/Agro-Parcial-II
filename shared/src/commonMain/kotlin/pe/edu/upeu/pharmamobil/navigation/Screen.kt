package pe.edu.upeu.pharmamobil.navigation

sealed class Screen {

    data object Inicio : Screen()

    data object Productos : Screen()

    data class DetalleProducto(val productoId: Long) : Screen()

    data object Clientes : Screen()

    data object Pedidos : Screen()

    data object AcercaDe : Screen()
}