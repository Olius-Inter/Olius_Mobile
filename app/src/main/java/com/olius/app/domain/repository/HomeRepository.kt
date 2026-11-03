package com.olius.app.domain.repository

import com.olius.app.domain.entity.HomeDashboard
import com.olius.app.domain.entity.PickupRequest

/** Backend principal (Postgres) para a Home. */
interface HomeRepository {
    suspend fun getDashboard(): Result<HomeDashboard>

    /** Devolve o dashboard já atualizado, para não precisar de outro GET. */
    suspend fun registerOil(liters: Double): Result<HomeDashboard>

    suspend fun requestPickup(request: PickupRequest): Result<Unit>
}
