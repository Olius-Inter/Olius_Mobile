package com.olius.app.presentation.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olius.app.data.remote.AppContainer
import com.olius.app.domain.entity.InvalidProfileException
import com.olius.app.domain.entity.PhoneCountry
import com.olius.app.domain.entity.ProfileConflictException
import com.olius.app.domain.entity.ProfileEditUnavailableException
import com.olius.app.domain.entity.ProfileUpdate
import com.olius.app.domain.repository.PhoneNumberRepository
import com.olius.app.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

data class EditProfileUiState(
    val name: String = "",
    val email: String = "",
    val phoneCountry: PhoneCountry = PhoneCountry.BRAZIL,
    /** Número nacional, só dígitos (a máscara fica em [phoneFormatted]). */
    val phone: String = "",
    val phoneFormatted: String = "",
    /** E.164 só com dígitos quando o número é válido para o país; null senão. */
    val phoneE164: String? = null,
    val nameError: String? = null,
    val emailError: String? = null,
    val phoneError: String? = null,
    /** Mensagem geral (servidor, rede...). */
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
    /** Sem rede (IOException): a tela abre o "Sem conexão", como a Home e o Histórico. */
    val isOffline: Boolean = false,
    /** Salvou na Core e no cache: a tela volta para o Perfil. */
    val saved: Boolean = false
)

/** "Editar Perfil": nome, e-mail e telefone (com DDI). */
class EditProfileViewModel(
    private val profiles: UserProfileRepository = AppContainer.userProfileRepository,
    private val phoneNumbers: PhoneNumberRepository = AppContainer.phoneNumberRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    val phoneCountries: List<PhoneCountry> by lazy { phoneNumbers.countries() }

    init {
        profiles.cachedProfile()?.let { profile ->
            val (country, national) = profile.phone
                ?.let { runCatching { phoneNumbers.fromE164Digits(it) }.getOrNull() }
                ?: (PhoneCountry.BRAZIL to "")
            _uiState.update { it.copy(name = profile.name, email = profile.email).withPhone(country, national) }
        }
    }

    fun onNameChange(value: String) =
        _uiState.update { it.copy(name = value, nameError = null, errorMessage = null) }

    fun onEmailChange(value: String) =
        _uiState.update { it.copy(email = value.trim(), emailError = null, errorMessage = null) }

    fun onPhoneChange(value: String) =
        _uiState.update { it.withPhone(it.phoneCountry, value.filter(Char::isDigit)) }

    fun onPhoneCountrySelected(country: PhoneCountry) = _uiState.update { it.withPhone(country, it.phone) }

    private fun EditProfileUiState.withPhone(country: PhoneCountry, digits: String) = copy(
        phoneCountry = country,
        phone = digits,
        phoneFormatted = if (digits.isEmpty()) "" else phoneNumbers.formatAsYouType(country.isoCode, digits),
        phoneE164 = if (digits.isEmpty()) null else phoneNumbers.toE164Digits(country.isoCode, digits),
        phoneError = null,
        errorMessage = null
    )

    fun onSaveClick() {
        val state = _uiState.value
        if (state.isSaving) return
        val name = state.name.trim()
        val email = state.email.trim()
        val nameError = when {
            name.isEmpty() -> "Informe o nome"
            name.length > NAME_MAX -> "Use até $NAME_MAX caracteres"
            else -> null
        }
        val emailError = if (email.length > EMAIL_MAX || !EMAIL_REGEX.matches(email)) "E-mail inválido" else null
        val phoneError = if (state.phone.isNotEmpty() && state.phoneE164 == null) {
            "Telefone inválido para ${state.phoneCountry.name}"
        } else null
        if (nameError != null || emailError != null || phoneError != null) {
            _uiState.update { it.copy(nameError = nameError, emailError = emailError, phoneError = phoneError) }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            profiles.updateMyProfile(ProfileUpdate(name, email, state.phoneE164))
                .onSuccess { _uiState.update { it.copy(isSaving = false, saved = true) } }
                .onFailure { error -> _uiState.update { it.copy(isSaving = false).withError(error) } }
        }
    }

    private fun EditProfileUiState.withError(error: Throwable): EditProfileUiState = when (error) {
        is ProfileEditUnavailableException -> copy(errorMessage = MESSAGE_UNAVAILABLE)
        is ProfileConflictException -> copy(errorMessage = "Esse e-mail ou telefone já está em uso em outra conta")
        is InvalidProfileException -> copy(
            nameError = "Nome inválido".takeIf { "name" in error.fields },
            emailError = "E-mail inválido".takeIf { "email" in error.fields },
            phoneError = "Telefone inválido".takeIf { error.fields.any { it.startsWith("telephone") } },
            errorMessage = "Confira os campos destacados"
        )
        is IOException -> copy(errorMessage = MESSAGE_OFFLINE, isOffline = true)

        else -> copy(errorMessage = "Não foi possível salvar agora. Tente de novo.")
    }

    companion object {
        const val NAME_MAX = 150
        const val EMAIL_MAX = 255
        const val MESSAGE_UNAVAILABLE = "Edição de perfil ainda não disponível no servidor"
        const val MESSAGE_OFFLINE = "Sem conexão com a internet. Confira sua rede e tente de novo."
        private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
