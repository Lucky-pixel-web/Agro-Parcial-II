package pe.edu.upeu.pharmamobil.presentation.producto

fun textoAntiguedad(ultima: Long?, ahora: Long): String {
    if (ultima == null) return "Nunca se ha sincronizado"

    val minutos = (ahora - ultima).coerceAtLeast(0L) / 60_000L
    val horas = minutos / 60
    val dias = horas / 24

    return when {
        minutos < 1 -> "Actualizado hace unos segundos"
        minutos < 60 -> "Actualizado hace ${plural(minutos, "minuto")}"
        horas < 24 -> "Actualizado hace ${plural(horas, "hora")}"
        else -> "Actualizado hace ${plural(dias, "día")}"
    }
}

private fun plural(n: Long, unidad: String) = if (n == 1L) "1 $unidad" else "$n ${unidad}s"