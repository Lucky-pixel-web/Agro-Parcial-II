package pe.edu.upeu.pharmamobil.data.local

import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VigenciaJwtTest {
    private fun token(exp: Long): String {
        val contenido = """{"exp":$exp}"""
        return "cabecera." +
            Base64.UrlSafe.encode(contenido.encodeToByteArray()).trimEnd('=') +
            ".firma"
    }

    @Test
    fun tokenNoExpirado() {
        assertTrue(VigenciaJwt.estaVigente(token(2000001000L), 2000000000L))
    }

    @Test
    fun tokenExpirado() {
        assertFalse(VigenciaJwt.estaVigente(token(2000000000L), 2000000000L))
    }

    @Test
    fun tokenInvalido() {
        assertFalse(VigenciaJwt.estaVigente("invalido", 2000000000L))
    }
}
