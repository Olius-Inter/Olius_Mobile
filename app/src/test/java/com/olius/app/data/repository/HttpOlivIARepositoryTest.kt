package com.olius.app.data.repository

import com.olius.app.data.remote.ApiClient
import com.olius.app.domain.entity.AttachmentKind
import com.olius.app.domain.entity.ChatAttachment
import com.olius.app.domain.entity.OlivIAException
import com.olius.app.domain.entity.OlivIAReplyChunk
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Um servidor HTTP local faz o papel da Core no POST /api/v1/chat. */
class HttpOlivIARepositoryTest {

    private lateinit var server: HttpServer
    private var status = 200
    private var responseBody = """{"reply": "Olá! Posso ajudar.", "conversationId": "c-1"}"""

    /** Quando true, o servidor só responde depois do [release] (simula a IA demorando). */
    private var hang = false
    private val release = CountDownLatch(1)
    private val requests = mutableListOf<Pair<String?, String>>()

    @Before
    fun startServer() {
        server = HttpServer.create(InetSocketAddress("localhost", 0), 0)
        server.executor = Executors.newCachedThreadPool()
        server.createContext("/api/v1/chat") { exchange ->
            val body = exchange.requestBody.bufferedReader().readText()
            synchronized(requests) { requests += exchange.requestHeaders.getFirst("Authorization") to body }
            if (hang) release.await(10, TimeUnit.SECONDS)
            val bytes = responseBody.toByteArray()
            runCatching {
                exchange.sendResponseHeaders(status, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
        }
        server.start()
    }

    @After
    fun stopServer() {
        release.countDown()
        server.stop(0)
    }

    private fun repository(readTimeoutMs: Int = 5_000, port: Int = server.address.port) = HttpOlivIARepository(
        ApiClient(baseUrl = "http://localhost:$port/", tokenProvider = { "jwt-1" }, readTimeoutMs = readTimeoutMs)
    )

    private suspend fun expectError(repository: HttpOlivIARepository): OlivIAException {
        try {
            repository.reply("Oi", null, null).toList()
        } catch (e: OlivIAException) {
            return e
        }
        fail("esperava OlivIAException")
        error("inalcançável")
    }

    @Test
    fun `sucesso manda message com o JWT e devolve o id da conversa e a resposta`() = runTest {
        val chunks = repository().reply("Oi", null, null).toList()

        assertEquals(
            listOf(OlivIAReplyChunk.Conversation("c-1"), OlivIAReplyChunk.Text("Olá! Posso ajudar.")),
            chunks
        )
        val (authorization, body) = requests.single()
        assertEquals("Bearer jwt-1", authorization)
        val json = JSONObject(body)
        assertEquals("Oi", json.getString("message"))
        assertFalse(json.has("conversationId"))
        assertFalse(json.has("user_id"))
    }

    @Test
    fun `reenvia o conversationId da conversa`() = runTest {
        repository().reply("De novo", null, "c-1").toList()

        assertEquals("c-1", JSONObject(requests.single().second).getString("conversationId"))
    }

    @Test
    fun `rota inexistente na Core vira NotAvailable`() = runTest {
        status = 404
        responseBody = """{"status": 404}"""
        assertTrue(expectError(repository()) is OlivIAException.NotAvailable)

        status = 405
        assertTrue(expectError(repository()) is OlivIAException.NotAvailable)
    }

    @Test
    fun `503 vira Unavailable e 429 vira TooManyRequests`() = runTest {
        status = 503
        assertTrue(expectError(repository()) is OlivIAException.Unavailable)

        status = 429
        assertTrue(expectError(repository()) is OlivIAException.TooManyRequests)
    }

    @Test
    fun `resposta sem reply vira Unknown`() = runTest {
        responseBody = """{"conversationId": "c-1"}"""
        assertTrue(expectError(repository()) is OlivIAException.Unknown)
    }

    @Test
    fun `servidor demorando mais que o readTimeout vira Timeout`() = runTest {
        hang = true
        assertTrue(expectError(repository(readTimeoutMs = 300)) is OlivIAException.Timeout)
    }

    @Test
    fun `servidor fora do ar vira Offline`() = runTest {
        val closedPort = ServerSocket(0).use { it.localPort }
        assertTrue(expectError(repository(port = closedPort)) is OlivIAException.Offline)
    }

    @Test
    fun `anexo nao e enviado e a resposta avisa`() = runTest {
        val file = ChatAttachment("nota.pdf", AttachmentKind.FILE)

        val chunks = repository().reply("Leia isto", file, null).toList()

        assertEquals(listOf("message"), JSONObject(requests.single().second).keys().asSequence().toList())
        assertEquals(OlivIAReplyChunk.Text("\n\n" + HttpOlivIARepository.attachmentNotSentText("nota.pdf")), chunks.last())
    }

    @Test
    fun `so o anexo sem texto nem chama a Core`() = runTest {
        val chunks = repository().reply("", ChatAttachment("foto.jpg", AttachmentKind.PHOTO), null).toList()

        assertEquals(listOf(OlivIAReplyChunk.Text(HttpOlivIARepository.ATTACHMENT_ONLY_TEXT)), chunks)
        assertTrue(requests.isEmpty())
    }

    @Test
    fun `cancelar fecha a conexao sem esperar a resposta`() = runBlocking {
        hang = true
        val repository = repository(readTimeoutMs = 30_000)
        val job = launch(Dispatchers.Default) { repository.reply("Oi", null, null).toList() }
        while (synchronized(requests) { requests.isEmpty() }) delay(10)

        val start = System.currentTimeMillis()
        job.cancelAndJoin()

        assertTrue("cancelamento demorou", System.currentTimeMillis() - start < 2_000)
        assertTrue(job.isCancelled)
    }
}
