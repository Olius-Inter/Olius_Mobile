package com.olius.app.presentation.screen.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olius.app.data.remote.ApiException
import com.olius.app.data.remote.AppContainer
import com.olius.app.domain.entity.AchievementsSummary
import com.olius.app.domain.entity.ProfileType
import com.olius.app.domain.entity.RankingCategory
import com.olius.app.domain.entity.RankingPage
import com.olius.app.domain.repository.RankingRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Ranking de Recicladores (ref_ranking.png / ref_ranking_2.png /
 * ref_ranking_b2b.png): ranking anual por pontos da API Redis. Cada categoria
 * é buscada uma vez e guardada em [pages]: voltar a um toggle já visto não
 * chama a API de novo. Conquistas só são buscadas quando a aba é aberta.
 */
class RankingViewModel(
    private val repository: RankingRepository = AppContainer.rankingRepository,
    profileType: ProfileType = AppContainer.currentProfileType(),
    private val zone: ZoneId = ZoneId.systemDefault()
) : ViewModel() {

    /** Toggle abre no público da pessoa (Figma: B2B em "Estabelecimento", B2C em "Cidadão"). */
    private val ownCategory = profileType.rankingCategory

    private val _uiState = MutableStateFlow(RankingUiState(category = ownCategory))
    val uiState: StateFlow<RankingUiState> = _uiState.asStateFlow()

    private val pages = mutableMapOf<RankingCategory, RankingPage>()
    private var achievementsCache: AchievementsSummary? = null
    private var rankingJob: Job? = null

    init {
        loadRanking()
    }

    fun onCategorySelected(category: RankingCategory) {
        _uiState.update { it.copy(category = category) }
        loadRanking()
    }

    fun onTabSelected(tab: RankingTab) {
        _uiState.update { it.copy(tab = tab) }
        if (tab == RankingTab.ACHIEVEMENTS) loadAchievements()
    }

    /** Botão "Tentar novamente": descarta o cache da categoria atual. */
    fun onRetry() {
        val state = _uiState.value
        pages.remove(state.category)
        achievementsCache = null
        loadRanking()
        if (state.tab == RankingTab.ACHIEVEMENTS) loadAchievements()
    }

    private fun loadRanking() {
        val category = _uiState.value.category
        pages[category]?.let { return applyPage(it) }

        rankingJob?.cancel()
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        rankingJob = viewModelScope.launch {
            repository.getRanking(category)
                .onSuccess { page ->
                    pages[category] = page
                    if (_uiState.value.category == category) applyPage(page)
                }
                .onFailure { error -> _uiState.update { it.copy(isLoading = false, errorMessage = rankingError(error)) } }
        }
    }

    private fun applyPage(page: RankingPage) {
        _uiState.update {
            it.copy(
                periodLabel = "Ano de ${page.year}",
                updatedLabel = updatedLabel(page.updatedAt),
                entries = page.entries,
                myPosition = page.myPosition,
                myPoints = page.myPoints,
                isLoading = false,
                errorMessage = null
            )
        }
    }

    /** "Atualizado em 24/10 às 10:15" no fuso do aparelho; data inválida é ignorada. */
    private fun updatedLabel(updatedAt: String?): String? =
        updatedAt?.let { raw ->
            runCatching {
                val local = OffsetDateTime.parse(raw).atZoneSameInstant(zone)
                "Atualizado em ${local.format(DAY_FORMAT)} às ${local.format(TIME_FORMAT)}"
            }.getOrNull()
        }

    /** Conquistas são sempre da própria pessoa, independente do toggle de público. */
    private fun loadAchievements() {
        achievementsCache?.let { cached ->
            return _uiState.update { it.copy(achievements = cached, achievementsError = null) }
        }
        _uiState.update { it.copy(achievements = null, isAchievementsLoading = true, achievementsError = null) }
        viewModelScope.launch {
            repository.getAchievements(ownCategory)
                .onSuccess { summary ->
                    achievementsCache = summary
                    _uiState.update { it.copy(achievements = summary, isAchievementsLoading = false) }
                }
                .onFailure {
                    _uiState.update { it.copy(isAchievementsLoading = false, achievementsError = ACHIEVEMENTS_ERROR) }
                }
        }
    }

    private companion object {
        /** Textos da tela "Mobile / Erro" do Figma. */
        const val RANKING_ERROR = "Sem conexão"
        /** API no ar, mas sem ranking publicado ou com o banco indisponível (HTTP 503). */
        const val RANKING_UNAVAILABLE = "Ranking indisponível no momento"
        const val ACHIEVEMENTS_ERROR = "Conquistas indisponíveis para este perfil no momento."

        val DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM")
        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        fun rankingError(error: Throwable): String =
            if (error is ApiException && error.code == 503) RANKING_UNAVAILABLE else RANKING_ERROR
    }
}
