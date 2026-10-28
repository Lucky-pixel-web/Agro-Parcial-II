
package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.serialization.json.Json
import pe.edu.upeu.pharmamobil.data.local.PerfilSesionLocal
import pe.edu.upeu.pharmamobil.data.local.VigenciaJwt
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import kotlin.coroutines.cancellation.CancellationException
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobil.data.remote.dto.*
import pe.edu.upeu.pharmamobil.domain.model.*
import pe.edu.upeu.pharmamobil.domain.repository.AuthRepository

class AuthRepositorioRest(
    private val cliente: HttpClient,
    private val almacen: AlmacenSeguro
) : AuthRepository {

    override suspend fun iniciarSesion(
        credenciales: Credenciales
    ): Sesion = try {

        val dto: LoginResponseDto =
            cliente.post("auth/login") {
                setBody(
                    LoginRequestDto(
                        usuario = credenciales.usuario,
                        contrasena = credenciales.clave
                    )
                )
            }.body()

        val sesion = dto.aSesion()
        almacen.guardarToken(dto.token)
        almacen.guardarPerfil(Json.encodeToString(PerfilSesionLocal.desde(sesion)))
        cliente.authProvider<BearerAuthProvider>()
            ?.clearToken()

        sesion

    } catch (e: CancellationException) {
        throw e
    } catch (e: ClientRequestException) {
        throw ErrorSesionException(
            mapearErrorSesion(e.response.status, null)
        )
    } catch (e: Exception) {
        throw ErrorSesionException(ErrorSesion.SinConexion)
    }

    override suspend fun sesionGuardada(): Sesion? {
        val token = almacen.leerToken() ?: return null
        if (!VigenciaJwt.estaVigente(token)) {
            limpiarSesionLocal()
            return null
        }

        // El perfil se guarda cifrado en Android y en Keychain para iOS.
        // Un token sin perfil se descarta: no se inventan permisos.
        val perfil = try {
            almacen.leerPerfil()?.let {
                Json.decodeFromString<PerfilSesionLocal>(it)
            }
        } catch (_: Exception) {
            null
        }
        if (perfil == null) {
            limpiarSesionLocal()
            return null
        }
        return perfil.aSesion(token)
    }

    private fun limpiarSesionLocal() {
        almacen.borrarToken()
        almacen.borrarPerfil()
        cliente.authProvider<BearerAuthProvider>()?.clearToken()
    }

    override suspend fun cerrarSesion() {
        limpiarSesionLocal()
    }
}

fun mapearErrorSesion(
    estado: HttpStatusCode,
    mensaje: String?
): ErrorSesion = when (estado) {

    HttpStatusCode.Unauthorized ->
        ErrorSesion.CredencialesInvalidas

    HttpStatusCode.BadRequest ->
        ErrorSesion.CredencialesInvalidas

    HttpStatusCode.Forbidden ->
        ErrorSesion.SinPermiso

    else -> ErrorSesion.SinConexion
}
