package com.olius.app.data.repository

import com.olius.app.data.remote.ApiClient
import com.olius.app.data.remote.ApiConfig
import com.olius.app.data.remote.ApiException
import com.olius.app.domain.entity.BecomeCollectorRequest
import com.olius.app.domain.entity.CitizenHistory
import com.olius.app.domain.entity.CollectionHistoryItem
import com.olius.app.domain.entity.CollectionPoint
import com.olius.app.domain.entity.CollectionPointDetails
import com.olius.app.domain.entity.CollectionPointType
import com.olius.app.domain.entity.MapBounds
import com.olius.app.domain.entity.PointSuggestion
import com.olius.app.domain.entity.CollectionStatus
import com.olius.app.domain.entity.CollectorRequest
import com.olius.app.domain.entity.DisposalRecord
import com.olius.app.domain.entity.EstablishmentHistory
import com.olius.app.domain.entity.GeoPoint
import com.olius.app.domain.entity.PevAlreadyExistsException
import com.olius.app.domain.entity.ImpactStats
import com.olius.app.domain.entity.Tip
import com.olius.app.domain.repository.AddressGeocoder
import com.olius.app.domain.repository.CollectionPointRepository
import com.olius.app.domain.repository.CollectorRepository
import com.olius.app.domain.repository.HistoryRepository
import com.olius.app.domain.repository.TipsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * Implementações HTTP do backend principal. Onde a Core já tem a rota, segue o
 * DTO dela; o resto é o contrato combinado (ApiConfig, bloco "AINDA NÃO EXISTE").
 */

private fun JSONObject.stats() = getJSONObject("stats").let {
    ImpactStats(
        oilCollectedLiters = it.optDouble("oilCollectedLiters", 0.0),
        waterPreservedLiters = it.optDouble("waterPreservedLiters", 0.0),
        co2AvoidedKg = it.optDouble("co2AvoidedKg", 0.0),
        biodieselLiters = it.optDouble("biodieselLiters", 0.0)
    )
}

/**
 * Lê todas as páginas de uma rota paginada da Core (`size` máx. 100) até `totalElements`,
 * com teto de segurança. [itemsKey]: `items` na maioria; `movements` em /users/me/points.
 */
internal suspend fun ApiClient.fetchAllPages(
    path: String,
    filters: Map<String, String> = emptyMap(),
    itemsKey: String = "items"
): List<JSONObject> {
    val all = mutableListOf<JSONObject>()
    var page = 0
    while (page < MAX_PAGES) {
        val query = filters + mapOf("page" to page.toString(), "size" to PAGE_SIZE.toString())
        val json = JSONObject(get(path, query))
        val items = json.optJSONArray(itemsKey).mapObjects { it }
        all += items
        if (items.isEmpty() || all.size >= json.optLong("totalElements", 0L)) break
        page++
    }
    return all
}

private const val PAGE_SIZE = 100
private const val MAX_PAGES = 20

/**
 * Histórico. B2B na Core real (não existe `/history/collections`), juntando:
 * - "Coletado": GET /collections?recordStatus=RECORDED (`collectionDate`, `collectedVolumeLiters`);
 * - "Pendente": GET /collection-requests?status=PENDING e ?status=APPROVED, só os sem `collectionId`
 *   (`requestAt`, `estimatedVolumeLiters`);
 * - topo: GET /establishments/me/score (`collectedVolumeLiters`).
 * As três exigem ROLE_ESTABLISHMENT. [clock] é injetável nos testes.
 * B2C: GET /users/me/points (entregas do cidadão com pontos e data). Os litros ainda não vêm
 * (pedido `oilVolumeLiters` à Core); sem eles a tela mostra o erro de carregamento.
 */
class HttpHistoryRepository(
    private val client: ApiClient,
    private val clock: Clock = Clock.systemDefaultZone()
) : HistoryRepository {

    override suspend fun getEstablishmentHistory(): Result<EstablishmentHistory> = runCatching {
        coroutineScope {
            val collected = async {
                client.fetchAllPages(ApiConfig.Main.COLLECTIONS, mapOf("recordStatus" to "RECORDED")).map {
                    HistoryRow(
                        date = Instant.parse(it.getString("collectionDate")),
                        id = it.getString("id"),
                        liters = it.getDouble("collectedVolumeLiters"),
                        status = CollectionStatus.COLLECTED
                    )
                }
            }
            val pending = PENDING_REQUEST_STATUSES.map { status ->
                async {
                    client.fetchAllPages(ApiConfig.Main.COLLECTION_REQUESTS, mapOf("status" to status))
                        // APPROVED com collectionId já virou coleta: aparece como "Coletado".
                        .filter { it.isNull("collectionId") }
                        .map {
                            HistoryRow(
                                date = Instant.parse(it.getString("requestAt")),
                                id = it.getString("id"),
                                liters = it.getDouble("estimatedVolumeLiters"),
                                status = CollectionStatus.PENDING
                            )
                        }
                }
            }
            val score = async { JSONObject(client.get(ApiConfig.Main.ESTABLISHMENT_SCORE)) }

            val rows = (collected.await() + pending.flatMap { it.await() }).sortedByDescending { it.date }
            EstablishmentHistory(
                monthLabel = monthLabel(),
                stats = ImpactStats(
                    oilCollectedLiters = score.await().optDouble("collectedVolumeLiters", 0.0),
                    // A Core ainda não calcula água, CO2 e biodiesel (iriam no GET /home).
                    waterPreservedLiters = 0.0,
                    co2AvoidedKg = 0.0,
                    biodieselLiters = 0.0
                ),
                items = rows.map {
                    CollectionHistoryItem(it.id, DATE_FORMAT.format(it.date.atZone(clock.zone)), it.liters, it.status)
                }
            )
        }
    }

    /** "Outubro de 2026": mês atual, como no Figma. */
    private fun monthLabel(): String =
        MONTH_FORMAT.format(LocalDate.now(clock)).replaceFirstChar { it.titlecase(PT_BR) }

    private class HistoryRow(val date: Instant, val id: String, val liters: Double, val status: CollectionStatus)

    private companion object {
        val PENDING_REQUEST_STATUSES = listOf("PENDING", "APPROVED")
        const val RECORDED = "RECORDED"
        val PT_BR: Locale = Locale.forLanguageTag("pt-BR")
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", PT_BR)
    }

    /**
     * Cada `movement` é uma entrega vigente: {deliveryId, points, deliveryDate, revision}, mais
     * `oilVolumeLiters` e `recordStatus` pedidos à Core. Entrega anulada não aparece nem soma.
     */
    override suspend fun getCitizenHistory(): Result<CitizenHistory> = runCatching {
        val movements = client.fetchAllPages(ApiConfig.Main.USERS_ME_POINTS, itemsKey = "movements")
            .filter { it.optString("recordStatus", RECORDED) == RECORDED }
        val records = movements.map {
            check(it.has("oilVolumeLiters")) { "A Core ainda não manda oilVolumeLiters em /users/me/points" }
            DisposalRecord(
                id = it.getString("deliveryId"),
                liters = it.getDouble("oilVolumeLiters"),
                dateLabel = DATE_FORMAT.format(Instant.parse(it.getString("deliveryDate")).atZone(clock.zone)),
                // Água, CO2 e biodiesel: fórmulas ainda não definidas na Core (igual ao B2B).
                waterPreservedLiters = 0.0,
                co2AvoidedKg = 0.0,
                biodieselKg = 0.0
            )
        }
        CitizenHistory(
            monthLabel = monthLabel(),
            stats = ImpactStats(
                oilCollectedLiters = records.sumOf { it.liters },
                waterPreservedLiters = 0.0,
                co2AvoidedKg = 0.0,
                biodieselLiters = 0.0
            ),
            records = records
        )
    }
}

/**
 * Pontos de coleta na rota pública que a Core já tem: GET /pevs (só PEVs APPROVED,
 * de estabelecimento e de cidadão) → {items: [{id, address{..., latitude, longitude}}], totalElements}.
 * A Core não filtra por área nem busca por nome: o app lê todas as páginas uma vez
 * a cada [CACHE_TTL_MS] e filtra aqui. Sem nome nem horário na Core: o nome é
 * "PEV <bairro>" e o horário fica vazio (as telas escondem). PEV sem latitude/longitude
 * não tem pin. O complemento não aparece (pode ser a casa de um cidadão).
 */
class HttpCollectionPointRepository(
    private val client: ApiClient,
    private val geocoder: AddressGeocoder = AddressGeocoder.NONE,
    private val now: () -> Long = System::currentTimeMillis
) : CollectionPointRepository {

    private val cacheLock = Mutex()
    private var cache: List<CollectionPoint>? = null
    private var cachedAt = 0L

    private suspend fun allPoints(): List<CollectionPoint> = cacheLock.withLock {
        cache?.takeIf { now() - cachedAt < CACHE_TTL_MS }?.let { return it }
        client.fetchAllPages(ApiConfig.Main.PEVS).mapNotNull(::parsePev).also {
            cache = it
            cachedAt = now()
        }
    }

    override suspend fun getNearbyPoints(): Result<List<CollectionPoint>> = runCatching { allPoints() }

    override suspend fun getPointsInArea(bounds: MapBounds): Result<List<CollectionPoint>> = runCatching {
        allPoints().filter { bounds.contains(it.latitude, it.longitude) }
    }

    override suspend fun searchPoints(query: String): Result<List<PointSuggestion>> = runCatching {
        val q = normalize(query)
        if (q.length < MIN_SEARCH_LENGTH) return@runCatching emptyList()
        allPoints()
            .filter { normalize(it.name).contains(q) || normalize(it.address).contains(q) }
            .take(MAX_SUGGESTIONS)
            .map { PointSuggestion(it.id, it.name, it.type, it.address, it.latitude, it.longitude) }
    }

    /** A ficha usa o mesmo item da lista: a Core não tem `GET /pevs/{id}` público. */
    override suspend fun getPointDetails(id: String): Result<CollectionPointDetails> = runCatching {
        val point = allPoints().firstOrNull { it.id == id } ?: throw ApiException(404, "")
        CollectionPointDetails(
            id = point.id,
            name = point.name,
            type = point.type,
            address = point.address,
            latitude = point.latitude,
            longitude = point.longitude,
            acceptedMaterials = point.acceptedMaterials
        )
    }

    private fun parsePev(json: JSONObject): CollectionPoint? {
        val address = json.optJSONObject("address") ?: return null
        if (address.isNull("latitude") || address.isNull("longitude")) return null
        return CollectionPoint(
            id = json.getString("id"),
            name = "PEV ${address.optString("neighborhood")}".trim(),
            type = CollectionPointType.PEV,
            address = formatAddress(address),
            openingHours = "",
            isOpenNow = false,
            // Calculada no app com o GPS (MapViewModel); o backend não precisa mandar.
            distanceKm = null,
            acceptedMaterials = listOf(OIL_MATERIAL),
            latitude = address.getDouble("latitude"),
            longitude = address.getDouble("longitude")
        )
    }

    /** "Rua X, 10 - Bairro, Cidade/UF". */
    private fun formatAddress(address: JSONObject): String =
        "${address.optString("street")}, ${address.optString("number")} - " +
            "${address.optString("neighborhood")}, ${address.optString("city")}/${address.optString("state")}"

    /**
     * "Vire Coletor": POST /api/v1/pevs {address: {...}} (PevRequests.Create), com latitude e
     * longitude do [geocoder] (sem elas o PEV não aparece no mapa). O horário não vai: a Core não
     * tem o campo e recusa campo desconhecido (400).
     * Já tem PEV (409): recusado → corrige o endereço e reapresenta; pendente → só corrige;
     * aprovado ou inativo → [PevAlreadyExistsException].
     */
    override suspend fun becomeCollector(request: BecomeCollectorRequest): Result<Unit> = runCatching {
        val body = becomeCollectorBody(request, geocoder.locate(request.geocodingQuery()))
        try {
            client.post(ApiConfig.Main.PEVS, body)
        } catch (e: ApiException) {
            if (e.code != 409) throw e
            updateOwnPev(body)
        }
        cache = null
    }

    private suspend fun updateOwnPev(body: String) {
        when (val status = JSONObject(client.get(ApiConfig.Main.PEVS_ME)).optString("status")) {
            "REJECTED" -> {
                client.patch(ApiConfig.Main.PEVS_ME_ADDRESS, body)
                client.post(ApiConfig.Main.PEVS_ME_RESUBMISSIONS, "")
            }
            "PENDING" -> client.patch(ApiConfig.Main.PEVS_ME_ADDRESS, body)
            else -> throw PevAlreadyExistsException(status)
        }
    }

    internal fun becomeCollectorBody(request: BecomeCollectorRequest, location: GeoPoint? = null): String {
        val address = JSONObject()
            .put("state", request.state)
            .put("city", request.city)
            .put("neighborhood", request.neighborhood)
            .put("street", request.street)
            .put("number", request.number)
            .put("cep", request.cep)
        request.complement?.takeIf { it.isNotBlank() }?.let { address.put("complement", it) }
        location?.let { address.put("latitude", it.latitude).put("longitude", it.longitude) }
        return JSONObject().put("address", address).toString()
    }

    private fun BecomeCollectorRequest.geocodingQuery() =
        "$street, $number - $neighborhood, $city - $state, $cep, Brasil"

    private companion object {
        const val CACHE_TTL_MS = 5 * 60 * 1000L
        const val MIN_SEARCH_LENGTH = 3
        const val MAX_SUGGESTIONS = 8
        const val OIL_MATERIAL = "Óleo de cozinha usado"

        /** Minúsculas e sem acento: "São" e "sao" casam. */
        fun normalize(text: String): String =
            java.text.Normalizer.normalize(text.trim().lowercase(), java.text.Normalizer.Form.NFD)
                .replace(Regex("\\p{M}+"), "")
    }
}

class HttpCollectorRepository(private val client: ApiClient) : CollectorRepository {

    override suspend fun getRequests(): Result<List<CollectorRequest>> = runCatching {
        JSONArray(client.get(ApiConfig.Main.COLLECTOR_REQUESTS)).let { array ->
            List(array.length()) { index ->
                val it = array.getJSONObject(index)
                CollectorRequest(
                    id = it.getString("id"),
                    establishmentName = it.optString("establishmentName"),
                    dateLabel = it.optString("dateLabel"),
                    liters = it.optDouble("liters", 0.0)
                )
            }
        }
    }

    override suspend fun collect(requestId: String): Result<Unit> = runCatching {
        client.post(ApiConfig.Main.COLLECTOR_REQUEST_COLLECT.replace("{id}", requestId), "{}")
        Unit
    }

    override suspend fun discard(requestId: String): Result<Unit> = runCatching {
        client.post(ApiConfig.Main.COLLECTOR_REQUEST_DISCARD.replace("{id}", requestId), "{}")
        Unit
    }
}

class HttpTipsRepository(private val client: ApiClient) : TipsRepository {

    override suspend fun getTips(): Result<List<Tip>> = runCatching {
        JSONArray(client.get(ApiConfig.Main.TIPS)).let { array ->
            List(array.length()) { index ->
                val it = array.getJSONObject(index)
                Tip(it.getString("id"), it.optString("title"), it.optString("description"))
            }
        }
    }
}
