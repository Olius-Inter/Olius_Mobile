package com.olius.app.presentation.screen.home

import org.junit.Assert.assertEquals
import org.junit.Test

class GreetingTest {
    @Test
    fun `saudacao segue a hora local`() {
        assertEquals("Boa noite!", greetingSubtitle(2))
        assertEquals("Bom dia!", greetingSubtitle(5))
        assertEquals("Bom dia!", greetingSubtitle(11))
        assertEquals("Boa tarde!", greetingSubtitle(12))
        assertEquals("Boa tarde!", greetingSubtitle(17))
        assertEquals("Boa noite!", greetingSubtitle(18))
        assertEquals("Boa noite!", greetingSubtitle(23))
    }
}
