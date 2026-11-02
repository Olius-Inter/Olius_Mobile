package com.olius.app.data.repository

import com.olius.app.data.local.InMemoryRefreshTokenStore
import com.olius.app.data.remote.ApiException
import com.olius.app.data.remote.CoreAuthApi
import com.olius.app.data.remote.MobileTokens
import com.olius.app.domain.entity.AccountConflictException
import com.olius.app.domain.entity.AccountRegistration
import com.olius.app.domain.entity.AuthUnavailableException
import com.olius.app.domain.entity.DuplicateTelephoneException
import com.olius.app.domain.entity.EstablishmentType
import com.olius.app.domain.entity.InvalidCredentialsException
import com.olius.app.domain.entity.InvalidRegistrationException
import com.olius.app.domain.entity.ProfileRequiredException
import com.olius.app.domain.entity.TooManyAttemptsException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class UserRepositoryImplTest {

    private val api = mockk<CoreAuthApi>(relaxUnitFun = true)
    private val store = InMemoryRefreshTokenStore()
    private val tokens = MobileTokens("a-1", 900, "r-1", null)
    private val citizen = AccountRegistration.Citizen("Bia", "bia@olius.com", emptyList(), "52998224725")

    private fun TestScope.repository() = UserRepositoryImpl(api, CoreSessionRepository(api, store, this))

    /** Corpo de erro da Core (ProblemDetail) com o `code` e, se houver, os campos recusados. */
    private fun problem(code: String, errors: String? = null): String {
        val json = JSONObject().put("code", code)
        if (errors != null) json.put("errors", JSONArray(errors))
        return json.toString()
    }

    @Test
    fun `login abre a sessao com os tokens da Core`() = runTest {
        coEvery { api.login("ana@olius.com", "Senha@12345") } returns tokens
        val repository = repository()

        assertTrue(repository.signInWithEmail(" ana@olius.com ", "Senha@12345").isSuccess)

        assertEquals("r-1", store.load()?.token)
        assertTrue(repository.isUserLoggedIn())
    }

    @Test
    fun `cadastro cria a conta e entra com a mesma senha`() = runTest {
        coEvery { api.login("bia@olius.com", "Senha@12345") } returns tokens
        val repository = repository()

        assertTrue(repository.register(citizen, "Senha@12345").isSuccess)

        coVerifyOrder {
            api.register(citizen, "Senha@12345")
            api.login("bia@olius.com", "Senha@12345")
        }
        assertEquals("r-1", store.load()?.token)
    }

    @Test
    fun `Google existente entra e Google novo pede o cadastro`() = runTest {
        val repository = repository()
        coEvery { api.googleLogin("g-1") } returns tokens
        assertTrue(repository.signInWithGoogleIdToken("g-1").isSuccess)

        coEvery { api.googleLogin("g-2") } throws ApiException(404, problem("PROFILE_REQUIRED"))
        assertTrue(repository.signInWithGoogleIdToken("g-2").exceptionOrNull() is ProfileRequiredException)

        // Rota ainda inexistente na Core: 404 genérico.
        coEvery { api.googleLogin("g-3") } throws ApiException(404, problem("RESOURCE_NOT_FOUND"))
        assertTrue(repository.signInWithGoogleIdToken("g-3").exceptionOrNull() is AuthUnavailableException)
    }

    @Test
    fun `completar cadastro do Google ja abre a sessao`() = runTest {
        coEvery { api.googleRegister("g-1", citizen) } returns tokens
        val repository = repository()

        assertTrue(repository.completeGoogleRegistration("g-1", citizen).isSuccess)
        assertEquals("r-1", store.load()?.token)
    }

    @Test
    fun `erros http viram excecoes do dominio`() = runTest {
        val repository = repository()
        val cases = listOf(
            ApiException(401, problem("AUTHENTICATION_REQUIRED")) to InvalidCredentialsException::class,
            ApiException(409, problem("REGISTRATION_CONFLICT")) to AccountConflictException::class,
            ApiException(409, problem("DUPLICATE_TELEPHONE")) to DuplicateTelephoneException::class,
            ApiException(429, "") to TooManyAttemptsException::class
        )
        cases.forEach { (error, expected) ->
            coEvery { api.login(any(), any()) } throws error
            assertEquals(expected, repository.signInWithEmail("a@b.com", "x").exceptionOrNull()!!::class)
        }

        coEvery { api.register(any(), any()) } throws
            ApiException(400, problem("VALIDATION_ERROR", """[{"field":"password"},{"field":"cpf"}]"""))
        val invalid = repository.register(citizen, "fraca").exceptionOrNull() as InvalidRegistrationException
        assertEquals(listOf("password", "cpf"), invalid.fields)
    }

    @Test
    fun `sem rede e erro desconhecido sobem como vieram`() = runTest {
        val repository = repository()
        val offline = IOException("sem rede")
        coEvery { api.login(any(), any()) } throws offline
        assertSame(offline, repository.signInWithEmail("a@b.com", "x").exceptionOrNull())

        val server = ApiException(500, "")
        coEvery { api.login(any(), any()) } throws server
        assertSame(server, repository.signInWithEmail("a@b.com", "x").exceptionOrNull())
        assertFalse(repository.isUserLoggedIn())
    }

    @Test
    fun `tipos de estabelecimento vem da Core`() = runTest {
        val types = listOf(EstablishmentType("t-1", "Restaurante"))
        coEvery { api.establishmentTypes() } returns types

        assertEquals(types, repository().establishmentTypes().getOrThrow())
    }

    @Test
    fun `signOut apaga a sessao e chama o logout da Core`() = runTest {
        coEvery { api.login(any(), any()) } returns tokens
        val repository = repository()
        repository.signInWithEmail("a@b.com", "Senha@12345")

        repository.signOut()
        advanceUntilIdle()

        assertFalse(repository.isUserLoggedIn())
        coVerify { api.logout("a-1") }
    }
}
