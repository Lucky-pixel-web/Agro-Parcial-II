
package pe.edu.upeu.pharmamobil.data.remote.dto

import kotlinx.serialization.Serializable
import pe.edu.upeu.pharmamobil.domain.model.Sesion

@Serializable
data class LoginRequestDto(
    val usuario: String,
    val contrasena: String
)

@Serializable
data class LoginResponseDto(
    val token: String,
    val nombre: String,
    val usuario: String,
    val rol: String,
    val privilegios: Set<String>
) {
    fun aSesion() = Sesion(
        token = token,
        usuario = usuario,
        nombreCompleto = nombre,
        permisos = privilegios,
        rol = rol
    )
}

@Serializable
data class UsuarioActualDto(
    val id: Long,
    val usuario: String,
    val nombre: String,
    val rol: String,
    val privilegios: Set<String>
)
