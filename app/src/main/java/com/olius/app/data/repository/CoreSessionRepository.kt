package com.olius.app.data.repository

import com.olius.app.data.local.RefreshTokenStore
import com.olius.app.data.local.StoredRefreshToken
import com.olius.app.data.remote.ApiException
import com.olius.app.data.remote.CoreAuthApi
import com.olius.app.data.remote.MobileTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

/** Sessão da Core no aparelho: - access token (15 min) SÓ na memória; refresh token no [store] (cifrado). */
class CoreSessionRepository(
    private val api: CoreAuthApi,
    private val store: RefreshTokenStore,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
    private val onSessionEnded: () -> Unit = {}
) {
    private class AccessToken(val value: String, val expiresAtMillis: Long)

    private val refreshMutex = Mutex()

    private val lock = Any()

    @Volatile
    private var access: AccessToken? = null

    @Volatile
    private var generation = 0L

    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    /** Há refresh salvo e ele ainda não venceu (a Splash usa para pular o login). */
    fun hasSession(): Boolean {
        val saved = store.load() ?: return false
        return saved.expiresAtMillis?.let { it > clock() } ?: true
    }

    /** Começa a sessão com os tokens do login/cadastro. */
    fun start(tokens: MobileTokens) {
        synchronized(lock) {
            generation++
            persist(tokens)
        }
    }

    /** Access token para o `Authorization: Bearer`. */
    suspend fun accessToken(): String? {
        access?.takeIf { it.isFresh() }?.let { return it.value }
        if (store.load() == null) return null
        return refresh(rejected = access?.value)
    }

    /** A Core respondeu 401 para [rejected]: renova uma vez (ou usa o que outra chamada já renovou). */
    suspend fun onUnauthorized(rejected: String): String? = refresh(rejected)

    private suspend fun refresh(rejected: String?): String? = refreshMutex.withLock {
        access?.takeIf { it.value != rejected && it.isFresh() }?.let { return it.value }
        val (saved, startedAt) = synchronized(lock) { store.load() to generation }
        if (saved == null) return null

        val tokens = try {
            api.refresh(saved.token)
        } catch (e: ApiException) {
            if (e.code == HTTP_BAD_REQUEST || e.code == HTTP_UNAUTHORIZED) {
                expire(startedAt)
                return null
            }
            throw e
        }
        synchronized(lock) {
            if (generation != startedAt) return null
            persist(tokens)
        }
        tokens.accessToken
    }

    fun signOut() {
        val (saved, bearer) = synchronized(lock) {
            val current = store.load() to access?.takeIf { it.isFresh() }?.value
            generation++
            access = null
            store.clear()
            current
        }
        onSessionEnded()
        if (saved == null && bearer == null) return
        scope.launch {
            runCatching {
                refreshMutex.withLock {
                    val token = bearer ?: saved?.let { api.refresh(it.token).accessToken } ?: return@withLock
                    api.logout(token)
                }
            }
        }
    }

    private fun expire(startedAt: Long) {
        synchronized(lock) {
            if (generation != startedAt) return
            generation++
            access = null
            store.clear()
        }
        onSessionEnded()
        _sessionExpired.tryEmit(Unit)
    }

    private fun persist(tokens: MobileTokens) {
        store.save(StoredRefreshToken(tokens.refreshToken, tokens.refreshExpiresAt?.let(::parseInstant)))
        access = AccessToken(tokens.accessToken, clock() + tokens.accessExpiresInSeconds * MILLIS_PER_SECOND)
    }

    private fun AccessToken.isFresh() = expiresAtMillis - RENEW_BEFORE_MS > clock()

    private fun parseInstant(value: String): Long? = runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()

    private companion object {
        const val HTTP_BAD_REQUEST = 400
        const val HTTP_UNAUTHORIZED = 401
        const val MILLIS_PER_SECOND = 1000L
        /** Renova 30 s antes de vencer, para o token não expirar no meio da chamada. */
        const val RENEW_BEFORE_MS = 30_000L
    }
}
