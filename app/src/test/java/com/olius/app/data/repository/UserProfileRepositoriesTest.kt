package com.olius.app.data.repository

import com.olius.app.data.local.InMemorySessionStore
import com.olius.app.data.remote.ApiClient
import com.olius.app.data.remote.ApiConfig
import com.olius.app.data.remote.ApiException
import com.olius.app.domain.entity.InvalidProfileException
import com.olius.app.domain.entity.ProfileConflictException
import com.olius.app.domain.entity.ProfileEditUnavailableException
import com.olius.app.domain.entity.ProfileNotFoundException
import com.olius.app.domain.entity.ProfileUpdate
import com.olius.app.domain.entity.ProfileType
import com.olius.app.domain.entity.UserProfile
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.json.JSONObject
import org.junit.Test


class UserProfileRepositoriesTest {

    private val store = InMemorySessionStore()
    private val client = mockk<ApiClient>()
    private val http = HttpUserProfileRepository(client, store)
    private val mock = MockUserProfileRepository(store)

    private val profile = UserProfile("u-1", "Ana Lima", "ana@olius.app", ProfileType.CITIZEN)

    @Test
    fun `http busca o perfil da core e guarda no aparelho`() = runTest {
        coEvery { client.get(ApiConfig.Main.USERS_ME, any()) } returns """
            {"id": "u-1", "name": "Ana Lima", "email": "ana@olius.app", "userType": "CITIZENS",
             "telephones": ["11999990000"], "qrToken": "qr-1"}
        """.trimIndent()

        val expected = profile.copy(phone = "11999990000", qrToken = "qr-1")
        assertEquals(expected, http.fetchMyProfile().getOrThrow())
        assertEquals(expected, http.cachedProfile())
    }

    @Test
    fun `http sem telefone e sem qrToken deixa os campos nulos`() = runTest {
        coEvery { client.get(any(), any()) } returns
            """{"id": "u-2", "userType": "ESTABLISHMENT", "telephones": [], "qrToken": null, "isCollector": true}"""

        val fetched = http.fetchMyProfile().getOrThrow()

        assertEquals(UserProfile("u-2", "", "", ProfileType.ESTABLISHMENT, isCollector = true), fetched)
    }

    @Test
    fun `http com erro devolve a falha e mantem o cache`() = runTest {
        store.save(profile)
        coEvery { client.get(any(), any()) } throws ApiException(500, "")

        assertEquals(500, (http.fetchMyProfile().exceptionOrNull() as ApiException).code)
        assertEquals(profile, http.cachedProfile())
    }

    @Test
    fun `saveLocal e clear usam o armazenamento do aparelho`() {
        http.saveLocal(profile)
        assertEquals(profile, mock.cachedProfile())

        mock.clear()
        assertNull(http.cachedProfile())
    }

    @Test
    fun `PATCH users me manda nome, e-mail e telefone e guarda a resposta`() = runTest {
        store.save(profile.copy(isCollector = true))
        coEvery { client.patch(ApiConfig.Main.USERS_ME, any()) } returns """
            {"id": "u-1", "name": "Ana Souza", "email": "ana.souza@olius.app", "userType": "CITIZENS",
             "telephones": ["5521988887777"]}
        """.trimIndent()

        val updated = http.updateMyProfile(ProfileUpdate("Ana Souza", "ana.souza@olius.app", "5521988887777"))
            .getOrThrow()

        coVerify {
            client.patch(ApiConfig.Main.USERS_ME, match {
                val body = JSONObject(it)
                body.length() == 3 && body.getString("name") == "Ana Souza" &&
                    body.getString("email") == "ana.souza@olius.app" && body.getString("telephone") == "5521988887777"
            })
        }
        assertEquals("Ana Souza", updated.name)
        // A Core não manda isCollector: fica o do cache (veio do /pevs/me).
        assertTrue(updated.isCollector)
        assertEquals(updated, http.cachedProfile())
    }

    @Test
    fun `PATCH sem rota na Core vira ProfileEditUnavailableException`() = runTest {
        store.save(profile)
        coEvery { client.patch(any(), any()) } throws ApiException(405, """{"code":"METHOD_NOT_ALLOWED"}""")
        assertTrue(http.updateMyProfile(ProfileUpdate("A", "a@b.co", null)).exceptionOrNull() is ProfileEditUnavailableException)

        coEvery { client.patch(any(), any()) } throws ApiException(404, "")
        assertTrue(http.updateMyProfile(ProfileUpdate("A", "a@b.co", null)).exceptionOrNull() is ProfileEditUnavailableException)
        assertEquals(profile, http.cachedProfile())
    }

    @Test
    fun `PATCH com 409 e 400 vira erro de conflito e de campos`() = runTest {
        coEvery { client.patch(any(), any()) } throws ApiException(409, "")
        assertTrue(http.updateMyProfile(ProfileUpdate("A", "a@b.co", null)).exceptionOrNull() is ProfileConflictException)

        coEvery { client.patch(any(), any()) } throws
            ApiException(400, """{"code":"VALIDATION_ERROR","errors":[{"field":"email"}]}""")
        val error = http.updateMyProfile(ProfileUpdate("A", "a@b", null)).exceptionOrNull()
        assertEquals(listOf("email"), (error as InvalidProfileException).fields)
    }

    @Test
    fun `coletor so com PEV APPROVED e 404 vira nao coletor`() = runTest {
        store.save(profile)
        coEvery { client.get(ApiConfig.Main.PEVS_ME, any()) } returns """{"id": "p-1", "status": "APPROVED"}"""
        assertTrue(http.fetchCollectorStatus().getOrThrow())
        assertTrue(http.cachedProfile()!!.isCollector)

        coEvery { client.get(ApiConfig.Main.PEVS_ME, any()) } returns """{"id": "p-1", "status": "PENDING"}"""
        assertFalse(http.fetchCollectorStatus().getOrThrow())

        coEvery { client.get(ApiConfig.Main.PEVS_ME, any()) } throws ApiException(404, "")
        assertFalse(http.fetchCollectorStatus().getOrThrow())
        assertFalse(http.cachedProfile()!!.isCollector)

        coEvery { client.get(ApiConfig.Main.PEVS_ME, any()) } throws ApiException(500, "")
        assertTrue(http.fetchCollectorStatus().isFailure)
    }

    @Test
    fun `mock devolve o perfil salvo ou ProfileNotFoundException`() = runTest {

        assertTrue(mock.fetchMyProfile().exceptionOrNull() is ProfileNotFoundException)

        mock.saveLocal(profile)
        assertEquals(profile, mock.fetchMyProfile().getOrThrow())
    }
}
