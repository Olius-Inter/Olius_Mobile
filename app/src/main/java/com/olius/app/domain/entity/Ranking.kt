package com.olius.app.domain.entity

/** Toggle "Estabelecimento | Cidadão". [apiValue] = parâmetro `category` da API de ranking. */
enum class RankingCategory(val label: String, val apiValue: String) {
    ESTABLISHMENT("Estabelecimento", "establishment"),
    CITIZEN("Cidadão", "citizen")
}

/**
 * Uma linha do ranking anual. [position] é densa: empatados dividem a posição
 * (1, 1, 2). [points] é a pontuação do ano, não litros.
 */
data class RankingEntry(
    val position: Int,
    val name: String,
    val city: String,
    val points: Long,
    val isCurrentUser: Boolean = false,
    /** Iniciais que a API já manda (`iniciais`); null = calcula pelo nome. */
    private val serverInitials: String? = null
) {
    /**
     * Iniciais do avatar (ref_ranking.png): primeira letra da primeira e da
     * última palavra ("Dogão do Adriano" -> "DA"). Nome de uma palavra só usa
     * a próxima maiúscula ("ToKonFome" -> "TK") ou a primeira consoante
     * ("Reuters" -> "RT").
     */
    val initials: String
        get() {
            if (!serverInitials.isNullOrBlank()) return serverInitials
            val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            if (words.isEmpty()) return ""
            if (words.size > 1) return "${words.first().first()}${words.last().first()}".uppercase()
            val word = words.first()
            val second = word.drop(1).firstOrNull { it.isUpperCase() }
                ?: word.drop(1).firstOrNull { it.isLetter() && it.lowercaseChar() !in VOWELS }
                ?: word.getOrNull(1)
            return "${word.first()}${second ?: ""}".uppercase()
        }

    private companion object {
        const val VOWELS = "aeiouáéíóúâêôãõà"
    }
}

/**
 * Uma página do ranking (RankingResponse): top N do [year] + a posição da
 * pessoa, mesmo fora do top. [updatedAt] é o último rebuild (ISO-8601): o
 * ranking não é instantâneo.
 */
data class RankingPage(
    val year: Int,
    val entries: List<RankingEntry>,
    val myPosition: Int? = null,
    val myPoints: Long? = null,
    val updatedAt: String? = null
)

/** Card "nível de sustentabilidade" (NivelSustentabilidade da API). */
data class SustainabilityLevel(
    val level: Int,
    val name: String?,
    val totalLiters: Double,
    val targetLiters: Double?,
    val progress: Float,
    val isMax: Boolean,
    val configured: Boolean
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String?,
    val badgeImageUrl: String?,
    val requiredLiters: Double,
    val unlocked: Boolean,
    val unlockedAt: String?,
    /** Fração (0..1) de participantes que já têm a conquista. */
    val rarity: Float?
)

data class AchievementsSummary(
    val level: SustainabilityLevel,
    val achievements: List<Achievement>
)
