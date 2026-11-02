package com.olius.app.data.remote

import com.olius.app.domain.entity.AccountRegistration
import com.olius.app.domain.entity.EstablishmentType
import com.olius.app.domain.entity.RegistrationAddress
import org.json.JSONArray
import org.json.JSONObject

/** Tokens devolvidos pela Core (IdentityResponses.MobileTokens). */
data class MobileTokens(
    val accessToken: String,
    val accessExpiresInSeconds: Long,
    val refreshToken: String,
    /** `refreshExpiresAt` (ISO-8601), guardado para saber se a sessão salva ainda vale. */
    val refreshExpiresAt: String?
) {
    override fun toString() = "MobileTokens[redacted]"
}

/** Rotas de autenticação e cadastro da Core. */
class CoreAuthApi(private val client: ApiClient) {
    suspend fun login(email: String, password: String): MobileTokens =
        parseTokens(client.post(ApiConfig.Main.AUTH_MOBILE_LOGIN, JSONObject().put("email", email).put("password", password).toString()))

    suspend fun refresh(refreshToken: String): MobileTokens =
        parseTokens(client.post(ApiConfig.Main.AUTH_MOBILE_REFRESH, JSONObject().put("refreshToken", refreshToken).toString()))

    /** 204; encerra só a sessão deste aparelho. */
    suspend fun logout(accessToken: String) {
        client.post(ApiConfig.Main.AUTH_MOBILE_LOGOUT, "{}", mapOf("Authorization" to "Bearer $accessToken"))
    }

    /** 201 {id, userType}. */
    suspend fun register(registration: AccountRegistration, password: String) {
        val body = registrationJson(registration).put("password", password)
        client.post(registrationPath(registration, google = false), body.toString())
    }

    /** Rota nova (ainda não existe na Core): 404 PROFILE_REQUIRED quando a conta não existe. */
    suspend fun googleLogin(idToken: String): MobileTokens =
        parseTokens(client.post(ApiConfig.Main.AUTH_MOBILE_GOOGLE, JSONObject().put("idToken", idToken).toString()))

    /** Rota nova (ainda não existe na Core): cadastro sem senha que já devolve a sessão. */
    suspend fun googleRegister(idToken: String, registration: AccountRegistration): MobileTokens {
        val body = registrationJson(registration).put("idToken", idToken)
        return parseTokens(client.post(registrationPath(registration, google = true), body.toString()))
    }

    suspend fun establishmentTypes(): List<EstablishmentType> {
        val array = JSONArray(client.get(ApiConfig.Main.ESTABLISHMENT_TYPES))
        return (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            EstablishmentType(
                id = item.getString("id"),
                name = item.getString("name"),
                description = item.optString("description").takeIf { it.isNotEmpty() && it != "null" }
            )
        }
    }

    private fun registrationPath(registration: AccountRegistration, google: Boolean) = when (registration) {
        is AccountRegistration.Citizen ->
            if (google) ApiConfig.Main.REGISTER_CITIZEN_GOOGLE else ApiConfig.Main.REGISTER_CITIZEN
        is AccountRegistration.Establishment ->
            if (google) ApiConfig.Main.REGISTER_ESTABLISHMENT_GOOGLE else ApiConfig.Main.REGISTER_ESTABLISHMENT
    }

    internal fun registrationJson(registration: AccountRegistration): JSONObject {
        val json = JSONObject()
            .put("name", registration.name)
            .put("email", registration.email)
            .put("telephones", JSONArray(registration.telephones))
        when (registration) {
            is AccountRegistration.Citizen -> json.put("cpf", registration.cpf)
            is AccountRegistration.Establishment -> json
                .put("cnpj", registration.cnpj)
                .put("typeId", registration.typeId)
                .put("description", registration.description ?: JSONObject.NULL)
                .put("address", addressJson(registration.address))
        }
        return json
    }

    private fun addressJson(address: RegistrationAddress) = JSONObject()
        .put("state", address.state)
        .put("city", address.city)
        .put("neighborhood", address.neighborhood)
        .put("street", address.street)
        .put("number", address.number)
        .put("cep", address.cep)
        .put("complement", address.complement ?: JSONObject.NULL)

    private fun parseTokens(body: String): MobileTokens {
        val json = JSONObject(body)
        return MobileTokens(
            accessToken = json.getString("accessToken"),
            accessExpiresInSeconds = json.optLong("expiresIn", DEFAULT_ACCESS_SECONDS),
            refreshToken = json.getString("refreshToken"),
            refreshExpiresAt = json.optString("refreshExpiresAt").ifEmpty { null }
        )
    }

    private companion object {
        /** Access token da Core: 15 minutos. */
        const val DEFAULT_ACCESS_SECONDS = 900L
    }
}
