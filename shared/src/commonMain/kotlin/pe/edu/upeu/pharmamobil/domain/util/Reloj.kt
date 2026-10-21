package pe.edu.upeu.pharmamobil.domain.util

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun ahoraEnMilisegundos(): Long = Clock.System.now().toEpochMilliseconds()