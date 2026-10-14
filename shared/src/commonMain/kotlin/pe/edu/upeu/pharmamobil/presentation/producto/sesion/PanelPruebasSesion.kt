package pe.edu.upeu.pharmamobil.presentation.sesion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.request.get
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguro

@Composable
fun PanelPruebasSesion() {
    val almacen: AlmacenSeguro = koinInject()
    val cliente: HttpClient = koinInject()
    val scope = rememberCoroutineScope()
    var resultado by remember { mutableStateOf("") }

    Surface(tonalElevation = 3.dp) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Pruebas de sesión (solo depuración)", style = MaterialTheme.typography.titleSmall)

            OutlinedButton(onClick = {
                val t = almacen.leerToken()
                if (t == null) {
                    resultado = "No hay token guardado"
                } else {
                    val i = t.lastIndexOf('.') + 1
                    val c = if (t[i] == 'A') 'B' else 'A'
                    almacen.guardarToken(t.substring(0, i) + c + t.substring(i + 1))
                    scope.launch { cliente.authProvider<BearerAuthProvider>()?.clearToken() }
                    resultado = "Token alterado. Ahora pulsa la petición de prueba."
                }
            }) { Text("Alterar token guardado") }

            OutlinedButton(onClick = {
                scope.launch {
                    resultado = try {
                        val r = cliente.get("productos")
                        "HTTP ${r.status.value}"
                    } catch (e: Exception) {
                        "Error: ${e.message}".take(300)
                    }
                }
            }) { Text("Petición de prueba a /productos") }

            if (resultado.isNotEmpty()) {
                Text(resultado, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}