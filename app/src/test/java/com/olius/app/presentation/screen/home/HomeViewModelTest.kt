package com.olius.app.presentation.screen.home

import com.olius.app.domain.repository.FakeUserRepository
import com.olius.app.domain.usecases.LogoutUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeViewModelTest {

    private val repository = FakeUserRepository().apply { loggedIn = true }
    private val viewModel = HomeViewModel(LogoutUseCase(repository))

    @Test
    fun `logout encerra a sessao e fecha o perfil`() {
        viewModel.onProfileClick()
        assertTrue(viewModel.uiState.value.isProfileSheetOpen)

        viewModel.onLogoutClick()

        assertEquals(1, repository.signOutCalls)
        assertFalse(repository.loggedIn)
        assertFalse(viewModel.uiState.value.isProfileSheetOpen)
    }

    @Test
    fun `fechar o perfil nao encerra a sessao`() {
        viewModel.onProfileClick()

        viewModel.onDismissProfileSheet()

        assertEquals(0, repository.signOutCalls)
        assertFalse(viewModel.uiState.value.isProfileSheetOpen)
    }
}
