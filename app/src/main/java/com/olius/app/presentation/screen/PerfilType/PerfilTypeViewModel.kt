package com.olius.app.presentation.screen.PerfilType

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Guarda o estado de todo o fluxo de onboarding (qual card está visível,
 * tipo de perfil escolhido, campos de login/cadastro). Não conhece
 * NavController nem nenhuma classe do Android além de ViewModel — quem
 * decide para onde navegar são os callbacks passados pro PerfilTypeScreen.
 */
class PerfilTypeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PerfilTypeUiState())
    val uiState: StateFlow<PerfilTypeUiState> = _uiState.asStateFlow()

    fun onHeaderIntroFinished() {
        _uiState.update { it.copy(step = OnboardingStep.CHOOSE_PROFILE) }
    }

    fun selectProfileType(type: ProfileType) {
        _uiState.update { it.copy(selectedProfileType = type) }
    }

    fun confirmProfileType() {
        _uiState.update { it.copy(step = OnboardingStep.LOGIN) }
    }

    fun goToLogin() {
        _uiState.update { it.copy(step = OnboardingStep.LOGIN) }
    }

    fun goToRegister() {
        _uiState.update { it.copy(step = OnboardingStep.REGISTER) }
    }

    fun updateLoginEmail(value: String) {
        _uiState.update { it.copy(loginEmail = value) }
    }

    fun updateLoginPassword(value: String) {
        _uiState.update { it.copy(loginPassword = value) }
    }

    fun toggleLoginPasswordVisibility() {
        _uiState.update { it.copy(loginPasswordVisible = !it.loginPasswordVisible) }
    }

    fun updateRegisterName(value: String) {
        _uiState.update { it.copy(registerName = value) }
    }

    fun updateRegisterEmail(value: String) {
        _uiState.update { it.copy(registerEmail = value) }
    }

    fun updateRegisterBirthDate(value: String) {
        _uiState.update { it.copy(registerBirthDate = value) }
    }

    fun updateRegisterPhone(value: String) {
        _uiState.update { it.copy(registerPhone = value) }
    }

    fun updateRegisterPassword(value: String) {
        _uiState.update { it.copy(registerPassword = value) }
    }

    fun toggleRegisterPasswordVisibility() {
        _uiState.update { it.copy(registerPasswordVisible = !it.registerPasswordVisible) }
    }
}
