package pe.edu.upeu.pharmamobil.domain.usecase

import pe.edu.upeu.pharmamobil.domain.model.Producto
import kotlin.test.Test
import kotlin.test.assertTrue

class TextoParaCompartirTest {

    @Test
    fun elTextoIncluyeNombreYStock() {
        val producto = Producto(id = 1L, nombre = "Paracetamol 500 mg", precio = 4.5, stock = 120)

        val texto = producto.comoTextoParaCompartir()

        assertTrue(texto.contains("Paracetamol 500 mg"))
        assertTrue(texto.contains("Stock: 120"))
    }
}