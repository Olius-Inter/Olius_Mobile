package com.olius.app.data.repository

import com.olius.app.data.local.InMemoryRefreshTokenStore
import com.olius.app.data.remote.ApiException
import com.olius.app.data.remote.CoreAuthApi
import com.olius.app.data.remote.MobileTokens
import com.olius.app.domain.entity.RankingCategory
import com.sun.net.httpserver.HttpServer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.net.InetSocketAddress

/** Ranking ligado à sessão da Core ([HttpRankingRepository.withCoreSession]). */
class HttpRankingCoreSessionTest {
    private val api = mockk<CoreAuthApi>(relaxUnitFun = true)
    private val store = InMemoryRefreshTokenStore()
    private val now = 1_000_000L
    private val authorizations = mutableListOf<String?>()

    /** Token que a "Core" aceita no GET /api/v1/auth/session. */
    private var validToken = "a-1"
    private lateinit var server: HttpServer

    @Before
    fun startServer() {
        server = HttpServer.create(InetSocketAddress("localhost", 0), 0)
        server.createContext("/api/ranking") { exchange ->
            val auth = exchange.requestHeaders.getFirst("Authorization")
            synchronized(authorizations) { authorizations += auth }
            val (status, body) = when (auth) {
                null -> 200 to page(myPosition = null)
                "Bearer $validToken" -> 200 to page(myPosition = """{"position": 7, "points": 30}""")
                else -> 401 to """{"status": 401, "detail": "Invalid or expired session."}"""
            }
            val bytes = body.toByteArray()
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
    }

    @After
    fun stopServer() = server.stop(0)

    private fun page(myPosition: String?) =
        """{"year": 2026, "updatedAt": null, "myPosition": $myPosition, "items": []}"""

    private fun tokens(access: String, refresh: String) =
        MobileTokens(access, accessExpiresInSeconds = 900, refreshToken = refresh, refreshExpiresAt = "2030-01-01T00:00:00Z")

    private fun TestScope.session() = CoreSessionRepository(api = api, store = store, scope = this, clock = { now })

    private fun repository(session: CoreSessionRepository) = HttpRankingRepository.withCoreSession(
        baseUrl = "http://localhost:${server.address.port}/",
        tokenProvider = { session.accessToken() },
        onUnauthorized = session::onUnauthorized
    )

    @Test
    fun `logado manda o JWT da Core e recebe sua posicao`() = runTest {
        val session = session()
        session.start(tokens("a-1", "r-1"))

        val page = repository(session).getRanking(RankingCategory.ESTABLISHMENT).getOrThrow()

        assertEquals(7, page.myPosition)
        assertEquals(30L, page.myPoints)
        assertEquals(listOf<String?>("Bearer a-1"), authorizations)
    }

    @Test
    fun `deslogado chama sem Authorization e o ranking vem sem posicao`() = runTest {
        val page = repository(session()).getRanking(RankingCategory.CITIZEN).getOrThrow()

        assertNull(page.myPosition)
        assertEquals(listOf<String?>(null), authorizations)
        coVerify(exactly = 0) { api.refresh(any()) }
    }

    @Test
    fun `token vencido leva 401, renova uma vez e repete a chamada`() = runTest {
        val session = session()
        session.start(tokens("a-1", "r-1"))
        validToken = "a-2"
        coEvery { api.refresh("r-1") } returns tokens("a-2", "r-2")

        val page = repository(session).getRanking(RankingCategory.ESTABLISHMENT).getOrThrow()

        assertEquals(7, page.myPosition)
        assertEquals(listOf<String?>("Bearer a-1", "Bearer a-2"), authorizations)
        coVerify(exactly = 1) { api.refresh("r-1") }
    }

    @Test
    fun `401 depois do refresh mostra o ranking sem posicao`() = runTest {
        val session = session()
        session.start(tokens("a-1", "r-1"))
        validToken = "nenhum"
        coEvery { api.refresh("r-1") } returns tokens("a-2", "r-2")

        val page = repository(session).getRanking(RankingCategory.ESTABLISHMENT).getOrThrow()

        assertNull(page.myPosition)
        assertEquals(listOf<String?>("Bearer a-1", "Bearer a-2", null), authorizations)
        coVerify(exactly = 1) { api.refresh(any()) }
    }

    @Test
    fun `refresh recusado encerra a sessao e o ranking aparece sem posicao`() = runTest {
        val session = session()
        session.start(tokens("a-1", "r-1"))
        validToken = "a-2"
        coEvery { api.refresh("r-1") } throws ApiException(401, "")

        val page = repository(session).getRanking(RankingCategory.ESTABLISHMENT).getOrThrow()

        assertNull(page.myPosition)
        assertEquals(listOf<String?>("Bearer a-1", null), authorizations)
        assertNull(store.load())
    }

    @Test
    fun `sem rede para renovar o ranking aparece sem posicao e a sessao continua`() = runTest {
        val session = session()
        session.start(tokens("a-1", "r-1"))
        validToken = "a-2"
        coEvery { api.refresh("r-1") } throws IOException("sem rede")

        val page = repository(session).getRanking(RankingCategory.ESTABLISHMENT).getOrThrow()

        assertNull(page.myPosition)
        assertEquals("r-1", store.load()?.token)
    }
}
