package com.olius.app.data.remote

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Resposta HTTP fora da faixa 2xx. [code] permite tratar 404/422 como sinal, não erro genérico. */
class ApiException(val code: Int, message: String) : Exception("HTTP $code: $message")

/**
 * Cliente HTTP mínimo sobre [HttpURLConnection] (já vem no Android): sem
 * Retrofit/OkHttp, o projeto não ganha dependências novas nem precisa
 * regenerar o `verification-metadata.xml`. Devolve o corpo como String; quem
 * chama converte com `org.json`.
 *
 * [tokenProvider] devolve o token enviado como `Authorization: Bearer` —
 * hoje a API de ranking não exige, mas o backend principal vai exigir.
 * [ioDispatcher] é injetável para os testes.
 */
class ApiClient(
    private val baseUrl: String,
    private val tokenProvider: suspend () -> String? = { null },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun get(path: String, query: Map<String, String?> = emptyMap()): String =
        request("GET", path, query, body = null)

    suspend fun post(path: String, jsonBody: String): String =
        request("POST", path, emptyMap(), jsonBody)

    private suspend fun request(
        method: String,
        path: String,
        query: Map<String, String?>,
        body: String?
    ): String {
        val token = tokenProvider()
        return withContext(ioDispatcher) {
            val connection = (URL(buildUrl(path, query)).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = ApiConfig.CONNECT_TIMEOUT_MS
                readTimeout = ApiConfig.READ_TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
                token?.let { setRequestProperty("Authorization", "Bearer $it") }
            }
            try {
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
            } finally {
                connection.disconnect()
            }
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
}
