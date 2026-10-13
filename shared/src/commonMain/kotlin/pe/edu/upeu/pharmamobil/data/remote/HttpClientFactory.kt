package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguro
import io.ktor.http.encodedPath

fun crearHttpClient(
    engine: HttpClientEngine,
    urlBase: String,
    almacen: AlmacenSeguro,
    esDepuracion: Boolean,
    onSesionExpirada: () -> Unit
): HttpClient = HttpClient(engine) {
    expectSuccess = true

    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true })
    }

    install(Auth) {
        bearer {
            loadTokens { almacen.leerToken()?.let { BearerTokens(it, "") } }
            sendWithoutRequest { peticion -> !peticion.url.encodedPath.endsWith("/auth/login") }
        }
    }

    install(Logging) {
        level = if (esDepuracion) LogLevel.HEADERS else LogLevel.NONE
        sanitizeHeader { nombre -> nombre == HttpHeaders.Authorization }
        logger = object : Logger {
            override fun log(message: String) { println("KtorClient: $message") }
        }
    }

    install(HttpTimeout) {
        requestTimeoutMillis = 15000
        connectTimeoutMillis = 10000
    }

    HttpResponseValidator {
        handleResponseExceptionWithRequest { causa, peticion ->
            val e = causa as? ClientRequestException ?: return@handleResponseExceptionWithRequest
            if (e.response.status == HttpStatusCode.Unauthorized &&
                !peticion.url.encodedPath.endsWith("/auth/login")
            ) {
                almacen.borrarToken()
                onSesionExpirada()
            }
        }
    }

    defaultRequest {
        url(urlBase)
        contentType(ContentType.Application.Json)
    }
}