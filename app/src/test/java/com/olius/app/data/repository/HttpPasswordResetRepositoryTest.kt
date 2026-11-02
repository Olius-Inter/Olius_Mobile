package com.olius.app.data.repository

import com.olius.app.data.remote.ApiClient
import com.olius.app.data.remote.ApiConfig
import com.olius.app.data.remote.ApiException
import com.olius.app.domain.entity.AuthUnavailableException
import com.olius.app.domain.entity.InvalidResetCodeException
import com.olius.app.domain.entity.TooManyAttemptsException
import com.olius.app.domain.entity.WeakPasswordException
import com.olius.app.domain.repository.PasswordResetTicket
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class HttpPasswordResetRepositoryTest {
    private val client = mockk<ApiClient>()
    private val repository = HttpPasswordResetRepository(client)

    private val weakPasswordProblem =
        """{"code":"VALIDATION_ERROR","errors":[{"field":"newPassword","message":"inválida"}]}"""

    @Test
    fun `pedir o link manda so o e-mail para password-reset-requests`() = runTest {
        val body = slot<String>()
        coEvery { client.post(ApiConfig.Main.PASSWORD_RESET_REQUESTS, capture(body), any()) } returns """{"message":"ok"}"""

        assertTrue(repository.requestResetLink("ana@olius.com").isSuccess)
        assertEquals("""{"email":"ana@olius.com"}""", body.captured)
    }
    @Test
    fun `pedir o codigo manda so o e-mail`() = runTest {
        val body = slot<String>()
        coEvery { client.post(ApiConfig.Main.PASSWORD_RESETS, capture(body), any()) } returns ""

        assertTrue(repository.requestCode("ana@olius.com").isSuccess)
        assertEquals("""{"email":"ana@olius.com"}""", body.captured)
    }

    @Test
    fun `verificar manda e-mail e codigo e le o resetToken`() = runTest {
        val body = slot<String>()
        coEvery { client.post(ApiConfig.Main.PASSWORD_RESETS_VERIFY, capture(body), any()) } returns
            """{"resetToken":"tok","expiresAt":"2030-01-01T00:10:00Z"}"""

        val ticket = repository.verifyCode("ana@olius.com", "012345").getOrThrow()

        assertEquals(PasswordResetTicket("tok", "2030-01-01T00:10:00Z"), ticket)
        val sent = JSONObject(body.captured)
        assertEquals(2, sent.length())
        assertEquals("ana@olius.com", sent.getString("email"))
        assertEquals("012345", sent.getString("code"))
    }

    @Test
    fun `confirmar manda resetToken e newPassword`() = runTest {
        val body = slot<String>()
        coEvery { client.post(ApiConfig.Main.PASSWORD_RESETS_CONFIRM, capture(body), any()) } returns ""

        assertTrue(repository.confirm("tok", "Senha@12345").isSuccess)
        val sent = JSONObject(body.captured)
        assertEquals(2, sent.length())
        assertEquals("tok", sent.getString("resetToken"))
        assertEquals("Senha@12345", sent.getString("newPassword"))
    }

    @Test
    fun `400 no verify vira codigo invalido`() = runTest {
        coEvery { client.post(ApiConfig.Main.PASSWORD_RESETS_VERIFY, any(), any()) } throws ApiException(400, "{}")

        assertTrue(repository.verifyCode("a@b.com", "000000").exceptionOrNull() is InvalidResetCodeException)
    }

    @Test
    fun `400 no confirm separa senha fraca de token vencido`() = runTest {
        coEvery { client.post(ApiConfig.Main.PASSWORD_RESETS_CONFIRM, any(), any()) } throws
            ApiException(400, weakPasswordProblem) andThenThrows ApiException(400, """{"detail":"Código inválido ou expirado"}""")

        assertTrue(repository.confirm("tok", "fraca").exceptionOrNull() is WeakPasswordException)
        assertTrue(repository.confirm("tok", "Senha@12345").exceptionOrNull() is InvalidResetCodeException)
    }

    @Test
    fun `rota ainda nao publicada, limite e sem rede`() = runTest {
        coEvery { client.post(ApiConfig.Main.PASSWORD_RESETS, any(), any()) } throws
            ApiException(401, "") andThenThrows ApiException(404, "") andThenThrows
            ApiException(429, "") andThenThrows IOException("offline")

        assertTrue(repository.requestCode("a@b.com").exceptionOrNull() is AuthUnavailableException)
        assertTrue(repository.requestCode("a@b.com").exceptionOrNull() is AuthUnavailableException)
        assertTrue(repository.requestCode("a@b.com").exceptionOrNull() is TooManyAttemptsException)
        assertTrue(repository.requestCode("a@b.com").exceptionOrNull() is IOException)
    }
}
