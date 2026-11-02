package com.olius.app.presentation.splash

import androidx.lifecycle.ViewModel
import com.olius.app.data.remote.AppContainer
import com.olius.app.domain.usecases.IsUserLoggedInUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Decide se a Splash manda a pessoa pra Home (já tem sessão da Core salva:
 * refresh token ainda válido) ou pro fluxo de login (`PerfilType`) — é o
 * "gate" que garante que só quem está logado chega na Home. O access token é
 * renovado na primeira chamada à Core.
 */
class SplashViewModel(
    private val isUserLoggedInUseCase: IsUserLoggedInUseCase = IsUserLoggedInUseCase(AppContainer.userRepository)
) : ViewModel() {

    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Loading)
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    /** Chamado quando a animação da Splash termina de tocar (ver SplashScreen.kt). */
    fun onSplashAnimationFinished() {
        _uiState.value = if (isUserLoggedInUseCase()) {
            SplashUiState.NavigateHome
        } else {
            SplashUiState.NavigatePerfilType
        }
    }
}
