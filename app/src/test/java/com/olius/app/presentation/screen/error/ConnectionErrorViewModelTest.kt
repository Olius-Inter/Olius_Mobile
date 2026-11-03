package com.olius.app.presentation.screen.error

import com.olius.app.MainDispatcherRule
import com.olius.app.domain.repository.FakeUserRepository
import com.olius.app.domain.usecases.IsUserLoggedInUseCase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ConnectionErrorViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeUserRepository()
    private val viewModel = ConnectionErrorViewModel(IsUserLoggedInUseCase(repository))

    @Test
    fun `comeca parada, sem navegar`() {
        assertEquals(ConnectionErrorUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `voltar ao inicio com sessao vai para a Home`() {
        repository.loggedIn = true

        viewModel.onBackToStartClick()

        assertEquals(ConnectionErrorUiState.NavigateHome, viewModel.uiState.value)
    }

    @Test
    fun `voltar ao inicio sem sessao vai para o login`() {
        repository.loggedIn = false

        viewModel.onBackToStartClick()

        assertEquals(ConnectionErrorUiState.NavigatePerfilType, viewModel.uiState.value)
    }
}
