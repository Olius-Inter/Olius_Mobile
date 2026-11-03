package com.olius.app.presentation.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olius.app.data.remote.AppContainer
import com.olius.app.domain.entity.CollectorRequest
import com.olius.app.domain.entity.HomeDashboard
import com.olius.app.domain.entity.OilImpact
import com.olius.app.domain.entity.PickupRequest
import com.olius.app.domain.entity.ProfileType
import com.olius.app.domain.entity.SustainabilityLevel
import com.olius.app.domain.entity.VISIBLE_POINT_TYPES
import com.olius.app.domain.repository.CollectionPointRepository
import com.olius.app.domain.repository.CollectorRepository
import com.olius.app.domain.repository.HomeRepository
import com.olius.app.domain.repository.RankingRepository
import com.olius.app.domain.repository.TipsRepository
import com.olius.app.domain.repository.UserProfileRepository
import com.olius.app.domain.usecases.SyncUserProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.io.IOException
import java.util.Locale
import java.util.UUID

/** Estado + eventos da tela Home. */
class HomeViewModel(
    private val homeRepository: HomeRepository = AppContainer.homeRepository,
    private val rankingRepository: RankingRepository = AppContainer.rankingRepository,
    private val userProfileRepository: UserProfileRepository = AppContainer.userProfileRepository,
    private val collectionPointRepository: CollectionPointRepository = AppContainer.collectionPointRepository,
    private val tipsRepository: TipsRepository = AppContainer.tipsRepository,
    private val collectorRepository: CollectorRepository = AppContainer.collectorRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState(referenceMonthLabel = currentMonthLabel()))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHome()
    }

    /** O tipo da conta vem do perfil em sessão (salvo no login). */
    fun loadHome() {
        viewModelScope.launch {
            val profile = userProfileRepository.cachedProfile()
                ?: SyncUserProfileUseCase(userProfileRepository)()
            val profileType = profile?.profileType ?: ProfileType.ESTABLISHMENT
            val isCollector = profile?.isCollector == true
            _uiState.update { it.copy(profileType = profileType, isCollector = isCollector) }

            val dashboard = async { homeRepository.getDashboard() }
            val achievements = async { rankingRepository.getAchievements(profileType.rankingCategory) }
            val points = async {
                if (profileType == ProfileType.CITIZEN) collectionPointRepository.getNearbyPoints() else null
            }
            val tips = async { if (profileType == ProfileType.CITIZEN) tipsRepository.getTips() else null }
            val requests = async { if (isCollector) collectorRepository.getRequests() else null }

            dashboard.await()
                .onSuccess { applyDashboard(it); _uiState.update { s -> s.copy(isOffline = false) } }
                .onFailure { error -> if (error is IOException) _uiState.update { it.copy(isOffline = true) } }
            achievements.await().onSuccess { applyLevel(it.level) }
            points.await()?.onSuccess { list ->
                _uiState.update { it.copy(nearbyPoints = list.filter { point -> point.type in VISIBLE_POINT_TYPES }) }
            }
            tips.await()?.onSuccess { list -> _uiState.update { it.copy(tips = list) } }
            requests.await()?.onSuccess { list -> _uiState.update { it.copy(collectorRequests = list) } }
        }
    }

    private fun applyDashboard(dashboard: HomeDashboard) {
        _uiState.update {
            it.copy(
                userName = dashboard.firstName,
                fullName = dashboard.fullName,
                accountSubtitle = dashboard.accountSubtitle,
                profileImageUrl = dashboard.profileImageUrl,
                waterPreservedLiters = dashboard.waterPreservedLiters,
                oilCollectedLiters = dashboard.oilCollectedLiters,
                co2AvoidedKg = dashboard.co2AvoidedKg,
                biodieselLiters = dashboard.biodieselLiters,
                oilAccumulatedLiters = dashboard.oilAccumulatedLiters,
                minimumForPickupLiters = dashboard.minimumForPickupLiters,
                oilRegisteredThisMonthLiters = dashboard.oilRegisteredThisMonthLiters,
                showPickupReadyBadge = dashboard.pickupReady,
                pendingRequests = dashboard.pendingRequests,
                notifications = dashboard.notifications.map { notification ->
                    NotificationItem(
                        id = notification.id,
                        title = notification.title,
                        description = notification.description,
                        timeAgo = notification.timeAgo,
                        category = if (notification.category == "FROTAS") NotificationCategory.FROTAS else NotificationCategory.COLETAS,
                        hasAction = notification.hasAction,
                        read = notification.read
                    )
                },
                hasUnreadNotifications = dashboard.notifications.any { n -> !n.read }
            )
        }
    }

    private fun applyLevel(level: SustainabilityLevel) {
        _uiState.update {
            it.copy(
                sustainabilityLevel = level.level,
                currentLevelProgressLiters = level.totalLiters,
                nextLevelTargetLiters = level.targetLiters ?: level.totalLiters,
                levelProgress = level.progress,
                levelName = level.name
            )
        }
    }

    /** Única regra já definida: sem nenhum registro no mês, a coleta fica inativa. */
    private fun hasRegisteredOilThisMonth(state: HomeUiState): Boolean =
        state.oilRegisteredThisMonthLiters > 0.0

    private fun meetsMinimumOilRequirement(state: HomeUiState): Boolean =
        state.oilAccumulatedLiters >= state.minimumForPickupLiters

    private fun canUseOilActions(state: HomeUiState): Boolean =
        state.profileType == ProfileType.ESTABLISHMENT || state.isCollector

    /** Só estabelecimento pede coleta; o PEV de cidadão só recebe. */
    fun canRequestPickup(state: HomeUiState = _uiState.value): Boolean =
        state.profileType == ProfileType.ESTABLISHMENT &&
            hasRegisteredOilThisMonth(state) && meetsMinimumOilRequirement(state)

    fun onCollectorTabSelected(tab: CollectorHomeTab) {
        _uiState.update { it.copy(collectorTab = tab) }
    }

    fun onCitizenTabSelected(tab: CitizenHomeTab) {
        _uiState.update { it.copy(citizenTab = tab) }
    }

    /** "Coletar" / "Descartar": tira o card da lista na hora e avisa o backend. */
    fun onCollectRequest(request: CollectorRequest) = answerRequest(request) { collectorRepository.collect(it) }

    fun onDiscardRequest(request: CollectorRequest) = answerRequest(request) { collectorRepository.discard(it) }

    private fun answerRequest(request: CollectorRequest, call: suspend (String) -> Result<Unit>) {
        val before = _uiState.value.collectorRequests
        _uiState.update { it.copy(collectorRequests = before - request) }
        viewModelScope.launch {
            call(request.id).onFailure { _uiState.update { it.copy(collectorRequests = before) } }
        }
    }

    fun onRegisterOilClick() {
        if (!canUseOilActions(_uiState.value)) return
        _uiState.update { it.copy(isRegisterOilDialogOpen = true, registerOilVolumeInput = "") }
    }

    fun onDismissRegisterOilDialog() {
        _uiState.update { it.copy(isRegisterOilDialogOpen = false) }
    }

    fun onRegisterOilVolumeChange(value: String) {
        _uiState.update { it.copy(registerOilVolumeInput = value) }
    }

    /** Prévia do popup "Registrar óleo": mesmas fórmulas da Home e do Histórico ([OilImpact]). */
    fun estimateWaterPreserved(volumeLiters: Double): Double = OilImpact.waterPreservedLiters(volumeLiters)

    fun estimateCo2Avoided(volumeLiters: Double): Double = OilImpact.co2AvoidedKg(volumeLiters)

    /** Envia o registro; a resposta já traz o dashboard atualizado (sem GET extra). */
    fun onConfirmRegisterOil() {
        val liters = _uiState.value.registerOilVolumeInput.toLitersOrNull()
        _uiState.update { it.copy(isRegisterOilDialogOpen = false) }
        if (liters == null || liters <= 0.0) return
        viewModelScope.launch { homeRepository.registerOil(liters).onSuccess(::applyDashboard) }
    }

    fun onRequestPickupClick() {
        if (!canRequestPickup()) return
        _uiState.update {
            it.copy(
                isRequestPickupDialogOpen = true,
                requestPickupVolumeInput = it.oilAccumulatedLiters.toInt().toString()
            )
        }
    }

    fun onDismissRequestPickupDialog() {
        _uiState.update { it.copy(isRequestPickupDialogOpen = false) }
    }

    fun onRequestPickupVolumeChange(value: String) {
        _uiState.update { it.copy(requestPickupVolumeInput = value) }
    }

    fun onRequestPickupDateChange(value: String) {
        _uiState.update { it.copy(requestPickupDateInput = value) }
    }

    fun onRequestPickupNotesChange(value: String) {
        _uiState.update { it.copy(requestPickupNotesInput = value) }
    }

    fun onConfirmRequestPickup() {
        val state = _uiState.value
        _uiState.update { it.copy(isRequestPickupDialogOpen = false) }
        val liters = state.requestPickupVolumeInput.toLitersOrNull() ?: return
        val request = PickupRequest(
            estimatedVolumeLiters = liters,
            scheduledDate = state.requestPickupDateInput,
            observation = state.requestPickupNotesInput.ifBlank { null },
            idempotencyKey = UUID.randomUUID().toString()
        )
        viewModelScope.launch { homeRepository.requestPickup(request) }
    }

    private fun String.toLitersOrNull(): Double? = trim().replace(',', '.').toDoubleOrNull()

    fun onNotificationsClick() {
        _uiState.update { it.copy(isNotificationsSheetOpen = true, hasUnreadNotifications = false) }
    }

    fun onDismissNotificationsSheet() {
        _uiState.update { it.copy(isNotificationsSheetOpen = false) }
    }

    fun onNotificationFilterChange(filter: NotificationFilter) {
        _uiState.update { it.copy(notificationFilter = filter) }
    }

    /** TODO: navegar pro destino da notificação (ex.: detalhe da frota). */
    fun onNotificationActionClick(notification: NotificationItem) = Unit

    private fun currentMonthLabel(): String {
        val formatter = SimpleDateFormat("MMMM 'de' yyyy", Locale.forLanguageTag("pt-BR"))
        return formatter.format(Calendar.getInstance().time)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.forLanguageTag("pt-BR")) else it.toString() }
    }
}
