package com.olius.app.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRulesTest {
    @Test
    fun `senha segue a regra da Core`() {
        assertEquals(PasswordRule.entries.toList(), PasswordRule.unmet(""))
        assertEquals(listOf(PasswordRule.LENGTH), PasswordRule.unmet("Ab1!"))
        assertEquals(listOf(PasswordRule.UPPERCASE), PasswordRule.unmet("senha@12345"))
        assertEquals(listOf(PasswordRule.DIGIT), PasswordRule.unmet("Senha@abcde"))
        assertEquals(listOf(PasswordRule.SYMBOL), PasswordRule.unmet("Senha12345"))
        assertEquals(listOf(PasswordRule.SYMBOL), PasswordRule.unmet("Senha 12345"))
        assertTrue(PasswordRule.unmet("Senha@12345").isEmpty())
        assertEquals(listOf(PasswordRule.LENGTH), PasswordRule.unmet("A1!" + "a".repeat(126)))
    }

    @Test
    fun `cpf e cnpj conferem os digitos verificadores`() {
        assertTrue(BrazilianDocuments.isValidCpf("52998224725"))
        assertFalse(BrazilianDocuments.isValidCpf("52998224724"))
        assertFalse(BrazilianDocuments.isValidCpf("11111111111"))
        assertFalse(BrazilianDocuments.isValidCpf("5299822472"))

        assertTrue(BrazilianDocuments.isValidCnpj("11222333000181"))
        assertFalse(BrazilianDocuments.isValidCnpj("11222333000180"))
        assertFalse(BrazilianDocuments.isValidCnpj("00000000000000"))
    }

    @Test
    fun `mascaras acompanham a digitacao`() {
        assertEquals("529.982.247-25", BrazilianDocuments.formatCpf("52998224725"))
        assertEquals("529.98", BrazilianDocuments.formatCpf("52998"))
        assertEquals("11.222.333/0001-81", BrazilianDocuments.formatCnpj("11222333000181"))
        assertEquals("", BrazilianDocuments.formatCnpj(""))
    }
}
