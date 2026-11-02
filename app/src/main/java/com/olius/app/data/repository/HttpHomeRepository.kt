package com.olius.app.data.repository

import com.olius.app.data.remote.ApiClient
import com.olius.app.data.remote.ApiConfig
import com.olius.app.domain.entity.CollectionStatus
import com.olius.app.domain.entity.HomeDashboard
import com.olius.app.domain.entity.HomeNotification
import com.olius.app.domain.entity.PickupRequest
import com.olius.app.domain.entity.PickupRequestSummary
import com.olius.app.domain.repository.HomeRepository
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * [HomeRepository] sobre o backend principal. O contrato JSON (nomes dos
 * campos) está em API_00_CONEXAO_API.md, seção "Contratos do backend principal".
 */
class HttpHomeRepository(private val client: ApiClient) : HomeRepository {

    override suspend fun getDashboard(): Result<HomeDashboard> = runCatching {
        parseDashboard(JSONObject(client.get(ApiConfig.Main.HOME_DASHBOARD)))
    }

    override suspend fun registerOil(liters: Double): Result<HomeDashboard> = runCatching {
        val body = JSONObject().put("litros", liters).toString()
        parseDashboard(JSONObject(client.post(ApiConfig.Main.OIL_REGISTRATIONS, body)))
    }

    /**
     * Rota real da Core: corpo SÓ {estimatedVolumeLiters, observation?} (campo a mais = 400)
     * e a chave idempotente no header. A data escolhida no popup não vai: quem agenda é o admin.
     */
    override suspend fun requestPickup(request: PickupRequest): Result<Unit> = runCatching {
        client.post(
            ApiConfig.Main.COLLECTION_REQUESTS,
            pickupRequestBody(request),
            headers = mapOf("Idempotency-Key" to request.idempotencyKey)
        )
        Unit
    }

    internal fun pickupRequestBody(request: PickupRequest): String {
        // DECIMAL(8,2) no banco: a Core recusa mais de 2 casas (não arredonda sozinha).
        val liters = BigDecimal.valueOf(request.estimatedVolumeLiters).setScale(2, RoundingMode.HALF_UP)
        val body = JSONObject().put("estimatedVolumeLiters", liters)
        request.observation?.takeIf { it.isNotBlank() }?.let { body.put("observation", it) }
        return body.toString()
    }

    private fun parseDashboard(json: JSONObject): HomeDashboard {
        val notifications = json.optJSONArray("notifications")
        return HomeDashboard(
            firstName = json.getString("firstName"),
            fullName = json.getString("fullName"),
            accountSubtitle = json.optString("accountSubtitle"),
            profileImageUrl = json.optStringOrNull("profileImageUrl"),
            waterPreservedLiters = json.optDouble("waterPreservedLiters", 0.0),
            oilCollectedLiters = json.optDouble("oilCollectedLiters", 0.0),
            co2AvoidedKg = json.optDouble("co2AvoidedKg", 0.0),
            biodieselLiters = json.optDouble("biodieselLiters", 0.0),
            oilAccumulatedLiters = json.optDouble("oilAccumulatedLiters", 0.0),
            minimumForPickupLiters = json.optDouble("minimumForPickupLiters", 0.0),
            oilRegisteredThisMonthLiters = json.optDouble("oilRegisteredThisMonthLiters", 0.0),
            pickupReady = json.optBoolean("pickupReady"),
            pendingRequests = json.optJSONArray("pendingRequests").mapObjects { item ->
                PickupRequestSummary(
                    id = item.getString("id"),
                    liters = item.getDouble("liters"),
                    dateLabel = item.optString("dateLabel"),
                    status = CollectionStatus.fromApi(item.optString("status"))
                )
            },
            notifications = List(notifications?.length() ?: 0) { index ->
                val item = notifications!!.getJSONObject(index)
                HomeNotification(
                    id = item.getString("id"),
                    title = item.getString("title"),
                    description = item.optString("description"),
                    timeAgo = item.optString("timeAgo"),
                    category = item.optString("category", "COLETAS"),
                    hasAction = item.optBoolean("hasAction"),
                    read = item.optBoolean("read")
                )
            }
        )
    }
}
