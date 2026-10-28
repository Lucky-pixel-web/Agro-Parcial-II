                if (
                    numero != null &&
                    numeroJornales != null &&
                    numero > 0 &&
                    numeroJornales > 0 &&
                    fecha.isNotBlank() &&
                    observacion.isNotBlank()
                ) {
                erroresLocales = validarLaborFormulario(
                    lote, tipo, fecha, cantidad, jornales, observacion
                )
                if (erroresLocales.isEmpty()) {
                    val labor = Labor(
                        id = edicion?.id ?: -1L,
                        loteId = lote.id,
                        tipo = tipo,
                        fecha = fecha,
                        responsable =
                            edicion?.responsable ?: sesion.usuario,
                        cantidad = numero,
                        cantidadJornales = numeroJornales,
                        observacion = observacion,
                        pendiente = true
                    )

                    if (edicion == null) {
                        vm.registrar(labor)
                    } else {
                        vm.corregir(labor)
                    }

                    edicion = null
                    fecha = ""
                    cantidad = ""
                    observacion = ""
                    jornales = "1"
                    erroresLocales = emptyMap()
                }
            }) {
                Text(
                    if (edicion == null)
                        "Guardar labor"
                    else
                        "Guardar corrección"
                )
            }

            if (edicion != null) {
                TextButton(onClick = {
                    edicion = null
                }) {
                    Text("Cancelar edición")
                }
            }
        }
    }
}
