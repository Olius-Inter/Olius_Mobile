package com.olius.app.data.repository

import com.olius.app.data.remote.ApiClient
import com.olius.app.data.remote.ApiConfig
import com.olius.app.data.remote.ApiException
import com.olius.app.domain.entity.CepAddress
import com.olius.app.domain.repository.CepRepository
import org.json.JSONObject

/** [CepRepository] sobre o ViaCEP (grátis, sem chave). */
class ViaCepRepository(private val client: ApiClient) : CepRepository {
    override suspend fun lookup(cep: String): Result<CepAddress?> = runCatching {
        val body = try {
            client.get(ApiConfig.ViaCep.LOOKUP.replace("{cep}", cep))
        } catch (e: ApiException) {
            if (e.code == 400) return@runCatching null else throw e
        }
        parse(body)
    }

    companion object {
        /** null quando o ViaCEP responde `erro` (aceita true ou "true") ou sem UF válida. */
        fun parse(body: String): CepAddress? {
            val json = JSONObject(body)
            if (json.optBoolean("erro", false)) return null
            val uf = json.optString("uf").trim().uppercase()
            if (!uf.matches(Regex("[A-Z]{2}"))) return null
            return CepAddress(
                cep = json.optString("cep").filter(Char::isDigit),
                state = uf,
                city = json.optString("localidade").trim(),
                neighborhood = json.optString("bairro").trim(),
                street = json.optString("logradouro").trim()
            )
        }
    }
}
