package com.olius.app.domain.repository

import com.olius.app.domain.entity.AchievementsSummary
import com.olius.app.domain.entity.RankingCategory
import com.olius.app.domain.entity.RankingPage

/**
 * Ranking (API Redis) e conquistas. Quem é "a pessoa logada" fica dentro da
 * implementação — a tela não passa id.
 */
interface RankingRepository {

    /** Ranking anual por pontos: top N e a posição da pessoa ([RankingPage.myPosition]). */
    suspend fun getRanking(category: RankingCategory): Result<RankingPage>

    /** Nível de sustentabilidade + conquistas. Não vêm da API Redis: vão ficar no Room do app. */
    suspend fun getAchievements(category: RankingCategory): Result<AchievementsSummary>
}
