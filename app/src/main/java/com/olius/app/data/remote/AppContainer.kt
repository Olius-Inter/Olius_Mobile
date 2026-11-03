package com.olius.app.data.remote

import android.content.Context
import com.olius.app.data.local.InMemoryAppPreferences
import com.olius.app.data.local.InMemorySessionStore
import com.olius.app.data.local.SharedPrefsAppPreferences
import com.olius.app.data.local.SessionStore
import com.olius.app.data.local.EncryptedRefreshTokenStore
import com.olius.app.data.local.InMemoryRefreshTokenStore
import com.olius.app.data.local.RefreshTokenStore
import com.olius.app.data.local.RoomSessionStore
import com.olius.app.data.local.db.OliusDatabase
import com.olius.app.data.repository.CoreSessionRepository
import com.olius.app.data.repository.AndroidAddressGeocoder
import com.olius.app.data.repository.HttpCollectionPointRepository
import com.olius.app.data.repository.HttpCollectorRepository
import com.olius.app.data.repository.HttpHistoryRepository
import com.olius.app.data.repository.HttpHomeRepository
import com.olius.app.data.repository.HttpPasswordResetRepository
import com.olius.app.data.repository.AppTipsRepository
import com.olius.app.data.repository.HttpRankingRepository
import com.olius.app.data.repository.HttpUserProfileRepository
import com.olius.app.data.repository.LibPhoneNumberRepository
import com.olius.app.data.repository.HttpOlivIARepository
import com.olius.app.data.repository.UserRepositoryImpl
import com.olius.app.data.repository.ViaCepRepository
import com.olius.app.domain.entity.ProfileType
import com.olius.app.domain.repository.AddressGeocoder
import com.olius.app.domain.repository.AppPreferencesRepository
import com.olius.app.domain.repository.CepRepository
import com.olius.app.domain.repository.CollectionPointRepository
import com.olius.app.domain.repository.CollectorRepository
import com.olius.app.domain.repository.HistoryRepository
import com.olius.app.domain.repository.HomeRepository
import com.olius.app.domain.repository.OlivIARepository
import com.olius.app.domain.repository.PasswordResetRepository
import com.olius.app.domain.repository.PhoneNumberRepository
import com.olius.app.domain.repository.TipsRepository
import com.olius.app.domain.repository.RankingRepository
import com.olius.app.domain.repository.UserProfileRepository
import com.olius.app.domain.repository.UserRepository
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharedFlow

/** Monta os repositórios usados pelos construtores padrão dos ViewModels. */
object AppContainer {
    private var sessionStore: SessionStore = InMemorySessionStore()
    private var refreshTokenStore: RefreshTokenStore = InMemoryRefreshTokenStore()
    private var preferences: AppPreferencesRepository = InMemoryAppPreferences()
    private var appContext: Context? = null

    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        sessionStore = RoomSessionStore(OliusDatabase.create(context).userProfileDao())
        refreshTokenStore = EncryptedRefreshTokenStore(context)
        preferences = SharedPrefsAppPreferences(context)
    }

    /** Tema e notificações (só no aparelho). */
    val appPreferences: AppPreferencesRepository get() = preferences

    /** Escopo do app para o que não pode depender de uma tela (ex.: avisar o logout à Core). */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Core sem Bearer automático: rotas públicas de auth, cadastro e "esqueci a senha". */
    private val publicCoreClient by lazy { ApiClient(ApiConfig.MAIN_BASE_URL) }

    private val coreAuthApi by lazy { CoreAuthApi(publicCoreClient) }

    /** Sessão da Core: access token na memória, refresh cifrado, um refresh por vez. */
    private val sessionRepository by lazy {
        CoreSessionRepository(
            api = coreAuthApi,
            store = refreshTokenStore,
            scope = appScope,
            onSessionEnded = { sessionStore.clear() }
        )
    }

    /** Login, cadastro, Google, logout e "está logado?" (sempre na Core). */
    val userRepository: UserRepository by lazy { UserRepositoryImpl(coreAuthApi, sessionRepository) }

    /** "Esqueci a senha" com código de 6 dígitos (sempre na Core, sem sessão). */
    val passwordResetRepository: PasswordResetRepository by lazy { HttpPasswordResetRepository(publicCoreClient) }

    /** Access token da Core para o `Authorization: Bearer` de quem fala com a Core ou com APIs que validam o JWT dela. */
    val coreAccessTokenProvider: suspend () -> String? = { sessionRepository.accessToken() }

    /** Emite quando a sessão acaba sozinha (refresh recusado): a navegação volta para o login. */
    val sessionExpired: SharedFlow<Unit> get() = sessionRepository.sessionExpired

    /** Core com o JWT da sessão; 401 → renova uma vez e repete a chamada. */
    private val mainClient by lazy {
        ApiClient(
            baseUrl = ApiConfig.MAIN_BASE_URL,
            tokenProvider = coreAccessTokenProvider,
            onUnauthorized = sessionRepository::onUnauthorized,
            requireToken = true
        )
    }

    /** GET /users/me na Core. */
    val userProfileRepository: UserProfileRepository by lazy { HttpUserProfileRepository(mainClient, sessionStore) }

    /** Tipo da conta logada; sem perfil ainda, assume estabelecimento. */
    fun currentProfileType(): ProfileType =
        userProfileRepository.cachedProfile()?.profileType ?: ProfileType.ESTABLISHMENT

    /** "Sua posição" usa o JWT da Core; 401 → o mesmo refresh único da sessão e repete. */
    val rankingRepository: RankingRepository by lazy {
        HttpRankingRepository.withCoreSession(
            baseUrl = ApiConfig.RANKING_BASE_URL,
            tokenProvider = coreAccessTokenProvider,
            onUnauthorized = sessionRepository::onUnauthorized,
            coreClient = mainClient
        )
    }

    /** Chat da OlivIA: a Core com o mesmo JWT e refresh do [mainClient], mas readTimeout de 60 s (a IA demora). */
    val olivIARepository: OlivIARepository by lazy {
        HttpOlivIARepository(
            ApiClient(
                baseUrl = ApiConfig.MAIN_BASE_URL,
                tokenProvider = coreAccessTokenProvider,
                onUnauthorized = sessionRepository::onUnauthorized,
                readTimeoutMs = ApiConfig.CHAT_READ_TIMEOUT_MS,
                requireToken = true
            )
        )
    }

    /** Home montada com as rotas que a Core já tem; o nome vem do perfil da conta logada. */
    val homeRepository: HomeRepository by lazy {
        HttpHomeRepository(mainClient, profileProvider = {
            userProfileRepository.cachedProfile() ?: userProfileRepository.fetchMyProfile().getOrNull()
        })
    }

    val historyRepository: HistoryRepository by lazy { HttpHistoryRepository(mainClient) }

    val collectionPointRepository: CollectionPointRepository by lazy {
        HttpCollectionPointRepository(
            mainClient,
            geocoder = appContext?.let(::AndroidAddressGeocoder) ?: AddressGeocoder.NONE
        )
    }

    /** DDI/máscara/validação do telefone, offline. */
    val phoneNumberRepository: PhoneNumberRepository by lazy {
        LibPhoneNumberRepository(utilProvider = {
            PhoneNumberUtil.createInstance(checkNotNull(appContext) { "AppContainer.init não foi chamado" })
        })
    }

    /** Serviço público (ViaCEP), sem mock: não depende da Core nem de token. */
    val cepRepository: CepRepository by lazy { ViaCepRepository(ApiClient(ApiConfig.VIACEP_BASE_URL)) }

    val collectorRepository: CollectorRepository by lazy { HttpCollectorRepository(mainClient) }

    /** Conteúdo educativo fixo do app, não vem da Core. */
    val tipsRepository: TipsRepository by lazy { AppTipsRepository() }

    /** Coletor = estabelecimento com is_pev. */
    fun isCollector(): Boolean = userProfileRepository.cachedProfile()?.isCollector == true
}
