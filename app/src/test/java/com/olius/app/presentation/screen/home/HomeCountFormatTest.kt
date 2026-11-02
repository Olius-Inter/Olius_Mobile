package com.olius.app.presentation.screen.home

import org.junit.Assert.assertEquals
import org.junit.Test

/** Formato do número durante a contagem dos cards da Home. */
class HomeCountFormatTest {
    @Test
    fun `no fim da contagem o texto e igual ao formato normal`() {
        listOf(0.0, 50.0, 7.5, 50.3, 999.0).forEach { value ->
            assertEquals(formatNumber(value), formatCounting(value, value, compact = false))
            assertEquals(formatCompact(value), formatCounting(value, value, compact = true))
        }
        assertEquals(formatCompact(625_900.0), formatCounting(625_900.0, 625_900.0, compact = true))
    }

    @Test
    fun `durante a contagem mantem as casas decimais do valor final`() {
        assertEquals("12", formatCounting(12.4, 50.0, compact = false))
        assertEquals("12.0", formatCounting(12.0, 50.3, compact = false))
    }

    @Test
    fun `agua em milhares mostra o K desde o comeco`() {
        assertEquals("0.0K", formatCounting(0.0, 625_900.0, compact = true))
        assertEquals("312.9K", formatCounting(312_900.0, 625_900.0, compact = true))
    }
}
