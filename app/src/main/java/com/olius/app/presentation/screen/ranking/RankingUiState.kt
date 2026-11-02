package com.olius.app.presentation.screen.ranking

import com.olius.app.domain.entity.AchievementsSummary
import com.olius.app.domain.entity.RankingCategory
import com.olius.app.domain.entity.RankingEntry

enum class RankingTab { COMPETITIONS, ACHIEVEMENTS }

data class RankingUiState(
    val category: RankingCategory = RankingCategory.ESTABLISHMENT,
    val tab: RankingTab = RankingTab.COMPETITIONS,
    /** "Ano de 2026": o ranking é sempre do ano corrente. */
    val periodLabel: String = "",
    /** "Atualizado em 24/10 às 10:15"; null quando a API não informa. */
    val updatedLabel: String? = null,
    val entries: List<RankingEntry> = emptyList(),
    /** Posição da pessoa vinda da API — vale mesmo quando ela está fora do top N. */
    val myPosition: Int? = null,
    val myPoints: Long? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val achievements: AchievementsSummary? = null,
    val isAchievementsLoading: Boolean = false,
    val achievementsError: String? = null
) {
    /** As três primeiras linhas — o pódio. Com empate, duas podem ter a mesma posição. */
    val podium: List<RankingEntry>
        get() = entries.take(3)

    val currentUser: RankingEntry?
        get() = entries.firstOrNull { it.isCurrentUser }
            ?: if (myPosition != null && myPoints != null) RankingEntry(myPosition, "", "", myPoints, isCurrentUser = true) else null
}
