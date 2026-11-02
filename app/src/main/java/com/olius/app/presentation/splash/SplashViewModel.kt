package com.olius.app.presentation.splash

import androidx.lifecycle.ViewModel
import com.olius.app.data.remote.AppContainer
import com.olius.app.domain.usecases.IsUserLoggedInUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SplashViewModel(
    private val isUserLoggedInUseCase: IsUserLoggedInUseCase = IsUserLoggedInUseCase(AppContainer.userRepository)
) : ViewModel() {
    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Loading)
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    /** Chamado quando a animação da Splash termina de tocar. */
    fun onSplashAnimationFinished() {
        _uiState.value = if (isUserLoggedInUseCase()) {
            SplashUiState.NavigateHome
        } else {
            SplashUiState.NavigatePerfilType
        }
    }
}
