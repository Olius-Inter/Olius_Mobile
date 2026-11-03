package com.olius.app.data.repository

import com.olius.app.data.remote.ApiClient
import com.olius.app.data.remote.ApiConfig
import com.olius.app.domain.entity.CollectionStatus
import com.olius.app.domain.entity.HomeDashboard
import com.olius.app.domain.entity.HomeNotification
import com.olius.app.domain.entity.ImpactStats
import com.olius.app.domain.entity.PickupRequest
import com.olius.app.domain.entity.PickupRequestSummary
import com.olius.app.domain.entity.ProfileType
import com.olius.app.domain.entity.UserProfile
import com.olius.app.domain.repository.HomeRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.Instant
import java.time.format.DateTimeFormatter

/** [HomeRepository] sobre a Core. */
class HttpHomeRepository(
    private val client: ApiClient,
    private val profileProvider: suspend () -> UserProfile?,
    private val clock: Clock = Clock.systemDefaultZone()
) : HomeRepository {
    override suspend fun getDashboard(): Result<HomeDashboard> = runCatching {
        val profile = checkNotNull(profileProvider()) { "Perfil da conta ainda não carregado" }
        val dashboard = HomeDashboard(
            firstName = profile.name.trim().substringBefore(' '),
            fullName = profile.name.trim(),
            accountSubtitle = "",
            profileImageUrl = null,
            waterPreservedLiters = 0.0,
            oilCollectedLiters = 0.0,
            co2AvoidedKg = 0.0,
            biodieselLiters = 0.0,
            oilAccumulatedLiters = 0.0,
            minimumForPickupLiters = 0.0,
            oilRegisteredThisMonthLiters = 0.0,
            pickupReady = false,
            notifications = emptyList()
        )
        val withOil = when (profile.profileType) {
            ProfileType.ESTABLISHMENT -> coroutineScope {
                val score = async { JSONObject(client.get(ApiConfig.Main.ESTABLISHMENT_SCORE)) }
                val requests = PENDING_REQUEST_STATUSES.map { status ->
                    async { client.fetchAllPages(ApiConfig.Main.COLLECTION_REQUESTS, mapOf("status" to status)) }
                }
                dashboard.copy(
                    oilCollectedLiters = score.await().optDouble("collectedVolumeLiters", 0.0),
                    pendingRequests = requests.flatMap { it.await() }
                        .filter { it.isNull("collectionId") }
                        .map { it to Instant.parse(it.getString("requestAt")) }
                        .sortedByDescending { (_, requestAt) -> requestAt }
                        .map { (it, requestAt) ->
                            PickupRequestSummary(
                                id = it.getString("id"),
                                liters = it.getDouble("estimatedVolumeLiters"),
                                dateLabel = DATE_FORMAT.format(requestAt.atZone(clock.zone)),
                                status = CollectionStatus.PENDING
                            )
                        }
                )
            }
            ProfileType.CITIZEN -> {
                val deliveries = client.fetchAllPages(ApiConfig.Main.USERS_ME_DELIVERIES)
                    .filter { it.optString("recordStatus", RECORDED) == RECORDED }
                dashboard.copy(oilCollectedLiters = deliveries.sumOf { it.optDouble("oilVolumeLiters", 0.0) })
            }
        }
        val impact = ImpactStats.fromOilLiters(withOil.oilCollectedLiters)
        withOil.copy(
            waterPreservedLiters = impact.waterPreservedLiters,
            co2AvoidedKg = impact.co2AvoidedKg,
            biodieselLiters = impact.biodieselLiters
        )
    }

    override suspend fun registerOil(liters: Double): Result<HomeDashboard> = runCatching {
        val body = JSONObject().put("litros", liters).toString()
        parseDashboard(JSONObject(client.post(ApiConfig.Main.OIL_REGISTRATIONS, body)))
    }

    /** Rota real da Core: corpo SÓ {estimatedVolumeLiters, observation?} (campo a mais = 400) e a chave idempotente no header. */
    override suspend fun requestPickup(request: PickupRequest): Result<Unit> = runCatching {
        client.post(
            ApiConfig.Main.COLLECTION_REQUESTS,
            pickupRequestBody(request),
            headers = mapOf("Idempotency-Key" to request.idempotencyKey)
        )
        Unit
    }

    internal fun pickupRequestBody(request: PickupRequest): String {
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

    private companion object {
        val PENDING_REQUEST_STATUSES = listOf("PENDING", "APPROVED")
        const val RECORDED = "RECORDED"
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    }
}
