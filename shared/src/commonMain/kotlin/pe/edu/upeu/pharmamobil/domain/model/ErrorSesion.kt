package pe.edu.upeu.pharmamobil.domain.model

sealed interface ErrorSesion {
    data object CredencialesInvalidas : ErrorSesion
    data object SesionExpirada : ErrorSesion
    data object SinPermiso : ErrorSesion
    data object SinConexion : ErrorSesion
}

class ErrorSesionException(val error: ErrorSesion) : Exception(error.mensajeUsuario())

fun ErrorSesion.mensajeUsuario(): String = when (this) {
    ErrorSesion.CredencialesInvalidas -> "Usuario o contraseña incorrectos."
    ErrorSesion.SesionExpirada -> "Tu sesión expiró. Inicia sesión nuevamente."
    ErrorSesion.SinPermiso -> "No tienes permiso para realizar esta acción."
    ErrorSesion.SinConexion -> "No se pudo conectar con el servidor."
}