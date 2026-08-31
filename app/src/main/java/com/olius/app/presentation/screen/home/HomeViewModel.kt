package com.olius.app.presentation.screen.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Estado + eventos da tela Home. Os números iniciais abaixo são os mesmos
 * das referências (referencia_home.png / ref_home_cell_1.png) só pra dar
 * uma visualização fiel enquanto a integração com o backend não existe —
 * troque por dados reais (repositório/use case) quando estiverem prontos.
 */
class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            userName = "Naldo",
            fullName = "Naldo Canal",
            accountSubtitle = "Empresa Exemplo LTDA · Situação: Ativa · CNPJ: 00.000.000/0001-00",
            referenceMonthLabel = currentMonthLabel(),
            waterPreservedLiters = 625_900.0,
            oilCollectedLiters = 50.0,
            co2AvoidedKg = 7.5,
            biodieselLiters = 50.3,
            oilAccumulatedLiters = 64.0,
            minimumForPickupLiters = 50.0,
            oilRegisteredThisMonthLiters = 64.0,
            sustainabilityLevel = 1,
            currentLevelProgressLiters = 64.0,
            nextLevelTargetLiters = 100.0,
            notifications = sampleNotifications(),
            hasUnreadNotifications = true
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // ---- Regra de habilitação do botão "Solicitar coleta" -----------------

    /** Única regra já definida: sem nenhum registro no mês, a coleta fica inativa. */
    private fun hasRegisteredOilThisMonth(state: HomeUiState): Boolean =
        state.oilRegisteredThisMonthLiters > 0.0

    /**
     * TODO: o valor mínimo de óleo acumulado para liberar a coleta ainda não
     * foi decidido pelo negócio. Quando for definido, comparar aqui (ex.:
     * `state.oilAccumulatedLiters >= state.minimumForPickupLiters`).
     */
    private fun meetsMinimumOilRequirement(state: HomeUiState): Boolean = true

    fun canRequestPickup(state: HomeUiState = _uiState.value): Boolean =
        hasRegisteredOilThisMonth(state) && meetsMinimumOilRequirement(state)

    // ---- Popup: Registrar Óleo --------------------------------------------

    fun onRegisterOilClick() {
        _uiState.update { it.copy(isRegisterOilDialogOpen = true, registerOilVolumeInput = "") }
    }

    fun onDismissRegisterOilDialog() {
        _uiState.update { it.copy(isRegisterOilDialogOpen = false) }
    }

    fun onRegisterOilVolumeChange(value: String) {
        _uiState.update { it.copy(registerOilVolumeInput = value) }
    }

    /** TODO: fórmula de conversão óleo -> água preservada ainda não definida. */
    fun estimateWaterPreserved(volumeLiters: Double): Double = 0.0

    /** TODO: fórmula de conversão óleo -> CO² evitado ainda não definida. */
    fun estimateCo2Avoided(volumeLiters: Double): Double = 0.0

    /** TODO: persistir o registro (repositório/use case) e atualizar o estado. */
    fun onConfirmRegisterOil() {
        _uiState.update { it.copy(isRegisterOilDialogOpen = false) }
    }

    // ---- Popup: Solicitar Coleta -------------------------------------------

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

    /** TODO: enviar a solicitação de coleta (repositório/use case). */
    fun onConfirmRequestPickup() {
        _uiState.update { it.copy(isRequestPickupDialogOpen = false) }
    }

    // ---- Bottom sheet: Perfil -----------------------------------------------

    fun onProfileClick() {
        _uiState.update { it.copy(isProfileSheetOpen = true) }
    }

    fun onDismissProfileSheet() {
        _uiState.update { it.copy(isProfileSheetOpen = false) }
    }

    /** TODO: chamar LogoutUseCases e navegar de volta pro fluxo de login. */
    fun onLogoutClick() {
        _uiState.update { it.copy(isProfileSheetOpen = false) }
    }

    /** TODO: navegar pro fluxo de adicionar/criar outra conta Olius. */
    fun onAddAccountClick() = Unit

    // ---- Bottom sheet: Notificações -----------------------------------------

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

    private fun sampleNotifications(): List<NotificationItem> = List(5) { index ->
        NotificationItem(
            id = index.toString(),
            title = "Nova Frota disponível em Itaquaquecetupa SP",
            description = "Lorem ipsum s simply dummy text of the printing and typesetting industry. Lorem...",
            timeAgo = when (index) {
                0 -> "Há 55 min"
                1 -> "Há 2 h"
                2 -> "Há 5 h"
                3 -> "Há 3 dias"
                else -> "Há 67 dias"
            },
            category = NotificationCategory.FROTAS,
            hasAction = index == 0
        )
    }
}
