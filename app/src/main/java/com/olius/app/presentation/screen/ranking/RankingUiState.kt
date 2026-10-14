package com.olius.app.presentation.screen.ranking

enum class RankingCategory(val label: String) {
    ESTABLISHMENT("Estabelecimento"),
    CITIZEN("Cidadão")
}

enum class RankingPeriod(val label: String) {
    MONTH("Este Mês"),
    YEAR("Este Ano"),
    ALL_TIME("Geral")
}

enum class RankingTab { COMPETITIONS, ACHIEVEMENTS }

data class RankingEntry(
    val position: Int,
    val name: String,
    val city: String,
    val liters: Double,
    val isCurrentUser: Boolean = false
) {
    /**
     * Iniciais do avatar (ref_ranking.png): primeira letra da primeira e da
     * última palavra ("Dogão do Adriano" -> "DA"). Nome de uma palavra só usa
     * a próxima maiúscula ("ToKonFome" -> "TK") ou a primeira consoante
     * ("Reuters" -> "RT").
     */
    val initials: String
        get() {
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

data class RankingUiState(
    val category: RankingCategory = RankingCategory.ESTABLISHMENT,
    val period: RankingPeriod = RankingPeriod.MONTH,
    val tab: RankingTab = RankingTab.COMPETITIONS,
    val periodLabel: String = "",
    val entries: List<RankingEntry> = emptyList()
) {
    /** 1º, 2º e 3º lugares — o pódio. */
    val podium: List<RankingEntry>
        get() = entries.take(3)

    val currentUser: RankingEntry?
        get() = entries.firstOrNull { it.isCurrentUser }
}
