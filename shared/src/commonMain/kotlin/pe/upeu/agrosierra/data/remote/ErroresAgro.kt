package pe.upeu.agrosierra.data.remote

import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import pe.upeu.agrosierra.domain.model.ErrorApi

@Serializable
private data class ErrorAgroRespuesta(
    val message: String? = null,
    val validationErrors: Map<String, String> = emptyMap()
)

class ErrorAgroException(val detalle: ErrorApi) :
    Exception(detalle.mensajeParaUsuario())

fun clasificarErrorAgro(
    estado: Int,
    mensaje: String? = null,
    errores: Map<String, String> = emptyMap()
): ErrorApi = when (estado) {
    400 -> ErrorApi.Validacion(errores)
    401 -> ErrorApi.NoAutorizado
    403 -> ErrorApi.SinPrivilegio
    404 -> ErrorApi.NoEncontrado
    409 -> ErrorApi.Conflicto(mensaje?.takeIf { it.isNotBlank() }
        ?: "La operación entra en conflicto con el estado actual.")
    else -> ErrorApi.Servidor
}

fun ErrorApi.mensajeParaUsuario(): String = when (this) {
    is ErrorApi.Validacion -> if (errores.isEmpty()) {
        "Revisa los datos enviados; la solicitud no es válida."
    } else {
        "Corrige los campos indicados en el formulario."
    }
    ErrorApi.NoAutorizado -> "Tu sesión no es válida o ha vencido. Inicia sesión."
    ErrorApi.SinPrivilegio -> "No tienes privilegios para realizar esta operación."
    ErrorApi.NoEncontrado -> "El registro solicitado ya no existe."
    is ErrorApi.Conflicto -> mensaje
    ErrorApi.Servidor -> "El servidor no pudo completar la operación."
    ErrorApi.SinConexion -> "Sin conexión con AgroAPI. Los datos locales se conservan."
    ErrorApi.TiempoAgotado -> "La conexión tardó demasiado. Intenta sincronizar después."
}

/** Traduce los errores HTTP antes de que lleguen a la pantalla. */
suspend fun <T> controlarErrorAgro(bloque: suspend () -> T): T = try {
    bloque()
} catch (e: CancellationException) {
    throw e
} catch (e: ResponseException) {
    val respuesta = try {
        Json { ignoreUnknownKeys = true }
            .decodeFromString<ErrorAgroRespuesta>(e.response.bodyAsText())
    } catch (_: Exception) {
        null
    }
    throw ErrorAgroException(
        clasificarErrorAgro(
            e.response.status.value,
            respuesta?.message,
            respuesta?.validationErrors.orEmpty()
        )
    )
} catch (e: HttpRequestTimeoutException) {
    throw ErrorAgroException(ErrorApi.TiempoAgotado)
} catch (e: ErrorAgroException) {
    throw e
} catch (e: Exception) {
    throw ErrorAgroException(ErrorApi.SinConexion)
}
