package pe.edu.upeu.pharmamobil.data.local

interface AlmacenSeguro {
    fun guardarToken(token: String)
    fun leerToken(): String?
    fun borrarToken()
    fun guardarPerfil(perfil: String)
    fun leerPerfil(): String?
    fun borrarPerfil()
}