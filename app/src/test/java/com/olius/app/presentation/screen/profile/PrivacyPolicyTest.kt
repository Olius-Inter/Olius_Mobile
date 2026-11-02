package com.olius.app.presentation.screen.profile

import com.olius.app.data.remote.ApiConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyPolicyTest {
    @Test
    fun `segue as secoes do Figma e termina com os direitos`() {
        val titles = PRIVACY_POLICY_SECTIONS.map { it.title }

        assertEquals(
            listOf(
                "1. Dados que coletamos",
                "2. Como usamos seus dados",
                "3. Compartilhamento dos seus dados",
                "4. Seus direitos"
            ),
            titles
        )
        assertTrue(PRIVACY_POLICY_SECTIONS.all { it.paragraphs.isNotEmpty() })
    }

    @Test
    fun `informa o e-mail oficial para pedidos da LGPD`() {
        val text = PRIVACY_POLICY_SECTIONS.flatMap { it.paragraphs }.joinToString(" ")

        assertTrue(ApiConfig.SUPPORT_EMAIL in text)
    }
}
