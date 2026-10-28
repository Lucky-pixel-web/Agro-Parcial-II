package pe.edu.upeu.pharmamobil.domain.model

data class Producto(
    val id: Long,
    val nombre: String,
    val precio: Double,
    val stock: Int,
    val activo: Boolean = true
) {
    companion object {
        const val STOCK_MINIMO = 5
    }

    fun tieneStock(): Boolean {
        return stock > 0
    }

    fun esBajoStock(): Boolean {
        return stock in 1..STOCK_MINIMO
    }

    fun requiereReposicion(): Boolean {
        return stock <= STOCK_MINIMO
    }
}