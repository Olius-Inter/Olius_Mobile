package com.olius.app.data.remote

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress

/** Sobe um servidor HTTP local (JDK) para testar o [ApiClient] de ponta a ponta. */
class ApiClientTest {

    private lateinit var server: HttpServer
    private var status = 200
    private var responseBody = """{"ok":true}"""
    private var lastExchange: Request? = null

    private class Request(val method: String, val uri: String, val authorization: String?, val contentType: String?, val body: String)

    private val baseUrl get() = "http://localhost:${server.address.port}/"

    @Before
    fun setUp() {
        server = HttpServer.create(InetSocketAddress("localhost", 0), 0).apply {
            createContext("/") { exchange -> respond(exchange) }
            start()
        }
    }

    @After
    fun tearDown() = server.stop(0)

    private fun respond(exchange: HttpExchange) {
        lastExchange = Request(
            method = exchange.requestMethod,
            uri = exchange.requestURI.toString(),
            authorization = exchange.requestHeaders.getFirst("Authorization"),
            contentType = exchange.requestHeaders.getFirst("Content-Type"),
            body = exchange.requestBody.bufferedReader().readText()
        )
        val bytes = responseBody.toByteArray()
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    @Test
    fun `get monta a url com a query e manda o token`() = runTest {
        val client = ApiClient(baseUrl, tokenProvider = { "abc" })

        val body = client.get("/api/ranking", mapOf("category" to "citizen", "q" to "a b", "vazio" to null))

        assertEquals("""{"ok":true}""", body)
        val request = lastExchange!!
        assertEquals("GET", request.method)
        assertEquals("/api/ranking?category=citizen&q=a+b", request.uri)
        assertEquals("Bearer abc", request.authorization)
    }

    @Test
    fun `sem token nao manda Authorization e sem query nao manda interrogacao`() = runTest {
        ApiClient(baseUrl).get("api/ranking")

        assertEquals("/api/ranking", lastExchange!!.uri)
        assertNull(lastExchange!!.authorization)
    }

    @Test
    fun `post envia o corpo em json`() = runTest {
        ApiClient(baseUrl).post("api/v1/users", """{"name":"Ana"}""")

        val request = lastExchange!!
        assertEquals("POST", request.method)
        assertEquals("""{"name":"Ana"}""", request.body)
        assertTrue(request.contentType!!.startsWith("application/json"))
    }

    @Test
    fun `resposta fora de 2xx vira ApiException com o codigo e o corpo`() = runTest {
        status = 503
        responseBody = "fora do ar"

        try {
            ApiClient(baseUrl).get("api/ranking")
            fail("deveria lançar ApiException")
        } catch (e: ApiException) {
            assertEquals(503, e.code)
            assertEquals("HTTP 503: fora do ar", e.message)
        }
    }
}
