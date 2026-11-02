package com.olius.app.presentation.screen.PerfilType

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olius.app.data.remote.AppContainer
import com.olius.app.domain.entity.AccountConflictException
import com.olius.app.domain.entity.AccountRegistration
import com.olius.app.domain.entity.AuthUnavailableException
import com.olius.app.domain.entity.BrazilianDocuments
import com.olius.app.domain.entity.CepAddress
import com.olius.app.domain.entity.DuplicateTelephoneException
import com.olius.app.domain.entity.GoogleAccount
import com.olius.app.domain.entity.InvalidCredentialsException
import com.olius.app.domain.entity.InvalidRegistrationException
import com.olius.app.domain.entity.PasswordRule
import com.olius.app.domain.entity.PhoneCountry
import com.olius.app.domain.entity.ProfileRequiredException
import com.olius.app.domain.entity.ProfileType
import com.olius.app.domain.entity.RegistrationAddress
import com.olius.app.domain.entity.TooManyAttemptsException
import com.olius.app.domain.repository.CepRepository
import com.olius.app.domain.repository.PhoneNumberRepository
import com.olius.app.domain.usecases.CompleteGoogleSignUpUseCase
import com.olius.app.domain.usecases.GetEstablishmentTypesUseCase
import com.olius.app.domain.usecases.GoogleSignInUseCase
import com.olius.app.domain.usecases.LoginUseCase
import com.olius.app.domain.usecases.RegisterUseCase
import com.olius.app.domain.usecases.SyncUserProfileUseCase
import com.olius.app.presentation.screen.becomecollector.CepStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

// E.164 tem no máximo 15 dígitos; o número nacional nunca passa disso.
private const val MAX_PHONE_DIGITS = 15
private const val CEP_LENGTH = 8

/**
 * Guarda o estado de todo o fluxo de onboarding (qual card está visível,
 * tipo de perfil escolhido, campos de login/cadastro) e chama os usecases de
 * autenticação da Core. Não conhece `NavController` nem HTTP — quem fala com
 * o Credential Manager do Google (precisa de Activity Context) é a própria
 * tela, que só repassa a conta escolhida para [signInWithGoogle].
 */
class PerfilTypeViewModel(
    // Injeção manual simples (sem Hilt/Koin no projeto ainda): o valor
    // padrão usa a implementação real (Core); em teste, dá pra passar
    // usecases construídos com um UserRepository fake sem mudar a assinatura.
    private val loginUseCase: LoginUseCase = LoginUseCase(AppContainer.userRepository),
    private val registerUseCase: RegisterUseCase = RegisterUseCase(AppContainer.userRepository),
    private val googleSignInUseCase: GoogleSignInUseCase = GoogleSignInUseCase(AppContainer.userRepository),
    // `Patterns` vem do android.jar, que nos testes unitários (JVM) é só um
    // stub (EMAIL_ADDRESS fica null) — por isso a validação é injetável.
    private val isValidEmail: (String) -> Boolean = { Patterns.EMAIL_ADDRESS.matcher(it).matches() },
    // Depois do login, carrega o perfil da Core (GET /users/me) para o cache.
    private val syncUserProfileUseCase: SyncUserProfileUseCase = SyncUserProfileUseCase(AppContainer.userProfileRepository),
    // DDI, máscara e validação do telefone (libphonenumber, offline).
    private val phoneNumberRepository: PhoneNumberRepository = AppContainer.phoneNumberRepository,
    private val completeGoogleSignUpUseCase: CompleteGoogleSignUpUseCase = CompleteGoogleSignUpUseCase(AppContainer.userRepository),
    private val getEstablishmentTypesUseCase: GetEstablishmentTypesUseCase = GetEstablishmentTypesUseCase(AppContainer.userRepository),
    // ViaCEP: preenche o endereço do estabelecimento.
    private val cepRepository: CepRepository = AppContainer.cepRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PerfilTypeUiState())
    val uiState: StateFlow<PerfilTypeUiState> = _uiState.asStateFlow()

    // Canal de eventos únicos — ver PerfilTypeUiState.kt pro porquê de não
    // ser um campo comum dentro do PerfilTypeUiState.
    private val _navigationEvents = Channel<PerfilTypeNavigationEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<PerfilTypeNavigationEvent> = _navigationEvents.receiveAsFlow()

    private var cepJob: Job? = null

    fun onHeaderIntroFinished() {
        _uiState.update { it.copy(step = OnboardingStep.CHOOSE_PROFILE) }
    }

    /** No onboarding e no "Completar cadastro". Trocar o tipo apaga o CPF/CNPJ (tamanhos diferentes). */
    fun selectProfileType(type: ProfileType) {
        _uiState.update {
            if (it.selectedProfileType == type) it
            else it.copy(
                selectedProfileType = type,
                registerDocument = "",
                registerFieldErrors = it.registerFieldErrors - RegisterField.DOCUMENT
            )
        }
        if (_uiState.value.step == OnboardingStep.COMPLETE_PROFILE) loadEstablishmentTypesIfNeeded()
    }

    fun confirmProfileType() {
        _uiState.update { it.copy(step = OnboardingStep.LOGIN) }
    }

    fun goToLogin() {
        _uiState.update {
            it.copy(step = OnboardingStep.LOGIN, authErrorMessage = null, googleAccount = null, registerFieldErrors = emptyMap())
        }
    }

    fun goToRegister() {
        _uiState.update { it.copy(step = OnboardingStep.REGISTER, authErrorMessage = null) }
        loadEstablishmentTypesIfNeeded()
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

    fun updateRegisterName(value: String) = updateField(RegisterField.NAME) { it.copy(registerName = value) }

    fun updateRegisterEmail(value: String) = updateField(RegisterField.EMAIL) { it.copy(registerEmail = value) }

    fun updateRegisterBirthDate(value: String) {
        _uiState.update { it.copy(registerBirthDate = value) }
    }

    /** Países do seletor de telefone (bandeira + nome em português + DDI). */
    val phoneCountries: List<PhoneCountry> by lazy { phoneNumberRepository.countries() }

    /** Recebe o texto do campo (com ou sem máscara) e guarda só os dígitos. */
    fun updateRegisterPhone(value: String) {
        val digits = value.filter(Char::isDigit).take(MAX_PHONE_DIGITS)
        _uiState.update { it.withPhone(it.registerPhoneCountry, digits) }
    }

    /** Troca o país: o DDI, a máscara e a validação passam a ser os dele. */
    fun selectPhoneCountry(country: PhoneCountry) {
        _uiState.update { it.withPhone(country, it.registerPhone) }
    }

    // Recalcula máscara e E.164 juntos, pra nunca ficarem de países diferentes.
    // Qualquer mudança no telefone apaga o erro até a próxima tentativa.
    private fun PerfilTypeUiState.withPhone(country: PhoneCountry, digits: String) = copy(
        registerPhoneCountry = country,
        registerPhone = digits,
        registerPhoneFormatted = if (digits.isEmpty()) "" else phoneNumberRepository.formatAsYouType(country.isoCode, digits),
        registerPhoneE164 = phoneNumberRepository.toE164Digits(country.isoCode, digits),
        registerPhoneError = null
    )

    fun updateRegisterPassword(value: String) = updateField(RegisterField.PASSWORD) { it.copy(registerPassword = value) }

    fun toggleRegisterPasswordVisibility() {
        _uiState.update { it.copy(registerPasswordVisible = !it.registerPasswordVisible) }
    }

    /** CPF (11) ou CNPJ (14), conforme o tipo; guarda só os dígitos. */
    fun updateRegisterDocument(value: String) {
        val max = if (_uiState.value.registrationType == ProfileType.CITIZEN) BrazilianDocuments.CPF_LENGTH
        else BrazilianDocuments.CNPJ_LENGTH
        updateField(RegisterField.DOCUMENT) { it.copy(registerDocument = value.filter(Char::isDigit).take(max)) }
    }

    fun selectEstablishmentType(id: String) =
        updateField(RegisterField.ESTABLISHMENT_TYPE) { it.copy(registerEstablishmentTypeId = id) }

    fun retryEstablishmentTypes() = loadEstablishmentTypesIfNeeded()

    fun updateRegisterStreet(value: String) = updateField(RegisterField.STREET) { it.copy(registerStreet = value) }

    fun updateRegisterNumber(value: String) = updateField(RegisterField.NUMBER) { it.copy(registerNumber = value) }

    fun updateRegisterNeighborhood(value: String) =
        updateField(RegisterField.NEIGHBORHOOD) { it.copy(registerNeighborhood = value) }

    fun updateRegisterCity(value: String) = updateField(RegisterField.CITY) { it.copy(registerCity = value) }

    fun updateRegisterComplement(value: String) {
        _uiState.update { it.copy(registerComplement = value) }
    }

    /** CEP com 8 dígitos → ViaCEP preenche UF, cidade, bairro e rua (continuam editáveis). */
    fun updateRegisterCep(value: String) {
        val cep = value.filter(Char::isDigit).take(CEP_LENGTH)
        if (cep == _uiState.value.registerCep) return
        cepJob?.cancel()
        updateField(RegisterField.CEP) { it.copy(registerCep = cep, registerState = "", cepStatus = CepStatus.IDLE) }
        if (cep.length == CEP_LENGTH) lookupCep(cep)
    }

    fun retryCep() {
        val cep = _uiState.value.registerCep
        if (cep.length == CEP_LENGTH) lookupCep(cep)
    }

    private fun lookupCep(cep: String) {
        cepJob?.cancel()
        _uiState.update { it.copy(cepStatus = CepStatus.LOADING) }
        cepJob = viewModelScope.launch {
            val result = cepRepository.lookup(cep)
            // A pessoa pode ter mudado o CEP enquanto a resposta vinha.
            if (_uiState.value.registerCep != cep) return@launch
            result
                .onSuccess { address ->
                    if (address == null) _uiState.update { it.copy(cepStatus = CepStatus.NOT_FOUND) }
                    else fillAddress(address)
                }
                .onFailure { _uiState.update { it.copy(cepStatus = CepStatus.NETWORK_ERROR) } }
        }
    }

    // Campo vazio no ViaCEP não apaga o que a pessoa digitou.
    private fun fillAddress(address: CepAddress) {
        _uiState.update {
            it.copy(
                registerState = address.state,
                registerCity = address.city.ifBlank { it.registerCity },
                registerNeighborhood = address.neighborhood.ifBlank { it.registerNeighborhood },
                registerStreet = address.street.ifBlank { it.registerStreet },
                cepStatus = CepStatus.FOUND,
                registerFieldErrors = it.registerFieldErrors - listOf(
                    RegisterField.CITY, RegisterField.NEIGHBORHOOD, RegisterField.STREET
                )
            )
        }
    }

    // Lista de tipos: só para estabelecimento, uma vez (ou de novo depois de erro).
    private fun loadEstablishmentTypesIfNeeded() {
        val state = _uiState.value
        if (state.registrationType != ProfileType.ESTABLISHMENT) return
        if (state.establishmentTypesStatus == LoadStatus.LOADING || state.establishmentTypesStatus == LoadStatus.LOADED) return
        _uiState.update { it.copy(establishmentTypesStatus = LoadStatus.LOADING) }
        viewModelScope.launch {
            getEstablishmentTypesUseCase()
                .onSuccess { types ->
                    _uiState.update { it.copy(establishmentTypes = types, establishmentTypesStatus = LoadStatus.LOADED) }
                }
                .onFailure { _uiState.update { it.copy(establishmentTypesStatus = LoadStatus.ERROR) } }
        }
    }

    /** Login com e-mail/senha. Chamado pelo botão "Entrar" do LoginCard. */
    fun login() {
        val state = _uiState.value
        val validationError = when {
            state.loginEmail.isBlank() -> "Digite seu e-mail"
            !isValidEmail(state.loginEmail.trim()) -> "E-mail inválido"
            state.loginPassword.isEmpty() -> "Digite sua senha"
            else -> null
        }
        if (validationError != null) {
            _uiState.update { it.copy(authErrorMessage = validationError) }
            return
        }

        _uiState.update { it.copy(isAuthenticating = true, authErrorMessage = null) }
        viewModelScope.launch {
            loginUseCase(state.loginEmail.trim(), state.loginPassword)
                .onSuccess { onAuthSuccess() }
                .onFailure { error -> onAuthFailure(mapLoginError(error)) }
        }
    }

    /** Cadastro com senha. Chamado pelo botão "Continuar" do RegisterCard. */
    fun register() {
        val password = _uiState.value.registerPassword
        val registration = validateRegistration(requirePassword = true) ?: return

        _uiState.update { it.copy(isAuthenticating = true, authErrorMessage = null) }
        viewModelScope.launch {
            registerUseCase(registration, password)
                .onSuccess { onAuthSuccess() }
                .onFailure { error -> onAuthFailure(mapRegistrationError(error)) }
        }
    }

    /** Chamado pela tela depois que o Credential Manager devolveu a conta do Google. */
    fun signInWithGoogle(account: GoogleAccount) {
        _uiState.update { it.copy(isAuthenticating = true, authErrorMessage = null) }
        viewModelScope.launch {
            googleSignInUseCase(account.idToken)
                .onSuccess { onAuthSuccess() }
                .onFailure { error ->
                    if (error is ProfileRequiredException) startGoogleCompletion(account)
                    else onAuthFailure(mapGoogleError(error))
                }
        }
    }

    /** Conta do Google sem cadastro na Core: abre "Completar cadastro" com nome e e-mail do Google. */
    private fun startGoogleCompletion(account: GoogleAccount) {
        _uiState.update {
            it.copy(
                step = OnboardingStep.COMPLETE_PROFILE,
                googleAccount = account,
                registerName = account.name.orEmpty().ifBlank { it.registerName },
                registerEmail = account.email,
                registerFieldErrors = emptyMap(),
                isAuthenticating = false,
                authErrorMessage = null
            )
        }
        loadEstablishmentTypesIfNeeded()
    }

    /** "Concluir" do "Completar cadastro": mesmos dados do cadastro, sem senha. */
    fun completeGoogleProfile() {
        val account = _uiState.value.googleAccount ?: return
        val registration = validateRegistration(requirePassword = false) ?: return

        _uiState.update { it.copy(isAuthenticating = true, authErrorMessage = null) }
        viewModelScope.launch {
            completeGoogleSignUpUseCase(account.idToken, registration)
                .onSuccess { onAuthSuccess() }
                .onFailure { error ->
                    // 401 aqui = o ID token do Google venceu ou não foi aceito.
                    if (error is InvalidCredentialsException) onAuthFailure("Não foi possível confirmar sua conta Google. Entre com o Google de novo")
                    else onAuthFailure(mapRegistrationError(error))
                }
        }
    }

    /** Chamado pela tela quando o usuário cancela/fecha o seletor de conta do Google. */
    fun onGoogleSignInCancelled() {
        _uiState.update { it.copy(isAuthenticating = false) }
    }

    /**
     * Confere todos os campos do cadastro (cada erro aparece embaixo do seu
     * campo; o primeiro também aparece perto do botão) e monta o que vai
     * para a Core. null = tem erro.
     */
    private fun validateRegistration(requirePassword: Boolean): AccountRegistration? {
        val state = _uiState.value
        val errors = linkedMapOf<RegisterField, String>()
        val name = state.registerName.trim()
        val email = state.registerEmail.trim()

        if (name.isEmpty()) errors[RegisterField.NAME] = "Digite seu nome"
        when {
            email.isEmpty() -> errors[RegisterField.EMAIL] = "Digite seu e-mail"
            !isValidEmail(email) -> errors[RegisterField.EMAIL] = "E-mail inválido"
        }
        val isCitizen = state.registrationType == ProfileType.CITIZEN
        when {
            state.registerDocument.isEmpty() -> errors[RegisterField.DOCUMENT] = if (isCitizen) "Digite seu CPF" else "Digite o CNPJ"
            isCitizen && !BrazilianDocuments.isValidCpf(state.registerDocument) -> errors[RegisterField.DOCUMENT] = "CPF inválido"
            !isCitizen && !BrazilianDocuments.isValidCnpj(state.registerDocument) -> errors[RegisterField.DOCUMENT] = "CNPJ inválido"
        }
        if (!isCitizen) validateEstablishment(state, errors)
        if (requirePassword) {
            val unmet = PasswordRule.unmet(state.registerPassword)
            if (unmet.isNotEmpty()) {
                errors[RegisterField.PASSWORD] = "A senha precisa ter: " + unmet.joinToString(", ") { it.message.lowercase() }
            }
        }
        // Telefone é opcional (a Core aceita a lista vazia), mas se veio tem
        // que ser válido para o país escolhido.
        val phoneError = if (state.registerPhone.isNotEmpty() && state.registerPhoneE164 == null) {
            "Telefone inválido para ${state.registerPhoneCountry.name}"
        } else null

        if (errors.isNotEmpty() || phoneError != null) {
            _uiState.update {
                it.copy(
                    registerFieldErrors = errors,
                    registerPhoneError = phoneError,
                    authErrorMessage = errors.values.firstOrNull() ?: phoneError
                )
            }
            return null
        }
        _uiState.update { it.copy(registerFieldErrors = emptyMap()) }
        return buildRegistration(state, name, email)
    }

    private fun validateEstablishment(state: PerfilTypeUiState, errors: MutableMap<RegisterField, String>) {
        if (state.registerEstablishmentTypeId == null) errors[RegisterField.ESTABLISHMENT_TYPE] = "Escolha o tipo de estabelecimento"
        if (state.registerCep.length != CEP_LENGTH || state.registerState.length != 2) {
            errors[RegisterField.CEP] = "Digite um CEP válido para preencher o endereço"
        }
        if (state.registerStreet.isBlank()) errors[RegisterField.STREET] = "Digite a rua"
        if (state.registerNumber.isBlank()) errors[RegisterField.NUMBER] = "Digite o número"
        if (state.registerNeighborhood.isBlank()) errors[RegisterField.NEIGHBORHOOD] = "Digite o bairro"
        if (state.registerCity.isBlank()) errors[RegisterField.CITY] = "Digite a cidade"
    }

    private fun buildRegistration(state: PerfilTypeUiState, name: String, email: String): AccountRegistration {
        val telephones = listOfNotNull(state.registerPhoneE164)
        return if (state.registrationType == ProfileType.CITIZEN) {
            AccountRegistration.Citizen(name, email, telephones, cpf = state.registerDocument)
        } else {
            AccountRegistration.Establishment(
                name = name,
                email = email,
                telephones = telephones,
                cnpj = state.registerDocument,
                typeId = checkNotNull(state.registerEstablishmentTypeId),
                address = RegistrationAddress(
                    state = state.registerState,
                    city = state.registerCity.trim(),
                    neighborhood = state.registerNeighborhood.trim(),
                    street = state.registerStreet.trim(),
                    number = state.registerNumber.trim(),
                    cep = state.registerCep,
                    complement = state.registerComplement.trim().ifEmpty { null }
                )
            )
        }
    }

    // Muda um campo do cadastro e apaga o erro dele até a próxima tentativa.
    private fun updateField(field: RegisterField, change: (PerfilTypeUiState) -> PerfilTypeUiState) {
        _uiState.update { change(it).copy(registerFieldErrors = it.registerFieldErrors - field) }
    }

    /**
     * Sessão da Core aberta: antes de ir pra Home, carrega o perfil
     * (GET /users/me) para o cache. O tipo da conta vem da Core, não do toggle.
     */
    private fun onAuthSuccess() {
        viewModelScope.launch {
            syncUserProfileUseCase()
            _uiState.update { it.copy(isAuthenticating = false) }
            _navigationEvents.trySend(PerfilTypeNavigationEvent.NavigateHome)
        }
    }

    private fun onAuthFailure(message: String) {
        _uiState.update { it.copy(isAuthenticating = false, authErrorMessage = message) }
    }

    // Exceções do domínio → mensagens em português. Fica no ViewModel (não no
    // repositório) porque é texto voltado pra UI.
    private fun mapLoginError(error: Throwable): String = when (error) {
        is InvalidCredentialsException -> "E-mail ou senha incorretos"
        else -> commonError(error) ?: "Não foi possível entrar. Tente novamente"
    }

    private fun mapGoogleError(error: Throwable): String = when (error) {
        is AuthUnavailableException -> "O login com Google ainda não está disponível no servidor"
        is InvalidCredentialsException -> "Não foi possível confirmar sua conta Google"
        else -> commonError(error) ?: "Não foi possível entrar com o Google"
    }

    private fun mapRegistrationError(error: Throwable): String = when (error) {
        is AccountConflictException -> "Já existe uma conta com esse e-mail, CPF ou CNPJ"
        is DuplicateTelephoneException -> "Esse telefone já está em uso em outra conta"
        is InvalidRegistrationException -> {
            markInvalidFields(error.fields)
            "Confira os campos destacados"
        }
        is AuthUnavailableException -> "O cadastro com Google ainda não está disponível no servidor"
        // Cadastro aceito, mas o login automático logo depois foi recusado.
        is InvalidCredentialsException -> "Cadastro feito. Entre com seu e-mail e senha"
        else -> commonError(error) ?: "Não foi possível concluir o cadastro. Tente novamente"
    }

    private fun commonError(error: Throwable): String? = when (error) {
        is TooManyAttemptsException -> "Muitas tentativas. Aguarde um minuto e tente de novo"
        is IOException -> "Sem conexão — verifique sua internet"
        else -> null
    }

    // Campos que a Core recusou (400 VALIDATION_ERROR) → erro embaixo deles.
    private fun markInvalidFields(fields: List<String>) {
        val marked = fields.mapNotNull { CORE_FIELDS[it.substringBefore('[')] }.associateWith { "Valor inválido" }
        if (fields.any { it.startsWith("telephones") }) {
            _uiState.update { it.copy(registerPhoneError = "Telefone inválido") }
        }
        _uiState.update { it.copy(registerFieldErrors = it.registerFieldErrors + marked) }
    }

    private companion object {
        // Nome do campo no DTO da Core → campo da tela.
        val CORE_FIELDS = mapOf(
            "name" to RegisterField.NAME,
            "email" to RegisterField.EMAIL,
            "password" to RegisterField.PASSWORD,
            "cpf" to RegisterField.DOCUMENT,
            "cnpj" to RegisterField.DOCUMENT,
            "typeId" to RegisterField.ESTABLISHMENT_TYPE,
            "address.cep" to RegisterField.CEP,
            "address.state" to RegisterField.CEP,
            "address.street" to RegisterField.STREET,
            "address.number" to RegisterField.NUMBER,
            "address.neighborhood" to RegisterField.NEIGHBORHOOD,
            "address.city" to RegisterField.CITY
        )
    }
}
