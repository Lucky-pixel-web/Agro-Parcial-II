package pe.edu.upeu.pharmamobil.presentation.producto

import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.platform.formatearSoles

data class ProductoUi(
    val id: Long,
    val nombre: String,
    val precio: String,
    val stock: String,
    val activo: Boolean,
    val esBajoStock: Boolean,
    val requiereReposicion: Boolean,
    val precioValor: String,
    val stockValor: String
)

fun Producto.aUi(): ProductoUi = ProductoUi(
    id = id,
    nombre = nombre,
    precio = formatearSoles(precio),
    stock = "$stock u.",
    activo = activo,
    esBajoStock = esBajoStock(),
    requiereReposicion = requiereReposicion(),
    precioValor = precio.toString(),
    stockValor = stock.toString()
)