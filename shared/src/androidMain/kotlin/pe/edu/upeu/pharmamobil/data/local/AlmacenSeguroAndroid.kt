package pe.edu.upeu.pharmamobil.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class AlmacenSeguroAndroid(contexto: Context) : AlmacenSeguro {
    private val llave = MasterKey.Builder(contexto)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    private val prefs = EncryptedSharedPreferences.create(
        contexto, "sesion_segura", llave,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    override fun guardarToken(token: String) = prefs.edit().putString(CLAVE, token).apply()
    override fun leerToken(): String? = prefs.getString(CLAVE, null)
    override fun borrarToken() = prefs.edit().remove(CLAVE).apply()
    override fun guardarPerfil(perfil: String) {
        prefs.edit().putString("perfil", perfil).apply()
    }
    override fun leerPerfil(): String? = prefs.getString("perfil", null)
    override fun borrarPerfil() { prefs.edit().remove("perfil").apply() }
    private companion object { const val CLAVE = "jwt" }
}