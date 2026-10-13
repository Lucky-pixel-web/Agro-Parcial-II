package pe.edu.upeu.pharmamobil.presentation.sesion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
    ui: SesionUiState,
    onUsuario: (String) -> Unit,
    onClave: (String) -> Unit,
    onIngresar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("PharmaMobil", style = MaterialTheme.typography.headlineMedium)
        Text("Inicia sesión", style = MaterialTheme.typography.bodyLarge)
        OutlinedTextField(
            value = ui.usuario,
            onValueChange = onUsuario,
            label = { Text("Usuario") },
            singleLine = true,
            enabled = !ui.cargando,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = ui.clave,
            onValueChange = onClave,
            label = { Text("Contraseña") },
            singleLine = true,
            enabled = !ui.cargando,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = onIngresar,
            enabled = !ui.cargando && ui.usuario.isNotBlank() && ui.clave.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (ui.cargando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("Ingresar")
        }
    }
}