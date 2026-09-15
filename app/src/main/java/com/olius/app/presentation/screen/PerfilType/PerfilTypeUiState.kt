package com.olius.app.presentation.screen.PerfilType

/**
 * Passos do fluxo único de onboarding (splash final -> escolha de perfil ->
 * login/cadastro). Ficam num único enum pra controlar qual card o
 * PerfilTypeScreen mostra dentro do mesmo AnimatedContent.
 */
enum class OnboardingStep {
    HEADER_INTRO,
    CHOOSE_PROFILE,
    LOGIN,
    REGISTER
}

enum class ProfileType {
    ESTABLISHMENT,
    CITIZEN
}

data class PerfilTypeUiState(
    val step: OnboardingStep = OnboardingStep.HEADER_INTRO,
    val selectedProfileType: ProfileType? = null,

    val loginEmail: String = "",
    val loginPassword: String = "",
    val loginPasswordVisible: Boolean = false,

    val registerName: String = "",
    val registerEmail: String = "",
    val registerBirthDate: String = "",
    val registerPhone: String = "",
    val registerPassword: String = "",
    val registerPasswordVisible: Boolean = false
)
