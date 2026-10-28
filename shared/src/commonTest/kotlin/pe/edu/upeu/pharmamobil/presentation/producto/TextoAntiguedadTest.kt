package pe.edu.upeu.pharmamobil.presentation.producto

import kotlin.test.Test
import kotlin.test.assertEquals

class TextoAntiguedadTest {
    private val minuto = 60_000L
    private val hora = 60 * minuto
    private val dia = 24 * hora

    @Test fun nuncaSincronizado() =
        assertEquals("Nunca se ha sincronizado", textoAntiguedad(null, 1_000L))

    @Test fun minutos() =
        assertEquals("Actualizado hace 5 minutos", textoAntiguedad(0L, 5 * minuto))

    @Test fun horas() =
        assertEquals("Actualizado hace 2 horas", textoAntiguedad(0L, 2 * hora))

    @Test fun masDeUnDia() =
        assertEquals("Actualizado hace 2 días", textoAntiguedad(0L, 2 * dia + hora))
}