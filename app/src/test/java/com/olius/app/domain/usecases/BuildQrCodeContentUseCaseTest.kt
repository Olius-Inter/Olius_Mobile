package com.olius.app.domain.usecases

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BuildQrCodeContentUseCaseTest {

    private val build = BuildQrCodeContentUseCase()

    @Test
    fun `QR guarda o qrToken exato, sem URL em volta`() {
        assertEquals("a1b2c3", build("a1b2c3"))
    }

    @Test
    fun `sem token nao ha QR`() {
        assertNull(build(null))
        assertNull(build("  "))
    }
}
