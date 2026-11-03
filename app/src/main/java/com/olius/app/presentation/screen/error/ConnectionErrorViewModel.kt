package com.olius.app.presentation.screen.error

import androidx.lifecycle.ViewModel
import com.olius.app.data.remote.AppContainer
import com.olius.app.domain.usecases.IsUserLoggedInUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Para onde o "Voltar ao início" leva (mesma regra da Splash). */
sealed class ConnectionErrorUiState {
    object Idle : ConnectionErrorUiState()
    object NavigateHome : ConnectionErrorUiState()
    object NavigatePerfilType : ConnectionErrorUiState()
}

/** Tela "Sem conexão". */
class ConnectionErrorViewModel(
    private val isUserLoggedInUseCase: IsUserLoggedInUseCase = IsUserLoggedInUseCase(AppContainer.userRepository)
) : ViewModel() {
    private val _uiState = MutableStateFlow<ConnectionErrorUiState>(ConnectionErrorUiState.Idle)
    val uiState: StateFlow<ConnectionErrorUiState> = _uiState.asStateFlow()

    fun onBackToStartClick() {
        _uiState.value = if (isUserLoggedInUseCase()) {
            ConnectionErrorUiState.NavigateHome
        } else {
            ConnectionErrorUiState.NavigatePerfilType
        }
    }
}
