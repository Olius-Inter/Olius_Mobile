package com.olius.app.data.repository

import com.olius.app.data.remote.ApiClient
import com.olius.app.domain.entity.PickupRequest
import com.olius.app.domain.entity.ProfileType
import com.olius.app.domain.entity.UserProfile
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
import java.time.ZoneOffset

/** Home e "Solicitar coleta" no contrato real da Core, com servidor local. */
class HttpHomeRepositoryTest {
    private lateinit var server: HttpServer
    private var path: String? = null
    private var idempotencyKey: String? = null
    private var body: String? = null

    private val responses = mutableMapOf<String, String>()

    @Before
    fun setUp() {
        server = HttpServer.create(InetSocketAddress("localhost", 0), 0).apply {
            createContext("/") { exchange ->
                path = exchange.requestURI.path
                idempotencyKey = exchange.requestHeaders.getFirst("Idempotency-Key")
                body = exchange.requestBody.bufferedReader().readText()
                val query = exchange.requestURI.query.orEmpty()
                val key = responses.keys.firstOrNull { "${exchange.requestURI.path}?$query".contains(it) }
                val bytes = (key?.let(responses::get) ?: """{"id":"r-1","status":"PENDING"}""").toByteArray()
                exchange.sendResponseHeaders(if (key != null) 200 else 201, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            start()
        }
    }

    @After
    fun tearDown() = server.stop(0)

    private var profile: UserProfile? = null

    private val repository
        get() = HttpHomeRepository(
            ApiClient("http://localhost:${server.address.port}/"),
            profileProvider = { profile },
            clock = Clock.system(ZoneOffset.UTC)
        )

    private val key = "3f2b9c1e-8a4d-4f6b-9c2e-1d7a5b3c9e0f"

    @Test
    fun `cidadao sem entregas ve o proprio nome e zero litros`() = runTest {
        profile = UserProfile("u-1", "Carla Mendes", "carla@ex.com", ProfileType.CITIZEN)
        responses["/api/v1/users/me/deliveries"] = """{"items":[],"totalElements":0}"""

        val dashboard = repository.getDashboard().getOrThrow()

        assertEquals("Carla", dashboard.firstName)
        assertEquals("Carla Mendes", dashboard.fullName)
        assertEquals(0.0, dashboard.oilCollectedLiters, 0.0)
        assertFalse(dashboard.pickupReady)
        assertTrue(dashboard.notifications.isEmpty())
    }

    @Test
    fun `cidadao soma so os litros das entregas vigentes`() = runTest {
        profile = UserProfile("u-1", "Carla Mendes", "carla@ex.com", ProfileType.CITIZEN)
        responses["/api/v1/users/me/deliveries"] =
            """{"items":[{"id":"d1","oilVolumeLiters":1.5,"recordStatus":"RECORDED"},
               {"id":"d2","oilVolumeLiters":4,"recordStatus":"ANNULLED"}],"totalElements":2}"""

        assertEquals(1.5, repository.getDashboard().getOrThrow().oilCollectedLiters, 0.0)
    }

    @Test
    fun `estabelecimento le os litros do score e os pedidos sem coleta`() = runTest {
        profile = UserProfile("u-2", "Bar do Zé", "bar@ex.com", ProfileType.ESTABLISHMENT)
        responses["/api/v1/establishments/me/score"] = """{"points":10,"collectedVolumeLiters":42.5}"""
        responses["status=PENDING"] =
            """{"items":[{"id":"p-1","estimatedVolumeLiters":20,"requestAt":"2026-10-05T12:00:00Z","collectionId":null}],"totalElements":1}"""
        responses["status=APPROVED"] =
            """{"items":[{"id":"p-2","estimatedVolumeLiters":30,"requestAt":"2026-10-06T12:00:00Z","collectionId":"c-1"}],"totalElements":1}"""

        val dashboard = repository.getDashboard().getOrThrow()

        assertEquals("Bar", dashboard.firstName)
        assertEquals(42.5, dashboard.oilCollectedLiters, 0.0)
        assertEquals(listOf("p-1"), dashboard.pendingRequests.map { it.id })
        assertEquals("05/10/2026", dashboard.pendingRequests.single().dateLabel)
    }

    @Test
    fun `sem perfil carregado o dashboard falha em vez de inventar dados`() = runTest {
        profile = null

        assertTrue(repository.getDashboard().isFailure)
    }

    @Test
    fun `solicitar coleta manda a chave no header e so volume e observacao no corpo`() = runTest {
        val result = repository.requestPickup(PickupRequest(64.0, "23/12/2026", "Portão lateral", key))

        assertTrue(result.isSuccess)
        assertEquals("/api/v1/collection-requests", path)
        assertEquals(key, idempotencyKey)
        val json = JSONObject(body!!)
        assertEquals(setOf("estimatedVolumeLiters", "observation"), json.keys().asSequence().toSet())
        assertEquals(64.0, json.getDouble("estimatedVolumeLiters"), 0.0)
        assertEquals("Portão lateral", json.getString("observation"))
    }

    @Test
    fun `sem observacao o campo nao vai e o volume fica com 2 casas`() {
        val json = JSONObject(repository.pickupRequestBody(PickupRequest(10.256, "", null, key)))

        assertFalse(json.has("observation"))
        assertEquals(10.26, json.getDouble("estimatedVolumeLiters"), 0.0)
    }
}
