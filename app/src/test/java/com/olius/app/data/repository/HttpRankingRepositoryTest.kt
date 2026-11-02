package com.olius.app.data.repository

import com.olius.app.data.remote.ApiClient
import com.olius.app.data.remote.ApiConfig
import com.olius.app.data.remote.ApiException
import com.olius.app.domain.entity.RankingCategory
import com.olius.app.domain.entity.RankingEntry
import com.olius.app.domain.entity.RankingPage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpRankingRepositoryTest {

    private val client = mockk<ApiClient>()
    private val anonymous = mockk<ApiClient>()
    private val repository = HttpRankingRepository(client, anonymous)

    private val body = """
        {
          "year": 2026,
          "updatedAt": "2026-10-24T13:15:00Z",
          "myPosition": {"position": 7, "points": 30},
          "items": [
            {"position": 1, "name": "ToKonFome", "city": "Itaquaquecetuba", "points": 120, "currentUser": true, "initials": "TK"},
            {"position": 1, "name": "Dogão do Adriano", "city": null, "points": 120, "initials": null}
          ]
        }
    """.trimIndent()

    @Test
    fun `converte a resposta da api e pede a categoria certa`() = runTest {
        coEvery { client.get(ApiConfig.Ranking.RANKING, any()) } returns body

        val page = repository.getRanking(RankingCategory.CITIZEN).getOrThrow()

        assertEquals(
            RankingPage(
                year = 2026,
                entries = listOf(
                    RankingEntry(1, "ToKonFome", "Itaquaquecetuba", 120, isCurrentUser = true, serverInitials = "TK"),
                    RankingEntry(1, "Dogão do Adriano", "", 120)
                ),
                myPosition = 7,
                myPoints = 30,
                updatedAt = "2026-10-24T13:15:00Z"
            ),
            page
        )
        assertEquals("DA", page.entries[1].initials)
        coVerify { client.get(ApiConfig.Ranking.RANKING, mapOf("category" to "citizen", "limit" to ApiConfig.RANKING_LIMIT.toString())) }
    }

    @Test
    fun `sem myPosition a posicao da pessoa fica nula`() = runTest {
        coEvery { client.get(any(), any()) } returns """{"year": 2026, "items": []}"""

        val page = repository.getRanking(RankingCategory.ESTABLISHMENT).getOrThrow()

        assertEquals(RankingPage(2026, emptyList()), page)
    }

    @Test
    fun `sessao recusada ou core fora tenta de novo sem token`() = runTest {
        for (code in listOf(401, 503)) {
            coEvery { client.get(any(), any()) } throws ApiException(code, "")
            coEvery { anonymous.get(any(), any()) } returns body

            assertEquals(2026, repository.getRanking(RankingCategory.ESTABLISHMENT).getOrThrow().year)
        }
        coVerify(exactly = 2) { anonymous.get(any(), any()) }
    }

    @Test
    fun `outros erros nao tentam de novo`() = runTest {
        coEvery { client.get(any(), any()) } throws ApiException(500, "")

        val error = repository.getRanking(RankingCategory.ESTABLISHMENT).exceptionOrNull()

        assertEquals(500, (error as ApiException).code)
        coVerify(exactly = 0) { anonymous.get(any(), any()) }
    }

    @Test
    fun `com um cliente so o erro de sessao sobe direto`() = runTest {
        coEvery { client.get(any(), any()) } throws ApiException(401, "")

        val error = HttpRankingRepository(client).getRanking(RankingCategory.ESTABLISHMENT).exceptionOrNull()

        assertEquals(401, (error as ApiException).code)
    }

    @Test
    fun `conquistas ainda nao vem da api`() = runTest {
        val result = repository.getAchievements(RankingCategory.ESTABLISHMENT)

        assertTrue(result.exceptionOrNull() is UnsupportedOperationException)
    }
}
