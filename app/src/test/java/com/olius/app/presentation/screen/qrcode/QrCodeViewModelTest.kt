package com.olius.app.presentation.screen.qrcode

import com.olius.app.MainDispatcherRule
import com.olius.app.data.local.InMemorySessionStore
import com.olius.app.data.repository.MockUserProfileRepository
import com.olius.app.domain.entity.ProfileType
import com.olius.app.domain.entity.UserProfile
import com.olius.app.domain.repository.UserProfileRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class QrCodeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val store = InMemorySessionStore()

    private val naldo = UserProfile(
        id = "u-1",
        name = "Naldo Canal",
        email = "lois@gmail.com",
        profileType = ProfileType.CITIZEN,
        qrToken = "qr-123"
    )

    /** Sem perfil no cache; a "rede" responde [fetched] no GET /users/me. */
    private fun network(fetched: Result<UserProfile>): UserProfileRepository =
        object : UserProfileRepository by MockUserProfileRepository(store) {
            override fun cachedProfile(): UserProfile? = null
            override suspend fun fetchMyProfile() = fetched
        }

    @Test
    fun `usa o qrToken do cache sem ir na rede`() {
        store.save(naldo)
        var fetches = 0
        val profiles = object : UserProfileRepository by MockUserProfileRepository(store) {
            override suspend fun fetchMyProfile(): Result<UserProfile> {
                fetches++
                return Result.success(naldo)
            }
        }

        val state = QrCodeViewModel(profiles).uiState.value

        assertFalse(state.isLoading)
        assertEquals("qr-123", state.content)
        assertEquals(0, fetches)
    }

    @Test
    fun `sem cache busca o perfil na Core`() {
        val state = QrCodeViewModel(network(Result.success(naldo))).uiState.value

        assertFalse(state.isLoading)
        assertEquals("qr-123", state.content)
    }

    @Test
    fun `falha na rede termina o carregamento sem QR`() {
        val state = QrCodeViewModel(network(Result.failure(IOException("sem rede")))).uiState.value

        assertFalse(state.isLoading)
        assertNull(state.content)
    }

    @Test
    fun `qrToken em branco nao gera QR`() {
        val state = QrCodeViewModel(network(Result.success(naldo.copy(qrToken = " ")))).uiState.value

        assertFalse(state.isLoading)
        assertNull(state.content)
    }
}
