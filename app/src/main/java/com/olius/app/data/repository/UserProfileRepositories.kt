package com.olius.app.data.repository

import com.olius.app.data.local.SessionStore
import com.olius.app.data.remote.ApiClient
import com.olius.app.data.remote.ApiConfig
import com.olius.app.data.remote.ApiException
import com.olius.app.domain.entity.InvalidProfileException
import com.olius.app.domain.entity.ProfileConflictException
import com.olius.app.domain.entity.ProfileEditUnavailableException
import com.olius.app.domain.entity.ProfileNotFoundException
import com.olius.app.domain.entity.ProfileType
import com.olius.app.domain.entity.ProfileUpdate
import com.olius.app.domain.entity.UserProfile
import com.olius.app.domain.repository.UserProfileRepository
import org.json.JSONObject

/** O perfil fica salvo no [store] nas duas versões; só muda de onde ele vem. */
abstract class StoredUserProfileRepository(protected val store: SessionStore) : UserProfileRepository {

    override fun cachedProfile(): UserProfile? = store.load()

    override fun saveLocal(profile: UserProfile) = store.save(profile)

    override fun clear() = store.clear()

    /** Guarda o "Coletor" no perfil do cache (se houver) e devolve o valor. */
    protected fun saveCollector(isCollector: Boolean): Boolean {
        store.load()?.let { store.save(it.copy(isCollector = isCollector)) }
        return isCollector
    }
}

/**
 * Perfil na Core (GET /api/v1/users/me com o JWT da sessão). O [client] é o
 * mainClient do AppContainer: manda o access token e renova no 401.
 */
class HttpUserProfileRepository(
    private val client: ApiClient,
    store: SessionStore
) : StoredUserProfileRepository(store) {

    override suspend fun fetchMyProfile(): Result<UserProfile> = runCatching {
        parse(JSONObject(client.get(ApiConfig.Main.USERS_ME))).also(store::save)
    }

    /** A rota ainda não existe na Core: hoje cai no 405 (só há GET em /users/me). */
    override suspend fun updateMyProfile(update: ProfileUpdate): Result<UserProfile> = runCatching {
        val body = JSONObject()
            .put("name", update.name)
            .put("email", update.email)
            .put("telephone", update.telephone ?: JSONObject.NULL)
        try {
            parse(JSONObject(client.patch(ApiConfig.Main.USERS_ME, body.toString()))).also(store::save)
        } catch (e: ApiException) {
            throw when (e.code) {
                HTTP_NOT_FOUND, HTTP_METHOD_NOT_ALLOWED -> ProfileEditUnavailableException()
                HTTP_CONFLICT -> ProfileConflictException()
                HTTP_BAD_REQUEST -> InvalidProfileException(e.problemFields)
                else -> e
            }
        }
    }

    /** GET /pevs/me: Coletor só com status APPROVED; 404 = a conta não tem PEV. */
    override suspend fun fetchCollectorStatus(): Result<Boolean> = runCatching {
        val approved = try {
            JSONObject(client.get(ApiConfig.Main.PEVS_ME)).optString("status") == PEV_APPROVED
        } catch (e: ApiException) {
            if (e.code != HTTP_NOT_FOUND) throw e
            false
        }
        saveCollector(approved)
    }

    /**
     * Formato do `Profile` da Core (IdentityResponses.Profile). `isCollector`
     * ainda não existe lá: enquanto não vier, vale false (ver MAPA_PENDENCIAS.md).
     */
    private fun parse(json: JSONObject): UserProfile {
        val id = json.getString("id")
        // Sem `isCollector` no JSON, mantém o do cache (vem do GET /pevs/me) da mesma conta.
        val cachedCollector = store.load()?.takeIf { it.id == id }?.isCollector ?: false
        return UserProfile(
            id = id,
            name = json.optString("name"),
            email = json.optString("email"),
            profileType = ProfileType.fromApi(json.getString("userType")),
            phone = json.optJSONArray("telephones")?.takeIf { it.length() > 0 }?.optString(0),
            isCollector = json.optBoolean("isCollector", cachedCollector),
            qrToken = json.optStringOrNull("qrToken")
        )
    }

    private companion object {
        const val PEV_APPROVED = "APPROVED"
        const val HTTP_BAD_REQUEST = 400
        const val HTTP_NOT_FOUND = 404
        const val HTTP_METHOD_NOT_ALLOWED = 405
        const val HTTP_CONFLICT = 409
    }
}

/**
 * Perfil só do aparelho, sem rede: devolve o que estiver salvo. Usado nos
 * testes das telas; o app usa sempre o [HttpUserProfileRepository], porque o
 * login é real (Core) mesmo com ApiConfig.USE_MOCK_MAIN_BACKEND.
 */
class MockUserProfileRepository(store: SessionStore) : StoredUserProfileRepository(store) {

    override suspend fun fetchMyProfile(): Result<UserProfile> =
        store.load()?.let { Result.success(it) } ?: Result.failure(ProfileNotFoundException())

    /** Edição só no aparelho: atualiza o cache, como se a Core tivesse aceitado. */
    override suspend fun updateMyProfile(update: ProfileUpdate): Result<UserProfile> {
        val current = store.load() ?: return Result.failure(ProfileNotFoundException())
        val updated = current.copy(name = update.name, email = update.email, phone = update.telephone)
        store.save(updated)
        return Result.success(updated)
    }

    override suspend fun fetchCollectorStatus(): Result<Boolean> =
        Result.success(store.load()?.isCollector == true)
}
