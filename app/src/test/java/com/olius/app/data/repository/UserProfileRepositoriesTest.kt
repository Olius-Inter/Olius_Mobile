package com.olius.app.data.repository

import com.olius.app.data.local.InMemorySessionStore
import com.olius.app.data.remote.ApiClient
import com.olius.app.data.remote.ApiConfig
import com.olius.app.data.remote.ApiException
import com.olius.app.domain.entity.ProfileNotFoundException
import com.olius.app.domain.entity.ProfileRegistration
import com.olius.app.domain.entity.ProfileType
import com.olius.app.domain.entity.UserProfile
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserProfileRepositoriesTest {

    private val store = InMemorySessionStore()
    private val client = mockk<ApiClient>()
    private val http = HttpUserProfileRepository(client, store)
    private val mock = MockUserProfileRepository(store)

    private val citizen = ProfileRegistration(ProfileType.CITIZEN, "Ana Lima", "ana@olius.app", phone = null, birthDate = "2000-01-31")
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
    fun `http 404 vira ProfileNotFoundException e outros erros sobem`() = runTest {
        coEvery { client.get(any(), any()) } throws ApiException(404, "")
        assertTrue(http.fetchMyProfile().exceptionOrNull() is ProfileNotFoundException)

        coEvery { client.get(any(), any()) } throws ApiException(500, "")
        assertEquals(500, (http.fetchMyProfile().exceptionOrNull() as ApiException).code)
    }

    @Test
    fun `http cria o perfil com o corpo do cadastro`() = runTest {
        val body = slot<String>()
        coEvery { client.post(ApiConfig.Main.USERS, capture(body)) } returns
            """{"id": "u-1", "name": "Ana Lima", "email": "ana@olius.app", "userType": "CITIZENS"}"""

        assertEquals(profile, http.createProfile(citizen).getOrThrow())

        val sent = JSONObject(body.captured)
        assertEquals("CITIZENS", sent.getString("userType"))
        assertTrue(sent.isNull("phone"))
        assertEquals("2000-01-31", sent.getString("birthDate"))
        assertEquals(profile, store.load())
    }

    @Test
    fun `saveLocal e clear usam o armazenamento do aparelho`() {
        http.saveLocal(profile)
        assertEquals(profile, mock.cachedProfile())

        mock.clear()
        assertNull(http.cachedProfile())
    }

    @Test
    fun `mock sem perfil salvo ou com perfil pendente nao encontra`() = runTest {
        assertTrue(mock.fetchMyProfile().exceptionOrNull() is ProfileNotFoundException)

        mock.saveLocal(profile.copy(pendingSync = true))
        assertTrue(mock.fetchMyProfile().exceptionOrNull() is ProfileNotFoundException)

        mock.saveLocal(profile)
        assertEquals(profile, mock.fetchMyProfile().getOrThrow())
    }

    @Test
    fun `mock cria o perfil com os ids do seed da api redis`() = runTest {
        assertEquals("c-01", mock.createProfile(citizen).getOrThrow().id)

        val establishment = mock.createProfile(citizen.copy(profileType = ProfileType.ESTABLISHMENT)).getOrThrow()
        assertEquals("e-01", establishment.id)
        assertEquals("olius-demo-e-01", establishment.qrToken)
        assertEquals(establishment, store.load())
    }
}
