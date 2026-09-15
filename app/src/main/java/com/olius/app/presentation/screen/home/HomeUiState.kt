package com.olius.app.presentation.screen.home

/** Uma notificação exibida no bottom sheet de Notificações (ver ref_notification.png). */
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

data class HomeUiState(
    val userName: String = "",
    val fullName: String = "",
    val accountSubtitle: String = "",
    val profileImageUrl: String? = null,
    val referenceMonthLabel: String = "",

    // Indicadores do topo (2x2 grid).
    val waterPreservedLiters: Double = 0.0,
    val oilCollectedLiters: Double = 0.0,
    val co2AvoidedKg: Double = 0.0,
    val biodieselLiters: Double = 0.0,

    // Nível de óleo acumulado (tanque + selo de coleta).
    val oilAccumulatedLiters: Double = 0.0,
    val minimumForPickupLiters: Double = 0.0,
    val oilRegisteredThisMonthLiters: Double = 0.0,
    // Dado (não regra calculada) que liga/desliga o selo "Pronto para coleta" —
    // nas duas referências (referencia_home.png e ref_home_cell_1.png) ele
    // aparece sempre, independente do volume acumulado, então não travamos
    // isso a nenhuma fórmula ainda. Quando o critério for definido, passe a
    // computar esse valor em vez de setá-lo diretamente.
    val showPickupReadyBadge: Boolean = true,

    // Nível de sustentabilidade (selo + barra de progresso).
    val sustainabilityLevel: Int = 1,
    val currentLevelProgressLiters: Double = 0.0,
    val nextLevelTargetLiters: Double = 0.0,

    // Popups/bottom sheets.
    val isRegisterOilDialogOpen: Boolean = false,
    val isRequestPickupDialogOpen: Boolean = false,
    val isProfileSheetOpen: Boolean = false,
    val isNotificationsSheetOpen: Boolean = false,

    // Formulário "Registrar Óleo".
    val registerOilVolumeInput: String = "",

    // Formulário "Solicitar Coleta".
    val requestPickupVolumeInput: String = "",
    val requestPickupDateInput: String = "",
    val requestPickupNotesInput: String = "",

    // Notificações.
    val notifications: List<NotificationItem> = emptyList(),
    val notificationFilter: NotificationFilter = NotificationFilter.TODAS,
    val hasUnreadNotifications: Boolean = false
) {
    /** Fração (0..1) do tanque de óleo preenchida — usada pela animação de "enchendo". */
    val oilTankFraction: Float
        get() = if (minimumForPickupLiters <= 0.0) 0f
        else (oilAccumulatedLiters / minimumForPickupLiters).toFloat().coerceIn(0f, 1f)

    /** Fração (0..1) da barra de progresso do nível de sustentabilidade atual. */
    val levelProgressFraction: Float
        get() = if (nextLevelTargetLiters <= 0.0) 0f
        else (currentLevelProgressLiters / nextLevelTargetLiters).toFloat().coerceIn(0f, 1f)

    val filteredNotifications: List<NotificationItem>
        get() = when (notificationFilter) {
            NotificationFilter.TODAS -> notifications
            NotificationFilter.COLETAS -> notifications.filter { it.category == NotificationCategory.COLETAS }
            NotificationFilter.FROTAS -> notifications.filter { it.category == NotificationCategory.FROTAS }
        }
}
