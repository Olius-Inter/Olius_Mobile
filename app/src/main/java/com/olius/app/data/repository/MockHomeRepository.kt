package com.olius.app.data.repository

import com.olius.app.domain.entity.CollectionStatus
import com.olius.app.domain.entity.HomeDashboard
import com.olius.app.domain.entity.HomeNotification
import com.olius.app.domain.entity.PickupRequest
import com.olius.app.domain.entity.PickupRequestSummary
import com.olius.app.domain.repository.HomeRepository

/**
 * Números das referências (referencia_home.png / ref_home_cell_1.png) enquanto
 * o backend principal não existe (ApiConfig.USE_MOCK_MAIN_BACKEND = true).
 */
class MockHomeRepository : HomeRepository {

    private var dashboard = HomeDashboard(
        firstName = "Naldo",
        fullName = "Naldo Canal",
        accountSubtitle = "Empresa Exemplo LTDA · Situação: Ativa · CNPJ: 00.000.000/0001-00",
        profileImageUrl = null,
        waterPreservedLiters = 625_900.0,
        oilCollectedLiters = 50.0,
        co2AvoidedKg = 7.5,
        biodieselLiters = 50.3,
        oilAccumulatedLiters = 64.0,
        minimumForPickupLiters = 50.0,
        oilRegisteredThisMonthLiters = 64.0,
        pickupReady = true,
        notifications = List(5) { index ->
            HomeNotification(
                id = index.toString(),
                title = "Nova Frota disponível em Itaquaquecetupa SP",
                description = "Lorem ipsum s simply dummy text of the printing and typesetting industry. Lorem...",
                timeAgo = listOf("Há 55 min", "Há 2 h", "Há 5 h", "Há 3 dias", "Há 67 dias")[index],
                category = "FROTAS",
                hasAction = index == 0,
                read = false
            )
        },
        pendingRequests = listOf(PickupRequestSummary("p-1", 50.0, "23/12/2026", CollectionStatus.PENDING))
    )

    override suspend fun getDashboard(): Result<HomeDashboard> = Result.success(dashboard)

    override suspend fun registerOil(liters: Double): Result<HomeDashboard> {
        dashboard = dashboard.copy(
            oilAccumulatedLiters = dashboard.oilAccumulatedLiters + liters,
            oilRegisteredThisMonthLiters = dashboard.oilRegisteredThisMonthLiters + liters
        )
        return Result.success(dashboard)
    }

    override suspend fun requestPickup(request: PickupRequest): Result<Unit> = Result.success(Unit)
}
