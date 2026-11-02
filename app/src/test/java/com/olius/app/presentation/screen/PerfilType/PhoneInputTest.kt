package com.olius.app.presentation.screen.PerfilType

import androidx.compose.ui.text.AnnotatedString
import com.olius.app.domain.entity.PhoneCountry
import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneInputTest {

    private val countries = listOf(
        PhoneCountry("DE", "Alemanha", 49),
        PhoneCountry("BR", "Brasil", 55),
        PhoneCountry("US", "Estados Unidos", 1),
        PhoneCountry("IS", "Islândia", 354)
    )

    @Test
    fun `busca por nome sem acento, ISO e DDI`() {
        assertEquals(listOf("IS"), filterPhoneCountries(countries, "islandia").map { it.isoCode })
        assertEquals(listOf("BR"), filterPhoneCountries(countries, "br").map { it.isoCode })
        assertEquals(listOf("BR"), filterPhoneCountries(countries, "+55").map { it.isoCode })
        assertEquals(countries, filterPhoneCountries(countries, "  "))
    }

    @Test
    fun `bandeira sai do codigo ISO`() {
        assertEquals("🇧🇷", PhoneCountry.BRAZIL.flag)
        assertEquals("+55", PhoneCountry.BRAZIL.dialCodeLabel)
    }

    @Test
    fun `mascara visual mapeia o cursor entre digitos e texto formatado`() {
        val transformed = PhoneMaskTransformation("11 99999-8888").filter(AnnotatedString("11999998888"))

        assertEquals("11 99999-8888", transformed.text.text)
        assertEquals(0, transformed.offsetMapping.originalToTransformed(0))
        assertEquals(4, transformed.offsetMapping.originalToTransformed(3)) // depois do 1º "9"
        assertEquals(13, transformed.offsetMapping.originalToTransformed(11))
        assertEquals(3, transformed.offsetMapping.transformedToOriginal(4))
    }

    @Test
    fun `mascara desatualizada mostra os digitos puros`() {
        val transformed = PhoneMaskTransformation("11 9").filter(AnnotatedString("1199"))

        assertEquals("1199", transformed.text.text)
    }
}
