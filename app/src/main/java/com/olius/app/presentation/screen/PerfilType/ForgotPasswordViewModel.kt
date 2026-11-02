package com.olius.app.presentation.screen.PerfilType

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olius.app.data.remote.ApiConfig
import com.olius.app.data.remote.AppContainer
import com.olius.app.domain.entity.AuthUnavailableException
import com.olius.app.domain.entity.InvalidResetCodeException
import com.olius.app.domain.entity.PasswordRule
import com.olius.app.domain.entity.TooManyAttemptsException
import com.olius.app.domain.entity.WeakPasswordException
import com.olius.app.domain.repository.PasswordResetRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

const val RESET_CODE_LENGTH = 6

/** Espera entre um envio de código e outro (a Core aceita 1 por e-mail a cada 60 s). */
const val RESEND_COOLDOWN_SECONDS = 60

/** Passos da recuperação. */
enum class ForgotPasswordStep { EMAIL, CODE, NEW_PASSWORD, LINK_SENT }

data class ForgotPasswordUiState(
    val step: ForgotPasswordStep = ForgotPasswordStep.EMAIL,
    val email: String = "",
    val code: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val newPasswordVisible: Boolean = false,
    val confirmPasswordVisible: Boolean = false,
    /** Esperando a Core: o botão fica travado. */
    val isLoading: Boolean = false,
    /** Segundos até liberar "Reenviar código" (0 = liberado). */
    val resendSecondsLeft: Int = 0,
    /** Aviso neutro (ex.: "enviamos um novo código"); some ao digitar. */
    val infoMessage: String? = null,
    val errorMessage: String? = null
) {
    /** As regras da senha da Core, cada uma marcada quando cumprida. */
    val passwordChecklist: List<Pair<PasswordRule, Boolean>>
        get() = PasswordRule.entries.map { it to it.isSatisfiedBy(newPassword) }
}

sealed class ForgotPasswordEvent {
    /** Senha trocada — a tela volta pro login. */
    object PasswordReset : ForgotPasswordEvent()
}

/** Recuperar senha na Core. */
class ForgotPasswordViewModel(
    private val repository: PasswordResetRepository = AppContainer.passwordResetRepository,
    private val isValidEmail: (String) -> Boolean = { Patterns.EMAIL_ADDRESS.matcher(it).matches() },
    val useCode: Boolean = ApiConfig.PASSWORD_RESET_WITH_CODE
) : ViewModel() {
    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    private val _events = Channel<ForgotPasswordEvent>(Channel.BUFFERED)
    val events: Flow<ForgotPasswordEvent> = _events.receiveAsFlow()

    /** Token do passo 2 → 3. Fica só na memória, nunca no estado da tela. */
    private var resetToken: String? = null

    private var lastRequestedEmail: String? = null

    private var requestJob: Job? = null
    private var countdownJob: Job? = null

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value.trim(), errorMessage = null, infoMessage = null) }
    }

    /** Aceita só dígitos e no máximo [RESET_CODE_LENGTH]. */
    fun onCodeChange(value: String) {
        _uiState.update {
            it.copy(code = value.filter(Char::isDigit).take(RESET_CODE_LENGTH), errorMessage = null, infoMessage = null)
        }
    }

    fun onNewPasswordChange(value: String) {
        _uiState.update { it.copy(newPassword = value, errorMessage = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update { it.copy(confirmPassword = value, errorMessage = null) }
    }

    fun onToggleNewPasswordVisibility() {
        _uiState.update { it.copy(newPasswordVisible = !it.newPasswordVisible) }
    }

    fun onToggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(confirmPasswordVisible = !it.confirmPasswordVisible) }
    }

    /** Botão "Continuar": valida o passo atual e chama a Core. */
    fun onContinue() {
        val state = _uiState.value
        if (state.isLoading) return
        val error = validate(state)
        if (error != null) {
            _uiState.update { it.copy(errorMessage = error) }
            return
        }
        val sentStep = if (useCode) ForgotPasswordStep.CODE else ForgotPasswordStep.LINK_SENT
        when (state.step) {
            ForgotPasswordStep.EMAIL ->
                if (state.email == lastRequestedEmail && state.resendSecondsLeft > 0) goTo(sentStep)
                else requestCode(state.email, isResend = false)

            ForgotPasswordStep.CODE -> verifyCode(state.email, state.code)
            ForgotPasswordStep.NEW_PASSWORD -> confirm(state.newPassword)
            ForgotPasswordStep.LINK_SENT -> Unit
        }
    }

    /** "Reenviar código"/"Reenviar e-mail": só depois da espera de [RESEND_COOLDOWN_SECONDS]. */
    fun onResendCode() {
        val state = _uiState.value
        val sentStep = if (useCode) ForgotPasswordStep.CODE else ForgotPasswordStep.LINK_SENT
        if (state.isLoading || state.resendSecondsLeft > 0 || state.step != sentStep) return
        requestCode(state.email, isResend = true)
    }

    /** "Voltar": volta um passo. */
    fun onBack(): Boolean {
        val previous = when (_uiState.value.step) {
            ForgotPasswordStep.EMAIL -> return false
            ForgotPasswordStep.CODE, ForgotPasswordStep.LINK_SENT -> ForgotPasswordStep.EMAIL
            ForgotPasswordStep.NEW_PASSWORD -> ForgotPasswordStep.CODE
        }
        requestJob?.cancel()
        if (previous == ForgotPasswordStep.CODE) resetToken = null
        _uiState.update { it.copy(step = previous, isLoading = false, errorMessage = null, infoMessage = null) }
        return true
    }

    /** Pede o código ou, no fluxo da Core, o link por e-mail. */
    private fun requestCode(email: String, isResend: Boolean) = launchRequest {
        val result = if (useCode) repository.requestCode(email) else repository.requestResetLink(email)
        result
            .onSuccess {
                lastRequestedEmail = email
                startResendCountdown()
                _uiState.update {
                    it.copy(
                        step = if (useCode) ForgotPasswordStep.CODE else ForgotPasswordStep.LINK_SENT,
                        code = "",
                        isLoading = false,
                        infoMessage = when {
                            !isResend -> null
                            useCode -> "Se houver uma conta com esse e-mail, enviamos um novo código"
                            else -> "Se houver uma conta com esse e-mail, enviamos um novo link"
                        }
                    )
                }
            }
            .onFailure { showError(it) }
    }

    private fun verifyCode(email: String, code: String) = launchRequest {
        repository.verifyCode(email, code)
            .onSuccess { ticket ->
                resetToken = ticket.resetToken
                _uiState.update { it.copy(step = ForgotPasswordStep.NEW_PASSWORD, isLoading = false) }
            }
            .onFailure { showError(it) }
    }

    private fun confirm(newPassword: String) {
        val token = resetToken ?: return expireCode()
        launchRequest {
            repository.confirm(token, newPassword)
                .onSuccess {
                    resetToken = null
                    _uiState.update { it.copy(isLoading = false) }
                    _events.trySend(ForgotPasswordEvent.PasswordReset)
                }
                .onFailure { error ->
                    if (error is InvalidResetCodeException) expireCode() else showError(error)
                }
        }
    }

    private fun launchRequest(block: suspend () -> Unit) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
        requestJob = viewModelScope.launch { block() }
    }

    /** O `resetToken` venceu (10 min) ou já foi usado: volta pro código. */
    private fun expireCode() {
        resetToken = null
        _uiState.update {
            it.copy(
                step = ForgotPasswordStep.CODE,
                code = "",
                isLoading = false,
                errorMessage = "O código expirou. Peça um novo código"
            )
        }
    }

    private fun startResendCountdown() {
        countdownJob?.cancel()
        _uiState.update { it.copy(resendSecondsLeft = RESEND_COOLDOWN_SECONDS) }
        countdownJob = viewModelScope.launch {
            while (_uiState.value.resendSecondsLeft > 0) {
                delay(ONE_SECOND_MS)
                _uiState.update { it.copy(resendSecondsLeft = it.resendSecondsLeft - 1) }
            }
        }
    }

    private fun showError(error: Throwable) {
        _uiState.update { it.copy(isLoading = false, errorMessage = mapError(error)) }
    }

    private fun mapError(error: Throwable): String = when (error) {
        is InvalidResetCodeException -> "Código inválido ou expirado"
        is WeakPasswordException -> "A senha não cumpre as regras abaixo"
        is TooManyAttemptsException -> "Muitas tentativas. Aguarde um minuto e tente de novo"
        is AuthUnavailableException -> "A recuperação de senha ainda não está disponível no servidor"
        is IOException -> "Sem conexão — verifique sua internet"
        else -> "Não foi possível continuar. Tente novamente"
    }

    private fun goTo(step: ForgotPasswordStep) {
        _uiState.update { it.copy(step = step, errorMessage = null, infoMessage = null) }
    }

    private fun validate(state: ForgotPasswordUiState): String? = when (state.step) {
        ForgotPasswordStep.EMAIL -> when {
            state.email.isBlank() -> "Digite seu e-mail"
            !isValidEmail(state.email) -> "E-mail inválido"
            else -> null
        }

        ForgotPasswordStep.CODE ->
            if (state.code.length < RESET_CODE_LENGTH) "Digite os $RESET_CODE_LENGTH dígitos do código" else null

        ForgotPasswordStep.LINK_SENT -> null

        ForgotPasswordStep.NEW_PASSWORD -> {
            val unmet = PasswordRule.unmet(state.newPassword)
            when {
                state.newPassword.isEmpty() -> "Digite a nova senha"
                unmet.isNotEmpty() -> "A senha precisa ter: " + unmet.joinToString(", ") { it.message.lowercase() }
                state.confirmPassword != state.newPassword -> "As senhas não coincidem"
                else -> null
            }
        }
    }

    private companion object {
        const val ONE_SECOND_MS = 1_000L
    }
}
