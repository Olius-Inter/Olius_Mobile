package com.olius.app.presentation.screen.home

import com.olius.app.MainDispatcherRule
import com.olius.app.data.remote.ApiException
import com.olius.app.domain.entity.CollectionPointType
import com.olius.app.domain.entity.HomeDashboard
import com.olius.app.domain.repository.HomeRepository
import java.io.IOException
import java.net.SocketTimeoutException
import com.olius.app.data.repository.MockCollectionPointRepository
import com.olius.app.data.repository.MockCollectorRepository
import com.olius.app.data.repository.MockHomeRepository
import com.olius.app.data.repository.MockTipsRepository
import com.olius.app.data.repository.MockRankingRepository
import com.olius.app.data.repository.MockUserProfileRepository
import com.olius.app.data.local.InMemorySessionStore
import com.olius.app.domain.entity.ProfileType
import com.olius.app.domain.entity.UserProfile
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val profiles = MockUserProfileRepository(InMemorySessionStore())
    private val viewModel by lazy {
        HomeViewModel(
            MockHomeRepository(), MockRankingRepository(), profiles,
            collectionPointRepository = MockCollectionPointRepository(),
            tipsRepository = MockTipsRepository(),
            collectorRepository = MockCollectorRepository()
        )
    }

    /** Coletor (PEV): continua com os botões e ganha as abas. */
    private fun loginAsPev() =
        profiles.saveLocal(UserProfile("e-01", "ToKonFome", "t@x.com", ProfileType.ESTABLISHMENT, isCollector = true))

    /** Estabelecimento comum (não é PEV): também registra óleo e pede coleta. */
    private fun loginAsEstablishment() =
        profiles.saveLocal(UserProfile("e-02", "Padaria", "p@x.com", ProfileType.ESTABLISHMENT))

    @Test
    fun `coleta so libera quando o oleo acumulado atinge o minimo`() {
        val base = viewModel.uiState.value.copy(
            profileType = ProfileType.ESTABLISHMENT,
            oilRegisteredThisMonthLiters = 10.0,
            minimumForPickupLiters = 50.0
        )

        assertFalse(viewModel.canRequestPickup(base.copy(oilAccumulatedLiters = 49.9)))
        assertTrue(viewModel.canRequestPickup(base.copy(oilAccumulatedLiters = 50.0)))
        assertTrue(viewModel.canRequestPickup(base.copy(oilAccumulatedLiters = 64.0)))
    }

    @Test
    fun `coleta continua bloqueada sem registro de oleo no mes`() {
        val state = viewModel.uiState.value.copy(
            profileType = ProfileType.ESTABLISHMENT,
            oilRegisteredThisMonthLiters = 0.0,
            oilAccumulatedLiters = 80.0
        )

        assertFalse(viewModel.canRequestPickup(state))
    }

    @Test
    fun `estabelecimento que nao e PEV registra oleo e solicita coleta`() {
        loginAsEstablishment()
        val state = viewModel.uiState.value
        assertFalse(state.isCollector)
        assertTrue(viewModel.canRequestPickup(state))

        viewModel.onRegisterOilClick()
        assertTrue(viewModel.uiState.value.isRegisterOilDialogOpen)
        viewModel.onDismissRegisterOilDialog()

        viewModel.onRequestPickupClick()
        assertTrue(viewModel.uiState.value.isRequestPickupDialogOpen)
    }

    @Test
    fun `cidadao que nao e PEV nao registra oleo nem solicita coleta`() {
        profiles.saveLocal(UserProfile("c-01", "Maria", "m@x.com", ProfileType.CITIZEN))
        val state = viewModel.uiState.value
        assertFalse(state.isCollector)
        assertFalse(viewModel.canRequestPickup(state.copy(oilRegisteredThisMonthLiters = 10.0, oilAccumulatedLiters = 80.0)))

        viewModel.onRegisterOilClick()
        viewModel.onRequestPickupClick()

        assertFalse(viewModel.uiState.value.isRegisterOilDialogOpen)
        assertFalse(viewModel.uiState.value.isRequestPickupDialogOpen)
    }

    @Test
    fun `cidadao PEV continua registrando oleo`() {
        profiles.saveLocal(UserProfile("c-01", "Maria", "m@x.com", ProfileType.CITIZEN, isCollector = true))

        viewModel.onRegisterOilClick()

        assertTrue(viewModel.uiState.value.isRegisterOilDialogOpen)
    }

    @Test
    fun `cidadao PEV tambem e coletor`() {
        profiles.saveLocal(UserProfile("c-01", "Maria", "m@x.com", ProfileType.CITIZEN, isCollector = true))

        assertTrue(viewModel.uiState.value.isCollector)
    }

    @Test
    fun `cidadao PEV so recebe oleo - sem solicitar coleta e sem QR de entrega`() {
        profiles.saveLocal(UserProfile("c-01", "Maria", "m@x.com", ProfileType.CITIZEN, isCollector = true))
        val state = viewModel.uiState.value.copy(oilRegisteredThisMonthLiters = 10.0, oilAccumulatedLiters = 80.0)

        assertTrue(state.isCitizenPev)
        assertFalse(state.showQrCode)
        assertFalse(viewModel.canRequestPickup(state))
    }

    @Test
    fun `estabelecimento PEV continua com QR e pode solicitar coleta`() {
        loginAsPev()
        val state = viewModel.uiState.value

        assertTrue(state.showQrCode)
        assertTrue(viewModel.canRequestPickup(state))
    }

    @Test
    fun `solicitar coleta abre o popup com o volume acumulado quando liberado`() {
        loginAsPev()
        viewModel.onRequestPickupClick()

        assertTrue(viewModel.uiState.value.isRequestPickupDialogOpen)
        assertEquals("64", viewModel.uiState.value.requestPickupVolumeInput)
    }

    @Test
    fun `dashboard e nivel carregam dos repositorios`() {
        val state = viewModel.uiState.value
        assertEquals("Naldo", state.userName)
        assertEquals(64.0, state.oilAccumulatedLiters, 0.0)
        assertEquals(4, state.sustainabilityLevel)
        assertTrue(state.hasUnreadNotifications)
    }

    @Test
    fun `registrar oleo atualiza o tanque com a resposta`() {
        loginAsPev()
        viewModel.onRegisterOilClick()
        viewModel.onRegisterOilVolumeChange("10,5")
        viewModel.onConfirmRegisterOil()

        assertEquals(74.5, viewModel.uiState.value.oilAccumulatedLiters, 0.0)
        assertFalse(viewModel.uiState.value.isRegisterOilDialogOpen)
    }

    @Test
    fun `perfil cidadao troca o layout da home`() {
        profiles.saveLocal(UserProfile("c-01", "Maria", "m@x.com", ProfileType.CITIZEN))

        assertEquals(ProfileType.CITIZEN, viewModel.uiState.value.profileType)
    }

    @Test
    fun `sem perfil carregado a home usa estabelecimento ate a Core responder`() {
        assertNull(profiles.cachedProfile())

        assertEquals(ProfileType.ESTABLISHMENT, viewModel.uiState.value.profileType)
    }

    @Test
    fun `coletor ganha as abas e responde solicitacoes`() {
        profiles.saveLocal(UserProfile("e-01", "ToKonFome", "t@x.com", ProfileType.ESTABLISHMENT, isCollector = true))
        val state = viewModel.uiState.value
        assertTrue(state.isCollector)
        val first = state.collectorRequests.first()

        viewModel.onCollectorTabSelected(CollectorHomeTab.REQUESTS)
        viewModel.onCollectRequest(first)

        assertEquals(CollectorHomeTab.REQUESTS, viewModel.uiState.value.collectorTab)
        assertFalse(first in viewModel.uiState.value.collectorRequests)
    }

    @Test
    fun `cidadao carrega pontos PEV e dicas e nao carrega solicitacoes`() {
        profiles.saveLocal(UserProfile("c-01", "Maria", "m@x.com", ProfileType.CITIZEN))
        val state = viewModel.uiState.value

        assertTrue(state.nearbyPoints.isNotEmpty())
        assertTrue(state.nearbyPoints.all { it.type == CollectionPointType.PEV })
        assertTrue(state.tips.isNotEmpty())
        assertTrue(state.collectorRequests.isEmpty())

        viewModel.onCitizenTabSelected(CitizenHomeTab.TIPS)
        assertEquals(CitizenHomeTab.TIPS, viewModel.uiState.value.citizenTab)
    }

    @Test
    fun `estabelecimento recebe as solicitacoes pendentes do dashboard`() {
        assertEquals(1, viewModel.uiState.value.pendingRequests.size)
    }

    /** Home cujo dashboard falha com [error]; o resto vem do mock. */
    private fun homeFailingWith(error: Throwable): HomeViewModel {
        val failing = object : HomeRepository by MockHomeRepository() {
            override suspend fun getDashboard(): Result<HomeDashboard> = Result.failure(error)
        }
        return HomeViewModel(
            failing, MockRankingRepository(), profiles,
            collectionPointRepository = MockCollectionPointRepository(),
            tipsRepository = MockTipsRepository(),
            collectorRepository = MockCollectorRepository()
        )

    }

    @Test
    fun `sem internet marca offline para abrir a tela Sem conexao`() {
        assertTrue(homeFailingWith(IOException("sem rede")).uiState.value.isOffline)
        assertTrue(homeFailingWith(SocketTimeoutException()).uiState.value.isOffline)
    }

    @Test
    fun `erro 4xx da API nao abre a tela Sem conexao`() {
        assertFalse(homeFailingWith(ApiException(404, "nao existe")).uiState.value.isOffline)
    }
}
