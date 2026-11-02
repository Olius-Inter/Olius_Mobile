package com.olius.app.data.remote

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.olius.app.data.local.InMemorySessionStore
import com.olius.app.data.local.SessionStore
import com.olius.app.data.local.SharedPrefsSessionStore
import com.olius.app.data.repository.HttpRankingRepository
import com.olius.app.data.repository.HttpUserProfileRepository
import com.olius.app.data.repository.MockRankingRepository
import com.olius.app.data.repository.MockUserProfileRepository
import com.olius.app.domain.entity.ProfileType
import com.olius.app.domain.repository.RankingRepository
import com.olius.app.domain.repository.UserProfileRepository
import kotlinx.coroutines.tasks.await

/**
 * Monta os repositórios usados pelos construtores padrão dos ViewModels.
 * Mock ou HTTP é decidido só pelas flags do [ApiConfig].
 * [init] é chamado pela MainActivity; sem ele (testes) a sessão fica em memória.
 */
object AppContainer {

    private var sessionStore: SessionStore = InMemorySessionStore()

    fun init(context: Context) {
        sessionStore = SharedPrefsSessionStore(context)
    }

    /** Token do Firebase enviado como Bearer; o backend valida e acha o users.id por ele. */
    private val firebaseToken: suspend () -> String? = {
        runCatching { FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token }.getOrNull()
    }

    private val mainClient by lazy { ApiClient(ApiConfig.MAIN_BASE_URL, firebaseToken) }

    val userProfileRepository: UserProfileRepository by lazy {
        if (ApiConfig.USE_MOCK_MAIN_BACKEND) MockUserProfileRepository(sessionStore)
        else HttpUserProfileRepository(mainClient, sessionStore)
    }

    /**
     * Access token da Core (`POST /api/v1/auth/mobile/login`), usado pela API
     * Redis para achar "Sua posição". O app ainda faz login pelo Firebase, e o
     * token do Firebase é recusado pela Core; por isso fica null até o login
     * na Core ser integrado. Sem ele o ranking aparece, só sem a posição.
     */
    private val coreAccessToken: suspend () -> String? = { null }

    /** Tipo da conta logada; sem perfil ainda, assume estabelecimento. */
    fun currentProfileType(): ProfileType =
        userProfileRepository.cachedProfile()?.profileType ?: ProfileType.ESTABLISHMENT

    val rankingRepository: RankingRepository by lazy {
        if (ApiConfig.USE_MOCK_RANKING) MockRankingRepository()
        else HttpRankingRepository(
            client = ApiClient(ApiConfig.RANKING_BASE_URL, coreAccessToken),
            anonymousClient = ApiClient(ApiConfig.RANKING_BASE_URL)
        )
    }
}
