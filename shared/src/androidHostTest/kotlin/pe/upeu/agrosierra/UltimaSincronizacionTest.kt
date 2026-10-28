package pe.upeu.agrosierra

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import pe.upeu.agrosierra.data.local.AgroLocal
import pe.upeu.agrosierra.db.AgroDb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UltimaSincronizacionTest {
    @Test
    fun fechaPersistenteSoloCambiaAlRegistrarla() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            AgroDb.Schema.create(driver)
            val local = AgroLocal(AgroDb(driver))
            assertNull(local.observarUltimaSincronizacion().first())
            local.guardarUltimaSincronizacion(1_782_000_000_000L)
            assertEquals(
                1_782_000_000_000L,
                local.observarUltimaSincronizacion().first()
            )
        } finally {
            driver.close()
        }
    }
}
