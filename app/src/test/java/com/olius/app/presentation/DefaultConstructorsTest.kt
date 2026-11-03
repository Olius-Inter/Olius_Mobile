package com.olius.app.presentation

import com.olius.app.MainDispatcherRule
import com.olius.app.data.remote.AppContainer
import com.olius.app.presentation.screen.PerfilType.ForgotPasswordViewModel
import com.olius.app.presentation.screen.PerfilType.PerfilTypeViewModel
import com.olius.app.presentation.screen.chat.ChatViewModel
import com.olius.app.presentation.screen.error.ConnectionErrorUiState
import com.olius.app.presentation.screen.error.ConnectionErrorViewModel
import com.olius.app.presentation.screen.ranking.RankingTab
import com.olius.app.presentation.screen.ranking.RankingViewModel
import com.olius.app.presentation.screen.home.HomeViewModel
import com.olius.app.presentation.screen.profile.EditProfileViewModel
import com.olius.app.presentation.screen.profile.ProfileViewModel
import com.olius.app.presentation.splash.SplashUiState
import com.olius.app.presentation.splash.SplashViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/** Garante que os ViewModels sobem com as dependências padrão (as que o app usa de verdade). */
class DefaultConstructorsTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `SplashViewModel usa a sessao da Core por padrao`() {
        val viewModel = SplashViewModel()

        viewModel.onSplashAnimationFinished()

        assertEquals(SplashUiState.NavigatePerfilType, viewModel.uiState.value)
    }

    @Test
    fun `HomeViewModel, ProfileViewModel e PerfilTypeViewModel sobem com as dependencias padrao`() {
        HomeViewModel().onNotificationsClick()
        ProfileViewModel().onLogoutClick()
        EditProfileViewModel().onNameChange("Ana")
        PerfilTypeViewModel().onHeaderIntroFinished()
        ForgotPasswordViewModel().onToggleNewPasswordVisibility()
        ChatViewModel().onToggleAttachmentMenu()
        RankingViewModel().onTabSelected(RankingTab.ACHIEVEMENTS)
        val error = ConnectionErrorViewModel()
        error.onBackToStartClick()

        assertEquals(ConnectionErrorUiState.NavigatePerfilType, error.uiState.value)
        assertFalse(AppContainer.userRepository.isUserLoggedIn())
    }

    @Test
    fun `sem sessao o provedor de token da Core devolve null`() = runTest {
        assertNull(AppContainer.coreAccessTokenProvider())
    }
}
