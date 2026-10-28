package pe.edu.upeu.pharmamobil.presentation.detalle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun DetalleProductoScreen(
    productoId: Long,
    viewModel: DetalleProductoViewModel,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(productoId) {
        viewModel.cargar(productoId)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        TextButton(onClick = onVolver) {
            Text("← Volver a productos")
        }

        when (val fase = estado.fase) {

            DetalleProductoUiState.Fase.Cargando -> CircularProgressIndicator()

            is DetalleProductoUiState.Fase.Error -> Text(
                text = fase.mensaje,
                color = MaterialTheme.colorScheme.error
            )

            is DetalleProductoUiState.Fase.Listo -> {

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = fase.detalle.nombre,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text("Precio: ${fase.detalle.precio}")
                        Text("Stock: ${fase.detalle.stock}")
                        Text(if (fase.detalle.activo) "Estado: Activo" else "Estado: Inactivo")
                    }
                }

                Button(onClick = { viewModel.compartir(fase.producto) }) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Compartir")
                }
            }
        }
    }
}