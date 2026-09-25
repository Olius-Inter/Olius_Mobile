package com.olius.app.presentation.splash

sealed class SplashUiState {
    object Loading : SplashUiState()
    object NavigateHome : SplashUiState()
    object NavigatePerfilType : SplashUiState()
}
