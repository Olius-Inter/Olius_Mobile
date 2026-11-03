package com.olius.app.domain.entity

/** Tudo que a Home precisa do backend principal numa chamada só (GET ApiConfig.Main.HOME_DASHBOARD). */
data class HomeDashboard(
    val firstName: String,
    val fullName: String,
    val accountSubtitle: String,
    val profileImageUrl: String?,
    val waterPreservedLiters: Double,
    val oilCollectedLiters: Double,
    val co2AvoidedKg: Double,
    val biodieselLiters: Double,
    val oilAccumulatedLiters: Double,
    val minimumForPickupLiters: Double,
    val oilRegisteredThisMonthLiters: Double,
    val pickupReady: Boolean,
    val notifications: List<HomeNotification>,
    /** Card "Solicitações de Coletas". */
    val pendingRequests: List<PickupRequestSummary> = emptyList()
)

data class HomeNotification(
    val id: String,
    val title: String,
    val description: String,
    /** Texto pronto ("Há 55 min") — o backend sabe o fuso e evita lógica de data no app. */
    val timeAgo: String,
    /** "COLETAS" ou "FROTAS" */
    val category: String,
    val hasAction: Boolean,
    val read: Boolean
)

/** Corpo de "Solicitar Coleta" → tabela collection_request. */
data class PickupRequest(
    val estimatedVolumeLiters: Double,
    val scheduledDate: String,
    val observation: String?,
    val idempotencyKey: String
)
