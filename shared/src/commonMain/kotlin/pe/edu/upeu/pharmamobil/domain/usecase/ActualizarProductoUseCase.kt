package pe.edu.upeu.pharmamobil.domain.usecase

import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

class ActualizarProductoUseCase(
    private val productoRepository: ProductoRepository
) {

    suspend operator fun invoke(
        id: Long,
        nombre: String,
        precio: String,
        stock: String,
        activo: Boolean = true
    ): Result<Producto> {

        val errores = validarProducto(nombre, precio, stock)

        if (errores.hayErrores) {
            return Result.failure(ProductoInvalidoException(errores))
        }

        return resultadoDe {
            productoRepository.actualizar(
                Producto(
                    id = id,
                    nombre = nombre.trim(),
                    precio = precio.toDouble(),
                    stock = stock.toInt(),
                    activo = activo
                )
            )
        }
    }
}