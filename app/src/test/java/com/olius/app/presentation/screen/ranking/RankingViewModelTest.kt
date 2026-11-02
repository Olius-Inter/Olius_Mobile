package com.olius.app.presentation.screen.ranking

import com.olius.app.MainDispatcherRule
import com.olius.app.data.remote.ApiException
import com.olius.app.data.repository.MockRankingRepository
import com.olius.app.domain.entity.AchievementsSummary
import com.olius.app.domain.entity.ProfileType
import com.olius.app.domain.entity.RankingCategory
import com.olius.app.domain.entity.RankingEntry
import com.olius.app.domain.entity.RankingPage
import com.olius.app.domain.repository.RankingRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class RankingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val saoPaulo = ZoneId.of("America/Sao_Paulo")
    private val repository = CountingRepository(MockRankingRepository(today = { LocalDate.of(2026, 8, 15) }))
    private val viewModel by lazy { RankingViewModel(repository, ProfileType.ESTABLISHMENT, saoPaulo) }
    private val state get() = viewModel.uiState.value

    @Test
    fun `comeca no ranking anual de estabelecimentos`() {
        assertEquals(RankingCategory.ESTABLISHMENT, state.category)
        assertEquals(RankingTab.COMPETITIONS, state.tab)
        assertEquals("Ano de 2026", state.periodLabel)
    }

    @Test
    fun `lista fica ordenada por pontos com as posicoes certas`() {
        val entries = state.entries

        assertEquals(entries.sortedByDescending { it.points }, entries)
        assertEquals((1..entries.size).toList(), entries.map { it.position })
        assertEquals(entries.take(3), state.podium)
    }

    @Test
    fun `empate divide a posicao sem pular a seguinte`() {
        viewModel.onCategorySelected(RankingCategory.CITIZEN)

        assertEquals(listOf(1, 2, 2, 3, 4), state.entries.map { it.position })
    }

    @Test
    fun `posicao da pessoa vem do proprio ranking`() {
        assertEquals("ToKonFome", state.currentUser?.name)
        assertEquals(1, state.currentUser?.position)
        assertEquals(120L, state.currentUser?.points)

        viewModel.onCategorySelected(RankingCategory.CITIZEN)
        assertEquals("Naldo Canal", state.currentUser?.name)
        assertEquals(3, state.currentUser?.position)
    }

    @Test
    fun `posicao fora do top vem da api`() {
        val page = RankingPage(2026, emptyList(), myPosition = 42, myPoints = 3)
        val current = RankingUiState(myPosition = page.myPosition, myPoints = page.myPoints).currentUser
        assertEquals(42, current?.position)
        assertEquals(3L, current?.points)
    }

    @Test
    fun `data de atualizacao vira texto no fuso do aparelho`() {
        val vm = RankingViewModel(
            fixedRepository(RankingPage(2026, emptyList(), updatedAt = "2026-10-24T13:15:00Z")),
            ProfileType.ESTABLISHMENT,
            saoPaulo
        )
        assertEquals("Atualizado em 24/10 às 10:15", vm.uiState.value.updatedLabel)

        val invalid = RankingViewModel(
            fixedRepository(RankingPage(2026, emptyList(), updatedAt = "ontem")),
            ProfileType.ESTABLISHMENT,
            saoPaulo
        )
        assertNull(invalid.uiState.value.updatedLabel)
    }

    @Test
    fun `trocar de aba mantem a categoria`() {
        viewModel.onCategorySelected(RankingCategory.CITIZEN)
        viewModel.onTabSelected(RankingTab.ACHIEVEMENTS)

        assertEquals(RankingTab.ACHIEVEMENTS, state.tab)
        assertEquals(RankingCategory.CITIZEN, state.category)
    }

    @Test
    fun `iniciais do avatar seguem a referencia`() {
        fun initials(name: String) = RankingEntry(1, name, "", 0).initials

        assertEquals("DA", initials("Dogão do Adriano"))
        assertEquals("TK", initials("ToKonFome"))
        assertEquals("RT", initials("Reuters"))
        assertEquals("BK", initials("Burger King"))
        assertEquals("AE", initials("Aeiou"))
        assertEquals("A", initials("A"))
        assertEquals("", initials("   "))
    }

    @Test
    fun `sem a pessoa no ranking nao existe posicao dela`() {
        assertNull(RankingUiState().currentUser)
        assertTrue(RankingUiState().podium.isEmpty())
    }

    @Test
    fun `voltar a uma categoria ja vista nao chama a api de novo`() {
        viewModel.onCategorySelected(RankingCategory.CITIZEN)
        viewModel.onCategorySelected(RankingCategory.ESTABLISHMENT)

        assertEquals(2, repository.rankingCalls)
    }

    @Test
    fun `conquistas so carregam ao abrir a aba`() {
        assertEquals(0, repository.achievementsCalls)

        viewModel.onTabSelected(RankingTab.ACHIEVEMENTS)

        assertEquals(1, repository.achievementsCalls)
        assertEquals(4, state.achievements?.achievements?.size)
    }

    @Test
    fun `falha da api vira mensagem de erro e retry tenta de novo`() {
        val failing = object : RankingRepository {
            var fail = true
            override suspend fun getRanking(category: RankingCategory): Result<RankingPage> =
                if (fail) Result.failure(RuntimeException("offline")) else Result.success(RankingPage(2026, emptyList()))
            override suspend fun getAchievements(category: RankingCategory): Result<AchievementsSummary> =
                Result.failure(RuntimeException("offline"))
        }
        val vm = RankingViewModel(failing, ProfileType.ESTABLISHMENT, saoPaulo)
        assertEquals("Sem conexão", vm.uiState.value.errorMessage)

        failing.fail = false
        vm.onRetry()

        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `503 da api mostra ranking indisponivel`() {
        val unavailable = object : RankingRepository {
            override suspend fun getRanking(category: RankingCategory): Result<RankingPage> =
                Result.failure(ApiException(503, "Ranking not built yet."))
            override suspend fun getAchievements(category: RankingCategory): Result<AchievementsSummary> =
                Result.failure(RuntimeException("offline"))
        }
        val vm = RankingViewModel(unavailable, ProfileType.ESTABLISHMENT, saoPaulo)
        assertEquals("Ranking indisponível no momento", vm.uiState.value.errorMessage)
    }

    @Test
    fun `cidadao abre o ranking no toggle Cidadao`() {
        val vm = RankingViewModel(repository, ProfileType.CITIZEN, saoPaulo)
        assertEquals(RankingCategory.CITIZEN, vm.uiState.value.category)
    }

    private fun fixedRepository(page: RankingPage) = object : RankingRepository {
        override suspend fun getRanking(category: RankingCategory): Result<RankingPage> = Result.success(page)
        override suspend fun getAchievements(category: RankingCategory): Result<AchievementsSummary> =
            Result.failure(RuntimeException("offline"))
    }

    private class CountingRepository(private val delegate: RankingRepository) : RankingRepository {
        var rankingCalls = 0
        var achievementsCalls = 0
        override suspend fun getRanking(category: RankingCategory): Result<RankingPage> {
            rankingCalls++
            return delegate.getRanking(category)
        }
        override suspend fun getAchievements(category: RankingCategory): Result<AchievementsSummary> {
            achievementsCalls++
            return delegate.getAchievements(category)
        }
    }
}
