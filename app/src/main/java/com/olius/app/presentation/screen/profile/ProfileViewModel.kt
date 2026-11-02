package com.olius.app.presentation.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olius.app.data.remote.ApiConfig
import com.olius.app.data.remote.AppContainer
import com.olius.app.domain.entity.ProfileType
import com.olius.app.domain.entity.ThemeMode
import com.olius.app.domain.entity.UserProfile
import com.olius.app.domain.repository.AppPreferencesRepository
import com.olius.app.domain.repository.PhoneNumberRepository
import com.olius.app.domain.repository.UserProfileRepository
import com.olius.app.domain.usecases.LogoutUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val name: String = "",
    val email: String = "",
    /** Telefone já formatado com DDI (ex.: "+55 11 95937-7578"); vazio = sem telefone. */
    val phone: String = "",
    /** "NC": avatar com iniciais (o banco não tem foto). */
    val initials: String = "",
    /** null = perfil ainda não carregado. */
    val profileType: ProfileType? = null,
    /** GET /pevs/me com status APPROVED. */
    val isCollector: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.DEFAULT,
    val isThemeDialogOpen: Boolean = false,
    val notificationsEnabled: Boolean = true,
    /** Só existe português no app: o item aparece, mas não troca. */
    val language: String = "Português",
    val supportEmail: String = ApiConfig.SUPPORT_EMAIL
) {
    /** "Estabelecimento" ou "Cidadão" (users.user_type). */
    val profileTypeLabel: String
        get() = when (profileType) {
            ProfileType.ESTABLISHMENT -> "Estabelecimento"
            ProfileType.CITIZEN -> "Cidadão"
            null -> ""
        }
}

/**
 * Tela de Perfil (aberta pelo avatar do topo). Mostra o perfil do cache (Room)
 * na hora e depois atualiza com GET /users/me e GET /pevs/me. Tema e
 * notificações ficam só no aparelho.
 */
class ProfileViewModel(
    private val profiles: UserProfileRepository = AppContainer.userProfileRepository,
    private val preferences: AppPreferencesRepository = AppContainer.appPreferences,
    private val phoneNumbers: PhoneNumberRepository = AppContainer.phoneNumberRepository,
    private val logoutUseCase: LogoutUseCase = LogoutUseCase(AppContainer.userRepository)
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ProfileUiState(
            themeMode = preferences.themeMode.value,
            notificationsEnabled = preferences.notificationsEnabled()
        )
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        // Cache primeiro: a tela abre com os dados sem esperar a rede.
        profiles.cachedProfile()?.let(::applyProfile)
        refresh()
    }

    /** Busca de novo (ex.: ao voltar do "Editar Perfil"). Falhas mantêm o cache na tela. */
    fun refresh() {
        viewModelScope.launch {
            profiles.fetchMyProfile().onSuccess(::applyProfile)
            profiles.fetchCollectorStatus().onSuccess { collector ->
                _uiState.update { it.copy(isCollector = collector) }
            }
        }
    }

    /** A tela voltou a aparecer (ex.: depois do "Editar Perfil" salvar no cache). */
    fun reloadFromCache() {
        profiles.cachedProfile()?.let(::applyProfile)
    }

    private fun applyProfile(profile: UserProfile) {

        _uiState.update {
            it.copy(
                name = profile.name,
                email = profile.email,
                phone = profile.phone?.let(::formatPhone).orEmpty(),
                initials = initialsOf(profile.name),
                profileType = profile.profileType,
                isCollector = profile.isCollector
            )
        }
    }

    /** "5511959377578" → "+55 11 95937-7578"; país não reconhecido → "+5511959377578". */
    private fun formatPhone(e164Digits: String): String {
        val (country, national) = runCatching { phoneNumbers.fromE164Digits(e164Digits) }.getOrNull()
            ?: return "+$e164Digits"
        return "${country.dialCodeLabel} ${phoneNumbers.formatAsYouType(country.isoCode, national)}"
    }

    // ---- Tema ---------------------------------------------------------------

    fun onThemeClick() = _uiState.update { it.copy(isThemeDialogOpen = true) }

    fun onDismissThemeDialog() = _uiState.update { it.copy(isThemeDialogOpen = false) }

    /** Salva no aparelho; o app inteiro troca na hora (a MainActivity observa o tema). */
    fun onThemeSelected(mode: ThemeMode) {
        preferences.setThemeMode(mode)
        _uiState.update { it.copy(themeMode = mode, isThemeDialogOpen = false) }
    }

    // ---- Notificações -------------------------------------------------------

    /** Ainda não há push: a chave só guarda a escolha para quando houver. */
    fun onNotificationsToggle(enabled: Boolean) {
        preferences.setNotificationsEnabled(enabled)
        _uiState.update { it.copy(notificationsEnabled = enabled) }
    }

    // ---- Sair ---------------------------------------------------------------

    /**
     * Logout real na Core (POST /auth/mobile/logout) e limpa o perfil do cache.
     * A navegação para o login é feita por quem chama, logo depois.
     */
    fun onLogoutClick() {
        logoutUseCase()
        profiles.clear()
    }

    companion object {
        /** Primeira letra do primeiro e do último nome ("Naldo Canal" → "NC"). */
        fun initialsOf(name: String): String {
            val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            val letters = when {
                words.isEmpty() -> ""
                words.size == 1 -> words.first().take(1)
                else -> words.first().take(1) + words.last().take(1)
            }
            return letters.uppercase()
        }
    }
}
