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

/**
 * Sessão da Core no aparelho (docs/identidade-seguranca.md da Core):
 *
 * - access token (15 min) SÓ na memória; refresh token no [store] (cifrado).
 * - Um refresh por vez ([refreshMutex]): a Core revoga a sessão se o mesmo
 *   refresh for usado duas vezes. Quem chega enquanto outro renova espera e
 *   usa o token novo.
 * - O refresh novo é gravado ANTES de liberar o token para qualquer chamada.
 * - Refresh recusado (400/401) → sessão encerrada: limpa tudo e avisa por
 *   [sessionExpired] (a navegação volta para o login). Sem rede ou 5xx → a
 *   chamada falha, mas a sessão continua.
 *
 * [scope] roda o aviso de logout à Core sem segurar a tela. [onSessionEnded]
 * limpa o resto da sessão (ex.: o perfil em cache).
 */
class CoreSessionRepository(
    private val api: CoreAuthApi,
    private val store: RefreshTokenStore,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
    private val onSessionEnded: () -> Unit = {}
) {

    private class AccessToken(val value: String, val expiresAtMillis: Long)

    private val refreshMutex = Mutex()

    // Protege access + store + generation juntos (login, logout e fim do refresh).
    private val lock = Any()

    @Volatile
    private var access: AccessToken? = null

    // Muda a cada login/logout: um refresh que termina depois de um logout não
    // pode gravar o token de volta.
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

    /**
     * Access token para o `Authorization: Bearer`. Vencido (ou app recém-aberto)
     * → renova antes. null = não há sessão.
     */
    suspend fun accessToken(): String? {
        access?.takeIf { it.isFresh() }?.let { return it.value }
        if (store.load() == null) return null
        return refresh(rejected = access?.value)
    }

    /** A Core respondeu 401 para [rejected]: renova uma vez (ou usa o que outra chamada já renovou). */
    suspend fun onUnauthorized(rejected: String): String? = refresh(rejected)

    private suspend fun refresh(rejected: String?): String? = refreshMutex.withLock {
        // Outra chamada já renovou enquanto esta esperava: só usa o token novo.
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

    /**
     * Logout: apaga a sessão do aparelho na hora e, em segundo plano, chama
     * POST /auth/mobile/logout. Se o access já venceu, renova uma última vez
     * só para ter o Bearer do logout. Falha de rede aqui não volta a sessão.
     */
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

    // Grava o refresh primeiro (síncrono); só depois o access fica disponível.
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
