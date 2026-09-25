package com.olius.app.presentation.splash

import com.olius.app.domain.repository.FakeUserRepository
import com.olius.app.domain.usecases.IsUserLoggedInUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class SplashViewModelTest {

    private val repository = FakeUserRepository()
    private val viewModel = SplashViewModel(IsUserLoggedInUseCase(repository))

    @Test
    fun `comeca carregando`() {
        assertEquals(SplashUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `vai pra Home quando ja existe sessao`() {
        repository.loggedIn = true

        viewModel.onSplashAnimationFinished()

        assertEquals(SplashUiState.NavigateHome, viewModel.uiState.value)
    }

    @Test
    fun `vai pro fluxo de login quando nao existe sessao`() {
        repository.loggedIn = false

        viewModel.onSplashAnimationFinished()

        assertEquals(SplashUiState.NavigatePerfilType, viewModel.uiState.value)
    }
}
