package com.olius.app.presentation.screen.PerfilType

import app.cash.turbine.test
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.olius.app.MainDispatcherRule
import com.olius.app.domain.repository.FakeUserRepository
import com.olius.app.domain.usecases.GoogleSignInUseCase
import com.olius.app.domain.usecases.LoginUseCase
import com.olius.app.domain.usecases.RegisterUseCase
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PerfilTypeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeUserRepository()
    private val viewModel = PerfilTypeViewModel(
        loginUseCase = LoginUseCase(repository),
        registerUseCase = RegisterUseCase(repository),
        googleSignInUseCase = GoogleSignInUseCase(repository),
        isValidEmail = { it.contains("@") }
    )

    private val state get() = viewModel.uiState.value

    private fun fillLogin(email: String = "ana@olius.com", password: String = "123456") {
        viewModel.updateLoginEmail(email)
        viewModel.updateLoginPassword(password)
    }

    private fun fillRegister(email: String = "bia@olius.com", password: String = "abcdef") {
        viewModel.updateRegisterEmail(email)
        viewModel.updateRegisterPassword(password)
    }

    // ---- Navegação entre os cards -------------------------------------------

    @Test
    fun `fluxo de onboarding avanca da intro ate o login`() {
        assertEquals(OnboardingStep.HEADER_INTRO, state.step)

        viewModel.onHeaderIntroFinished()
        assertEquals(OnboardingStep.CHOOSE_PROFILE, state.step)

        viewModel.selectProfileType(ProfileType.CITIZEN)
        viewModel.confirmProfileType()
        assertEquals(ProfileType.CITIZEN, state.selectedProfileType)
        assertEquals(OnboardingStep.LOGIN, state.step)
    }

    @Test
    fun `trocar entre login e cadastro limpa a mensagem de erro`() {
        viewModel.login()
        assertEquals("Digite seu e-mail", state.authErrorMessage)

        viewModel.goToRegister()
        assertEquals(OnboardingStep.REGISTER, state.step)
        assertNull(state.authErrorMessage)

        viewModel.register()
        viewModel.goToLogin()
        assertEquals(OnboardingStep.LOGIN, state.step)
        assertNull(state.authErrorMessage)
    }

    // ---- Campos ---------------------------------------------------------------

    @Test
    fun `campos de login atualizam o estado`() {
        fillLogin("x@y.com", "senha1")
        viewModel.toggleLoginPasswordVisibility()

        assertEquals("x@y.com", state.loginEmail)
        assertEquals("senha1", state.loginPassword)
        assertTrue(state.loginPasswordVisible)
    }

    @Test
    fun `campos de cadastro atualizam o estado`() {
        viewModel.updateRegisterName("Bia Souza")
        viewModel.updateRegisterBirthDate("01/02/2000")
        viewModel.updateRegisterPhone("11999999999")
        fillRegister("bia@y.com", "senha2")
        viewModel.toggleRegisterPasswordVisibility()

        assertEquals("Bia Souza", state.registerName)
        assertEquals("01/02/2000", state.registerBirthDate)
        assertEquals("11999999999", state.registerPhone)
        assertEquals("bia@y.com", state.registerEmail)
        assertEquals("senha2", state.registerPassword)
        assertTrue(state.registerPasswordVisible)
    }

    // ---- Validação ------------------------------------------------------------

    @Test
    fun `login valida e-mail e senha antes de chamar o Firebase`() {
        val cases = listOf(
            ("" to "123456") to "Digite seu e-mail",
            ("sem-arroba" to "123456") to "E-mail inválido",
            ("ana@olius.com" to "") to "Digite sua senha",
            ("ana@olius.com" to "123") to "A senha precisa ter pelo menos 6 caracteres"
        )

        cases.forEach { (input, expected) ->
            fillLogin(input.first, input.second)
            viewModel.login()
            assertEquals(expected, state.authErrorMessage)
        }
        assertNull(repository.lastEmail)
    }

    // ---- Login / cadastro / Google ------------------------------------------

    @Test
    fun `login com sucesso envia evento de navegar pra Home`() = runTest {
        fillLogin()

        viewModel.navigationEvents.test {
            viewModel.login()
            assertEquals(PerfilTypeNavigationEvent.NavigateHome, awaitItem())
        }
        assertEquals("ana@olius.com", repository.lastEmail)
        assertFalse(state.isAuthenticating)
        assertNull(state.authErrorMessage)
    }

    @Test
    fun `cadastro com sucesso envia evento de navegar pra Home`() = runTest {
        fillRegister()

        viewModel.navigationEvents.test {
            viewModel.register()
            assertEquals(PerfilTypeNavigationEvent.NavigateHome, awaitItem())
        }
        assertEquals("bia@olius.com", repository.lastEmail)
    }

    @Test
    fun `cadastro invalido nao chama o Firebase`() {
        fillRegister(email = "")

        viewModel.register()

        assertEquals("Digite seu e-mail", state.authErrorMessage)
        assertNull(repository.lastEmail)
    }

    @Test
    fun `erros do Firebase viram mensagens em portugues`() {
        val cases = listOf(
            mockk<FirebaseAuthInvalidUserException>() to "Não encontramos uma conta com esse e-mail",
            mockk<FirebaseAuthInvalidCredentialsException>() to "E-mail ou senha incorretos",
            mockk<FirebaseAuthUserCollisionException>() to "Já existe uma conta com esse e-mail",
            mockk<FirebaseAuthWeakPasswordException>() to "Senha muito fraca — use pelo menos 6 caracteres",
            mockk<FirebaseNetworkException>() to "Sem conexão — verifique sua internet",
            IllegalStateException() to "Não foi possível entrar. Tente novamente"
        )
        fillLogin()

        cases.forEach { (error, expected) ->
            repository.signInResult = Result.failure(error)
            viewModel.login()
            assertEquals(expected, state.authErrorMessage)
            assertFalse(state.isAuthenticating)
        }
    }

    @Test
    fun `login com Google com sucesso envia evento de navegar pra Home`() = runTest {
        viewModel.navigationEvents.test {
            viewModel.signInWithGoogleIdToken("token")
            assertEquals(PerfilTypeNavigationEvent.NavigateHome, awaitItem())
        }
        assertEquals("token", repository.lastIdToken)
    }

    @Test
    fun `falha no login com Google mostra mensagem generica`() {
        repository.googleSignInResult = Result.failure(mockk<FirebaseNetworkException>())

        viewModel.signInWithGoogleIdToken("token")

        assertEquals("Não foi possível entrar com o Google", state.authErrorMessage)
        assertFalse(state.isAuthenticating)
    }

    @Test
    fun `cancelar o seletor do Google libera o botao`() {
        viewModel.onGoogleSignInCancelled()

        assertFalse(state.isAuthenticating)
    }
}
