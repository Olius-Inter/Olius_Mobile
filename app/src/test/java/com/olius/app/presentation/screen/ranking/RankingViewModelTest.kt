package com.olius.app.presentation.screen.ranking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RankingViewModelTest {

    private val viewModel = RankingViewModel(today = { LocalDate.of(2026, 8, 15) })
    private val state get() = viewModel.uiState.value

    @Test
    fun `comeca no ranking de estabelecimentos do mes`() {
        assertEquals(RankingCategory.ESTABLISHMENT, state.category)
        assertEquals(RankingPeriod.MONTH, state.period)
        assertEquals(RankingTab.COMPETITIONS, state.tab)
        assertEquals("Agosto de 2026", state.periodLabel)
    }

    @Test
    fun `lista fica ordenada por litros com as posicoes certas`() {
        val entries = state.entries

        assertEquals(entries.sortedByDescending { it.liters }, entries)
        assertEquals((1..entries.size).toList(), entries.map { it.position })
        assertEquals(entries.take(3), state.podium)
    }

    @Test
    fun `posicao da pessoa vem do proprio ranking`() {
        assertEquals("ToKonFome", state.currentUser?.name)
        assertEquals(1, state.currentUser?.position)

        viewModel.onCategorySelected(RankingCategory.CITIZEN)
        assertEquals("Naldo Canal", state.currentUser?.name)
        assertEquals(4, state.currentUser?.position)
    }

    @Test
    fun `trocar o periodo reordena o ranking e muda o rotulo`() {
        viewModel.onPeriodSelected(RankingPeriod.YEAR)
        assertEquals("Ano de 2026", state.periodLabel)
        assertEquals("Dogão do Adriano", state.entries.first().name)

        viewModel.onPeriodSelected(RankingPeriod.ALL_TIME)
        assertEquals("Desde o início", state.periodLabel)
        assertEquals(2_870.0, state.entries.first().liters, 0.0)
    }

    @Test
    fun `trocar de aba mantem categoria e periodo`() {
        viewModel.onPeriodSelected(RankingPeriod.YEAR)
        viewModel.onTabSelected(RankingTab.ACHIEVEMENTS)

        assertEquals(RankingTab.ACHIEVEMENTS, state.tab)
        assertEquals(RankingPeriod.YEAR, state.period)
    }

    @Test
    fun `iniciais do avatar seguem a referencia`() {
        fun initials(name: String) = RankingEntry(1, name, "", 0.0).initials

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
}
