
package pe.upeu.agrosierra.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import pe.upeu.agrosierra.domain.model.*
import pe.edu.upeu.pharmamobil.domain.model.Sesion

@Composable
fun AgroCampoScreen(
    sesion: Sesion,
    onCerrarSesion: () -> Unit
) {
    val vm: AgroViewModel = koinViewModel(
        key = sesion.usuario
    )

    val lotes by vm.lotes.collectAsStateWithLifecycle()
    val pendientesPorLote by
    vm.pendientesPorLote.collectAsStateWithLifecycle()
    val estado by vm.estado.collectAsStateWithLifecycle()

    var seleccionado by remember {
        mutableStateOf<Lote?>(null)
    }
    var perfil by remember { mutableStateOf(false) }
    var texto by remember { mutableStateOf("") }
    var valle by remember { mutableStateOf("") }
    var abiertas by remember { mutableStateOf(false) }

    LaunchedEffect(sesion.usuario) {
        vm.sincronizar()
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text(
            "AgroCampo",
            style = MaterialTheme.typography.headlineSmall
        )

        Text("Usuario: ${sesion.usuario}")

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = {
                perfil = false
                seleccionado = null
            }) {
                Text("Lotes")
            }

            Button(onClick = { perfil = true }) {
                Text("Perfil")
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { vm.sincronizar() }
            ) {
                Text("Sincronizar")
            }

            OutlinedButton(onClick = onCerrarSesion) {
                Text("Salir")
            }
        }

        when (val e = estado) {
            AgroUiState.Cargando ->
                CircularProgressIndicator()

            is AgroUiState.Exito ->
                Text(e.mensaje)

            is AgroUiState.Error ->
                Text(
                    e.mensaje,
                    color = MaterialTheme.colorScheme.error
                )

            AgroUiState.Listo -> Unit
        }

        if (perfil) {
            Text("Nombre: ${sesion.nombreCompleto}")
            Text("Rol: ${sesion.rol}")
            Text("Privilegios:")
            sesion.permisos.sorted().forEach {
                Text("• $it")
            }
        } else if (seleccionado == null) {

            OutlinedTextField(
                value = texto,
                onValueChange = {
                    texto = it
                    vm.cambiarBusqueda(it)
                },
                label = { Text("Buscar nombre o código") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = valle,
                onValueChange = {
                    valle = it
                    vm.cambiarValle(it)
                },
                label = { Text("Valle") },
                modifier = Modifier.fillMaxWidth()
            )

            Row {
                Checkbox(
                    checked = abiertas,
                    onCheckedChange = {
                        abiertas = it
                        vm.cambiarSoloAbiertas(it)
                    }
                )
                Text(
                    "Solo campañas abiertas",
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            TextButton(
                onClick = {
                    texto = ""
                    valle = ""
                    abiertas = false

                    vm.cambiarBusqueda("")
                    vm.cambiarValle("")
                    vm.cambiarSoloAbiertas(false)
                }
            ) {
                Text("Limpiar filtros")
            }

            if (lotes.isEmpty()) {
                Text("No hay lotes almacenados.")
            }

            LazyColumn {
                items(lotes, key = { it.id }) { lote ->

                    val totalPendientes =
                        pendientesPorLote[lote.id] ?: 0L

                    ListItem(
                        headlineContent = {
                            Text(lote.nombre)
                        },
                        supportingContent = {
                            Text(
                                "${lote.codigo} · ${lote.valle} · " +
                                        if (lote.campaniaCerrada)
                                            "Cerrada"
                                        else
                                            "Abierta"
                            )
                        },
                        trailingContent = {
                            Column {
                                Badge(
                                    containerColor =
                                        if (totalPendientes > 0)
                                            MaterialTheme.colorScheme.error
                                        else
                                            MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor =
                                        if (totalPendientes > 0)
                                            MaterialTheme.colorScheme.onError
                                        else
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                ) {
                                    Text(totalPendientes.toString())
                                }

                                Text(
                                    "Pendientes",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        },
                        modifier = Modifier.clickable {
                            seleccionado = lote
                        }
                    )

                    HorizontalDivider()
                }
            }

        } else {
            val lote = seleccionado!!

            DetalleAgro(
                lote = lote,
                sesion = sesion,
                vm = vm,
                erroresServidor = (estado as? AgroUiState.Error)?.campos.orEmpty(),
                onVolver = { seleccionado = null }
            )
        }
    }
}

@Composable
private fun DetalleAgro(
    lote: Lote,
    sesion: Sesion,
    vm: AgroViewModel,
    erroresServidor: Map<String, String>,
    onVolver: () -> Unit
) {
    val flujoLabores = remember(lote.id) {
        vm.observarLabores(lote.id)
    }
    val flujoPendientes = remember(lote.id) {
        vm.contarPendientes(lote.id)
    }

    val labores by flujoLabores.collectAsState(
        initial = emptyList()
    )
    val pendientes by flujoPendientes.collectAsState(
        initial = 0L
    )

    var edicion by remember {
        mutableStateOf<Labor?>(null)
    }
    var fecha by remember {
        mutableStateOf("")
    }
    var cantidad by remember {
        mutableStateOf("")
    }
    var jornales by remember {
        mutableStateOf("1")
    }
    var observacion by remember {
        mutableStateOf("")
    }
    var tipo by remember {
        mutableStateOf(TipoLabor.RIEGO)
    }
    var erroresLocales by remember { mutableStateOf(emptyMap<String, String>()) }
    val errores = erroresServidor + erroresLocales
    fun limpiarErrores() {
        erroresLocales = emptyMap()
        vm.limpiarMensaje()
    }

    Column(
        Modifier.verticalScroll(rememberScrollState())
    ) {
        TextButton(onClick = onVolver) {
            Text("Volver a lotes")
        }

        Text(
            lote.nombre,
            style = MaterialTheme.typography.titleLarge
        )

        Text("Valle: ${lote.valle}")
        Text("Cultivo: ${lote.cultivo}")
        Text("Hectáreas: ${lote.hectareas}")
        Text("Pendientes de sincronizar: $pendientes")

        if (
            sesion.puede("CAMPANIA_CERRAR") &&
            !lote.campaniaCerrada
        ) {
            Button(
                onClick = {
                    vm.cerrarCampania(lote.id)
                }
            ) {
                Text("Cerrar campaña")
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Text(
            "Labores registradas",
            style = MaterialTheme.typography.titleMedium
        )

        labores.forEach { labor ->
            Column(
                Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Text(
                    "${labor.tipo} · ${labor.fecha}"
                )
                Text("Responsable: ${labor.responsable}")
                Text("Cantidad: ${labor.cantidad}")
                Text("Jornales: ${labor.cantidadJornales}")

                if (labor.pendiente) {
                    Text("Pendiente de sincronizar")
                }

                if (sesion.puede("LABOR_EDITAR")) {
                    TextButton(onClick = {
                        edicion = labor
                        fecha = labor.fecha
                        cantidad = labor.cantidad.toString()
                        jornales = labor.cantidadJornales.toString()
                        observacion = labor.observacion
                        tipo = labor.tipo
                    }) {
                        Text("Corregir")
                    }
                }

                if (
                    sesion.puede("LABOR_ELIMINAR") ||
                    labor.id < 0L
                ) {
                    TextButton(
                        onClick = {
                            vm.eliminar(labor.id)
                        }
                    ) {
                        Text("Eliminar")
                    }
                }
            }
            HorizontalDivider()
        }

        if (
            !lote.campaniaCerrada &&
            (
                    sesion.puede("LABOR_CREAR") ||
                            (edicion != null &&
                                    sesion.puede("LABOR_EDITAR"))
                    )
        ) {
            Text(
                if (edicion == null)
                    "Registrar labor"
                else
                    "Corregir labor",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TipoLabor.entries.forEach { opcion ->
                    FilterChip(
                        selected = tipo == opcion,
                        onClick = { tipo = opcion },
                        label = { Text(opcion.name) }
                    )
                }
            }

            OutlinedTextField(
                value = fecha,
                onValueChange = { fecha = it; limpiarErrores() },
                label = { Text("Fecha AAAA-MM-DD") },
                isError = errores.containsKey("fecha"),
                supportingText = {
                    errores["fecha"]?.let { Text(it) }
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = cantidad,
                onValueChange = { cantidad = it; limpiarErrores() },
                label = { Text("Cantidad") },
                isError = errores.containsKey("cantidad"),
                supportingText = {
                    errores["cantidad"]?.let { Text(it) }
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = jornales,
                onValueChange = { jornales = it; limpiarErrores() },
                label = { Text("Cantidad de jornales") },
                isError = errores.containsKey("cantidadJornales"),
                supportingText = {
                    errores["cantidadJornales"]?.let { Text(it) }
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = observacion,
                onValueChange = { observacion = it; limpiarErrores() },
                label = { Text("Observación") },
                isError = errores.containsKey("observacion"),
                supportingText = {
                    errores["observacion"]?.let { Text(it) }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Button(onClick = {
                val numero = cantidad.toDoubleOrNull()
                val numeroJornales = jornales.toIntOrNull()

                erroresLocales = validarLaborFormulario(
                    lote, tipo, fecha, cantidad, jornales, observacion
                )
                if (erroresLocales.isEmpty()) {
                    val labor = Labor(
                        id = edicion?.id ?: -1L,
                        loteId = lote.id,
                        tipo = tipo,
                        fecha = fecha,
                        responsable =
                            edicion?.responsable ?: sesion.usuario,
                        cantidad = numero,
                        cantidadJornales = numeroJornales,
                        observacion = observacion,
                        pendiente = true
                    )

                    if (edicion == null) {
                        vm.registrar(labor)
                    } else {
                        vm.corregir(labor)
                    }

                    edicion = null
                    fecha = ""
                    cantidad = ""
                    observacion = ""
                    jornales = "1"
                    erroresLocales = emptyMap()
                }
            }) {
                Text(
                    if (edicion == null)
                        "Guardar labor"
                    else
                        "Guardar corrección"
                )
            }

            if (edicion != null) {
                TextButton(onClick = {
                    edicion = null
                }) {
                    Text("Cancelar edición")
                }
            }
        }
    }
}
