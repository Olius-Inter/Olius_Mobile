package com.olius.app.presentation.screen.becomecollector

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olius.app.data.remote.AppContainer
import com.olius.app.domain.entity.BecomeCollectorRequest
import com.olius.app.domain.entity.CepAddress
import com.olius.app.domain.entity.PevAlreadyExistsException
import com.olius.app.domain.entity.ProfileType
import com.olius.app.domain.entity.RegistrationAddress
import com.olius.app.domain.repository.CepRepository
import com.olius.app.domain.repository.CollectionPointRepository
import com.olius.app.domain.repository.UserProfileRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Campos do formulário (ordem da tela). */
enum class CollectorField(val label: String, val required: Boolean = true) {
    CEP("CEP"),
    STREET("Rua"),
    NUMBER("Número"),
    NEIGHBORHOOD("Bairro"),
    CITY("Cidade"),
    COMPLEMENT("Complemento (opcional)", required = false),
    OPENING_HOURS("Horário de funcionamento (ex.: Seg-Sex 10h - 16h)")
}

/** Situação da consulta do CEP. */
enum class CepStatus { IDLE, LOADING, FOUND, NOT_FOUND, NETWORK_ERROR }

data class BecomeCollectorUiState(
    /** Estabelecimento usa o endereço do cadastro e só informa o horário. */
    val isEstablishment: Boolean = false,
    val establishmentAddress: RegistrationAddress? = null,
    val isLoadingAddress: Boolean = false,
    val addressError: String? = null,
    val values: Map<CollectorField, String> = emptyMap(),
    /** UF escondida, preenchida pelo CEP; a Core exige `state` [A-Z]{2}. */
    val state: String = "",
    val cepStatus: CepStatus = CepStatus.IDLE,
    val isSending: Boolean = false,
    val errorMessage: String? = null,
    /** Enviado: o pev nasce PENDING e só aparece no mapa depois da aprovação. */
    val isSent: Boolean = false
) {
    fun value(field: CollectorField) = values[field].orEmpty()

    /** Texto embaixo do CEP: carregando ou erro da consulta. */
    val cepMessage: String?
        get() = when (cepStatus) {
            CepStatus.LOADING -> "Buscando endereço..."
            CepStatus.NOT_FOUND -> "CEP não encontrado. Confira os números."
            CepStatus.NETWORK_ERROR -> "Sem conexão para consultar o CEP. Verifique a internet e tente de novo."
            else -> null
        }

    /** Campos que aparecem na tela. */
    val visibleFields: List<CollectorField>
        get() = if (isEstablishment) listOf(CollectorField.OPENING_HOURS) else CollectorField.entries

    val canSend: Boolean
        get() = !isSending && if (isEstablishment) {
            establishmentAddress != null && value(CollectorField.OPENING_HOURS).isNotBlank()
        } else {
            cepStatus == CepStatus.FOUND && state.length == 2 &&
                CollectorField.entries.filter { it.required }.all { value(it).isNotBlank() }
        }
}

/** "Virando um Coletor". */
class BecomeCollectorViewModel(
    private val repository: CollectionPointRepository = AppContainer.collectionPointRepository,
    private val cepRepository: CepRepository = AppContainer.cepRepository,
    private val profileRepository: UserProfileRepository = AppContainer.userProfileRepository,
    profileType: ProfileType = AppContainer.currentProfileType()
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        BecomeCollectorUiState(isEstablishment = profileType == ProfileType.ESTABLISHMENT)
    )
    val uiState: StateFlow<BecomeCollectorUiState> = _uiState.asStateFlow()

    private var cepJob: Job? = null

    init {
        if (_uiState.value.isEstablishment) loadEstablishmentAddress()
    }

    /** Endereço do cadastro do estabelecimento. */
    fun loadEstablishmentAddress() {
        _uiState.update { it.copy(isLoadingAddress = true, addressError = null) }
        viewModelScope.launch {
            val address = profileRepository.fetchMyAddress().getOrNull()
            _uiState.update {
                it.copy(
                    establishmentAddress = address,
                    isLoadingAddress = false,
                    addressError = if (address == null) "Não foi possível carregar o endereço do cadastro." else null
                )
            }
        }
    }

    fun onFieldChange(field: CollectorField, value: String) {
        if (field == CollectorField.CEP) return onCepChange(value)
        _uiState.update { it.copy(values = it.values + (field to value), errorMessage = null) }
    }

    /** Com 8 dígitos consulta o CEP; menos que isso limpa a UF e a consulta anterior. */
    private fun onCepChange(value: String) {
        val cep = value.filter(Char::isDigit).take(CEP_LENGTH)
        if (cep == _uiState.value.value(CollectorField.CEP)) return
        cepJob?.cancel()
        _uiState.update {
            it.copy(
                values = it.values + (CollectorField.CEP to cep),
                state = "",
                cepStatus = CepStatus.IDLE,
                errorMessage = null
            )
        }
        if (cep.length == CEP_LENGTH) lookupCep(cep)
    }

    /** "Tentar de novo" depois de falha de rede. */
    fun onRetryCep() {
        val cep = _uiState.value.value(CollectorField.CEP)
        if (cep.length == CEP_LENGTH) lookupCep(cep)
    }

    private fun lookupCep(cep: String) {
        cepJob?.cancel()
        _uiState.update { it.copy(cepStatus = CepStatus.LOADING) }
        cepJob = viewModelScope.launch {
            val result = cepRepository.lookup(cep)
            if (_uiState.value.value(CollectorField.CEP) != cep) return@launch
            result
                .onSuccess { address ->
                    if (address == null) _uiState.update { it.copy(cepStatus = CepStatus.NOT_FOUND) }
                    else fillAddress(address)
                }
                .onFailure { _uiState.update { it.copy(cepStatus = CepStatus.NETWORK_ERROR) } }
        }
    }

    /** Preenche cidade, bairro e rua (continuam editáveis). */
    private fun fillAddress(address: CepAddress) {
        val filled = listOf(
            CollectorField.CITY to address.city,
            CollectorField.NEIGHBORHOOD to address.neighborhood,
            CollectorField.STREET to address.street
        ).filter { it.second.isNotBlank() }
        _uiState.update {
            it.copy(values = it.values + filled, state = address.state, cepStatus = CepStatus.FOUND)
        }
    }

    fun onSend() {
        val state = _uiState.value
        if (!state.canSend) return
        val address = state.establishmentAddress
        val request = if (state.isEstablishment && address != null) BecomeCollectorRequest(
            cep = address.cep,
            street = address.street,
            number = address.number,
            neighborhood = address.neighborhood,
            city = address.city,
            state = address.state,
            complement = address.complement,
            openingHours = state.value(CollectorField.OPENING_HOURS).trim()
        ) else BecomeCollectorRequest(
            cep = state.value(CollectorField.CEP),
            street = state.value(CollectorField.STREET).trim(),
            number = state.value(CollectorField.NUMBER).trim(),
            neighborhood = state.value(CollectorField.NEIGHBORHOOD).trim(),
            city = state.value(CollectorField.CITY).trim(),
            state = state.state,
            complement = state.value(CollectorField.COMPLEMENT).trim().ifBlank { null },
            openingHours = state.value(CollectorField.OPENING_HOURS).trim()
        )
        _uiState.update { it.copy(isSending = true) }
        viewModelScope.launch {
            repository.becomeCollector(request)
                .onSuccess { _uiState.update { it.copy(isSending = false, isSent = true) } }
                .onFailure { error ->
                    val message = if (error is PevAlreadyExistsException) "Você já tem um ponto de coleta cadastrado."
                    else "Não foi possível enviar. Tente novamente."
                    _uiState.update { it.copy(isSending = false, errorMessage = message) }
                }
        }
    }

    private companion object {
        const val CEP_LENGTH = 8
    }
}
