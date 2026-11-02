package com.olius.app.data.repository

import com.olius.app.data.local.InMemoryRefreshTokenStore
import com.olius.app.data.local.RefreshTokenStore
import com.olius.app.data.local.StoredRefreshToken
import com.olius.app.data.remote.ApiClient
import com.olius.app.data.remote.ApiException
import com.olius.app.data.remote.CoreAuthApi
import com.olius.app.data.remote.MobileTokens
import com.sun.net.httpserver.HttpServer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.net.InetSocketAddress
import java.time.Instant

class CoreSessionRepositoryTest {

    private val api = mockk<CoreAuthApi>(relaxUnitFun = true)
    private val store = InMemoryRefreshTokenStore()
    private var now = 1_000_000L
    private var sessionEndedCalls = 0

    private fun tokens(access: String, refresh: String, refreshExpiresAt: String? = "2030-01-01T00:00:00Z") =
        MobileTokens(access, accessExpiresInSeconds = 900, refreshToken = refresh, refreshExpiresAt = refreshExpiresAt)

    private fun TestScope.session() = CoreSessionRepository(
        api = api,
        store = store,
        scope = this,
        clock = { now },
        onSessionEnded = { sessionEndedCalls++ }
    )

    /** Passa dos 15 minutos do access token. */
    private fun expireAccess() {
        now += 16 * 60 * 1000
    }

    @Test
    fun `login guarda o refresh no aparelho e o access so na memoria`() = runTest {
        val session = session()

        session.start(tokens("a-1", "r-1"))

        assertEquals("r-1", store.load()?.token)
        assertEquals(Instant.parse("2030-01-01T00:00:00Z").toEpochMilli(), store.load()?.expiresAtMillis)
        assertEquals("a-1", session.accessToken())
        assertTrue(session.hasSession())
        coVerify(exactly = 0) { api.refresh(any()) }
    }

    @Test
    fun `sem sessao nao ha token nem refresh`() = runTest {
        val session = session()

        assertNull(session.accessToken())
        assertFalse(session.hasSession())
    }

    @Test
    fun `refresh salvo e vencido nao conta como sessao`() = runTest {
        store.save(StoredRefreshToken("r-1", expiresAtMillis = now - 1))

        assertFalse(session().hasSession())
    }

    @Test
    fun `access vencido renova e grava o refresh novo`() = runTest {
        val session = session()
        session.start(tokens("a-1", "r-1"))
        coEvery { api.refresh("r-1") } returns tokens("a-2", "r-2")

        expireAccess()

        assertEquals("a-2", session.accessToken())
        assertEquals("r-2", store.load()?.token)
    }

    @Test
    fun `app reaberto com refresh salvo renova na primeira chamada`() = runTest {
        store.save(StoredRefreshToken("r-1", null))
        coEvery { api.refresh("r-1") } returns tokens("a-2", "r-2")

        assertEquals("a-2", session().accessToken())
        assertEquals("r-2", store.load()?.token)
    }

    @Test
    fun `chamadas ao mesmo tempo fazem um refresh so`() = runTest {
        val session = session()
        session.start(tokens("a-1", "r-1"))
        val gate = CompletableDeferred<Unit>()
        coEvery { api.refresh("r-1") } coAnswers { gate.await(); tokens("a-2", "r-2") }
        expireAccess()

        val first = async { session.accessToken() }
        val second = async { session.accessToken() }
        val third = async { session.onUnauthorized("a-1") }
        runCurrent()
        gate.complete(Unit)

        assertEquals(listOf("a-2", "a-2", "a-2"), listOf(first.await(), second.await(), third.await()))
        coVerify(exactly = 1) { api.refresh(any()) }
    }

    @Test
    fun `401 de um token ja renovado usa o novo sem outro refresh`() = runTest {
        val session = session()
        session.start(tokens("a-2", "r-2"))

        assertEquals("a-2", session.onUnauthorized("a-1"))
        coVerify(exactly = 0) { api.refresh(any()) }
    }

    @Test
    fun `refresh recusado encerra a sessao e avisa`() = runTest {
        val session = session()
        session.start(tokens("a-1", "r-1"))
        coEvery { api.refresh("r-1") } throws ApiException(401, """{"code":"AUTHENTICATION_REQUIRED"}""")
        val expired = mutableListOf<Unit>()
        val collector = launch { session.sessionExpired.collect { expired += it } }
        runCurrent()

        assertNull(session.onUnauthorized("a-1"))
        runCurrent()

        assertNull(store.load())
        assertFalse(session.hasSession())
        assertNull(session.accessToken())
        assertEquals(1, expired.size)
        assertEquals(1, sessionEndedCalls)
        collector.cancel()
    }

    @Test
    fun `sem rede no refresh a chamada falha mas a sessao continua`() = runTest {
        val session = session()
        session.start(tokens("a-1", "r-1"))
        coEvery { api.refresh("r-1") } throws IOException("sem rede")
        expireAccess()

        try {
            session.accessToken()
            fail("deveria lançar IOException")
        } catch (e: IOException) {
            assertEquals("r-1", store.load()?.token)
            assertEquals(0, sessionEndedCalls)
        }
    }

    @Test
    fun `se o refresh novo nao for gravado o access novo nao e usado`() = runTest {
        val failingStore = object : RefreshTokenStore by store {
            override fun save(token: StoredRefreshToken) {
                check(token.token != "r-2") { "disco cheio" }
                store.save(token)
            }
        }
        val session = CoreSessionRepository(api, failingStore, this, clock = { now })
        session.start(tokens("a-1", "r-1"))
        coEvery { api.refresh("r-1") } returns tokens("a-2", "r-2")
        expireAccess()

        try {
            session.accessToken()
            fail("deveria lançar")
        } catch (e: IllegalStateException) {
            assertEquals("r-1", store.load()?.token)
        }
    }

    @Test
    fun `logout limpa o aparelho na hora e avisa a Core com o access atual`() = runTest {
        val session = session()
        session.start(tokens("a-1", "r-1"))

        session.signOut()

        assertNull(store.load())
        assertNull(session.accessToken())
        assertEquals(1, sessionEndedCalls)
        advanceUntilIdle()
        coVerify(exactly = 1) { api.logout("a-1") }
        coVerify(exactly = 0) { api.refresh(any()) }
    }

    @Test
    fun `logout com access vencido renova uma vez so para avisar a Core`() = runTest {
        val session = session()
        session.start(tokens("a-1", "r-1"))
        coEvery { api.refresh("r-1") } returns tokens("a-2", "r-2")
        expireAccess()

        session.signOut()
        advanceUntilIdle()

        coVerify(exactly = 1) { api.logout("a-2") }
        // O refresh usado só para o logout não volta para o aparelho.
        assertNull(store.load())
    }

    @Test
    fun `logout sem sessao nao chama a Core`() = runTest {
        session().signOut()
        advanceUntilIdle()

        coVerify(exactly = 0) { api.logout(any()) }
    }

    @Test
    fun `refresh que termina depois do logout nao grava o token de volta`() = runTest {
        val session = session()
        session.start(tokens("a-1", "r-1"))
        val gate = CompletableDeferred<Unit>()
        coEvery { api.refresh("r-1") } coAnswers { gate.await(); tokens("a-2", "r-2") }
        expireAccess()

        val pending = async { session.accessToken() }
        runCurrent()
        session.signOut()
        gate.complete(Unit)

        assertNull(pending.await())
        assertNull(store.load())
    }

    @Test
    fun `api recebe 401, renova uma vez e repete a chamada`() = runTest {
        val server = HttpServer.create(InetSocketAddress("localhost", 0), 0)
        val authorizations = mutableListOf<String?>()
        server.createContext("/") { exchange ->
            val auth = exchange.requestHeaders.getFirst("Authorization")
            synchronized(authorizations) { authorizations += auth }
            val (status, body) = if (auth == "Bearer a-2") 200 to """{"ok":true}""" else 401 to "{}"
            val bytes = body.toByteArray()
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            val session = session()
            session.start(tokens("a-1", "r-1"))
            coEvery { api.refresh("r-1") } returns tokens("a-2", "r-2")
            val client = ApiClient(
                baseUrl = "http://localhost:${server.address.port}/",
                tokenProvider = { session.accessToken() },
                onUnauthorized = session::onUnauthorized
            )

            assertEquals("""{"ok":true}""", client.get("api/v1/users/me"))
            assertEquals(listOf("Bearer a-1", "Bearer a-2"), authorizations)
            coVerify(exactly = 1) { api.refresh("r-1") }
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `401 de novo depois do refresh sobe o erro sem repetir de novo`() = runTest {
        val server = HttpServer.create(InetSocketAddress("localhost", 0), 0)
        var calls = 0
        server.createContext("/") { exchange ->
            calls++
            exchange.sendResponseHeaders(401, 2)
            exchange.responseBody.use { it.write("{}".toByteArray()) }
        }
        server.start()
        try {
            val session = session()
            session.start(tokens("a-1", "r-1"))
            coEvery { api.refresh("r-1") } returns tokens("a-2", "r-2")
            val client = ApiClient(
                baseUrl = "http://localhost:${server.address.port}/",
                tokenProvider = { session.accessToken() },
                onUnauthorized = session::onUnauthorized
            )

            try {
                client.get("api/v1/users/me")
                fail("deveria lançar ApiException")
            } catch (e: ApiException) {
                assertEquals(401, e.code)
            }
            assertEquals(2, calls)
        } finally {
            server.stop(0)
        }
    }
}
