package pe.edu.upeu.pharmamobil.presentation.sesion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.request.get
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguro


/** Ponlo en false antes de entregar para ocultar el panel. */
const val MOSTRAR_PANEL_PRUEBAS = false

data class ResultadoPrueba(
    val peticion: String,
    val codigo: Int?,
    val mensaje: String?,
    val detalle: String
)

object PruebasSesion {
    var resultado by mutableStateOf<ResultadoPrueba?>(null)
}

@Composable
fun PanelPruebasSesion() {
    val almacen: AlmacenSeguro = koinInject()
    val cliente: HttpClient = koinInject()
    val scope = rememberCoroutineScope()
    var aviso by remember { mutableStateOf("") }
    var abierto by remember { mutableStateOf(false) }

    Surface(tonalElevation = 3.dp) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TextButton(onClick = { abierto = !abierto }) {
                Text(
                    if (abierto) "Ocultar pruebas ▲" else "Pruebas (debug) ▼",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            if (abierto) {
                OutlinedButton(onClick = {
                    val t = almacen.leerToken()
                    if (t == null) {
                        aviso = "No hay token guardado"
                    } else {
                        val i = t.lastIndexOf('.') + 1
                        val c = if (t[i] == 'A') 'B' else 'A'
                        almacen.guardarToken(t.substring(0, i) + c + t.substring(i + 1))
                        scope.launch { cliente.authProvider<BearerAuthProvider>()?.clearToken() }
                        aviso = "Token alterado. Pulsa la petición de prueba."
                    }
                }) { Text("Alterar token guardado") }

                OutlinedButton(onClick = {
                    scope.launch {
                        val peticion = "GET /api/v1/productos"
                        try {
                            val r = cliente.get("productos")
                            PruebasSesion.resultado = ResultadoPrueba(
                                peticion, r.status.value, null, "La petición se completó correctamente."
                            )
                        } catch (e: Exception) {
                            val texto = e.message ?: ""
                            println("PRUEBA_PANEL -> $texto")
                            PruebasSesion.resultado = ResultadoPrueba(
                                peticion = peticion,
                                codigo = (e as? ResponseException)?.response?.status?.value,
                                mensaje = Regex("\"message\"\\s*:\\s*\"([^\"]*)\"")
                                    .find(texto)?.groupValues?.get(1),
                                detalle = texto
                            )
                        }
                    }
                }) { Text("Petición de prueba a /productos") }

                if (aviso.isNotEmpty()) {
                    Text(aviso, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun DialogoResultadoPrueba() {
    val r = PruebasSesion.resultado ?: return
    AlertDialog(
        onDismissRequest = { PruebasSesion.resultado = null },
        confirmButton = {
            TextButton(onClick = { PruebasSesion.resultado = null }) { Text("Cerrar") }
        },
        title = { Text("Resultado de la petición") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Petición: ${r.peticion}")
                Text("Código HTTP: ${r.codigo ?: "—"}")
                Text("Mensaje del servidor: ${r.mensaje ?: "—"}")
                Text("Detalle:", style = MaterialTheme.typography.labelMedium)
                Text(r.detalle, style = MaterialTheme.typography.bodySmall)
            }
        }
    )
}