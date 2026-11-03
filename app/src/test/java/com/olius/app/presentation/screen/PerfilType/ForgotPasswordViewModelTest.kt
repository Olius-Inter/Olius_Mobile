package com.olius.app.presentation.screen.PerfilType

import app.cash.turbine.test
import com.olius.app.MainDispatcherRule
import com.olius.app.domain.entity.AuthUnavailableException
import com.olius.app.domain.entity.InvalidResetCodeException
import com.olius.app.domain.entity.TooManyAttemptsException
import com.olius.app.domain.entity.WeakPasswordException
import com.olius.app.domain.repository.PasswordResetRepository
import com.olius.app.domain.repository.PasswordResetTicket
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/** Repositório falso: guarda as chamadas e responde o que o teste mandar. */
private class FakePasswordResetRepository : PasswordResetRepository {
    val requested = mutableListOf<String>()
    val linksRequested = mutableListOf<String>()
    val verified = mutableListOf<Pair<String, String>>()
    val confirmed = mutableListOf<Pair<String, String>>()

    var requestResult: Result<Unit> = Result.success(Unit)
    var verifyResult: Result<PasswordResetTicket> = Result.success(PasswordResetTicket("token-1", "2030-01-01T00:10:00Z"))
    var confirmResult: Result<Unit> = Result.success(Unit)

    override suspend fun requestResetLink(email: String): Result<Unit> = requestResult.also { linksRequested += email }

    override suspend fun requestCode(email: String): Result<Unit> = requestResult.also { requested += email }

    override suspend fun verifyCode(email: String, code: String) = verifyResult.also { verified += email to code }

    override suspend fun confirm(resetToken: String, newPassword: String) =
        confirmResult.also { confirmed += resetToken to newPassword }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ForgotPasswordViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(dispatcher)

    private val repository = FakePasswordResetRepository()
    private val viewModel = ForgotPasswordViewModel(repository, isValidEmail = { it.contains("@") }, useCode = true)
    private val linkViewModel = ForgotPasswordViewModel(repository, isValidEmail = { it.contains("@") }, useCode = false)
    private val state get() = viewModel.uiState.value

    private val strongPassword = "NovaSenha@123"

    private fun goToCodeStep() {
        viewModel.onEmailChange(" lois@gmail.com ")
        viewModel.onContinue()
    }

    private fun goToNewPasswordStep() {
        goToCodeStep()
        viewModel.onCodeChange("123456")
        viewModel.onContinue()
    }

    @Test
    fun `passo do e-mail valida antes de chamar a Core`() {
        viewModel.onContinue()
        assertEquals("Digite seu e-mail", state.errorMessage)

        viewModel.onEmailChange("sem-arroba")
        assertNull(state.errorMessage)
        viewModel.onContinue()
        assertEquals("E-mail inválido", state.errorMessage)
        assertEquals(ForgotPasswordStep.EMAIL, state.step)
        assertTrue(repository.requested.isEmpty())
    }

    @Test
    fun `e-mail valido pede o codigo e vai pro passo do codigo`() {
        goToCodeStep()

        assertEquals(listOf("lois@gmail.com"), repository.requested)
        assertEquals(ForgotPasswordStep.CODE, state.step)
        assertEquals("lois@gmail.com", state.email)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `sem rede mostra a mensagem de conexao e fica no e-mail`() {
        repository.requestResult = Result.failure(IOException("offline"))

        goToCodeStep()

        assertEquals("Sem conexão — verifique sua internet", state.errorMessage)
        assertEquals(ForgotPasswordStep.EMAIL, state.step)
        assertFalse(state.isLoading)
    }

    @Test
    fun `rota ainda nao publicada na Core tem mensagem propria`() {
        repository.requestResult = Result.failure(AuthUnavailableException())

        goToCodeStep()

        assertEquals("A recuperação de senha ainda não está disponível no servidor", state.errorMessage)
        assertEquals(ForgotPasswordStep.EMAIL, state.step)
    }

    @Test
    fun `limite de tentativas da Core`() {
        repository.requestResult = Result.failure(TooManyAttemptsException())

        goToCodeStep()

        assertEquals("Muitas tentativas. Aguarde um minuto e tente de novo", state.errorMessage)
    }

    @Test
    fun `codigo aceita so 6 digitos`() {
        goToCodeStep()

        viewModel.onCodeChange("12a3-4567 8")

        assertEquals("123456", state.code)
    }

    @Test
    fun `codigo incompleto nao chama a Core`() {
        goToCodeStep()
        viewModel.onCodeChange("123")

        viewModel.onContinue()

        assertEquals("Digite os 6 dígitos do código", state.errorMessage)
        assertEquals(ForgotPasswordStep.CODE, state.step)
        assertTrue(repository.verified.isEmpty())
    }

    @Test
    fun `codigo certo vai pra nova senha`() {
        goToNewPasswordStep()

        assertEquals(listOf("lois@gmail.com" to "123456"), repository.verified)
        assertEquals(ForgotPasswordStep.NEW_PASSWORD, state.step)
    }

    @Test
    fun `codigo errado ou vencido mostra o erro e fica no codigo`() {
        repository.verifyResult = Result.failure(InvalidResetCodeException())

        goToNewPasswordStep()

        assertEquals("Código inválido ou expirado", state.errorMessage)
        assertEquals(ForgotPasswordStep.CODE, state.step)
        assertEquals("123456", state.code)
    }

    @Test
    fun `reenviar so libera depois de 60 segundos`() = runTest(dispatcher) {
        goToCodeStep()
        assertEquals(RESEND_COOLDOWN_SECONDS, state.resendSecondsLeft)

        viewModel.onResendCode()
        assertEquals(1, repository.requested.size)

        advanceTimeBy(30_000)
        runCurrent()
        assertEquals(30, state.resendSecondsLeft)
        viewModel.onResendCode()
        assertEquals(1, repository.requested.size)

        advanceTimeBy(30_000)
        runCurrent()
        assertEquals(0, state.resendSecondsLeft)

        viewModel.onCodeChange("999")
        viewModel.onResendCode()
        assertEquals(2, repository.requested.size)
        assertEquals("", state.code)
        assertEquals("Se houver uma conta com esse e-mail, enviamos um novo código", state.infoMessage)
        assertEquals(RESEND_COOLDOWN_SECONDS, state.resendSecondsLeft)
        advanceTimeBy(RESEND_COOLDOWN_SECONDS * 1_000L)
    }

    @Test
    fun `voltar e continuar com o mesmo e-mail durante a espera nao pede outro codigo`() = runTest(dispatcher) {
        goToCodeStep()
        viewModel.onBack()

        viewModel.onContinue()
        assertEquals(1, repository.requested.size)
        assertEquals(ForgotPasswordStep.CODE, state.step)

        viewModel.onBack()
        viewModel.onEmailChange("outra@gmail.com")
        viewModel.onContinue()
        assertEquals(listOf("lois@gmail.com", "outra@gmail.com"), repository.requested)
        advanceTimeBy(RESEND_COOLDOWN_SECONDS * 1_000L)
    }

    @Test
    fun `senha fraca nao chama a Core e diz a regra que falta`() {
        goToNewPasswordStep()

        viewModel.onContinue()
        assertEquals("Digite a nova senha", state.errorMessage)

        viewModel.onNewPasswordChange("senha123")
        viewModel.onConfirmPasswordChange("senha123")
        viewModel.onContinue()
        assertEquals(
            "A senha precisa ter: de 10 a 128 caracteres, uma letra maiúscula, um símbolo (ex.: ! @ # \$)",
            state.errorMessage
        )
        assertTrue(repository.confirmed.isEmpty())
    }

    @Test
    fun `checklist marca as regras da Core`() {
        goToNewPasswordStep()
        viewModel.onNewPasswordChange("abc1")

        val ok = state.passwordChecklist.associate { it.first.name to it.second }
        assertEquals(mapOf("LENGTH" to false, "UPPERCASE" to false, "DIGIT" to true, "SYMBOL" to false), ok)
    }

    @Test
    fun `senhas diferentes nao chamam a Core`() {
        goToNewPasswordStep()
        viewModel.onNewPasswordChange(strongPassword)
        viewModel.onConfirmPasswordChange("$strongPassword!")

        viewModel.onContinue()

        assertEquals("As senhas não coincidem", state.errorMessage)
        assertTrue(repository.confirmed.isEmpty())
    }

    @Test
    fun `senha aceita manda o resetToken e volta pro login`() = runTest {
        goToNewPasswordStep()
        viewModel.onNewPasswordChange(strongPassword)
        viewModel.onConfirmPasswordChange(strongPassword)

        viewModel.events.test {
            viewModel.onContinue()
            assertEquals(ForgotPasswordEvent.PasswordReset, awaitItem())
        }
        assertEquals(listOf("token-1" to strongPassword), repository.confirmed)
        assertNull(state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun `Core recusa a senha mostra o erro e fica no passo`() {
        repository.confirmResult = Result.failure(WeakPasswordException())
        goToNewPasswordStep()
        viewModel.onNewPasswordChange(strongPassword)
        viewModel.onConfirmPasswordChange(strongPassword)

        viewModel.onContinue()

        assertEquals("A senha não cumpre as regras abaixo", state.errorMessage)
        assertEquals(ForgotPasswordStep.NEW_PASSWORD, state.step)
    }

    @Test
    fun `resetToken vencido volta pro codigo`() {
        repository.confirmResult = Result.failure(InvalidResetCodeException())
        goToNewPasswordStep()
        viewModel.onNewPasswordChange(strongPassword)
        viewModel.onConfirmPasswordChange(strongPassword)

        viewModel.onContinue()

        assertEquals("O código expirou. Peça um novo código", state.errorMessage)
        assertEquals(ForgotPasswordStep.CODE, state.step)
        assertEquals("", state.code)
    }

    @Test
    fun `mostrar e esconder cada senha separadamente`() {
        viewModel.onToggleNewPasswordVisibility()
        assertTrue(state.newPasswordVisible)
        assertFalse(state.confirmPasswordVisible)

        viewModel.onToggleConfirmPasswordVisibility()
        assertTrue(state.confirmPasswordVisible)
    }

    @Test
    fun `voltar desce um passo por vez e avisa quando sair da tela`() {
        goToNewPasswordStep()

        assertTrue(viewModel.onBack())
        assertEquals(ForgotPasswordStep.CODE, state.step)

        assertTrue(viewModel.onBack())
        assertEquals(ForgotPasswordStep.EMAIL, state.step)

        assertFalse(viewModel.onBack())
        assertEquals(ForgotPasswordStep.EMAIL, state.step)
    }

    @Test
    fun `fluxo do link pede o link e mostra o passo de link enviado`() {
        linkViewModel.onEmailChange("lois@gmail.com")
        linkViewModel.onContinue()

        assertEquals(listOf("lois@gmail.com"), repository.linksRequested)
        assertTrue(repository.requested.isEmpty())
        assertEquals(ForgotPasswordStep.LINK_SENT, linkViewModel.uiState.value.step)
        assertEquals(RESEND_COOLDOWN_SECONDS, linkViewModel.uiState.value.resendSecondsLeft)
    }

    @Test
    fun `fluxo do link volta para o e-mail e falha mostra o erro`() {
        repository.requestResult = Result.failure(TooManyAttemptsException())
        linkViewModel.onEmailChange("lois@gmail.com")
        linkViewModel.onContinue()
        assertEquals(ForgotPasswordStep.EMAIL, linkViewModel.uiState.value.step)
        assertEquals("Muitas tentativas. Aguarde um minuto e tente de novo", linkViewModel.uiState.value.errorMessage)

        repository.requestResult = Result.success(Unit)
        linkViewModel.onContinue()
        assertTrue(linkViewModel.onBack())
        assertEquals(ForgotPasswordStep.EMAIL, linkViewModel.uiState.value.step)
        assertFalse(linkViewModel.onBack())
    }}
