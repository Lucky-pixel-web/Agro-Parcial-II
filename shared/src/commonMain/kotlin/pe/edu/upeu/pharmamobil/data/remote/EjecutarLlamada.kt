package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.SerializationException
import pe.edu.upeu.pharmamobil.data.remote.dto.ErrorResponseDto
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException

suspend fun <T> ejecutarLlamada(bloque: suspend () -> T): Result<T> =
    try {
        Result.success(bloque())
    } catch (cancelacion: CancellationException) {
        throw cancelacion
    } catch (e: ClientRequestException) {
        Result.failure(ErrorApiException(traducirCliente(e)))
    } catch (e: ServerResponseException) {
        Result.failure(ErrorApiException(ErrorApi.Servidor))
    } catch (e: HttpRequestTimeoutException) {
        Result.failure(ErrorApiException(ErrorApi.TiempoAgotado))
    } catch (e: Throwable) {
        println("KtorClient: ${e::class.simpleName} - ${e.message}")
        val error = if (e.tieneCausaDeSerializacion()) ErrorApi.Servidor else ErrorApi.SinConexion
        Result.failure(ErrorApiException(error))
    }

private suspend fun traducirCliente(e: ClientRequestException): ErrorApi {
    val cuerpo = runCatching {
        e.response.body<ErrorResponseDto>()
    }.getOrNull()
    return when (e.response.status.value) {
        400 -> ErrorApi.Validacion(cuerpo?.validationErrors.orEmpty(), cuerpo?.message)
        401 -> ErrorApi.SesionExpirada
        403 -> ErrorApi.SinPermiso
        404 -> ErrorApi.NoEncontrado
        409 -> ErrorApi.Conflicto(cuerpo?.message ?: "Operación no permitida")
        else -> ErrorApi.Servidor
    }
}

private fun Throwable.tieneCausaDeSerializacion(): Boolean =
    generateSequence(this) { it.cause }.any { it is SerializationException }