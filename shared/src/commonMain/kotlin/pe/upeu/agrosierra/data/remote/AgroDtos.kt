
package pe.upeu.agrosierra.data.remote

import kotlinx.serialization.Serializable
import pe.upeu.agrosierra.domain.model.*

@Serializable
data class PaginaAgroDto<T>(
    val contenido: List<T>,
    val pagina: Int,
    val tamanio: Int,
    val totalElementos: Long,
    val totalPaginas: Int,
    val ultima: Boolean
)

@Serializable
data class LoteDto(
    val id: Long,
    val codigo: String,
    val nombre: String,
    val hectareas: Double,
    val cultivo: String,
    val valle: String,
    val campaniaCerrada: Boolean,
    val dosisMaximaPorHectarea: Double
) {
    fun aDominio() = Lote(
        id, codigo, nombre, hectareas,
        cultivo, valle, campaniaCerrada,
        dosisMaximaPorHectarea
    )
}

@Serializable
data class LaborDto(
    val id: Long,
    val loteId: Long,
    val tipo: String,
    val fecha: String,
    val responsable: String,
    val cantidad: Double,
    val observacion: String,
    val pendiente: Boolean = false
) {
    fun aDominio() = Labor(
        id = id,
        loteId = loteId,
        tipo = TipoLabor.valueOf(tipo),
        fecha = fecha,
        responsable = responsable,
        cantidad = cantidad,
        observacion = observacion,
        pendiente = false
    )
}

@Serializable
data class LaborRequestDto(
    val loteId: Long,
    val tipo: String,
    val fecha: String,
    val cantidad: Double,
    val observacion: String
)

fun Labor.aSolicitud() = LaborRequestDto(
    loteId = loteId,
    tipo = tipo.name,
    fecha = fecha,
    cantidad = cantidad,
    observacion = observacion
)
