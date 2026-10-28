package com.olius.app.data.repository

import com.olius.app.data.local.SessionStore
import com.olius.app.data.remote.ApiClient
import com.olius.app.data.remote.ApiConfig
import com.olius.app.data.remote.ApiException
import com.olius.app.domain.entity.ProfileNotFoundException
import com.olius.app.domain.entity.ProfileRegistration
import com.olius.app.domain.entity.ProfileType
import com.olius.app.domain.entity.UserProfile
import com.olius.app.domain.repository.UserProfileRepository
import org.json.JSONObject

/** O perfil fica salvo no [store] nas duas versões; só muda de onde ele vem. */
abstract class StoredUserProfileRepository(protected val store: SessionStore) : UserProfileRepository {

    override fun cachedProfile(): UserProfile? = store.load()

    override fun saveLocal(profile: UserProfile) = store.save(profile)

    override fun clear() = store.clear()
}

/** Perfil no backend principal. Contrato JSON em API_00_CONEXAO_API.md. */
class HttpUserProfileRepository(
    private val client: ApiClient,
    store: SessionStore
) : StoredUserProfileRepository(store) {

    override suspend fun fetchMyProfile(): Result<UserProfile> = runCatching {
        try {
            parse(JSONObject(client.get(ApiConfig.Main.USERS_ME))).also(store::save)
        } catch (e: ApiException) {
            if (e.code == 404) throw ProfileNotFoundException() else throw e
        }
    }

    override suspend fun createProfile(registration: ProfileRegistration): Result<UserProfile> = runCatching {
        val body = JSONObject()
            .put("userType", registration.profileType.apiValue)
            .put("name", registration.name)
            .put("email", registration.email)
            .put("phone", registration.phone ?: JSONObject.NULL)
            .put("birthDate", registration.birthDate ?: JSONObject.NULL)
            .toString()
        parse(JSONObject(client.post(ApiConfig.Main.USERS, body))).also(store::save)
    }

    /**
     * Formato do `Profile` da Core (IdentityResponses.Profile). `isCollector`
     * ainda não existe lá: enquanto não vier, vale false (ver MAPA_PENDENCIAS.md).
     */
    private fun parse(json: JSONObject) = UserProfile(
        id = json.getString("id"),
        name = json.optString("name"),
        email = json.optString("email"),
        profileType = ProfileType.fromApi(json.getString("userType")),
        phone = json.optJSONArray("telephones")?.takeIf { it.length() > 0 }?.optString(0),
        isCollector = json.optBoolean("isCollector"),
        qrToken = json.optStringOrNull("qrToken")
    )
}

/**
 * Sem backend principal (ApiConfig.USE_MOCK_MAIN_BACKEND): o "banco" é o
 * próprio aparelho. Os ids batem com o seed da API Redis (e-01 = ToKonFome,
 * c-01 = Maria Silva) para o ranking destacar "você".
 */
class MockUserProfileRepository(store: SessionStore) : StoredUserProfileRepository(store) {

    override suspend fun fetchMyProfile(): Result<UserProfile> =
        store.load()?.takeIf { !it.pendingSync }?.let { Result.success(it) }
            ?: Result.failure(ProfileNotFoundException())

    override suspend fun createProfile(registration: ProfileRegistration): Result<UserProfile> {
        val id = if (registration.profileType == ProfileType.ESTABLISHMENT) "e-01" else "c-01"
        val profile = UserProfile(
            id = id,
            name = registration.name,
            email = registration.email,
            profileType = registration.profileType,
            isCollector = ApiConfig.DEBUG_MOCK_COLLECTOR,
            qrToken = "olius-demo-$id"
        )
        store.save(profile)
        return Result.success(profile)
    }
}
