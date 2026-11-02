package com.olius.app.data.repository

import com.olius.app.data.remote.ApiClient
import com.olius.app.domain.entity.RankingCategory
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress

/** Conquistas B2B na Core (GET /establishments/me/certificate-progress), com servidor local. */
class HttpAchievementsRepositoryTest {

    private lateinit var server: HttpServer
    private var path: String? = null

    private val progress = """
        {"establishmentId": "e-1", "eligibleVolumeLiters": 640.0, "levels": [
          {"levelId": "l-3", "name": "Ouro", "description": null, "requiredLiters": 1000.00, "reached": false,
           "remainingLiters": 360.0, "certificateId": null, "certificateStatus": null},
          {"levelId": "l-1", "name": "Bronze", "description": "Primeiros 100 L", "requiredLiters": 100.00, "reached": true,
           "remainingLiters": 0, "certificateId": "c-1", "certificateStatus": "ACTIVE"},
          {"levelId": "l-2", "name": "Prata", "description": null, "requiredLiters": 500.00, "reached": true,
           "remainingLiters": 0, "certificateId": null, "certificateStatus": null}
        ]}
    """.trimIndent()

    @Before
    fun setUp() {
        server = HttpServer.create(InetSocketAddress("localhost", 0), 0).apply {
            createContext("/") { exchange ->
                path = exchange.requestURI.path
                val bytes = progress.toByteArray()
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            start()
        }
    }

    @After
    fun tearDown() = server.stop(0)

    private val core get() = ApiClient("http://localhost:${server.address.port}/")
    private val ranking = ApiClient("http://localhost:1/")

    @Test
    fun `estabelecimento le o progresso de certificado da Core`() = runTest {
        val summary = HttpRankingRepository(ranking, coreClient = core)
            .getAchievements(RankingCategory.ESTABLISHMENT).getOrThrow()

        assertEquals("/api/v1/establishments/me/certificate-progress", path)
        assertEquals(listOf("Bronze", "Prata", "Ouro"), summary.achievements.map { it.title })
        assertEquals(listOf(true, true, false), summary.achievements.map { it.unlocked })
        assertEquals(2, summary.level.level)
        assertEquals("Prata", summary.level.name)
        assertEquals(640.0, summary.level.totalLiters, 0.0)
        assertEquals(1000.0, summary.level.targetLiters!!, 0.0)
        assertEquals(0.28f, summary.level.progress, 0.001f)
        assertFalse(summary.level.isMax)
        assertTrue(summary.level.configured)
    }

    @Test
    fun `cidadao nao tem rota e nao chama a Core`() = runTest {
        val result = HttpRankingRepository(ranking, coreClient = core).getAchievements(RankingCategory.CITIZEN)

        assertTrue(result.isFailure)
        assertNull(path)
    }

    @Test
    fun `sem cliente da Core (mock) continua indisponivel`() = runTest {
        assertTrue(HttpRankingRepository(ranking).getAchievements(RankingCategory.ESTABLISHMENT).isFailure)
    }

    @Test
    fun `catalogo vazio fica nao configurado`() {
        val summary = parseCertificateProgress(JSONObject("""{"eligibleVolumeLiters": 30, "levels": []}"""))

        assertFalse(summary.level.configured)
        assertEquals(0, summary.level.level)
        assertTrue(summary.achievements.isEmpty())
    }
}
