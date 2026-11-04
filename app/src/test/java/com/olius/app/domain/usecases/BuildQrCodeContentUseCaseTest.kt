package com.olius.app.domain.usecases

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BuildQrCodeContentUseCaseTest {
    private val build = BuildQrCodeContentUseCase("https://olius-frontend-forms-segundo.onrender.com/")

    @Test
    fun `QR leva ao Forms so com o qrToken`() {
        assertEquals(
            "https://olius-frontend-forms-segundo.onrender.com/?qrToken=Ab3_x-9Z",
            build("Ab3_x-9Z")
        )
    }

    @Test
    fun `token com caractere especial vai codificado`() {
        assertEquals(
            "https://olius-frontend-forms-segundo.onrender.com/?qrToken=a%2Bb%26fluxo%3Dx",
            build("a+b&fluxo=x")
        )
    }

    @Test
    fun `espacos em volta do token saem`() {
        assertEquals("https://olius-frontend-forms-segundo.onrender.com/?qrToken=abc", build("  abc "))
    }

    @Test
    fun `sem token nao ha QR`() {
        assertNull(build(null))
        assertNull(build("  "))
    }
}
