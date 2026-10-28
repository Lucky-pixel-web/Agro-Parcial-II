package pe.edu.upeu.pharmamobil.presentation.sesion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private data class Visual(
    val icono: ImageVector,
    val color: Color,
    val titulo: String,
    val detalle: String?
)

@Composable
fun ResultadoSesionOverlay(evento: EventoSesion?) {
    var ultimo by remember { mutableStateOf<EventoSesion?>(null) }
    if (evento != null) ultimo = evento
    val mostrado = ultimo

    AnimatedVisibility(visible = evento != null, enter = fadeIn(), exit = fadeOut()) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            if (mostrado != null) {
                val v = when (mostrado) {
                    is EventoSesion.Ingreso -> Visual(
                        Icons.Default.CheckCircle, Color(0xFF2E7D32),
                        "Ingreso correcto", "Bienvenido, ${mostrado.nombre}"
                    )
                    is EventoSesion.Rechazado -> Visual(
                        Icons.Default.Cancel, MaterialTheme.colorScheme.error,
                        "No se pudo ingresar", mostrado.motivo
                    )
                    EventoSesion.Cierre -> Visual(
                        Icons.AutoMirrored.Filled.ExitToApp, MaterialTheme.colorScheme.primary,
                        "Sesión cerrada", "Hasta pronto"
                    )
                }
                val escala = remember { Animatable(0.4f) }
                LaunchedEffect(Unit) {
                    escala.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                }
                Surface(shape = RoundedCornerShape(24.dp), tonalElevation = 8.dp) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = v.icono,
                            contentDescription = null,
                            tint = v.color,
                            modifier = Modifier.size(72.dp).scale(escala.value)
                        )
                        Text(v.titulo, style = MaterialTheme.typography.titleMedium)
                        v.detalle?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}