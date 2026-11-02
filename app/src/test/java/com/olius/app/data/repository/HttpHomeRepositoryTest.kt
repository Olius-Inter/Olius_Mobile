package com.olius.app.data.repository

import com.olius.app.data.remote.ApiClient
import com.olius.app.domain.entity.PickupRequest
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

/** "Solicitar coleta" no contrato real da Core (POST /api/v1/collection-requests), com servidor local. */
class HttpHomeRepositoryTest {

    private lateinit var server: HttpServer
    private var path: String? = null
    private var idempotencyKey: String? = null
    private var body: String? = null

    @Before
    fun setUp() {
        server = HttpServer.create(InetSocketAddress("localhost", 0), 0).apply {
            createContext("/") { exchange ->
                path = exchange.requestURI.path
                idempotencyKey = exchange.requestHeaders.getFirst("Idempotency-Key")
                body = exchange.requestBody.bufferedReader().readText()
                val bytes = """{"id":"r-1","status":"PENDING"}""".toByteArray()
                exchange.sendResponseHeaders(201, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            start()
        }
    }

    @After
    fun tearDown() = server.stop(0)

    private val repository get() = HttpHomeRepository(ApiClient("http://localhost:${server.address.port}/"))

    private val key = "3f2b9c1e-8a4d-4f6b-9c2e-1d7a5b3c9e0f"

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
