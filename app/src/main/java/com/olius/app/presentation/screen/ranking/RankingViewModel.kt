package com.olius.app.presentation.screen.ranking

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val PT_BR: Locale = Locale.forLanguageTag("pt-BR")

/**
 * Ranking de Recicladores (ref_ranking.png / ref_ranking_2.png /
 * ref_ranking_b2b.png). Os participantes são mockados até existir o
 * endpoint do ranking — a troca é só no [participants].
 */
class RankingViewModel(
    private val today: () -> LocalDate = LocalDate::now
) : ViewModel() {

    private val _uiState = MutableStateFlow(buildState(RankingUiState()))
    val uiState: StateFlow<RankingUiState> = _uiState.asStateFlow()

    fun onCategorySelected(category: RankingCategory) {
        _uiState.update { buildState(it.copy(category = category)) }
    }

    fun onPeriodSelected(period: RankingPeriod) {
        _uiState.update { buildState(it.copy(period = period)) }
    }

    fun onTabSelected(tab: RankingTab) {
        _uiState.update { it.copy(tab = tab) }
    }

    private fun buildState(state: RankingUiState): RankingUiState {
        val entries = participants.getValue(state.category)
            .map { it to it.litersFor(state.period) }
            .sortedByDescending { (_, liters) -> liters }
            .mapIndexed { index, (participant, liters) ->
                RankingEntry(
                    position = index + 1,
                    name = participant.name,
                    city = participant.city,
                    liters = liters,
                    isCurrentUser = participant.isCurrentUser
                )
            }
        return state.copy(entries = entries, periodLabel = periodLabel(state.period))
    }

    private fun periodLabel(period: RankingPeriod): String {
        val date = today()
        return when (period) {
            RankingPeriod.MONTH -> {
                val month = date.month.getDisplayName(TextStyle.FULL, PT_BR).replaceFirstChar { it.titlecase(PT_BR) }
                "$month de ${date.year}"
            }
            RankingPeriod.YEAR -> "Ano de ${date.year}"
            RankingPeriod.ALL_TIME -> "Desde o início"
        }
    }
}

private class Participant(
    val name: String,
    val city: String,
    private val monthLiters: Double,
    private val yearLiters: Double,
    private val allTimeLiters: Double,
    val isCurrentUser: Boolean = false
) {
    fun litersFor(period: RankingPeriod): Double = when (period) {
        RankingPeriod.MONTH -> monthLiters
        RankingPeriod.YEAR -> yearLiters
        RankingPeriod.ALL_TIME -> allTimeLiters
    }
}

private val participants: Map<RankingCategory, List<Participant>> = mapOf(
    RankingCategory.ESTABLISHMENT to listOf(
        Participant("ToKonFome", "Itaquaquecetuba", 120.0, 890.0, 2_140.0, isCurrentUser = true),
        Participant("Dogão do Adriano", "Osasco", 90.0, 1_020.0, 2_870.0),
        Participant("Reuters", "Perus", 60.0, 640.0, 1_530.0),
        Participant("Mineiro", "Kemel Addans 1", 52.0, 410.0, 980.0),
        Participant("Burger King", "Amparo", 50.0, 575.0, 1_760.0)
    ),
    RankingCategory.CITIZEN to listOf(
        Participant("Maria Souza", "São Paulo", 98.0, 702.0, 1_310.0),
        Participant("João Pedro", "Osasco", 85.0, 655.0, 1_498.0),
        Participant("Ana Lima", "Guarulhos", 71.0, 540.0, 1_020.0),
        Participant("Naldo Canal", "Itaquaquecetuba", 64.0, 274.2, 690.0, isCurrentUser = true),
        Participant("Carla Dias", "Barueri", 40.0, 380.0, 725.0)
    )
)
