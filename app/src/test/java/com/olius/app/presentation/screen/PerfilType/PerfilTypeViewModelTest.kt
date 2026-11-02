package com.olius.app.presentation.screen.PerfilType

import app.cash.turbine.test
import com.olius.app.MainDispatcherRule
import com.olius.app.data.local.InMemorySessionStore
import com.olius.app.data.repository.MockUserProfileRepository
import com.olius.app.data.repository.TestPhoneNumbers
import com.olius.app.domain.entity.AccountConflictException
import com.olius.app.domain.entity.AccountRegistration
import com.olius.app.domain.entity.AuthUnavailableException
import com.olius.app.domain.entity.CepAddress
import com.olius.app.domain.entity.DuplicateTelephoneException
import com.olius.app.domain.entity.GoogleAccount
import com.olius.app.domain.entity.InvalidCredentialsException
import com.olius.app.domain.entity.InvalidRegistrationException
import com.olius.app.domain.entity.ProfileRequiredException
import com.olius.app.domain.entity.ProfileType
import com.olius.app.domain.entity.RegistrationAddress
import com.olius.app.domain.entity.TooManyAttemptsException
import com.olius.app.domain.repository.CepRepository
import com.olius.app.domain.repository.FakeUserRepository
import com.olius.app.domain.usecases.CompleteGoogleSignUpUseCase
import com.olius.app.domain.usecases.GetEstablishmentTypesUseCase
import com.olius.app.domain.usecases.GoogleSignInUseCase
import com.olius.app.domain.usecases.LoginUseCase
import com.olius.app.domain.usecases.RegisterUseCase
import com.olius.app.domain.usecases.SyncUserProfileUseCase
import com.olius.app.presentation.screen.becomecollector.CepStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class PerfilTypeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeUserRepository()
    private var cepResult: Result<CepAddress?> = Result.success(CepAddress("01001000", "SP", "São Paulo", "Sé", "Praça da Sé"))
    private val cepRepository = object : CepRepository {
        override suspend fun lookup(cep: String) = cepResult
    }
    private val viewModel = PerfilTypeViewModel(
        loginUseCase = LoginUseCase(repository),
        registerUseCase = RegisterUseCase(repository),
        googleSignInUseCase = GoogleSignInUseCase(repository),
        isValidEmail = { it.contains("@") },
        syncUserProfileUseCase = SyncUserProfileUseCase(MockUserProfileRepository(InMemorySessionStore())),
        phoneNumberRepository = TestPhoneNumbers.repository(),
        completeGoogleSignUpUseCase = CompleteGoogleSignUpUseCase(repository),
        getEstablishmentTypesUseCase = GetEstablishmentTypesUseCase(repository),
        cepRepository = cepRepository
    )

    private val state get() = viewModel.uiState.value
    private val googleAccount = GoogleAccount("g-token", "bia@gmail.com", "Bia Google")

    private fun fillLogin(email: String = "ana@olius.com", password: String = "Senha@12345") {
        viewModel.updateLoginEmail(email)
        viewModel.updateLoginPassword(password)
    }

    private fun chooseType(type: ProfileType) {
        viewModel.onHeaderIntroFinished()
        viewModel.selectProfileType(type)
        viewModel.confirmProfileType()
    }

    /** Cadastro de cidadão válido (CPF de teste com dígitos certos). */
    private fun fillCitizen(email: String = "bia@olius.com", password: String = "Senha@12345") {
        chooseType(ProfileType.CITIZEN)
        viewModel.goToRegister()
        viewModel.updateRegisterName("Bia Souza")
        viewModel.updateRegisterEmail(email)
        viewModel.updateRegisterDocument("529.982.247-25")
        viewModel.updateRegisterPassword(password)
    }

    private fun fillEstablishmentAddress() {
        viewModel.selectEstablishmentType("t-1")
        viewModel.updateRegisterCep("01001-000")
        viewModel.updateRegisterNumber("10")
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
        assertTrue(state.registerFieldErrors.isEmpty())
    }

    @Test
    fun `voltar do login e do cadastro leva para a escolha de tipo`() {
        viewModel.onHeaderIntroFinished()
        viewModel.selectProfileType(ProfileType.CITIZEN)
        viewModel.confirmProfileType()
        viewModel.login()
        assertEquals("Digite seu e-mail", state.authErrorMessage)

        viewModel.backToProfileChoice()
        assertEquals(OnboardingStep.CHOOSE_PROFILE, state.step)
        assertEquals(ProfileType.CITIZEN, state.selectedProfileType)
        assertNull(state.authErrorMessage)

        viewModel.selectProfileType(ProfileType.ESTABLISHMENT)
        viewModel.confirmProfileType()
        viewModel.goToRegister()
        viewModel.register()
        assertFalse(state.registerFieldErrors.isEmpty())

        viewModel.backToProfileChoice()
        assertEquals(OnboardingStep.CHOOSE_PROFILE, state.step)
        assertEquals(ProfileType.ESTABLISHMENT, state.selectedProfileType)
        assertTrue(state.registerFieldErrors.isEmpty())
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
        viewModel.updateRegisterEmail("bia@y.com")
        viewModel.updateRegisterPassword("senha2")
        viewModel.toggleRegisterPasswordVisibility()
        viewModel.updateRegisterComplement("Sala 2")

        assertEquals("Bia Souza", state.registerName)
        assertEquals("01/02/2000", state.registerBirthDate)
        assertEquals("11999999999", state.registerPhone)
        assertEquals("bia@y.com", state.registerEmail)
        assertEquals("senha2", state.registerPassword)
        assertEquals("Sala 2", state.registerComplement)
        assertTrue(state.registerPasswordVisible)
    }

    @Test
    fun `documento guarda so digitos, com mascara e limite do tipo`() {
        chooseType(ProfileType.CITIZEN)
        viewModel.updateRegisterDocument("529.982.247-25999")
        assertEquals("52998224725", state.registerDocument)
        assertEquals("529.982.247-25", state.registerDocumentFormatted)

        viewModel.selectProfileType(ProfileType.ESTABLISHMENT)
        assertEquals("", state.registerDocument) // trocar o tipo apaga o documento
        viewModel.updateRegisterDocument("11.222.333/0001-81")
        assertEquals("11.222.333/0001-81", state.registerDocumentFormatted)
    }

    @Test
    fun `lista de regras da senha acompanha a digitacao`() {
        viewModel.updateRegisterPassword("abc")
        assertEquals(listOf(false, false, false, false), state.passwordChecklist.map { it.second })

        viewModel.updateRegisterPassword("Senha@12345")
        assertTrue(state.passwordChecklist.all { it.second })
    }

    // ---- Telefone com DDI ------------------------------------------------------

    private fun country(iso: String) = viewModel.phoneCountries.first { it.isoCode == iso }

    @Test
    fun `telefone comeca no Brasil e guarda so digitos, mascara e E164`() {
        assertEquals("BR", state.registerPhoneCountry.isoCode)

        viewModel.updateRegisterPhone("(11) 99999-8888")

        assertEquals("11999998888", state.registerPhone)
        assertEquals("11 99999-8888", state.registerPhoneFormatted)
        assertEquals("5511999998888", state.registerPhoneE164)
    }

    @Test
    fun `trocar o pais muda DDI, mascara e validacao`() {
        viewModel.updateRegisterPhone("2015550123")
        assertNull(state.registerPhoneE164) // 10 dígitos não é celular/fixo válido no Brasil

        viewModel.selectPhoneCountry(country("US"))

        assertEquals("+1", state.registerPhoneCountry.dialCodeLabel)
        assertEquals("(201) 555-0123", state.registerPhoneFormatted)
        assertEquals("12015550123", state.registerPhoneE164)
    }

    @Test
    fun `cadastro com telefone brasileiro invalido mostra erro e nao chama a Core`() {
        fillCitizen()
        viewModel.updateRegisterPhone("1199999")

        viewModel.register()

        assertEquals("Telefone inválido para Brasil", state.registerPhoneError)
        assertNull(repository.lastRegistration)
    }

    @Test
    fun `cadastro com telefone de outro pais valida pelas regras dele`() {
        fillCitizen()
        viewModel.selectPhoneCountry(country("PT"))
        viewModel.updateRegisterPhone("11999998888") // número brasileiro, inválido em Portugal

        viewModel.register()
        assertEquals("Telefone inválido para Portugal", state.registerPhoneError)

        viewModel.updateRegisterPhone("912345678")
        assertNull(state.registerPhoneError) // digitar de novo limpa o erro
        assertEquals("351912345678", state.registerPhoneE164)
    }

    // ---- Validação ------------------------------------------------------------

    @Test
    fun `login valida e-mail e senha antes de chamar a Core`() {
        val cases = listOf(
            ("" to "Senha@12345") to "Digite seu e-mail",
            ("sem-arroba" to "Senha@12345") to "E-mail inválido",
            ("ana@olius.com" to "") to "Digite sua senha"
        )

        cases.forEach { (input, expected) ->
            fillLogin(input.first, input.second)
            viewModel.login()
            assertEquals(expected, state.authErrorMessage)
        }
        assertNull(repository.lastEmail)
    }

    @Test
    fun `login aceita senha curta (contas antigas) e deixa a Core decidir`() = runTest {
        fillLogin(password = "123")

        viewModel.navigationEvents.test {
            viewModel.login()
            assertEquals(PerfilTypeNavigationEvent.NavigateHome, awaitItem())
        }
        assertEquals("123", repository.lastPassword)
    }

    @Test
    fun `cadastro mostra o erro de cada campo e nao chama a Core`() {
        chooseType(ProfileType.CITIZEN)
        viewModel.goToRegister()
        viewModel.updateRegisterEmail("sem-arroba")
        viewModel.updateRegisterDocument("12345678900")
        viewModel.updateRegisterPassword("fraca")

        viewModel.register()

        assertEquals("Digite seu nome", state.fieldError(RegisterField.NAME))
        assertEquals("E-mail inválido", state.fieldError(RegisterField.EMAIL))
        assertEquals("CPF inválido", state.fieldError(RegisterField.DOCUMENT))
        assertEquals(
            "A senha precisa ter: de 10 a 128 caracteres, uma letra maiúscula, um número, um símbolo (ex.: ! @ # \$)",
            state.fieldError(RegisterField.PASSWORD)
        )
        assertEquals("Digite seu nome", state.authErrorMessage)
        assertNull(repository.lastRegistration)

        viewModel.updateRegisterName("Bia")
        assertNull(state.fieldError(RegisterField.NAME)) // digitar apaga o erro do campo
    }

    @Test
    fun `estabelecimento exige tipo e endereco`() {
        chooseType(ProfileType.ESTABLISHMENT)
        viewModel.goToRegister()
        viewModel.updateRegisterName("Padaria")
        viewModel.updateRegisterEmail("p@olius.com")
        viewModel.updateRegisterDocument("11222333000181")
        viewModel.updateRegisterPassword("Senha@12345")

        viewModel.register()

        assertEquals("Escolha o tipo de estabelecimento", state.fieldError(RegisterField.ESTABLISHMENT_TYPE))
        assertEquals("Digite um CEP válido para preencher o endereço", state.fieldError(RegisterField.CEP))
        assertEquals("Digite o número", state.fieldError(RegisterField.NUMBER))
        assertNull(repository.lastRegistration)
    }

    // ---- Tipos de estabelecimento e CEP ------------------------------------------

    @Test
    fun `tipos de estabelecimento carregam uma vez ao abrir o cadastro`() {
        chooseType(ProfileType.ESTABLISHMENT)
        viewModel.goToRegister()
        viewModel.goToLogin()
        viewModel.goToRegister()

        assertEquals(LoadStatus.LOADED, state.establishmentTypesStatus)
        assertEquals(listOf("Restaurante", "Hotel"), state.establishmentTypes.map { it.name })
        assertEquals(1, repository.establishmentTypesCalls)

        viewModel.selectEstablishmentType("t-2")
        assertEquals("Hotel", state.selectedEstablishmentType?.name)
    }

    @Test
    fun `cidadao nao carrega tipos e erro permite tentar de novo`() {
        chooseType(ProfileType.CITIZEN)
        viewModel.goToRegister()
        assertEquals(0, repository.establishmentTypesCalls)

        repository.establishmentTypesResult = Result.failure(IOException("sem rede"))
        viewModel.selectProfileType(ProfileType.ESTABLISHMENT)
        viewModel.retryEstablishmentTypes()
        assertEquals(LoadStatus.ERROR, state.establishmentTypesStatus)

        repository.establishmentTypesResult = Result.success(emptyList())
        viewModel.retryEstablishmentTypes()
        assertEquals(LoadStatus.LOADED, state.establishmentTypesStatus)
    }

    @Test
    fun `cep preenche o endereco e erros da consulta aparecem`() {
        viewModel.updateRegisterStreet("Minha rua")
        viewModel.updateRegisterCep("01001-000")

        assertEquals(CepStatus.FOUND, state.cepStatus)
        assertEquals("SP", state.registerState)
        assertEquals("São Paulo", state.registerCity)
        assertEquals("Praça da Sé", state.registerStreet)

        cepResult = Result.success(null)
        viewModel.updateRegisterCep("99999999")
        assertEquals(CepStatus.NOT_FOUND, state.cepStatus)
        assertEquals("", state.registerState)
        assertEquals("CEP não encontrado. Confira os números.", state.cepMessage)

        cepResult = Result.failure(IOException("sem rede"))
        viewModel.retryCep()
        assertEquals(CepStatus.NETWORK_ERROR, state.cepStatus)
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
    fun `cadastro de cidadao manda CPF e telefone e vai pra Home`() = runTest {
        fillCitizen()
        viewModel.updateRegisterPhone("11999998888")
        viewModel.updateRegisterBirthDate("01/02/2000")

        viewModel.navigationEvents.test {
            viewModel.register()
            assertEquals(PerfilTypeNavigationEvent.NavigateHome, awaitItem())
        }
        assertEquals(
            AccountRegistration.Citizen("Bia Souza", "bia@olius.com", listOf("5511999998888"), "52998224725"),
            repository.lastRegistration
        )
        assertEquals("Senha@12345", repository.lastPassword)
    }

    @Test
    fun `cadastro de estabelecimento manda cnpj, tipo e endereco do CEP`() = runTest {
        chooseType(ProfileType.ESTABLISHMENT)
        viewModel.goToRegister()
        viewModel.updateRegisterName("Padaria")
        viewModel.updateRegisterEmail("p@olius.com")
        viewModel.updateRegisterDocument("11222333000181")
        viewModel.updateRegisterPassword("Senha@12345")
        fillEstablishmentAddress()

        viewModel.navigationEvents.test {
            viewModel.register()
            assertEquals(PerfilTypeNavigationEvent.NavigateHome, awaitItem())
        }
        val sent = repository.lastRegistration as AccountRegistration.Establishment
        assertEquals("11222333000181", sent.cnpj)
        assertEquals("t-1", sent.typeId)
        assertEquals(RegistrationAddress("SP", "São Paulo", "Sé", "Praça da Sé", "10", "01001000", null), sent.address)
    }

    @Test
    fun `erros do login viram mensagens em portugues`() {
        val cases = listOf(
            InvalidCredentialsException() to "E-mail ou senha incorretos",
            TooManyAttemptsException() to "Muitas tentativas. Aguarde um minuto e tente de novo",
            IOException() to "Sem conexão — verifique sua internet",
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
    fun `erros do cadastro viram mensagens em portugues`() {
        val cases = listOf(
            AccountConflictException() to "Já existe uma conta com esse e-mail, CPF ou CNPJ",
            DuplicateTelephoneException() to "Esse telefone já está em uso em outra conta",
            IllegalStateException() to "Não foi possível concluir o cadastro. Tente novamente"
        )
        fillCitizen()

        cases.forEach { (error, expected) ->
            repository.registerResult = Result.failure(error)
            viewModel.register()
            assertEquals(expected, state.authErrorMessage)
        }
    }

    @Test
    fun `campos recusados pela Core ficam marcados`() {
        fillCitizen()
        repository.registerResult = Result.failure(InvalidRegistrationException(listOf("cpf", "telephones[0]")))

        viewModel.register()

        assertEquals("Confira os campos destacados", state.authErrorMessage)
        assertEquals("Valor inválido", state.fieldError(RegisterField.DOCUMENT))
        assertEquals("Telefone inválido", state.registerPhoneError)
    }

    @Test
    fun `login com Google de conta existente vai pra Home`() = runTest {
        viewModel.navigationEvents.test {
            viewModel.signInWithGoogle(googleAccount)
            assertEquals(PerfilTypeNavigationEvent.NavigateHome, awaitItem())
        }
        assertEquals("g-token", repository.lastIdToken)
    }

    @Test
    fun `Google de conta nova abre o completar cadastro e envia sem senha`() = runTest {
        chooseType(ProfileType.CITIZEN)
        repository.googleSignInResult = Result.failure(ProfileRequiredException())

        viewModel.signInWithGoogle(googleAccount)

        assertEquals(OnboardingStep.COMPLETE_PROFILE, state.step)
        assertEquals("Bia Google", state.registerName)
        assertEquals("bia@gmail.com", state.registerEmail)
        assertFalse(state.isAuthenticating)

        viewModel.updateRegisterDocument("52998224725")
        viewModel.updateRegisterPhone("11999998888")
        viewModel.navigationEvents.test {
            viewModel.completeGoogleProfile()
            assertEquals(PerfilTypeNavigationEvent.NavigateHome, awaitItem())
        }
        assertEquals(
            AccountRegistration.Citizen("Bia Google", "bia@gmail.com", listOf("5511999998888"), "52998224725"),
            repository.lastRegistration
        )
        assertNull(repository.lastPassword)
    }

    @Test
    fun `completar cadastro como estabelecimento carrega os tipos e valida o endereco`() {
        repository.googleSignInResult = Result.failure(ProfileRequiredException())
        viewModel.signInWithGoogle(googleAccount)
        viewModel.selectProfileType(ProfileType.ESTABLISHMENT)
        assertEquals(LoadStatus.LOADED, state.establishmentTypesStatus)

        viewModel.updateRegisterDocument("11222333000181")
        viewModel.completeGoogleProfile()
        assertEquals("Escolha o tipo de estabelecimento", state.authErrorMessage)

        fillEstablishmentAddress()
        repository.completeGoogleResult = Result.failure(InvalidCredentialsException())
        viewModel.completeGoogleProfile()
        assertEquals("Não foi possível confirmar sua conta Google. Entre com o Google de novo", state.authErrorMessage)
    }

    @Test
    fun `voltar do completar cadastro esquece a conta do Google`() {
        repository.googleSignInResult = Result.failure(ProfileRequiredException())
        viewModel.signInWithGoogle(googleAccount)

        viewModel.goToLogin()

        assertEquals(OnboardingStep.LOGIN, state.step)
        assertNull(state.googleAccount)
        viewModel.completeGoogleProfile() // sem conta, não faz nada
        assertNull(repository.lastRegistration)
    }

    @Test
    fun `falhas no login com Google mostram mensagem`() {
        val cases = listOf(
            AuthUnavailableException() to "O login com Google ainda não está disponível no servidor",
            IOException() to "Sem conexão — verifique sua internet",
            IllegalStateException() to "Não foi possível entrar com o Google"
        )
        cases.forEach { (error, expected) ->
            repository.googleSignInResult = Result.failure(error)
            viewModel.signInWithGoogle(googleAccount)
            assertEquals(expected, state.authErrorMessage)
            assertFalse(state.isAuthenticating)
        }
    }

    @Test
    fun `cancelar o seletor do Google libera o botao`() {
        viewModel.onGoogleSignInCancelled()

        assertFalse(state.isAuthenticating)
    }
}
