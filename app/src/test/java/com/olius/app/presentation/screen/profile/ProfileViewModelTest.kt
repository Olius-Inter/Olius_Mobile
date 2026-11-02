package com.olius.app.presentation.screen.profile

import com.olius.app.MainDispatcherRule
import com.olius.app.data.local.InMemoryAppPreferences
import com.olius.app.data.local.InMemorySessionStore
import com.olius.app.data.repository.MockUserProfileRepository
import com.olius.app.data.repository.TestPhoneNumbers
import com.olius.app.domain.entity.ProfileType
import com.olius.app.domain.entity.ThemeMode
import com.olius.app.domain.entity.UserProfile
import com.olius.app.domain.repository.FakeUserRepository
import com.olius.app.domain.repository.UserProfileRepository
import com.olius.app.domain.usecases.LogoutUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val store = InMemorySessionStore()
    private val users = FakeUserRepository().apply { loggedIn = true }
    private val preferences = InMemoryAppPreferences()
    private val phones = TestPhoneNumbers.repository()

    private val naldo = UserProfile(
        id = "u-1",
        name = "Naldo Canal",
        email = "lois@gmail.com",
        profileType = ProfileType.ESTABLISHMENT,
        phone = "5511959377578"
    )

    /** Perfil do cache; a "rede" responde [collector] no GET /pevs/me. */
    private fun profiles(collector: Result<Boolean> = Result.success(false)): UserProfileRepository =
        object : UserProfileRepository by MockUserProfileRepository(store) {
            override suspend fun fetchCollectorStatus() = collector
        }

    private fun viewModel(repository: UserProfileRepository = profiles()) =
        ProfileViewModel(repository, preferences, phones, LogoutUseCase(users))

    @Test
    fun `mostra o perfil do cache com iniciais, telefone com DDI e tipo`() {
        store.save(naldo)

        val state = viewModel().uiState.value

        assertEquals("Naldo Canal", state.name)
        assertEquals("lois@gmail.com", state.email)
        assertEquals("NC", state.initials)
        assertTrue(state.phone.startsWith("+55 11 9"))
        assertEquals("Estabelecimento", state.profileTypeLabel)
    }

    @Test
    fun `cidadao aparece como Cidadao e sem telefone o campo fica vazio`() {
        store.save(naldo.copy(profileType = ProfileType.CITIZEN, phone = null))

        val state = viewModel().uiState.value

        assertEquals("Cidadão", state.profileTypeLabel)
        assertEquals("", state.phone)
    }

    @Test
    fun `coletor vem do GET pevs me`() {
        store.save(naldo)

        assertTrue(viewModel(profiles(Result.success(true))).uiState.value.isCollector)
        assertFalse(viewModel(profiles(Result.success(false))).uiState.value.isCollector)
    }

    @Test
    fun `sem rede no pevs me mantem o coletor do cache`() {
        store.save(naldo.copy(isCollector = true))

        val state = viewModel(profiles(Result.failure(IOException("sem rede")))).uiState.value

        assertTrue(state.isCollector)
        assertEquals("Naldo Canal", state.name)
    }

    @Test
    fun `tema escolhido fica salvo e fecha o seletor`() {
        val viewModel = viewModel()
        assertEquals(ThemeMode.LIGHT, viewModel.uiState.value.themeMode)

        viewModel.onThemeClick()
        assertTrue(viewModel.uiState.value.isThemeDialogOpen)
        viewModel.onThemeSelected(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
        assertFalse(viewModel.uiState.value.isThemeDialogOpen)
        assertEquals(ThemeMode.DARK, preferences.themeMode.value)
    }

    @Test
    fun `tema salvo antes aparece ao abrir`() {
        preferences.setThemeMode(ThemeMode.SYSTEM)

        assertEquals(ThemeMode.SYSTEM, viewModel().uiState.value.themeMode)
    }

    @Test
    fun `modo do tema decide claro ou escuro`() {
        assertFalse(ThemeMode.LIGHT.isDark(systemIsDark = true))
        assertTrue(ThemeMode.DARK.isDark(systemIsDark = false))
        assertTrue(ThemeMode.SYSTEM.isDark(systemIsDark = true))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromStored(null))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromStored("lixo"))
    }

    @Test
    fun `chave de notificacoes fica salva`() {
        val viewModel = viewModel()
        assertTrue(viewModel.uiState.value.notificationsEnabled)

        viewModel.onNotificationsToggle(false)

        assertFalse(viewModel.uiState.value.notificationsEnabled)
        assertFalse(preferences.notificationsEnabled())
    }

    @Test
    fun `sair encerra a sessao da Core e apaga o perfil do cache`() {
        store.save(naldo)
        val repository = profiles()

        viewModel(repository).onLogoutClick()

        assertEquals(1, users.signOutCalls)
        assertFalse(users.loggedIn)
        assertNull(repository.cachedProfile())
    }

    @Test
    fun `volta do editar perfil le o cache de novo`() {
        store.save(naldo)
        val viewModel = viewModel()

        store.save(naldo.copy(name = "Naldo Editado"))
        viewModel.reloadFromCache()

        assertEquals("Naldo Editado", viewModel.uiState.value.name)
        assertEquals("NE", viewModel.uiState.value.initials)
    }

    @Test
    fun `iniciais usam o primeiro e o ultimo nome`() {
        assertEquals("AL", ProfileViewModel.initialsOf("ana maria lima"))
        assertEquals("A", ProfileViewModel.initialsOf("  Ana "))
        assertEquals("", ProfileViewModel.initialsOf(""))
    }

    @Test
    fun `idioma so tem portugues e o contato usa as constantes`() {
        val state = viewModel().uiState.value

        assertEquals("Português", state.language)
        assertEquals("oliusjef26@gmail.com", state.supportEmail)
    }
}
