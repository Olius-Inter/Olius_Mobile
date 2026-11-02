package com.olius.app.domain.usecases

import com.olius.app.data.local.InMemorySessionStore
import com.olius.app.data.repository.MockUserProfileRepository
import com.olius.app.domain.entity.ProfileType
import com.olius.app.domain.entity.UserProfile
import com.olius.app.domain.repository.UserProfileRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

class SyncUserProfileUseCaseTest {
    private val profile = UserProfile("u-1", "Maria", "maria@x.com", ProfileType.CITIZEN)

    @Test
    fun `sem perfil em lugar nenhum devolve null`() = runTest {
        assertNull(SyncUserProfileUseCase(MockUserProfileRepository(InMemorySessionStore()))())
    }

    @Test
    fun `usa o perfil que a Core devolve`() = runTest {
        val store = InMemorySessionStore().apply { save(profile) }

        assertEquals(profile, SyncUserProfileUseCase(MockUserProfileRepository(store))())
    }

    @Test
    fun `sem rede devolve o cache`() = runTest {
        val store = InMemorySessionStore().apply { save(profile) }
        val offline = object : UserProfileRepository by MockUserProfileRepository(store) {
            override suspend fun fetchMyProfile(): Result<UserProfile> = Result.failure(IOException("sem rede"))
        }

        assertEquals(profile, SyncUserProfileUseCase(offline)())
    }
}
