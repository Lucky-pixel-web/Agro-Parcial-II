
package pe.upeu.agrosierra.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import pe.upeu.agrosierra.domain.model.*

class AgroApi(private val cliente: HttpClient) {

    suspend fun listarLotes(
        pagina: Int = 0,
        tamanio: Int = 20,
        valle: String? = null,
        campaniaCerrada: Boolean? = null
    ): PaginaAgroDto<LoteDto> =
        cliente.get("lotes") {
            parameter("pagina", pagina)
            parameter("tamanio", tamanio)
            valle?.let { parameter("valle", it) }
            campaniaCerrada?.let {
                parameter("campaniaCerrada", it)
            }
        }.body()

    suspend fun obtenerLote(id: Long): Lote =
        cliente.get("lotes/$id")
            .body<LoteDto>().aDominio()

    suspend fun listarLabores(
        loteId: Long,
        pagina: Int = 0,
        tamanio: Int = 20
    ): PaginaAgroDto<LaborDto> =
        cliente.get("lotes/$loteId/labores") {
            parameter("pagina", pagina)
            parameter("tamanio", tamanio)
        }.body()

    suspend fun crearLabor(labor: Labor): Labor =
        cliente.post("labores") {
            setBody(labor.aSolicitud())
        }.body<LaborDto>().aDominio()

    suspend fun actualizarLabor(labor: Labor): Labor =
        cliente.put("labores/${labor.id}") {
            setBody(labor.aSolicitud())
        }.body<LaborDto>().aDominio()

    suspend fun eliminarLabor(id: Long) {
        cliente.delete("labores/$id")
    }

    suspend fun cerrarCampania(id: Long): Lote =
        cliente.post("lotes/$id/cerrar-campania")
            .body<LoteDto>().aDominio()

    suspend fun descargarTodosLosLotes(): List<Lote> {
        val resultado = mutableListOf<Lote>()
        var pagina = 0

        do {
            val respuesta = listarLotes(pagina, 100)
            resultado += respuesta.contenido.map {
                it.aDominio()
            }
            pagina++
        } while (!respuesta.ultima)

        return resultado
    }

    suspend fun descargarTodasLasLabores(
        loteId: Long
    ): List<Labor> {
        val resultado = mutableListOf<Labor>()
        var pagina = 0

        do {
            val respuesta = listarLabores(loteId, pagina, 100)
            resultado += respuesta.contenido.map {
                it.aDominio()
            }
            pagina++
        } while (!respuesta.ultima)

        return resultado
    }
}
