package com.olius.app.data.repository

import com.olius.app.data.remote.ApiClient
import com.olius.app.data.remote.ApiConfig
import com.olius.app.data.remote.ApiException
import com.olius.app.domain.entity.Achievement
import com.olius.app.domain.entity.AchievementsSummary
import com.olius.app.domain.entity.RankingCategory
import com.olius.app.domain.entity.RankingEntry
import com.olius.app.domain.entity.RankingPage
import com.olius.app.domain.entity.SustainabilityLevel
import com.olius.app.domain.repository.RankingRepository
import kotlinx.coroutines.CancellationException
import org.json.JSONObject

/**
 * [RankingRepository] sobre a API Redis (`DB_Segundo/redis-api`):
 * `GET /api/ranking?category=&limit=` → `{year, updatedAt, myPosition, items}`.
 *
 * [client] manda o access token da Core, que a API usa para achar a posição da
 * pessoa. Se a sessão for recusada (401) ou a Core estiver fora (503), tenta de
 * novo com [anonymousClient]: o ranking continua aparecendo, só sem "Sua posição".
 * Para montar os dois clientes a partir da sessão da Core, use [withCoreSession].
 *
 * Conquistas: a API Redis não tem. Para estabelecimento, vêm da Core
 * (GET /establishments/me/certificate-progress) pelo [coreClient]; sem ele
 * (USE_MOCK_MAIN_BACKEND) ou para cidadão (a Core não tem rota), falha e a tela mostra "indisponível".
 */
class HttpRankingRepository(
    private val client: ApiClient,
    private val anonymousClient: ApiClient = client,
    private val coreClient: ApiClient? = null
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

    override suspend fun getAchievements(category: RankingCategory): Result<AchievementsSummary> {
        val core = coreClient
        if (core == null || category != RankingCategory.ESTABLISHMENT) {
            return Result.failure(UnsupportedOperationException("Conquistas ainda não estão disponíveis no app"))
        }
        return runCatching {
            parseCertificateProgress(JSONObject(core.get(ApiConfig.Main.CERTIFICATE_PROGRESS)))
        }
    }

    companion object {
        private val SESSION_FALLBACK_CODES = setOf(401, 503)

        /**
         * Ranking ligado à sessão da Core. [tokenProvider] e [onUnauthorized] são
         * os da sessão (`AppContainer.coreAccessTokenProvider` e o refresh dela):
         * a API Redis repassa o Bearer para `GET /api/v1/auth/session` da Core,
         * então um 401 aqui é o access vencido → renova uma vez e repete.
         * Falha ao pegar/renovar o token (sem rede, Core fora) não derruba o
         * ranking: a chamada segue sem token, só sem "Sua posição".
         */
        fun withCoreSession(
            baseUrl: String,
            tokenProvider: suspend () -> String?,
            onUnauthorized: suspend (rejectedToken: String) -> String?,
            coreClient: ApiClient? = null
        ): HttpRankingRepository = HttpRankingRepository(
            client = ApiClient(
                baseUrl = baseUrl,
                tokenProvider = { orNull { tokenProvider() } },
                onUnauthorized = { rejected -> orNull { onUnauthorized(rejected) } }
            ),
            anonymousClient = ApiClient(baseUrl),
            coreClient = coreClient
        )

        // Erro da sessão vira "sem token"; cancelamento continua subindo.
        private suspend fun orNull(block: suspend () -> String?): String? =
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
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

/**
 * Converte o `CertificateProgress` da Core (CertificateContracts.Progress):
 * `{eligibleVolumeLiters, levels: [{levelId, name, description, requiredLiters, reached, ...}]}`.
 * Litros acumulados sem corte anual. Sem níveis no catálogo → `configured = false`.
 */
internal fun parseCertificateProgress(json: JSONObject): AchievementsSummary {
    val total = json.optDouble("eligibleVolumeLiters", 0.0)
    val levels = json.optJSONArray("levels").mapObjects {
        Achievement(
            id = it.getString("levelId"),
            title = it.getString("name"),
            description = it.optStringOrNull("description"),
            // O progresso não traz a imagem (fica em GET /certificate-levels).
            badgeImageUrl = null,
            requiredLiters = it.getDouble("requiredLiters"),
            unlocked = it.optBoolean("reached"),
            unlockedAt = null,
            rarity = null
        )
    }.sortedBy { it.requiredLiters }
    val reached = levels.count { it.unlocked }
    val current = levels.getOrNull(reached - 1)
    val next = levels.getOrNull(reached)
    val base = current?.requiredLiters ?: 0.0
    val level = SustainabilityLevel(
        level = reached,
        name = current?.title,
        totalLiters = total,
        targetLiters = next?.requiredLiters ?: levels.lastOrNull()?.requiredLiters,
        progress = next?.let { ((total - base) / (it.requiredLiters - base)).toFloat().coerceIn(0f, 1f) }
            ?: if (levels.isEmpty()) 0f else 1f,
        isMax = levels.isNotEmpty() && next == null,
        configured = levels.isNotEmpty()
    )
    return AchievementsSummary(level, levels)
}
