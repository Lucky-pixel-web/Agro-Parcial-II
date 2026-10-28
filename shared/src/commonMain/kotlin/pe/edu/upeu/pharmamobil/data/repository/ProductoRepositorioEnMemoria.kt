package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.delay
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository
import kotlin.random.Random
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ProductoRepositorioEnMemoria : ProductoRepository {

    private val productos = mutableListOf(
        Producto(id = 1L, nombre = "Paracetamol", precio = 8.50, stock = 100),
        Producto(id = 2L, nombre = "Ibuprofeno", precio = 12.00, stock = 50),
        Producto(id = 3L, nombre = "Amoxicilina", precio = 18.50, stock = 20)
    )

    override suspend fun registrar(producto: Producto): Producto {
        delay(Random.nextLong(300, 800))

        val productoConId = producto.copy(
            id = Random.nextLong(1, Long.MAX_VALUE)
        )

        productos.add(0, productoConId)
        return productoConId
    }

    override suspend fun listar(): List<Producto> {
        delay(Random.nextLong(300, 800))
        return productos.toList()
    }

    override suspend fun obtener(id: Long): Producto {
        delay(Random.nextLong(300, 800))
        return productos.firstOrNull { it.id == id }
            ?: throw ErrorApiException(ErrorApi.NoEncontrado)
    }

    override suspend fun actualizar(producto: Producto): Producto {
        delay(Random.nextLong(300, 800))
        val indice = productos.indexOfFirst { it.id == producto.id }
        if (indice < 0) throw ErrorApiException(ErrorApi.NoEncontrado)
        productos[indice] = producto
        return producto
    }

    override suspend fun eliminar(id: Long) {
        delay(Random.nextLong(300, 800))
        val indice = productos.indexOfFirst { it.id == id }
        if (indice < 0) throw ErrorApiException(ErrorApi.NoEncontrado)
        productos[indice] = productos[indice].copy(activo = false)
    }
    override fun observar(): Flow<List<Producto>> = flow { emit(listar()) }

    override suspend fun sincronizar() {}
}