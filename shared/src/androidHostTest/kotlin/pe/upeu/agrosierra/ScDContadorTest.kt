
package pe.upeu.agrosierra

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import pe.upeu.agrosierra.data.local.AgroLocal
import pe.upeu.agrosierra.db.AgroDb
import pe.upeu.agrosierra.domain.model.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ScDContadorTest {

    @Test
    fun contadorSeActualizaAlRegistrarYSincronizar() = runTest {

        val driver = JdbcSqliteDriver(
            JdbcSqliteDriver.IN_MEMORY
        )
        AgroDb.Schema.create(driver)

        val db = AgroDb(driver)
        val local = AgroLocal(db)

        val lote = Lote(
            id = 1L,
            codigo = "L001",
            nombre = "Lote de prueba",
            hectareas = 5.0,
            cultivo = "Papa",
            valle = "Norte",
            campaniaCerrada = false,
            dosisMaximaPorHectarea = 2.0
        )

        local.actualizarDesdeServidor(
            listOf(lote),
            emptyList()
        )

        val contador = local.observarPendientesPorLotes()

        assertEquals(0L, contador.first()[1L])

        val labor = Labor(
            id = 0L,
            loteId = 1L,
            tipo = TipoLabor.RIEGO,
            fecha = "2026-10-28",
            responsable = "tecnico1",
            cantidad = 2.0,
            observacion = "Prueba SC-D",
            pendiente = true,
            cantidadJornales = 2
        )

        val primera = local.guardarPendiente(labor)
        assertEquals(1L, contador.first()[1L])

        val segunda = local.guardarPendiente(labor)
        assertEquals(2L, contador.first()[1L])

        // Simulamos la confirmación local
        // después de una respuesta exitosa del servidor.
        local.confirmar(
            primera,
            primera.copy(id = 101L, pendiente = false)
        )

        assertEquals(1L, contador.first()[1L])

        local.confirmar(
            segunda,
            segunda.copy(id = 102L, pendiente = false)
        )

        assertEquals(0L, contador.first()[1L])
    }
}
