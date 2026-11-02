package com.olius.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibPhoneNumberRepositoryTest {

    private val repository = TestPhoneNumbers.repository()

    @Test
    fun `lista de paises tem DDI e nome em portugues`() {
        val countries = repository.countries()
        val brazil = countries.first { it.isoCode == "BR" }
        val usa = countries.first { it.isoCode == "US" }

        assertTrue(countries.size > 200)
        assertEquals(55, brazil.dialCode)
        assertEquals("🇧🇷", brazil.flag)
        assertEquals(1, usa.dialCode)
        assertEquals("Alemanha", countries.first { it.isoCode == "DE" }.name)
    }

    @Test
    fun `mascara segue o pais enquanto digita`() {
        assertEquals("11 99999-8888", repository.formatAsYouType("BR", "11999998888"))
        assertEquals("(201) 555-0123", repository.formatAsYouType("US", "2015550123"))
    }

    @Test
    fun `valida e devolve E164 so com digitos`() {
        assertEquals("5511999998888", repository.toE164Digits("BR", "11999998888"))
        assertEquals("12015550123", repository.toE164Digits("US", "2015550123"))
        assertEquals("351912345678", repository.toE164Digits("PT", "912345678"))
    }

    @Test
    fun `numero invalido ou de outro pais devolve null`() {
        assertNull(repository.toE164Digits("BR", "1199999"))
        assertNull(repository.toE164Digits("BR", "00999998888"))
        assertNull(repository.toE164Digits("US", "11999998888"))
        assertNull(repository.toE164Digits("BR", ""))
    }
}
