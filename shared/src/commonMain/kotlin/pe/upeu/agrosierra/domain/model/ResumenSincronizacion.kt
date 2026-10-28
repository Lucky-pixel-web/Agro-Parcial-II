package pe.upeu.agrosierra.domain.model

data class ResumenSincronizacion(
    val enviados: Int,
    val lotesRecibidos: Int,
    val laboresRecibidas: Int
)
