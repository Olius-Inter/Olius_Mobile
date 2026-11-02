package com.olius.app.data.remote

import com.olius.app.domain.entity.AccountRegistration
import com.olius.app.domain.entity.EstablishmentType
import com.olius.app.domain.entity.RegistrationAddress
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreAuthApiTest {
    private val client = mockk<ApiClient>()
    private val api = CoreAuthApi(client)
    private val tokensJson = """
        {"accessToken":"a-1","tokenType":"Bearer","expiresIn":900,"expiresAt":"2030-01-01T00:15:00Z",
         "refreshToken":"r-1","refreshExpiresAt":"2030-01-31T00:00:00Z"}
    """.trimIndent()

    private val citizen = AccountRegistration.Citizen("Bia", "bia@olius.com", listOf("5511999998888"), "52998224725")
    private val establishment = AccountRegistration.Establishment(
        name = "Padaria",
        email = "p@olius.com",
        telephones = emptyList(),
        cnpj = "11222333000181",
        typeId = "t-1",
        address = RegistrationAddress("SP", "São Paulo", "Centro", "Rua A", "10", "01001000")
    )

    @Test
    fun `login manda e-mail e senha e le os tokens`() = runTest {
        val body = slot<String>()
        coEvery { client.post(ApiConfig.Main.AUTH_MOBILE_LOGIN, capture(body), any()) } returns tokensJson

        val tokens = api.login("ana@olius.com", "Senha@12345")

        assertEquals(MobileTokens("a-1", 900, "r-1", "2030-01-31T00:00:00Z"), tokens)
        val sent = JSONObject(body.captured)
        assertEquals(setOf("email", "password"), sent.keyNames())
    }

    @Test
    fun `refresh manda so o refreshToken`() = runTest {
        val body = slot<String>()
        coEvery { client.post(ApiConfig.Main.AUTH_MOBILE_REFRESH, capture(body), any()) } returns tokensJson

        api.refresh("r-0")

        assertEquals("""{"refreshToken":"r-0"}""", body.captured)
    }

    @Test
    fun `logout manda o Bearer explicito`() = runTest {
        coEvery { client.post(any(), any(), any()) } returns ""

        api.logout("a-1")

        coVerify { client.post(ApiConfig.Main.AUTH_MOBILE_LOGOUT, "{}", mapOf("Authorization" to "Bearer a-1")) }
    }

    @Test
    fun `cadastro de cidadao manda so os campos da Core, sem nascimento`() = runTest {
        val body = slot<String>()
        coEvery { client.post(ApiConfig.Main.REGISTER_CITIZEN, capture(body), any()) } returns """{"id":"u-1"}"""

        api.register(citizen, "Senha@12345")

        val sent = JSONObject(body.captured)
        assertEquals(setOf("name", "email", "password", "cpf", "telephones"), sent.keyNames())
        assertEquals("52998224725", sent.getString("cpf"))
        assertEquals("5511999998888", sent.getJSONArray("telephones").getString(0))
    }

    @Test
    fun `cadastro de estabelecimento manda cnpj, tipo e endereco`() = runTest {
        val body = slot<String>()
        coEvery { client.post(ApiConfig.Main.REGISTER_ESTABLISHMENT, capture(body), any()) } returns """{"id":"u-2"}"""

        api.register(establishment, "Senha@12345")

        val sent = JSONObject(body.captured)
        assertEquals(
            setOf("name", "email", "password", "cnpj", "typeId", "description", "address", "telephones"),
            sent.keyNames()
        )
        val address = sent.getJSONObject("address")
        assertEquals("SP", address.getString("state"))
        assertEquals("01001000", address.getString("cep"))
        assertTrue(address.isNull("complement"))
    }

    @Test
    fun `Google manda o idToken e o completar cadastro vai sem senha`() = runTest {
        val loginBody = slot<String>()
        val registerBody = slot<String>()
        coEvery { client.post(ApiConfig.Main.AUTH_MOBILE_GOOGLE, capture(loginBody), any()) } returns tokensJson
        coEvery { client.post(ApiConfig.Main.REGISTER_ESTABLISHMENT_GOOGLE, capture(registerBody), any()) } returns tokensJson

        api.googleLogin("g-1")
        val tokens = api.googleRegister("g-1", establishment)

        assertEquals("""{"idToken":"g-1"}""", loginBody.captured)
        val sent = JSONObject(registerBody.captured)
        assertEquals("g-1", sent.getString("idToken"))
        assertFalse(sent.has("password"))
        assertEquals("r-1", tokens.refreshToken)
    }

    @Test
    fun `cidadao do Google usa a rota de cidadao`() = runTest {
        coEvery { client.post(ApiConfig.Main.REGISTER_CITIZEN_GOOGLE, any(), any()) } returns tokensJson

        assertEquals("a-1", api.googleRegister("g-1", citizen).accessToken)
    }

    @Test
    fun `tipos de estabelecimento vem da lista publica`() = runTest {
        coEvery { client.get(ApiConfig.Main.ESTABLISHMENT_TYPES, any()) } returns
            """[{"id":"t-1","name":"Hotel","description":null},{"id":"t-2","name":"Restaurante","description":"Comida"}]"""

        assertEquals(
            listOf(EstablishmentType("t-1", "Hotel", null), EstablishmentType("t-2", "Restaurante", "Comida")),
            api.establishmentTypes()
        )
    }

    @Test
    fun `problem code e campos vem do corpo do erro`() {
        val error = ApiException(400, """{"code":"VALIDATION_ERROR","errors":[{"field":"cpf"},{"field":"address.cep"}]}""")

        assertEquals("VALIDATION_ERROR", error.problemCode)
        assertEquals(listOf("cpf", "address.cep"), error.problemFields)
        assertEquals(null, ApiException(500, "fora do ar").problemCode)
        assertEquals(emptyList<String>(), ApiException(500, "fora do ar").problemFields)
    }
}

private fun JSONObject.keyNames(): Set<String> = keys().asSequence().toSet()
