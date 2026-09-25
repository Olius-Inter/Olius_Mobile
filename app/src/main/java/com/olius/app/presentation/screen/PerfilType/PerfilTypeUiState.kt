package com.olius.app.presentation.screen.PerfilType

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
    val registerPasswordVisible: Boolean = false,

    // Controla se o botão "Entrar"/"Continuar" mostra "..." e fica
    // desabilitado enquanto a chamada ao Firebase está em voo.
    val isAuthenticating: Boolean = false,

    // Mensagem de erro pronta pra mostrar (já traduzida). Fica no uiState
    // (não é evento único) porque deve continuar visível até a pessoa tentar
    // de novo ou trocar de tela, mesmo que a Activity seja recriada.
    val authErrorMessage: String? = null
)

/**
 * Evento de navegação único. Não é um campo do [PerfilTypeUiState] de
 * propósito: um StateFlow reentrega o último valor pra quem observar de
 * novo (ex.: depois de girar a tela), o que faria a navegação disparar uma
 * segunda vez sozinha. Ver AUTH_01_LOGIN_EMAIL_SENHA.md, seção 3.
 */
sealed class PerfilTypeNavigationEvent {
    object NavigateHome : PerfilTypeNavigationEvent()
}
