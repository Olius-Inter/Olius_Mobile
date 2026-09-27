package com.olius.app.domain.usecases

import com.olius.app.domain.repository.FakeUserRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthUseCasesTest {

    private val repository = FakeUserRepository()

    @Test
    fun `login repassa e-mail e senha e devolve o resultado do repositorio`() = runTest {
        val failure = Result.failure<Unit>(IllegalStateException("erro"))
        repository.signInResult = failure

        val result = LoginUseCase(repository)("ana@olius.com", "123456")

        assertEquals(failure, result)
        assertEquals("ana@olius.com", repository.lastEmail)
        assertEquals("123456", repository.lastPassword)
    }

    @Test
    fun `cadastro repassa e-mail e senha e devolve o resultado do repositorio`() = runTest {
        val result = RegisterUseCase(repository)("bia@olius.com", "abcdef")

        assertTrue(result.isSuccess)
        assertEquals("bia@olius.com", repository.lastEmail)
        assertEquals("abcdef", repository.lastPassword)
    }

    @Test
    fun `login com Google repassa o id token`() = runTest {
        val result = GoogleSignInUseCase(repository)("token-google")

        assertTrue(result.isSuccess)
        assertEquals("token-google", repository.lastIdToken)
    }

    @Test
    fun `logout encerra a sessao no repositorio`() {
        repository.loggedIn = true

        LogoutUseCase(repository)()

        assertEquals(1, repository.signOutCalls)
        assertFalse(repository.loggedIn)
    }

    @Test
    fun `isUserLoggedIn reflete a sessao do repositorio`() {
        val useCase = IsUserLoggedInUseCase(repository)

        assertFalse(useCase())
        repository.loggedIn = true
        assertTrue(useCase())
    }
}
