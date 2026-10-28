
package pe.upeu.agrosierra

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import pe.upeu.agrosierra.data.local.AgroLocal
import pe.upeu.agrosierra.db.AgroDb
import kotlin.test.Test
import kotlin.test.assertEquals

class ScAFiltrosTest {

    @Test
    fun filtraValleYCampaniasAbiertas() = runTest {
        val driver = JdbcSqliteDriver(
            JdbcSqliteDriver.IN_MEMORY
        )

        AgroDb.Schema.create(driver)

        val db = AgroDb(driver)
        val local = AgroLocal(db)
        val q = db.agroQueries

        q.guardarLote(
            1L, "L001", "Lote Norte A",
            5.0, "Papa", "Norte", 0L, 2.0
        )

        q.guardarLote(
            2L, "L002", "Lote Norte B",
            4.0, "Maiz", "Norte", 1L, 2.0
        )

        q.guardarLote(
            3L, "L003", "Lote Sur A",
            6.0, "Arroz", "Sur", 0L, 2.0
        )

        val filtrados = local.buscarLotes(
            texto = "",
            valle = "NORTE",
            soloAbiertas = true
        ).first()

        assertEquals(
            listOf("L001"),
            filtrados.map { it.codigo }
        )

        val soloNorte = local.buscarLotes(
            texto = "",
            valle = "Norte",
            soloAbiertas = false
        ).first()

        assertEquals(2, soloNorte.size)

        val todos = local.buscarLotes(
            texto = "",
            valle = "",
            soloAbiertas = false
        ).first()

        assertEquals(3, todos.size)
    }
}
