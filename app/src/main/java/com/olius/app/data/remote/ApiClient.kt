package com.olius.app.data.remote

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Resposta HTTP fora da faixa 2xx. */
class ApiException(val code: Int, val responseBody: String) : Exception("HTTP $code: $responseBody") {
    /** `code` do ProblemDetail da Core (ex.: "REGISTRATION_CONFLICT", "PROFILE_REQUIRED"); null se não houver. */
    val problemCode: String?
        get() = runCatching { JSONObject(responseBody).optString("code").ifEmpty { null } }.getOrNull()

    /** Campos recusados num 400 VALIDATION_ERROR (`errors[].field`). */
    val problemFields: List<String>
        get() = runCatching {
            val errors = JSONObject(responseBody).optJSONArray("errors") ?: return@runCatching emptyList()
            (0 until errors.length()).mapNotNull { errors.optJSONObject(it)?.optString("field")?.ifEmpty { null } }
        }.getOrDefault(emptyList())
}

/** Cliente HTTP mínimo sobre [HttpURLConnection] (já vem no Android). */
class ApiClient(
    private val baseUrl: String,
    private val tokenProvider: suspend () -> String? = { null },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val onUnauthorized: (suspend (rejectedToken: String) -> String?)? = null,
    private val readTimeoutMs: Int = ApiConfig.READ_TIMEOUT_MS,
    private val requireToken: Boolean = false
) {
    suspend fun get(path: String, query: Map<String, String?> = emptyMap()): String =
        request("GET", path, query, body = null)

    /** [headers]: cabeçalhos extras da rota (ex.: `Idempotency-Key` em POST /collection-requests). */
    suspend fun post(path: String, jsonBody: String, headers: Map<String, String> = emptyMap()): String =
        request("POST", path, emptyMap(), jsonBody, headers)

    /** PATCH (ex.: "Editar Perfil"). */
    suspend fun patch(path: String, jsonBody: String): String =
        request("PATCH", path, emptyMap(), jsonBody)

    private suspend fun request(

        method: String,
        path: String,
        query: Map<String, String?>,
        body: String?,
        headers: Map<String, String> = emptyMap()
    ): String {
        val token = tokenProvider()
        if (requireToken && token == null) throw ApiException(HTTP_UNAUTHORIZED, "")
        try {
            return execute(method, path, query, body, headers, token)
        } catch (e: ApiException) {
            val refresh = onUnauthorized
            if (e.code != HTTP_UNAUTHORIZED || token == null || refresh == null) throw e
            val renewed = refresh(token) ?: throw e
            return execute(method, path, query, body, headers, renewed)
        }
    }

    private suspend fun execute(
        method: String,
        path: String,
        query: Map<String, String?>,
        body: String?,
        headers: Map<String, String>,
        token: String?
    ): String = coroutineScope {
        val connection = (URL(buildUrl(path, query)).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = ApiConfig.CONNECT_TIMEOUT_MS
            readTimeout = readTimeoutMs
            setRequestProperty("Accept", "application/json")
            token?.let { setRequestProperty("Authorization", "Bearer $it") }
            headers.forEach { (name, value) -> setRequestProperty(name, value) }
        }
        val closeOnCancel = launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                awaitCancellation()
            } finally {
                connection.disconnect()
            }
        }
        try {
            withContext(ioDispatcher) {
                if (body != null) {
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }
                val code = connection.responseCode
                val success = code in 200..299
                val stream = if (success) connection.inputStream else connection.errorStream
                val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                if (!success) throw ApiException(code, text)
                text
            }
        } catch (e: IOException) {
            ensureActive()
            throw e
        } finally {
            closeOnCancel.cancel()
            connection.disconnect()
        }
    }

    private fun buildUrl(path: String, query: Map<String, String?>): String {
        val params = query.filterValues { it != null }.entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value!!)}"
        }
        val url = baseUrl.trimEnd('/') + "/" + path.trimStart('/')
        return if (params.isEmpty()) url else "$url?$params"
    }

    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
    }
}
