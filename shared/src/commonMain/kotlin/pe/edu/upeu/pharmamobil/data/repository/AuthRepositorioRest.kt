
package pe.edu.upeu.pharmamobil.data.repository

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

        almacen.guardarToken(dto.token)
        cliente.authProvider<BearerAuthProvider>()
            ?.clearToken()

        dto.aSesion()

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

        return try {
            val me: UsuarioActualDto =
                cliente.get("usuarios/me").body()

            Sesion(
                token = token,
                usuario = me.usuario,
                nombreCompleto = me.nombre,
                permisos = me.privilegios,
                rol = me.rol
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.Unauthorized) {
                almacen.borrarToken()
                cliente.authProvider<BearerAuthProvider>()
                    ?.clearToken()
                null
            } else {
                throw ErrorSesionException(
                    ErrorSesion.SinConexion
                )
            }
        } catch (e: Exception) {
            throw ErrorSesionException(ErrorSesion.SinConexion)
        }
    }

    override suspend fun cerrarSesion() {
        almacen.borrarToken()
        cliente.authProvider<BearerAuthProvider>()
            ?.clearToken()
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
