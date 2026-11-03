package com.olius.app.presentation.screen.profile

import com.olius.app.MainDispatcherRule
import com.olius.app.data.local.InMemorySessionStore
import com.olius.app.data.repository.MockUserProfileRepository
import com.olius.app.data.repository.TestPhoneNumbers
import com.olius.app.domain.entity.InvalidProfileException
import com.olius.app.domain.entity.ProfileConflictException
import com.olius.app.domain.entity.ProfileEditUnavailableException
import com.olius.app.domain.entity.ProfileType
import com.olius.app.domain.entity.ProfileUpdate
import com.olius.app.domain.entity.UserProfile
import com.olius.app.domain.repository.UserProfileRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class EditProfileViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val store = InMemorySessionStore().apply {
        save(UserProfile("u-1", "Ana Lima", "ana@olius.app", ProfileType.CITIZEN, phone = "5511999998888"))
    }
    private val phones = TestPhoneNumbers.repository()
    private var lastUpdate: ProfileUpdate? = null

    /** Repositório do aparelho; [failure] simula a resposta da Core no PATCH. */
    private fun profiles(failure: Throwable? = null): UserProfileRepository {
        val local = MockUserProfileRepository(store)
        return object : UserProfileRepository by local {
            override suspend fun updateMyProfile(update: ProfileUpdate): Result<UserProfile> {
                lastUpdate = update
                return if (failure != null) Result.failure(failure) else local.updateMyProfile(update)
            }
        }
    }

    private fun viewModel(failure: Throwable? = null) = EditProfileViewModel(profiles(failure), phones)

    @Test
    fun `abre com nome, e-mail e telefone do cache separando o DDI`() {
        val state = viewModel().uiState.value

        assertEquals("Ana Lima", state.name)
        assertEquals("ana@olius.app", state.email)
        assertEquals("BR", state.phoneCountry.isoCode)
        assertEquals("11999998888", state.phone)
        assertEquals("5511999998888", state.phoneE164)
    }

    @Test
    fun `alterar envia nome, e-mail e telefone e atualiza o cache`() {
        val viewModel = viewModel()
        viewModel.onNameChange("Ana Souza")
        viewModel.onEmailChange(" ana.souza@olius.app ")
        viewModel.onPhoneChange("21988887777")

        viewModel.onSaveClick()

        assertEquals(ProfileUpdate("Ana Souza", "ana.souza@olius.app", "5521988887777"), lastUpdate)
        assertTrue(viewModel.uiState.value.saved)
        assertEquals("Ana Souza", store.load()?.name)
        assertEquals("5521988887777", store.load()?.phone)
    }

    @Test
    fun `telefone apagado vai como null`() {
        val viewModel = viewModel()
        viewModel.onPhoneChange("")

        viewModel.onSaveClick()

        assertNull(lastUpdate?.telephone)
    }

    @Test
    fun `campos invalidos nao chamam a Core`() {
        val viewModel = viewModel()
        viewModel.onNameChange(" ")
        viewModel.onEmailChange("ana@")
        viewModel.onPhoneChange("123")

        viewModel.onSaveClick()

        val state = viewModel.uiState.value
        assertNotNull(state.nameError)
        assertNotNull(state.emailError)
        assertNotNull(state.phoneError)
        assertNull(lastUpdate)
        assertFalse(state.saved)
    }

    @Test
    fun `nome acima de 150 letras e recusado`() {
        val viewModel = viewModel()
        viewModel.onNameChange("a".repeat(151))

        viewModel.onSaveClick()

        assertNotNull(viewModel.uiState.value.nameError)
    }

    @Test
    fun `rota inexistente na Core mostra mensagem clara e nao mexe no cache`() {
        val viewModel = viewModel(ProfileEditUnavailableException())
        viewModel.onNameChange("Outro Nome")

        viewModel.onSaveClick()

        val state = viewModel.uiState.value
        assertEquals("Edição de perfil ainda não disponível no servidor", state.errorMessage)
        assertFalse(state.saved)
        assertFalse(state.isSaving)
        assertEquals("Ana Lima", store.load()?.name)
    }

    @Test
    fun `sem rede abre o Sem conexao`() {
        val viewModel = viewModel(IOException("sem rede"))

        viewModel.onSaveClick()

        assertTrue(viewModel.uiState.value.isOffline)
        assertEquals(EditProfileViewModel.MESSAGE_OFFLINE, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `e-mail em uso e campos recusados pela Core`() {
        val conflict = viewModel(ProfileConflictException())
        conflict.onSaveClick()
        assertNotNull(conflict.uiState.value.errorMessage)

        val invalid = viewModel(InvalidProfileException(listOf("email")))
        invalid.onSaveClick()
        assertEquals("E-mail inválido", invalid.uiState.value.emailError)
        assertNull(invalid.uiState.value.nameError)
    }

    @Test
    fun `trocar o pais refaz a validacao do telefone`() {
        val viewModel = viewModel()
        val portugal = viewModel.phoneCountries.first { it.isoCode == "PT" }

        viewModel.onPhoneCountrySelected(portugal)

        assertEquals("PT", viewModel.uiState.value.phoneCountry.isoCode)
        assertNull(viewModel.uiState.value.phoneE164)
    }
}
