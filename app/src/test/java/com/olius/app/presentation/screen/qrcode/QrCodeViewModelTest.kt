package com.olius.app.presentation.screen.qrcode

import com.olius.app.MainDispatcherRule
import com.olius.app.data.local.InMemorySessionStore
import com.olius.app.data.repository.MockUserProfileRepository
import com.olius.app.domain.entity.ProfileType
import com.olius.app.domain.entity.UserProfile
import com.olius.app.domain.repository.UserProfileRepository
import com.olius.app.domain.usecases.BuildQrCodeContentUseCase
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class QrCodeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val store = InMemorySessionStore()
    private val build = BuildQrCodeContentUseCase(FORMS)

    private val naldo = UserProfile(
        id = "u-1",
        name = "Naldo Canal",
        email = "lois@gmail.com",
        profileType = ProfileType.CITIZEN,
        qrToken = "qr-123"
    )
    private val restaurante = UserProfile(
        id = "u-2",
        name = "Restaurante do Seu Zé",
        email = "ze@restaurante.com",
        profileType = ProfileType.ESTABLISHMENT,
        qrToken = "qr-456"
    )

    /** Perfil salvo em [store]; o GET /users/me devolve o que [fetched] mandar. */
    private fun profiles(fetched: suspend () -> Result<UserProfile>): UserProfileRepository =
        object : UserProfileRepository by MockUserProfileRepository(store) {
            override suspend fun fetchMyProfile() = fetched()
        }

    @Test
    fun `QR leva ao Forms com o qrToken do perfil`() {
        val state = QrCodeViewModel(profiles { Result.success(naldo) }, build).uiState.value

        assertFalse(state.isLoading)
        assertEquals("${FORMS}?qrToken=qr-123", state.content)
    }

    @Test
    fun `sem perfil salvo espera a Core`() {
        val response = CompletableDeferred<Result<UserProfile>>()
        val viewModel = QrCodeViewModel(profiles { response.await() }, build)

        assertTrue(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.content)

        response.complete(Result.success(naldo))
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("${FORMS}?qrToken=qr-123", viewModel.uiState.value.content)
    }

    @Test
    fun `mostra o perfil salvo enquanto confere com a Core`() {
        store.save(naldo)
        val response = CompletableDeferred<Result<UserProfile>>()
        val viewModel = QrCodeViewModel(profiles { response.await() }, build)

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("${FORMS}?qrToken=qr-123", viewModel.uiState.value.content)
    }

    @Test
    fun `token da Core substitui o salvo`() {
        store.save(naldo)

        val state = QrCodeViewModel(profiles { Result.success(restaurante) }, build).uiState.value

        assertEquals("${FORMS}?qrToken=qr-456", state.content)
    }

    @Test
    fun `falha na Core mantem o QR do perfil salvo`() {
        store.save(naldo)

        val state = QrCodeViewModel(profiles { Result.failure(IOException("sem rede")) }, build).uiState.value

        assertEquals("${FORMS}?qrToken=qr-123", state.content)
    }

    @Test
    fun `falha sem perfil salvo termina sem QR`() {
        val state = QrCodeViewModel(profiles { Result.failure(IOException("sem rede")) }, build).uiState.value

        assertFalse(state.isLoading)
        assertNull(state.content)
    }

    @Test
    fun `perfil sem qrToken nao gera QR`() {
        store.save(naldo)

        val state = QrCodeViewModel(profiles { Result.success(naldo.copy(qrToken = " ")) }, build).uiState.value

        assertFalse(state.isLoading)
        assertNull(state.content)
    }

    @Test
    fun `tentar de novo busca o perfil outra vez`() {
        var result: Result<UserProfile> = Result.failure(IOException("sem rede"))
        val viewModel = QrCodeViewModel(profiles { result }, build)
        assertNull(viewModel.uiState.value.content)

        result = Result.success(naldo)
        viewModel.load()

        assertEquals("${FORMS}?qrToken=qr-123", viewModel.uiState.value.content)
    }

    @Test
    fun `outra conta mostra o QR dela`() {
        store.save(naldo)
        QrCodeViewModel(profiles { Result.success(naldo) }, build)

        store.clear()
        store.save(restaurante)
        val state = QrCodeViewModel(profiles { Result.failure(IOException("sem rede")) }, build).uiState.value

        assertEquals("${FORMS}?qrToken=qr-456", state.content)
    }

    private companion object {
        const val FORMS = "https://olius-frontend-forms-segundo.onrender.com/"
    }
}
