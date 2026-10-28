package pe.upeu.agrosierra.presentation

import pe.upeu.agrosierra.domain.model.Lote
import pe.upeu.agrosierra.domain.model.TipoLabor

/** Validación inmediata, útil también sin conexión. El servidor vuelve a validar. */
fun validarLaborFormulario(
    lote: Lote,
    tipo: TipoLabor,
    fecha: String,
    cantidadTexto: String,
    jornalesTexto: String,
    observacion: String
): Map<String, String> = buildMap {
    if (!Regex("""\d{4}-\d{2}-\d{2}""").matches(fecha.trim())) {
        put("fecha", "Escribe la fecha con formato AAAA-MM-DD.")
    }

    val cantidad = cantidadTexto.toDoubleOrNull()
    if (cantidad == null || !cantidad.isFinite() || cantidad <= 0.0) {
        put("cantidad", "La cantidad debe ser mayor que cero.")
    } else if (
        tipo == TipoLabor.FUMIGACION &&
        cantidad > lote.hectareas * lote.dosisMaximaPorHectarea
    ) {
        put("cantidad", "La dosis supera el máximo permitido para este lote.")
    }

    val jornales = jornalesTexto.toIntOrNull()
    if (jornales == null || jornales < 1) {
        put("cantidadJornales", "Los jornales deben ser un entero mayor que cero.")
    }

    if (observacion.trim().length !in 1..500) {
        put("observacion", "La observación debe tener entre 1 y 500 caracteres.")
    }
}
