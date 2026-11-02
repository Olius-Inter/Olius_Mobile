package com.olius.app.domain.usecases

import com.olius.app.domain.entity.AccountRegistration
import com.olius.app.domain.entity.ProfileType
import com.olius.app.domain.entity.ProfileUpdate
import com.olius.app.domain.entity.UserProfile
import com.olius.app.domain.repository.FakeUserRepository
import com.olius.app.domain.repository.UserProfileRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class AuthUseCasesTest {

    private val repository = FakeUserRepository()
    private val citizen = AccountRegistration.Citizen("Bia", "bia@olius.com", listOf("5511999998888"), "52998224725")

    @Test
    fun `login repassa e-mail e senha e devolve o resultado do repositorio`() = runTest {
        val failure = Result.failure<Unit>(IllegalStateException("erro"))
        repository.signInResult = failure

        val result = LoginUseCase(repository)("ana@olius.com", "Senha@12345")

        assertEquals(failure, result)
        assertEquals("ana@olius.com", repository.lastEmail)
        assertEquals("Senha@12345", repository.lastPassword)
    }

    @Test
    fun `cadastro repassa os dados e a senha`() = runTest {
        val result = RegisterUseCase(repository)(citizen, "Senha@12345")

        assertTrue(result.isSuccess)
        assertEquals(citizen, repository.lastRegistration)
        assertEquals("Senha@12345", repository.lastPassword)
    }

    @Test
    fun `login com Google repassa o id token`() = runTest {
        val result = GoogleSignInUseCase(repository)("token-google")

        assertTrue(result.isSuccess)
        assertEquals("token-google", repository.lastIdToken)
    }

    @Test
    fun `completar cadastro do Google repassa o token e os dados sem senha`() = runTest {
        val result = CompleteGoogleSignUpUseCase(repository)("token-google", citizen)

        assertTrue(result.isSuccess)
        assertEquals("token-google", repository.lastIdToken)
        assertEquals(citizen, repository.lastRegistration)
        assertNull(repository.lastPassword)
    }

    @Test
    fun `tipos de estabelecimento vem do repositorio`() = runTest {
        val types = GetEstablishmentTypesUseCase(repository)().getOrThrow()

        assertEquals(listOf("Restaurante", "Hotel"), types.map { it.name })
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

    @Test
    fun `sync devolve o perfil da Core e, sem rede, o do cache`() = runTest {
        val profile = UserProfile("u-1", "Ana", "ana@olius.com", ProfileType.CITIZEN)
        var remote: Result<UserProfile> = Result.success(profile)
        var cached: UserProfile? = null
        val profiles = object : UserProfileRepository {
            override fun cachedProfile() = cached
            override suspend fun fetchMyProfile() = remote
            override suspend fun updateMyProfile(update: ProfileUpdate) = Result.failure<UserProfile>(IOException())
            override suspend fun fetchCollectorStatus() = Result.success(false)
            override fun saveLocal(profile: UserProfile) { cached = profile }
            override fun clear() { cached = null }
        }
        val sync = SyncUserProfileUseCase(profiles)

        assertEquals(profile, sync())

        remote = Result.failure(IOException("sem rede"))
        assertNull(sync())

        cached = profile.copy(name = "Ana do cache")
        assertEquals("Ana do cache", sync()?.name)
    }
}
