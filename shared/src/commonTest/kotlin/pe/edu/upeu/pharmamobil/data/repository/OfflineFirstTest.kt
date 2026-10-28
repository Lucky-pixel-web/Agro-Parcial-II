package pe.edu.upeu.pharmamobil.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import pe.edu.upeu.pharmamobil.data.local.ProductoLocal
import pe.edu.upeu.pharmamobil.data.local.crearDriverEnMemoria
import pe.edu.upeu.pharmamobil.data.remote.ProductoApi
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.AlmacenSincronizacionEnMemoria
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Servidor falso: responde como PharmaSoft, o falla como si no hubiera red. */
private class ServidorSimulado {
    var disponible = true
    val productos = mutableListOf<Producto>()
    private var siguienteId = 1L

    fun agregar(nombre: String, precio: Double = 5.0, stock: Int = 10): Producto =
        Producto(id = siguienteId++, nombre = nombre, precio = precio, stock = stock)
            .also { productos.add(it) }

    private fun Producto.aJson() =
        """{"id":$id,"nombre":"$nombre","precio":$precio,"stock":$stock,"estado":$activo,""" +
                """"categoriaId":21,"categoriaNombre":"Ropa","fechaCreacion":"2026-10-20T10:00:00","fechaModificacion":null}"""

    val motor = MockEngine { peticion ->
        if (!disponible) throw IllegalStateException("Sin red")
        val cabeceras = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        when (peticion.method) {
            HttpMethod.Get -> respond(
                content = """{"contenido":[${productos.joinToString(",") { it.aJson() }}],""" +
                        """"pagina":0,"tamanio":20,"totalElementos":${productos.size},"totalPaginas":1,"ultima":true}""",
                status = HttpStatusCode.OK,
                headers = cabeceras
            )

            HttpMethod.Post -> {
                val cuerpo = Json.parseToJsonElement((peticion.body as TextContent).text).jsonObject
                val creado = agregar(
                    nombre = cuerpo.getValue("nombre").jsonPrimitive.content,
                    precio = cuerpo.getValue("precio").jsonPrimitive.double,
                    stock = cuerpo.getValue("stock").jsonPrimitive.int
                )
                respond(creado.aJson(), HttpStatusCode.Created, cabeceras)
            }

            else -> respond("", HttpStatusCode.NotFound)
        }
    }
}

/** Cada prueba crea su propio escenario: base nueva en memoria y servidor limpio. */
private class Escenario {
    val servidor = ServidorSimulado()
    val base = PharmaDb(crearDriverEnMemoria())
    val local = ProductoLocal(base)
    val almacen = AlmacenSincronizacionEnMemoria()
    var reloj = 1_000L

    private val cliente = HttpClient(servidor.motor) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true })
        }
        defaultRequest {
            url("http://servidor.test/api/v1/")
            contentType(ContentType.Application.Json)
        }
    }

    val repositorio = ProductoRepositorioOffline(
        local = local,
        remoto = ProductoRepositorioRest(ProductoApi(cliente), 21L),
        almacen = almacen,
        ahora = { reloj }
    )

    val aspirina = Producto(id = 0L, nombre = "Aspirina", precio = 5.0, stock = 10)

    fun filas() = base.productoQueries.listarTodos().executeAsList()
    fun filasPendientes() = base.productoQueries.listarPendientes().executeAsList()
}

class OfflineFirstTest {

    @Test
    fun prueba1_sincronizarConRedTraeLosProductosDelServidor() = runTest {
        val e = Escenario()
        e.servidor.agregar("Paracetamol")
        e.servidor.agregar("Ibuprofeno")

        e.repositorio.sincronizar()

        assertEquals(2, e.filas().size)
        assertEquals(
            listOf("Ibuprofeno", "Paracetamol"),
            e.repositorio.observar().first().map { it.nombre }
        )
    }

    @Test
    fun prueba2_registroSinRedQuedaPendienteYElFlowLoEmite() = runTest {
        val e = Escenario()
        e.servidor.disponible = false

        e.repositorio.registrar(e.aspirina)

        val pendientes = e.filasPendientes()
        assertEquals(1, pendientes.size)
        assertEquals(1L, pendientes.single().pendiente)
        assertEquals(listOf("Aspirina"), e.repositorio.observar().first().map { it.nombre })
        assertEquals(0, e.servidor.productos.size)
    }

    @Test
    fun prueba3_alReconectarSeEnvíaLoPendienteYLaFilaDejaDeEstarMarcada() = runTest {
        val e = Escenario()
        e.servidor.disponible = false
        e.repositorio.registrar(e.aspirina)

        e.servidor.disponible = true
        e.repositorio.sincronizar()

        assertEquals(listOf("Aspirina"), e.servidor.productos.map { it.nombre })
        assertTrue(e.filasPendientes().isEmpty())
        val lista = e.repositorio.observar().first()
        assertEquals(1, lista.size)
        assertTrue(lista.single().id > 0)
    }

    @Test
    fun prueba4_laBajadaNoBorraLoPendiente() = runTest {
        val e = Escenario()
        e.local.guardarPendiente(e.aspirina)

        // Lo que hace la bajada: reemplazar solo lo sincronizado por la lista del servidor.
        e.local.reemplazarSincronizados(
            listOf(Producto(id = 1L, nombre = "Paracetamol", precio = 8.5, stock = 100))
        )

        assertEquals(listOf("Aspirina"), e.local.pendientes().map { it.nombre })
        assertEquals(
            setOf("Aspirina", "Paracetamol"),
            e.repositorio.observar().first().map { it.nombre }.toSet()
        )
    }

    @Test
    fun prueba5_idsLocalesYDelServidorConviviendoSinSobrescribirse() = runTest {
        val e = Escenario()
        val a = e.local.guardarPendiente(e.aspirina)
        val b = e.local.guardarPendiente(e.aspirina.copy(nombre = "Ibuprofeno"))

        e.local.reemplazarSincronizados(
            listOf(
                Producto(id = 1L, nombre = "Paracetamol", precio = 8.5, stock = 100),
                Producto(id = 2L, nombre = "Amoxicilina", precio = 18.5, stock = 20)
            )
        )

        assertTrue(a.id < 0 && b.id < 0 && a.id != b.id)
        val ids = e.repositorio.observar().first().map { it.id }
        assertEquals(4, ids.toSet().size)
    }

    @Test
    fun prueba6_sincronizarDosVecesNoDuplica() = runTest {
        val e = Escenario()
        e.servidor.agregar("Paracetamol")
        e.servidor.agregar("Ibuprofeno")
        e.servidor.disponible = false
        e.repositorio.registrar(e.aspirina)
        e.servidor.disponible = true

        e.repositorio.sincronizar()
        val primera = e.filas().size
        e.repositorio.sincronizar()

        assertEquals(3, primera)
        assertEquals(primera, e.filas().size)
        assertEquals(3, e.servidor.productos.size)
    }

    @Test
    fun extra_laMarcaDeTiempoSoloSeGuardaSiLaSincronizacionSalioBien() = runTest {
        val e = Escenario()
        assertNull(e.almacen.ultima.value)

        e.servidor.disponible = false
        assertFailsWith<ErrorApiException> { e.repositorio.sincronizar() }
        assertNull(e.almacen.ultima.value)

        e.servidor.disponible = true
        e.reloj = 9_000L
        e.repositorio.sincronizar()
        assertEquals(9_000L, e.almacen.ultima.value)
    }
}