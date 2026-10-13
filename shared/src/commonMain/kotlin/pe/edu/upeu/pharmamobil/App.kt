package pe.edu.upeu.pharmamobil

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.compose.KoinContext
import org.koin.compose.viewmodel.koinViewModel
import pe.edu.upeu.pharmamobil.navigation.Screen
import pe.edu.upeu.pharmamobil.presentation.acercade.AcercaDeScreen
import pe.edu.upeu.pharmamobil.presentation.cliente.ClienteScreen
import pe.edu.upeu.pharmamobil.presentation.cliente.ClienteViewModel
import pe.edu.upeu.pharmamobil.presentation.detalle.DetalleProductoScreen
import pe.edu.upeu.pharmamobil.presentation.detalle.DetalleProductoViewModel
import pe.edu.upeu.pharmamobil.presentation.inicio.InicioScreen
import pe.edu.upeu.pharmamobil.presentation.sesion.LoginScreen
import pe.edu.upeu.pharmamobil.presentation.sesion.SesionViewModel
import pe.edu.upeu.pharmamobil.domain.model.Sesion
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoScreen
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoViewModel
import pe.edu.upeu.pharmamobil.theme.PharmaMobilTheme

@Composable
fun App() = KoinContext {
    val sesionVm: SesionViewModel = koinViewModel()
    val ui by sesionVm.uiState.collectAsStateWithLifecycle()
    val sesion = ui.sesion

    when {
        ui.verificando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        sesion == null -> PharmaMobilTheme {
            LoginScreen(ui, sesionVm::onUsuarioChange, sesionVm::onClaveChange, sesionVm::iniciar)
        }
        else -> ContenidoPrincipal(sesion, sesionVm::cerrar)
    }
}

@Composable
private fun ContenidoPrincipal(sesion: Sesion, onCerrarSesion: () -> Unit) {

    var pantallaActual by remember {
        mutableStateOf<Screen>(Screen.Inicio)
    }

    var darkTheme by remember {
        mutableStateOf(false)
    }

    val drawerState = rememberDrawerState(
        initialValue = DrawerValue.Closed
    )

    val scope = rememberCoroutineScope()

    PharmaMobilTheme(
        darkTheme = darkTheme
    ) {

        ModalNavigationDrawer(

            drawerState = drawerState,

            drawerContent = {

                ModalDrawerSheet {

                    DrawerHeader()

                    NavigationDrawerItem(
                        label = {
                            Text("Inicio")
                        },
                        selected = pantallaActual is Screen.Inicio,
                        onClick = {

                            pantallaActual = Screen.Inicio

                            scope.launch {
                                drawerState.close()
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Inicio"
                            )
                        }
                    )

                    NavigationDrawerItem(
                        label = {
                            Text("Productos")
                        },
                        selected = pantallaActual is Screen.Productos,
                        onClick = {

                            pantallaActual = Screen.Productos

                            scope.launch {
                                drawerState.close()
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Medication,
                                contentDescription = "Productos"
                            )
                        }
                    )

                    if (sesion.puede("CLIENTES_LEER")) {
                        NavigationDrawerItem(
                            label = {
                                Text("Clientes")
                            },
                            selected = pantallaActual is Screen.Clientes,
                            onClick = {

                                pantallaActual = Screen.Clientes

                                scope.launch {
                                    drawerState.close()
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Clientes"
                                )
                            }
                        )
                    }

                    NavigationDrawerItem(
                        label = {
                            Text("Pedidos")
                        },
                        selected = pantallaActual is Screen.Pedidos,
                        onClick = {

                            pantallaActual = Screen.Pedidos

                            scope.launch {
                                drawerState.close()
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Pedidos"
                            )
                        }
                    )

                    NavigationDrawerItem(
                        label = {
                            Text("Acerca de")
                        },
                        selected = pantallaActual is Screen.AcercaDe,
                        onClick = {
                            pantallaActual = Screen.AcercaDe
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Acerca de"
                            )
                        }
                    )

                    NavigationDrawerItem(
                        label = { Text("Cerrar sesión (${sesion.usuario})") },
                        selected = false,
                        onClick = { scope.launch { drawerState.close() }; onCerrarSesion() },
                        icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar sesión") }
                    )

                    Spacer(
                        modifier = Modifier.padding(8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 16.dp,
                                vertical = 12.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        Text(
                            text = "Modo oscuro"
                        )

                        Switch(
                            checked = darkTheme,
                            onCheckedChange = {
                                darkTheme = it
                            }
                        )
                    }
                }
            }
        ) {

            Scaffold(

                topBar = {

                    TopAppBar(

                        title = {
                            Text(
                                text = tituloPantalla(
                                    pantallaActual
                                )
                            )
                        },

                        navigationIcon = {

                            IconButton(
                                onClick = {

                                    scope.launch {

                                        drawerState.open()
                                    }
                                }
                            ) {

                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Abrir menú"
                                )
                            }
                        }
                    )
                }

            ) { paddingValues ->

                AnimatedContent(
                    targetState = pantallaActual,
                    transitionSpec = {
                        (slideInHorizontally(animationSpec = tween(350)) { it } + fadeIn(tween(350)))
                            .togetherWith(
                                slideOutHorizontally(animationSpec = tween(350)) { -it } + fadeOut(tween(350))
                            )
                    },
                    label = "screenTransition"
                ) { screen ->

                    when (screen) {

                        Screen.Inicio -> {

                            InicioScreen()
                        }

                        Screen.Productos -> {

                            Column(
                                modifier = Modifier
                                    .padding(paddingValues)
                            ) {

                                val productoViewModel: ProductoViewModel = koinViewModel(key = sesion.usuario)

                                ProductoScreen(
                                    viewModel = productoViewModel,
                                    onVerDetalle = { id ->
                                        pantallaActual = Screen.DetalleProducto(id)
                                    },
                                    puedeEscribir = sesion.puede("CATALOGO_ESCRIBIR")
                                )
                            }
                        }

                        is Screen.DetalleProducto -> {

                            Column(
                                modifier = Modifier
                                    .padding(paddingValues)
                            ) {

                                val detalleViewModel: DetalleProductoViewModel = koinViewModel(key = sesion.usuario)

                                DetalleProductoScreen(
                                    productoId = screen.productoId,
                                    viewModel = detalleViewModel,
                                    onVolver = { pantallaActual = Screen.Productos }
                                )
                            }
                        }

                        Screen.Clientes -> {

                            Column(
                                modifier = Modifier
                                    .padding(paddingValues)
                            ) {

                                val clienteViewModel: ClienteViewModel = koinViewModel(key = sesion.usuario)

                                ClienteScreen(
                                    viewModel = clienteViewModel
                                )
                            }
                        }

                        Screen.Pedidos -> {

                            Column(
                                modifier = Modifier
                                    .padding(paddingValues)
                            ) {

                                Text("Pantalla de pedidos en construcción")
                            }
                        }

                        Screen.AcercaDe -> {

                            Column(
                                modifier = Modifier
                                    .padding(paddingValues)
                            ) {

                                AcercaDeScreen()
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun DrawerHeader() {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {

        Text(
            text = "PharmaMobil",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Gestión farmacéutica",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}


private fun tituloPantalla(
    screen: Screen
): String {

    return when (screen) {

        Screen.Inicio ->
            "Inicio"

        Screen.Productos ->
            "Productos"

        is Screen.DetalleProducto ->
            "Detalle del producto"

        Screen.Clientes ->
            "Clientes"

        Screen.Pedidos ->
            "Pedidos"

        Screen.AcercaDe ->
            "Acerca de"
    }
}