package com.olius.app.presentation.screen.PerfilType

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.FirebaseNetworkException
import com.olius.app.data.repository.UserRepositoryImpl
import com.olius.app.domain.usecases.GoogleSignInUseCase
import com.olius.app.domain.usecases.LoginUseCase
import com.olius.app.domain.usecases.RegisterUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Guarda o estado de todo o fluxo de onboarding (qual card está visível,
 * tipo de perfil escolhido, campos de login/cadastro) e chama os usecases de
 * autenticação (ver AUTH_01/AUTH_02/AUTH_03 na raiz do projeto). Não conhece
 * `NavController` nem o Firebase diretamente — quem faz a ponte com o SDK do
 * Google (Credential Manager, que precisa de Activity Context) é a própria
 * tela, que só repassa o ID token pronto pra [signInWithGoogleIdToken].
 */
class PerfilTypeViewModel(
    // Injeção manual simples (sem Hilt/Koin no projeto ainda): o valor
    // padrão usa a implementação real (Firebase); em teste, dá pra passar
    // usecases construídos com um UserRepository fake sem mudar a assinatura.
    private val loginUseCase: LoginUseCase = LoginUseCase(UserRepositoryImpl()),
    private val registerUseCase: RegisterUseCase = RegisterUseCase(UserRepositoryImpl()),
    private val googleSignInUseCase: GoogleSignInUseCase = GoogleSignInUseCase(UserRepositoryImpl())
) : ViewModel() {

    private val _uiState = MutableStateFlow(PerfilTypeUiState())
    val uiState: StateFlow<PerfilTypeUiState> = _uiState.asStateFlow()

    // Canal de eventos únicos — ver PerfilTypeUiState.kt pro porquê de não
    // ser um campo comum dentro do PerfilTypeUiState.
    private val _navigationEvents = Channel<PerfilTypeNavigationEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<PerfilTypeNavigationEvent> = _navigationEvents.receiveAsFlow()

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
        _uiState.update { it.copy(step = OnboardingStep.LOGIN, authErrorMessage = null) }
    }

    fun goToRegister() {
        _uiState.update { it.copy(step = OnboardingStep.REGISTER, authErrorMessage = null) }
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

    /** Login com e-mail/senha. Chamado pelo botão "Entrar" do LoginCard. */
    fun login() {
        val state = _uiState.value
        val validationError = validateEmailPassword(state.loginEmail, state.loginPassword)
        if (validationError != null) {
            _uiState.update { it.copy(authErrorMessage = validationError) }
            return
        }

        _uiState.update { it.copy(isAuthenticating = true, authErrorMessage = null) }
        viewModelScope.launch {
            loginUseCase(state.loginEmail, state.loginPassword)
                .onSuccess { onAuthSuccess() }
                .onFailure { error -> onAuthFailure(error) }
        }
    }

    /** Cadastro com e-mail/senha. Chamado pelo botão "Continuar" do RegisterCard. */
    fun register() {
        val state = _uiState.value
        val validationError = validateEmailPassword(state.registerEmail, state.registerPassword)
        if (validationError != null) {
            _uiState.update { it.copy(authErrorMessage = validationError) }
            return
        }

        _uiState.update { it.copy(isAuthenticating = true, authErrorMessage = null) }
        viewModelScope.launch {
            registerUseCase(state.registerEmail, state.registerPassword)
                .onSuccess { onAuthSuccess() }
                .onFailure { error -> onAuthFailure(error) }
            // Nome/telefone/data de nascimento (registerName, registerPhone,
            // registerBirthDate) não vão pro Firebase Auth aqui — ver
            // AUTH_07_OBJETO_DE_CADASTRO.md pra como enviar esses dados pra
            // uma API própria depois que o cadastro no Firebase for aceito.
        }
    }

    /** Chamado pela tela depois que o Credential Manager devolveu o idToken (ver AUTH_02). */
    fun signInWithGoogleIdToken(idToken: String) {
        _uiState.update { it.copy(isAuthenticating = true, authErrorMessage = null) }
        viewModelScope.launch {
            googleSignInUseCase(idToken)
                .onSuccess { onAuthSuccess() }
                .onFailure { error -> onAuthFailure(error, isGoogle = true) }
        }
    }

    /** Chamado pela tela quando o usuário cancela/fecha o seletor de conta do Google. */
    fun onGoogleSignInCancelled() {
        _uiState.update { it.copy(isAuthenticating = false) }
    }

    private fun onAuthSuccess() {
        _uiState.update { it.copy(isAuthenticating = false) }
        _navigationEvents.trySend(PerfilTypeNavigationEvent.NavigateHome)
    }

    private fun onAuthFailure(error: Throwable, isGoogle: Boolean = false) {
        val message = if (isGoogle) "Não foi possível entrar com o Google" else mapAuthError(error)
        _uiState.update { it.copy(isAuthenticating = false, authErrorMessage = message) }
    }

    private fun validateEmailPassword(email: String, password: String): String? = when {
        email.isBlank() -> "Digite seu e-mail"
        !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "E-mail inválido"
        password.isBlank() -> "Digite sua senha"
        password.length < 6 -> "A senha precisa ter pelo menos 6 caracteres"
        else -> null
    }

    // Mapeia exceções do Firebase pra mensagens em português. Fica no
    // ViewModel (não no repositório) porque é texto voltado pra UI — o
    // repositório/usecase só devolve Result<Unit>/exceção crua, sem opinar
    // sobre como mostrar isso na tela. Ver AUTH_01_LOGIN_EMAIL_SENHA.md,
    // seção 5.
    private fun mapAuthError(error: Throwable): String = when (error) {
        is FirebaseAuthInvalidUserException -> "Não encontramos uma conta com esse e-mail"
        is FirebaseAuthInvalidCredentialsException -> "E-mail ou senha incorretos"
        is FirebaseAuthUserCollisionException -> "Já existe uma conta com esse e-mail"
        is FirebaseAuthWeakPasswordException -> "Senha muito fraca — use pelo menos 6 caracteres"
        is FirebaseNetworkException -> "Sem conexão — verifique sua internet"
        else -> "Não foi possível entrar. Tente novamente"
    }
}
