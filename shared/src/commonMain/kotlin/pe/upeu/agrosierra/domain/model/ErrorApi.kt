
package pe.upeu.agrosierra.domain.model

sealed interface ErrorApi {
    data class Validacion(
        val errores: Map<String, String>
    ) : ErrorApi

    data object NoAutorizado : ErrorApi
    data object SinPrivilegio : ErrorApi
    data object NoEncontrado : ErrorApi

    data class Conflicto(
        val mensaje: String
    ) : ErrorApi

    data object Servidor : ErrorApi
    data object SinConexion : ErrorApi
    data object TiempoAgotado : ErrorApi
}
