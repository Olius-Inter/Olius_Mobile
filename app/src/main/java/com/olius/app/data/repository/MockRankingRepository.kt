package com.olius.app.data.repository

import com.olius.app.domain.entity.Achievement
import com.olius.app.domain.entity.AchievementsSummary
import com.olius.app.domain.entity.RankingCategory
import com.olius.app.domain.entity.RankingEntry
import com.olius.app.domain.entity.RankingPage
import com.olius.app.domain.entity.SustainabilityLevel
import com.olius.app.domain.repository.RankingRepository
import java.time.LocalDate

/**
 * Dados das referências (ref_ranking.png / ref_ranking_2.png / ref_ranking_b2b.png)
 * para rodar sem a API (ApiConfig.USE_MOCK_RANKING = true) e nos testes.
 * Mesmo formato que a API Redis devolve: ordenado por pontos, com posição densa (1, 1, 2).
 */
class MockRankingRepository(
    private val today: () -> LocalDate = LocalDate::now
) : RankingRepository {

    override suspend fun getRanking(category: RankingCategory): Result<RankingPage> {
        val sorted = participants.getValue(category).sortedWith(compareByDescending<Participant> { it.points }.thenBy { it.name })
        var position = 0
        var previous: Long? = null
        val entries = sorted.map { participant ->
            if (participant.points != previous) {
                position++
                previous = participant.points
            }
            RankingEntry(
                position = position,
                name = participant.name,
                city = participant.city,
                points = participant.points,
                isCurrentUser = participant.isCurrentUser
            )
        }
        val mine = entries.firstOrNull { it.isCurrentUser }
        return Result.success(RankingPage(today().year, entries, mine?.position, mine?.points))
    }

    override suspend fun getAchievements(category: RankingCategory): Result<AchievementsSummary> {
        val total = demoTotalLiters.getValue(category)
        val reached = catalog.count { total >= it.second }
        val next = catalog.getOrNull(reached)
        val base = catalog.getOrNull(reached - 1)?.second ?: 0.0
        val level = SustainabilityLevel(
            level = reached,
            name = catalog.getOrNull(reached - 1)?.first,
            totalLiters = total,
            targetLiters = next?.second ?: catalog.last().second,
            progress = next?.let { ((total - base) / (it.second - base)).toFloat() } ?: 1f,
            isMax = next == null,
            configured = true
        )
        val achievements = catalog.mapIndexed { index, (title, liters) ->
            Achievement(
                id = "n-0${index + 1}",
                title = title,
                description = "${liters.toInt()} litros acumulados.",
                badgeImageUrl = null,
                requiredLiters = liters,
                unlocked = total >= liters,
                unlockedAt = null,
                rarity = null
            )
        }
        return Result.success(AchievementsSummary(level, achievements))
    }
}

/** Catálogo DEMO de níveis por litros (os oficiais estão em certificate_level). */
private val catalog = listOf("Semente" to 50.0, "Broto" to 100.0, "Árvore" to 250.0, "Floresta" to 500.0)

/** Litros acumulados da pessoa logada em cada perfil (demonstração dos níveis). */
private val demoTotalLiters = mapOf(RankingCategory.ESTABLISHMENT to 2_140.0, RankingCategory.CITIZEN to 690.0)

private class Participant(val name: String, val city: String, val points: Long, val isCurrentUser: Boolean = false)

private val participants: Map<RankingCategory, List<Participant>> = mapOf(
    RankingCategory.ESTABLISHMENT to listOf(
        Participant("ToKonFome", "Itaquaquecetuba", 120, isCurrentUser = true),
        Participant("Dogão do Adriano", "Osasco", 90),
        Participant("Reuters", "Perus", 60),
        Participant("Mineiro", "Kemel Addans 1", 52),
        Participant("Burger King", "Amparo", 50)
    ),
    RankingCategory.CITIZEN to listOf(
        Participant("Maria Souza", "", 98),
        Participant("João Pedro", "", 85),
        Participant("Ana Lima", "", 85),
        Participant("Naldo Canal", "", 64, isCurrentUser = true),
        Participant("Carla Dias", "", 40)
    )
)
