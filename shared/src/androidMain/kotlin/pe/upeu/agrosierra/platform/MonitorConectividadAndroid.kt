package pe.upeu.agrosierra.platform

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import pe.upeu.agrosierra.domain.platform.MonitorConectividad

class MonitorConectividadAndroid(context: Context) : MonitorConectividad {
    private val conectividad =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)
            as ConnectivityManager

    override fun observar(): Flow<Boolean> = callbackFlow {
        fun tieneConexion(capacidades: NetworkCapabilities?): Boolean =
            capacidades?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        // Emitir el estado inicial, tambien cuando la app comienza sin Internet.
        val redActual = conectividad.activeNetwork
        trySend(tieneConexion(conectividad.getNetworkCapabilities(redActual)))

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                trySend(tieneConexion(networkCapabilities))
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }

        conectividad.registerDefaultNetworkCallback(callback)
        awaitClose {
            conectividad.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()
}
