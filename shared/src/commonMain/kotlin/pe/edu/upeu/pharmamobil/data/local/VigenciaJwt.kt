package pe.edu.upeu.pharmamobil.data.local

import kotlin.io.encoding.Base64
import kotlin.time.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Comprueba la caducidad local del JWT (campo exp).
 * No verifica su firma: el backend sigue siendo la autoridad.
 */
object VigenciaJwt {
    fun estaVigente(
        token: String,
        ahoraSegundos: Long = Clock.System.now().epochSeconds
    ): Boolean = try {
        val partes = token.split('.')
        if (partes.size != 3 || partes.any { it.isBlank() }) {
            false
        } else {
            val segmento = partes[1]
            val relleno = "=".repeat((4 - segmento.length % 4) % 4)
            val contenido = Base64.UrlSafe.decode(segmento + relleno).decodeToString()
            val exp = Json.parseToJsonElement(contenido)
                .jsonObject["exp"]?.jsonPrimitive?.longOrNull
            exp != null && exp > ahoraSegundos
        }
    } catch (_: Exception) {
        false
    }
}
