package com.olius.app.presentation

import com.google.firebase.auth.FirebaseAuth
import com.olius.app.presentation.screen.PerfilType.PerfilTypeViewModel
import com.olius.app.presentation.screen.home.HomeViewModel
import com.olius.app.presentation.splash.SplashUiState
import com.olius.app.presentation.splash.SplashViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Garante que os ViewModels sobem com as dependências padrão (as que o app
 * usa de verdade), trocando só o `FirebaseAuth.getInstance()` por um mock.
 */
class DefaultConstructorsTest {

    private val auth = mockk<FirebaseAuth>(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(FirebaseAuth::class)
        every { FirebaseAuth.getInstance() } returns auth
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `SplashViewModel usa o Firebase por padrao`() {
        every { auth.currentUser } returns null
        val viewModel = SplashViewModel()

        viewModel.onSplashAnimationFinished()

        assertEquals(SplashUiState.NavigatePerfilType, viewModel.uiState.value)
    }

    @Test
    fun `HomeViewModel e PerfilTypeViewModel sobem com as dependencias padrao`() {
        HomeViewModel().onLogoutClick()
        PerfilTypeViewModel().onHeaderIntroFinished()

        verify { auth.signOut() }
    }
}
