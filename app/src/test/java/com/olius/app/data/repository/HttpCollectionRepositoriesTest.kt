package com.olius.app.data.repository

import com.olius.app.data.remote.ApiClient
import com.olius.app.data.remote.ApiConfig
import com.olius.app.data.remote.ApiException
import com.olius.app.domain.entity.BecomeCollectorRequest
import com.olius.app.domain.entity.CollectionPointType
import com.olius.app.domain.entity.CollectionStatus
import com.olius.app.domain.entity.GeoPoint
import com.olius.app.domain.entity.MapBounds
import com.olius.app.domain.entity.PevAlreadyExistsException
import com.olius.app.domain.repository.AddressGeocoder
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.mockk
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.Collections

/** Histórico, mapa (GET /pevs) e "Vire Coletor" nas rotas reais da Core, com servidor local. */
class HttpCollectionRepositoriesTest {

    private lateinit var server: HttpServer
    private val requests: MutableList<String> = Collections.synchronizedList(mutableListOf())
    private var postedBody: String? = null

    /** Resposta por caminho + query (sem page/size). */
    private val responses = mutableMapOf<String, (page: Int) -> String>()

    /** Status HTTP forçado por "MÉTODO caminho" (padrão: 201 no POST, 200 no resto). */
    private val statuses = mutableMapOf<String, Int>()

    @Before
    fun setUp() {
        server = HttpServer.create(InetSocketAddress("localhost", 0), 0).apply {
            createContext("/") { exchange ->
                val uri = exchange.requestURI
                requests += "${exchange.requestMethod} ${uri.path}?${uri.rawQuery.orEmpty()}"
                if (exchange.requestMethod == "POST") postedBody = exchange.requestBody.bufferedReader().readText()
                val params = uri.rawQuery.orEmpty().split("&").filter { it.isNotEmpty() }
                    .associate { it.substringBefore("=") to it.substringAfter("=") }
                val key = uri.path + params.filterKeys { it != "page" && it != "size" }
                    .entries.joinToString("&", prefix = "?") { "${it.key}=${it.value}" }
                val handler = responses[key] ?: responses[uri.path]
                val forced = statuses["${exchange.requestMethod} ${uri.path}"]
                val (status, body) = if (handler == null) 404 to "{}" else
                    (forced ?: if (exchange.requestMethod == "POST") 201 else 200) to handler(params["page"]?.toInt() ?: 0)
                val bytes = body.toByteArray()
                exchange.sendResponseHeaders(status, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            start()
        }
    }

    @After
    fun tearDown() = server.stop(0)

    private val client get() = ApiClient("http://localhost:${server.address.port}/")
    private val clock = Clock.fixed(Instant.parse("2026-10-31T12:00:00Z"), ZoneId.of("America/Sao_Paulo"))

    private fun page(items: List<String>, total: Int = items.size) =
        """{"items": [${items.joinToString(",")}], "page": 0, "size": 100, "totalElements": $total}"""

    private fun collection(id: String, date: String, liters: Double) =
        """{"id": "$id", "requestId": "r-$id", "collectedVolumeLiters": $liters, "collectionDate": "$date", "recordStatus": "RECORDED"}"""

    private fun request(id: String, date: String, liters: Double, collectionId: String? = null) =
        """{"id": "$id", "status": "APPROVED", "estimatedVolumeLiters": $liters, "requestAt": "$date",
           "collectionId": ${collectionId?.let { "\"$it\"" } ?: "null"}}"""

    @Test
    fun `historico B2B junta coletas e pedidos pendentes ordenados pela data`() = runTest {
        responses["/api/v1/collections?recordStatus=RECORDED"] = {
            page(listOf(collection("c-1", "2026-10-10T13:00:00Z", 40.5)))
        }
        responses["/api/v1/collection-requests?status=PENDING"] = {
            page(listOf(request("p-1", "2026-10-20T13:00:00Z", 12.0)))
        }
        responses["/api/v1/collection-requests?status=APPROVED"] = {
            // a-2 já virou coleta: não pode aparecer como Pendente.
            page(listOf(request("a-1", "2026-10-01T13:00:00Z", 8.0), request("a-2", "2026-09-01T13:00:00Z", 9.0, "c-1")))
        }
        responses["/api/v1/establishments/me/score"] = {
            """{"points": 120, "successfulCollectionsCount": 3, "collectedVolumeLiters": 140.25}"""
        }

        val history = HttpHistoryRepository(client, clock).getEstablishmentHistory().getOrThrow()

        assertEquals(listOf("p-1", "c-1", "a-1"), history.items.map { it.id })
        assertEquals(
            listOf(CollectionStatus.PENDING, CollectionStatus.COLLECTED, CollectionStatus.PENDING),
            history.items.map { it.status }
        )
        assertEquals("10/10/2026", history.items[1].dateLabel)
        assertEquals(40.5, history.items[1].liters, 0.0)
        assertEquals(140.25, history.stats.oilCollectedLiters, 0.0)
        assertEquals("Outubro de 2026", history.monthLabel)
        assertTrue(requests.all { it.startsWith("GET ") })
        assertTrue(requests.any { it.contains("size=100") })
    }

    @Test
    fun `historico B2B le todas as paginas ate totalElements`() = runTest {
        responses["/api/v1/collections?recordStatus=RECORDED"] = { pageIndex ->
            page(listOf(collection("c-$pageIndex", "2026-10-1${pageIndex}T13:00:00Z", 1.0)), total = 2)
        }
        responses["/api/v1/collection-requests?status=PENDING"] = { page(emptyList()) }
        responses["/api/v1/collection-requests?status=APPROVED"] = { page(emptyList()) }
        responses["/api/v1/establishments/me/score"] = { """{"collectedVolumeLiters": 2}""" }

        val history = HttpHistoryRepository(client, clock).getEstablishmentHistory().getOrThrow()

        assertEquals(listOf("c-1", "c-0"), history.items.map { it.id })
        assertEquals(2, requests.count { it.contains("/api/v1/collections?") })
    }

    @Test
    fun `historico B2B falha se uma das rotas falhar`() = runTest {
        responses["/api/v1/collections?recordStatus=RECORDED"] = { page(emptyList()) }
        responses["/api/v1/establishments/me/score"] = { "{}" }
        // collection-requests sem resposta → 404

        assertTrue(HttpHistoryRepository(client, clock).getEstablishmentHistory().isFailure)
    }

    @Test
    fun `vire coletor manda so o endereco para POST pevs, sem horario`() = runTest {
        responses["/api/v1/pevs"] = { """{"id": "pev-1", "status": "PENDING"}""" }
        val repository = HttpCollectionPointRepository(client)

        val result = repository.becomeCollector(
            BecomeCollectorRequest("01310100", "Av. Paulista", "1000", "Bela Vista", "São Paulo", "SP", null, "8h às 18h")
        )

        assertTrue(result.isSuccess)
        assertEquals("POST /api/v1/pevs?", requests.single())
        val json = JSONObject(postedBody!!)
        assertEquals(setOf("address"), json.keys().asSequence().toSet())
        val address = json.getJSONObject("address")
        assertEquals(setOf("state", "city", "neighborhood", "street", "number", "cep"), address.keys().asSequence().toSet())
        assertEquals("SP", address.getString("state"))
        assertEquals("01310100", address.getString("cep"))
    }

    @Test
    fun `vire coletor manda o complemento quando preenchido`() {
        val body = HttpCollectionPointRepository(client).becomeCollectorBody(
            BecomeCollectorRequest("01310100", "Rua A", "1", "Centro", "Osasco", "SP", "Fundos", "")
        )
        val address = JSONObject(body).getJSONObject("address")

        assertEquals("Fundos", address.getString("complement"))
        assertFalse(JSONObject(body).has("openingHours"))
    }

    // ---- Mapa: GET /pevs (público) ---------------------------------------------

    private fun pev(id: String, neighborhood: String, lat: Double?, lng: Double?) =
        """{"id": "$id", "address": {"id": "a-$id", "state": "SP", "city": "São Paulo", "neighborhood": "$neighborhood",
           "street": "Rua A", "number": "10", "cep": "01001000", "complement": "Casa 2",
           "latitude": ${lat ?: "null"}, "longitude": ${lng ?: "null"}}}"""

    private fun servePevs() {
        responses["/api/v1/pevs"] = { page ->
            if (page == 0) page(listOf(pev("p1", "Sé", -23.55, -46.63), pev("p2", "Pinheiros", null, null)), total = 3)
            else page(listOf(pev("p3", "São Miguel", -23.49, -46.44)), total = 3)
        }
    }

    @Test
    fun `mapa le todas as paginas de GET pevs e pula PEV sem coordenadas`() = runTest {
        servePevs()

        val points = HttpCollectionPointRepository(client).getNearbyPoints().getOrThrow()

        assertEquals(listOf("p1", "p3"), points.map { it.id })
        assertTrue(points.all { it.type == CollectionPointType.PEV })
        assertEquals("PEV Sé", points.first().name)
        assertEquals("Rua A, 10 - Sé, São Paulo/SP", points.first().address)
        // Complemento pode ser a casa de um cidadão: não aparece.
        assertFalse(points.first().address.contains("Casa 2"))
    }

    @Test
    fun `mapa filtra por area e busca sem acento no proprio app, com uma leitura so`() = runTest {
        servePevs()
        val repository = HttpCollectionPointRepository(client)

        val inArea = repository.getPointsInArea(MapBounds(-23.6, -46.7, -23.5, -46.6)).getOrThrow()
        val found = repository.searchPoints("sao miguel").getOrThrow()
        val details = repository.getPointDetails("p3").getOrThrow()

        assertEquals(listOf("p1"), inArea.map { it.id })
        assertEquals(listOf("p3"), found.map { it.id })
        assertEquals("PEV São Miguel", details.name)
        assertEquals(2, requests.count { it.startsWith("GET /api/v1/pevs?") })
        assertTrue(repository.searchPoints("sa").getOrThrow().isEmpty())
        assertTrue(repository.getPointDetails("nao-existe").isFailure)
    }

    // ---- Vire Coletor: coordenadas e PEV que já existe ---------------------------

    private val paulista =
        BecomeCollectorRequest("01310100", "Av. Paulista", "1000", "Bela Vista", "São Paulo", "SP", null, "8h às 18h")

    @Test
    fun `vire coletor manda latitude e longitude do geocoder`() = runTest {
        responses["/api/v1/pevs"] = { """{"id": "pev-1", "status": "PENDING"}""" }
        val repository = HttpCollectionPointRepository(client, geocoder = { GeoPoint(-23.56, -46.65) })

        assertTrue(repository.becomeCollector(paulista).isSuccess)

        val address = JSONObject(postedBody!!).getJSONObject("address")
        assertEquals(-23.56, address.getDouble("latitude"), 0.0)
        assertEquals(-46.65, address.getDouble("longitude"), 0.0)
    }

    @Test
    fun `vire coletor com PEV ja aprovado avisa em vez de criar outro`() = runTest {
        responses["/api/v1/pevs"] = { """{"code": "PEV_ALREADY_EXISTS"}""" }
        statuses["POST /api/v1/pevs"] = 409
        responses["/api/v1/pevs/me"] = { """{"id": "pev-1", "status": "APPROVED"}""" }

        val error = HttpCollectionPointRepository(client).becomeCollector(paulista).exceptionOrNull()

        assertTrue(error is PevAlreadyExistsException)
    }

    @Test
    fun `vire coletor com PEV recusado corrige o endereco e reapresenta`() = runTest {
        // PATCH não roda no HttpURLConnection da JVM: aqui o cliente é falso.
        val api = mockk<ApiClient>()
        coEvery { api.post(ApiConfig.Main.PEVS, any(), any()) } throws ApiException(409, "{}")
        coEvery { api.get(ApiConfig.Main.PEVS_ME, any()) } returns """{"id": "pev-1", "status": "REJECTED"}"""
        coEvery { api.patch(ApiConfig.Main.PEVS_ME_ADDRESS, any()) } returns "{}"
        coEvery { api.post(ApiConfig.Main.PEVS_ME_RESUBMISSIONS, "", any()) } returns "{}"

        assertTrue(HttpCollectionPointRepository(api, AddressGeocoder.NONE).becomeCollector(paulista).isSuccess)

        coVerifyOrder {
            api.patch(ApiConfig.Main.PEVS_ME_ADDRESS, any())
            api.post(ApiConfig.Main.PEVS_ME_RESUBMISSIONS, "", any())
        }
    }

    // ---- Histórico B2C: GET /users/me/points -------------------------------------

    private fun movement(id: String, date: String, liters: Double?, status: String = "RECORDED") =
        """{"deliveryId": "$id", "points": 3, "deliveryDate": "$date", "revision": 1, "recordStatus": "$status"
           ${liters?.let { ", \"oilVolumeLiters\": $it" } ?: ""}}"""

    @Test
    fun `historico B2C usa users me points e tira as entregas anuladas`() = runTest {
        responses["/api/v1/users/me/points"] = {
            """{"balance": 3, "movements": [${movement("d1", "2026-10-18T14:00:00Z", 3.7)},
               ${movement("d2", "2026-10-10T14:00:00Z", 2.0, status = "ANNULLED")}],
               "page": 0, "size": 100, "totalElements": 2}"""
        }

        val history = HttpHistoryRepository(client, clock).getCitizenHistory().getOrThrow()

        assertEquals(listOf("d1"), history.records.map { it.id })
        assertEquals("18/10/2026", history.records.single().dateLabel)
        assertEquals(3.7, history.stats.oilCollectedLiters, 0.0)
        assertEquals("Outubro de 2026", history.monthLabel)
    }

    @Test
    fun `historico B2C falha enquanto a Core nao manda os litros`() = runTest {
        responses["/api/v1/users/me/points"] = {
            """{"balance": 3, "movements": [${movement("d1", "2026-10-18T14:00:00Z", null)}], "page": 0, "size": 100, "totalElements": 1}"""
        }

        assertTrue(HttpHistoryRepository(client, clock).getCitizenHistory().isFailure)
    }
}
