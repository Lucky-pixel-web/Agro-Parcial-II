package pe.edu.upeu.pharmamobil.domain.error

sealed interface ErrorApi {
    data class Validacion(
        val porCampo: Map<String, String>,
        val mensaje: String? = null
    ) : ErrorApi

    data object NoEncontrado : ErrorApi
    data class Conflicto(val mensaje: String) : ErrorApi
    data object Servidor : ErrorApi
    data object SinConexion : ErrorApi
    data object TiempoAgotado : ErrorApi

    data object SesionExpirada : ErrorApi

    data object SinPermiso : ErrorApi
}

class ErrorApiException(val error: ErrorApi) : Exception(error.mensajeUsuario())

fun ErrorApi.mensajeUsuario(): String = when (this) {
    is ErrorApi.Validacion -> mensaje ?: "Revisa los datos del formulario."
    ErrorApi.NoEncontrado -> "No se encontró la información solicitada."
    is ErrorApi.Conflicto -> mensaje
    ErrorApi.Servidor -> "El servidor tuvo un problema. Inténtalo más tarde."
    ErrorApi.SinConexion -> "No se pudo conectar con el servidor. Revisa tu conexión a internet."
    ErrorApi.TiempoAgotado -> "El servidor tardó demasiado en responder. Inténtalo de nuevo."
    ErrorApi.SesionExpirada -> "Tu sesión expiró. Inicia sesión nuevamente."
    ErrorApi.SinPermiso -> "No tienes permiso para realizar esta acción."
}