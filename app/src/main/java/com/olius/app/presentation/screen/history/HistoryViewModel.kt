package com.olius.app.presentation.screen.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olius.app.data.remote.AppContainer
import com.olius.app.domain.entity.CitizenHistory
import com.olius.app.domain.entity.CollectionHistoryItem
import com.olius.app.domain.entity.CollectionStatus
import com.olius.app.domain.entity.EstablishmentHistory
import com.olius.app.domain.entity.ProfileType
import com.olius.app.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

data class HistoryUiState(
    val profileType: ProfileType = ProfileType.ESTABLISHMENT,
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val errorMessage: String? = null,
    val establishment: EstablishmentHistory? = null,
    val citizen: CitizenHistory? = null,
    /** Filtro da tabela B2B; null = "Todos". */
    val statusFilter: CollectionStatus? = null
) {
    val filteredItems: List<CollectionHistoryItem>
        get() = establishment?.items.orEmpty().filter { statusFilter == null || it.status == statusFilter }
}

/**
 * Aba Histórico (Figma "Histórico de Coletas" B2B / "Histórico de Registro" B2C).
 * Uma chamada só, para o tipo da conta; o filtro de status é local (não chama a API).
 */
class HistoryViewModel(
    private val profileType: ProfileType = AppContainer.currentProfileType(),
    private val repository: HistoryRepository = AppContainer.historyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState(profileType = profileType))
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = if (profileType == ProfileType.CITIZEN) {
                repository.getCitizenHistory().map { history -> _uiState.update { it.copy(citizen = history) } }
            } else {
                repository.getEstablishmentHistory().map { history -> _uiState.update { it.copy(establishment = history) } }
            }
            result
                .onSuccess { _uiState.update { it.copy(isLoading = false, isOffline = false) } }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isOffline = error is IOException,
                            errorMessage = if (error is IOException) null else "Não foi possível carregar o histórico."
                        )
                    }
                }
        }
    }

    fun onFilterSelected(status: CollectionStatus?) {
        _uiState.update { it.copy(statusFilter = status) }
    }
}
