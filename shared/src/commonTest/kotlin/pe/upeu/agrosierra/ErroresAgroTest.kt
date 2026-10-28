package pe.upeu.agrosierra

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import pe.upeu.agrosierra.data.remote.clasificarErrorAgro
import pe.upeu.agrosierra.domain.model.ErrorApi

class ErroresAgroTest {
    @Test
    fun traduceErroresHttp() {
        assertIs<ErrorApi.NoAutorizado>(clasificarErrorAgro(401))
        assertIs<ErrorApi.SinPrivilegio>(clasificarErrorAgro(403))
        assertIs<ErrorApi.NoEncontrado>(clasificarErrorAgro(404))
        assertIs<ErrorApi.Servidor>(clasificarErrorAgro(500))
        assertIs<ErrorApi.Conflicto>(clasificarErrorAgro(409, "Campaña cerrada"))
    }

    @Test
    fun conservaElErrorDelCampoDevueltoPorAgroApi() {
        val resultado = clasificarErrorAgro(
            400,
            "Datos invalidos",
            mapOf("cantidadJornales" to "Debe ser mayor que cero")
        )
        assertEquals(
            "Debe ser mayor que cero",
            assertIs<ErrorApi.Validacion>(resultado).errores["cantidadJornales"]
        )
    }
}
