package pe.edu.upeu.pharmamobil.presentation.sesion

sealed interface EventoSesion {
    data class Ingreso(val nombre: String) : EventoSesion
    data class Rechazado(val motivo: String) : EventoSesion
    data object Cierre : EventoSesion
}