@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package pe.edu.upeu.pharmamobil.data.local

import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.Foundation.*
import platform.Security.*
class AlmacenSeguroIos : AlmacenSeguro {

    override fun guardarToken(token: String) {
        borrarToken()
        val datos = CFBridgingRetain(token.aNSData())
        SecItemAdd(consulta(kSecValueData to datos), null)
        CFRelease(datos)
    }

    override fun leerToken(): String? = memScoped {
        val resultado = alloc<CFTypeRefVar>()
        val estado = SecItemCopyMatching(
            consulta(kSecReturnData to kCFBooleanTrue, kSecMatchLimit to kSecMatchLimitOne),
            resultado.ptr
        )
        if (estado != errSecSuccess) return@memScoped null
        val datos = CFBridgingRelease(resultado.value) as? NSData ?: return@memScoped null
        NSString.create(data = datos, encoding = NSUTF8StringEncoding)?.toString()
    }

    override fun borrarToken() {
        SecItemDelete(consulta())
    }

    override fun guardarPerfil(perfil: String) {
        SecItemDelete(consultaPerfil())
        val datos = CFBridgingRetain(perfil.aNSData())
        SecItemAdd(consultaPerfil(kSecValueData to datos), null)
        CFRelease(datos)
    }

    override fun leerPerfil(): String? = memScoped {
        val resultado = alloc<CFTypeRefVar>()
        val estado = SecItemCopyMatching(
            consultaPerfil(kSecReturnData to kCFBooleanTrue, kSecMatchLimit to kSecMatchLimitOne),
            resultado.ptr
        )
        if (estado != errSecSuccess) return@memScoped null
        val datos = CFBridgingRelease(resultado.value) as? NSData ?: return@memScoped null
        NSString.create(data = datos, encoding = NSUTF8StringEncoding)?.toString()
    }

    override fun borrarPerfil() { SecItemDelete(consultaPerfil()) }

    private fun consultaPerfil(vararg extras: Pair<CFStringRef?, CFTypeRef?>): CFDictionaryRef? = memScoped {
        val pares = listOf<Pair<CFStringRef?, CFTypeRef?>>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to CFBridgingRetain("pharmamobil"),
            kSecAttrAccount to CFBridgingRetain("perfil")
        ) + extras
        val llaves = allocArrayOf(*pares.map { it.first }.toTypedArray())
        val valores = allocArrayOf(*pares.map { it.second }.toTypedArray())
        CFDictionaryCreate(kCFAllocatorDefault, llaves.reinterpret(), valores.reinterpret(),
            pares.size.convert(), null, null)
    }

    private fun consulta(vararg extras: Pair<CFStringRef?, CFTypeRef?>): CFDictionaryRef? = memScoped {
        val pares = listOf<Pair<CFStringRef?, CFTypeRef?>>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to CFBridgingRetain("pharmamobil"),
            kSecAttrAccount to CFBridgingRetain("jwt")
        ) + extras
        val llaves = allocArrayOf(*pares.map { it.first }.toTypedArray())
        val valores = allocArrayOf(*pares.map { it.second }.toTypedArray())
        CFDictionaryCreate(kCFAllocatorDefault, llaves.reinterpret(), valores.reinterpret(),
            pares.size.convert(), null, null)
    }

    private fun String.aNSData(): NSData {
        val bytes = encodeToByteArray()
        return bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
    }
}