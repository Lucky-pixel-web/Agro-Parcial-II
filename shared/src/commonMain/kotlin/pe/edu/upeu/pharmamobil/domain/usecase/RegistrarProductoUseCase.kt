package pe.edu.upeu.pharmamobil.domain.usecase

import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

data class ErroresDeProducto(
    val nombre: String? = null,
    val precio: String? = null,
    val stock: String? = null
) {
    val hayErrores: Boolean
        get() = nombre != null || precio != null || stock != null
}

class ProductoInvalidoException(
    val errores: ErroresDeProducto
) : IllegalArgumentException("Los datos del producto no cumplen las reglas del negocio")

private val NOMBRE_REGEX = Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑ0-9 .,%/()-]+$")

internal fun validarProducto(nombre: String, precio: String, stock: String): ErroresDeProducto {
    val precioValor = precio.toDoubleOrNull()
    val stockValor = stock.toIntOrNull()

    return ErroresDeProducto(
        nombre = when {
            nombre.isBlank() -> "Ingrese nombre del producto, es obligatorio"
            !NOMBRE_REGEX.matches(nombre) -> "El nombre contiene caracteres no permitidos"
            else -> null
        },
        precio = when {
            precioValor == null -> "Ingrese un precio numerico"
            precioValor <= 0 -> "El precio debe ser mayor que cero"
            else -> null
        },
        stock = when {
            stockValor == null -> "Ingrese un stock entero"
            stockValor < 0 -> "El stock no puede ser negativo"
            else -> null
        }
    )
}

class RegistrarProductoUseCase(
    private val productoRepository: ProductoRepository
) {

    suspend operator fun invoke(
        nombre: String,
        precio: String,
        stock: String
    ): Result<Producto> {

        val errores = validarProducto(nombre, precio, stock)

        if (errores.hayErrores) {
            return Result.failure(ProductoInvalidoException(errores))
        }

        return resultadoDe {
            productoRepository.registrar(
                Producto(
                    id = 0L,
                    nombre = nombre.trim(),
                    precio = precio.toDouble(),
                    stock = stock.toInt()
                )
            )
        }
    }
}