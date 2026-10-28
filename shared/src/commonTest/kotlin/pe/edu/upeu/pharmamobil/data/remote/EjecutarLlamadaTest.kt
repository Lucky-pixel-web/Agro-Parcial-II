package pe.edu.upeu.pharmamobil.data.remote

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EjecutarLlamadaTest {

    @Test
    fun laCancelacionSeRelanzaYNoSeConvierteEnError() = runTest {
        assertFailsWith<CancellationException> {
            ejecutarLlamada<Unit> { throw CancellationException("cancelada") }
        }
    }

    @Test
    fun unFormatoInesperadoSeTraduceAServidor() = runTest {
        val resultado = ejecutarLlamada<Unit> { throw SerializationException("formato") }

        val error = (resultado.exceptionOrNull() as ErrorApiException).error
        assertEquals(ErrorApi.Servidor, error)
    }

    @Test
    fun unFalloInesperadoSeTraduceASinConexion() = runTest {
        val resultado = ejecutarLlamada<Unit> { throw IllegalStateException("red caida") }

        val error = (resultado.exceptionOrNull() as ErrorApiException).error
        assertEquals(ErrorApi.SinConexion, error)
    }

    @Test
    fun unaLlamadaExitosaDevuelveSuValor() = runTest {
        val resultado = ejecutarLlamada { 42 }

        assertTrue(resultado.isSuccess)
        assertEquals(42, resultado.getOrNull())
    }
}