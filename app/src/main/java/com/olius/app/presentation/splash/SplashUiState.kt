package com.olius.app.presentation.splash

sealed class SplashUiState {
    object loading : SplashUiState()
    object NavigateHome : SplashUiState()
    object NavigatePerfilType : SplashUiState()
}