package pe.edu.upeu.pharmamobil.data.local

import kotlinx.serialization.Serializable
import pe.edu.upeu.pharmamobil.domain.model.Sesion

@Serializable
data class PerfilSesionLocal(
    val usuario: String,
    val nombreCompleto: String,
    val rol: String,
    val permisos: Set<String>
) {
    fun aSesion(token: String) = Sesion(token, usuario, nombreCompleto, permisos, rol)
    companion object {
        fun desde(sesion: Sesion) = PerfilSesionLocal(
            sesion.usuario, sesion.nombreCompleto, sesion.rol, sesion.permisos
        )
    }
}
