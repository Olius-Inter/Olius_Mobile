package com.olius.app.data.repository

import com.olius.app.data.remote.ApiClient
import com.olius.app.data.remote.ApiConfig
import com.olius.app.data.remote.ApiException
import com.olius.app.domain.entity.AchievementsSummary
import com.olius.app.domain.entity.RankingCategory
import com.olius.app.domain.entity.RankingEntry
import com.olius.app.domain.entity.RankingPage
import com.olius.app.domain.repository.RankingRepository
import org.json.JSONObject

/**
 * [RankingRepository] sobre a API Redis (`DB_Segundo/redis-api`):
 * `GET /api/ranking?category=&limit=` → `{year, updatedAt, myPosition, items}`.
 *
 * [client] manda o access token da Core, que a API usa para achar a posição da
 * pessoa. Se a sessão for recusada (401) ou a Core estiver fora (503), tenta de
 * novo com [anonymousClient]: o ranking continua aparecendo, só sem "Sua posição".
 */
class HttpRankingRepository(
    private val client: ApiClient,
    private val anonymousClient: ApiClient = client
) : RankingRepository {

    override suspend fun getRanking(category: RankingCategory): Result<RankingPage> =
        runCatching {
            val query = mapOf("category" to category.apiValue, "limit" to ApiConfig.RANKING_LIMIT.toString())
            val body = try {
                client.get(ApiConfig.Ranking.RANKING, query)
            } catch (e: ApiException) {
                if (anonymousClient === client || e.code !in SESSION_FALLBACK_CODES) throw e
                anonymousClient.get(ApiConfig.Ranking.RANKING, query)
            }
            parseRankingPage(JSONObject(body))
        }

    /** A API Redis não serve conquistas; elas vão ficar no Room. Sem chamada de rede até lá. */
    override suspend fun getAchievements(category: RankingCategory): Result<AchievementsSummary> =
        Result.failure(UnsupportedOperationException("Conquistas ainda não estão disponíveis no app"))

    private companion object {
        val SESSION_FALLBACK_CODES = setOf(401, 503)
    }
}

/** Converte o RankingResponse da API Redis. */
internal fun parseRankingPage(json: JSONObject): RankingPage {
    val items = json.getJSONArray("items")
    val mine = json.optJSONObject("myPosition")
    return RankingPage(
        year = json.getInt("year"),
        entries = List(items.length()) { index ->
            val item = items.getJSONObject(index)
            RankingEntry(
                position = item.getInt("position"),
                name = item.getString("name"),
                city = item.optStringOrNull("city").orEmpty(),
                points = item.getLong("points"),
                isCurrentUser = item.optBoolean("currentUser"),
                serverInitials = item.optStringOrNull("initials")
            )
        },
        myPosition = mine?.getInt("position"),
        myPoints = mine?.getLong("points"),
        updatedAt = json.optStringOrNull("updatedAt")
    )
}

/** `optString` devolve "null" para JSON null; aqui vira null de verdade. */
internal fun JSONObject.optStringOrNull(key: String): String? =
    if (isNull(key)) null else optString(key)

internal fun JSONObject.optDoubleOrNull(key: String): Double? =
    if (isNull(key)) null else optDouble(key)

/** Lista de objetos de um JSONArray opcional (ausente = lista vazia). */
internal fun <T> org.json.JSONArray?.mapObjects(transform: (JSONObject) -> T): List<T> =
    if (this == null) emptyList() else List(length()) { transform(getJSONObject(it)) }
