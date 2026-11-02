package com.olius.app.presentation.screen.home

import com.olius.app.domain.entity.CollectionPoint
import com.olius.app.domain.entity.CollectorRequest
import com.olius.app.domain.entity.PickupRequestSummary
import com.olius.app.domain.entity.ProfileType
import com.olius.app.domain.entity.Tip

/** Uma notificação exibida no bottom sheet de Notificações. */
data class NotificationItem(
    val id: String,
    val title: String,
    val description: String,
    val timeAgo: String,
    val category: NotificationCategory,
    val hasAction: Boolean = false,
    val read: Boolean = false
)

enum class NotificationCategory {
    COLETAS,
    FROTAS
}

enum class NotificationFilter {
    TODAS,
    COLETAS,
    FROTAS
}

/** Abas da Home do Coletor: "Meus Dados" | "Solicitações". */
enum class CollectorHomeTab { MY_DATA, REQUESTS }

/** Abas da Home do cidadão. */
enum class CitizenHomeTab { PEV_POINTS, TIPS }

data class HomeUiState(
    val profileType: ProfileType = ProfileType.ESTABLISHMENT,
    /** Tem PEV aprovado (estabelecimento ou cidadão). */
    val isCollector: Boolean = false,
    val isOffline: Boolean = false,

    val pendingRequests: List<PickupRequestSummary> = emptyList(),

    val collectorTab: CollectorHomeTab = CollectorHomeTab.MY_DATA,
    val collectorRequests: List<CollectorRequest> = emptyList(),

    val citizenTab: CitizenHomeTab = CitizenHomeTab.PEV_POINTS,
    val nearbyPoints: List<CollectionPoint> = emptyList(),
    val tips: List<Tip> = emptyList(),

    val userName: String = "",
    val fullName: String = "",
    val accountSubtitle: String = "",
    val profileImageUrl: String? = null,
    val referenceMonthLabel: String = "",

    val waterPreservedLiters: Double = 0.0,
    val oilCollectedLiters: Double = 0.0,
    val co2AvoidedKg: Double = 0.0,
    val biodieselLiters: Double = 0.0,

    val oilAccumulatedLiters: Double = 0.0,
    val minimumForPickupLiters: Double = 0.0,
    val oilRegisteredThisMonthLiters: Double = 0.0,
    val showPickupReadyBadge: Boolean = false,

    val sustainabilityLevel: Int = 1,
    val currentLevelProgressLiters: Double = 0.0,
    val nextLevelTargetLiters: Double = 0.0,
    /** Progresso (0..1) dentro do nível, já calculado pela API; null = calcula pelos litros. */
    val levelProgress: Float? = null,
    /** certificate_level.name. */
    val levelName: String? = null,

    val isRegisterOilDialogOpen: Boolean = false,
    val isRequestPickupDialogOpen: Boolean = false,
    val isNotificationsSheetOpen: Boolean = false,

    val registerOilVolumeInput: String = "",

    val requestPickupVolumeInput: String = "",
    val requestPickupDateInput: String = "",
    val requestPickupNotesInput: String = "",

    val notifications: List<NotificationItem> = emptyList(),
    val notificationFilter: NotificationFilter = NotificationFilter.TODAS,
    val hasUnreadNotifications: Boolean = false
) {
    /** Cidadão que virou PEV: só recebe óleo; não entrega em outro ponto nem pede coleta. */
    val isCitizenPev: Boolean
        get() = profileType == ProfileType.CITIZEN && isCollector

    /** QR de identificação: o cidadão mostra ao entregar no PEV; o estabelecimento, ao motorista. */
    val showQrCode: Boolean
        get() = !isCitizenPev

    /** Fração (0..1) do tanque de óleo preenchida — usada pela animação de "enchendo". */
    val oilTankFraction: Float
        get() = if (minimumForPickupLiters <= 0.0) 0f
        else (oilAccumulatedLiters / minimumForPickupLiters).toFloat().coerceIn(0f, 1f)

    /** Fração (0..1) da barra de progresso do nível de sustentabilidade atual. */
    val levelProgressFraction: Float
        get() = levelProgress?.coerceIn(0f, 1f) ?: if (nextLevelTargetLiters <= 0.0) 0f
        else (currentLevelProgressLiters / nextLevelTargetLiters).toFloat().coerceIn(0f, 1f)

    val filteredNotifications: List<NotificationItem>
        get() = when (notificationFilter) {
            NotificationFilter.TODAS -> notifications
            NotificationFilter.COLETAS -> notifications.filter { it.category == NotificationCategory.COLETAS }
            NotificationFilter.FROTAS -> notifications.filter { it.category == NotificationCategory.FROTAS }
        }
}
