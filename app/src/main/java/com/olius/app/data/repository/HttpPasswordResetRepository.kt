package com.olius.app.data.repository

import com.olius.app.data.remote.ApiClient
import com.olius.app.data.remote.ApiConfig
import com.olius.app.data.remote.ApiException
import com.olius.app.domain.entity.AuthUnavailableException
import com.olius.app.domain.entity.InvalidResetCodeException
import com.olius.app.domain.entity.TooManyAttemptsException
import com.olius.app.domain.entity.WeakPasswordException
import com.olius.app.domain.repository.PasswordResetRepository
import com.olius.app.domain.repository.PasswordResetTicket
import kotlinx.coroutines.CancellationException
import org.json.JSONObject

/** [PasswordResetRepository] sobre as rotas públicas da Core. */
class HttpPasswordResetRepository(private val client: ApiClient) : PasswordResetRepository {
    override suspend fun requestResetLink(email: String): Result<Unit> = call(Step.REQUEST) {
        client.post(ApiConfig.Main.PASSWORD_RESET_REQUESTS, JSONObject().put("email", email).toString())
        Unit
    }

    override suspend fun requestCode(email: String): Result<Unit> = call(Step.REQUEST) {
        client.post(ApiConfig.Main.PASSWORD_RESETS, JSONObject().put("email", email).toString())
        Unit
    }

    override suspend fun verifyCode(email: String, code: String): Result<PasswordResetTicket> = call(Step.VERIFY) {
        val body = JSONObject().put("email", email).put("code", code).toString()
        val json = JSONObject(client.post(ApiConfig.Main.PASSWORD_RESETS_VERIFY, body))
        PasswordResetTicket(
            resetToken = json.getString("resetToken"),
            expiresAt = json.optString("expiresAt").ifEmpty { null }
        )
    }

    override suspend fun confirm(resetToken: String, newPassword: String): Result<Unit> = call(Step.CONFIRM) {
        val body = JSONObject().put("resetToken", resetToken).put("newPassword", newPassword).toString()
        client.post(ApiConfig.Main.PASSWORD_RESETS_CONFIRM, body)
        Unit
    }

    private enum class Step { REQUEST, VERIFY, CONFIRM }

    private suspend fun <T> call(step: Step, block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: ApiException) {
            Result.failure(e.toResetException(step))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    private fun ApiException.toResetException(step: Step): Exception = when {
        code == HTTP_UNAUTHORIZED || code == HTTP_NOT_FOUND || code == HTTP_METHOD_NOT_ALLOWED -> AuthUnavailableException()
        code == HTTP_TOO_MANY_REQUESTS -> TooManyAttemptsException()
        code == HTTP_BAD_REQUEST && step == Step.VERIFY -> InvalidResetCodeException()
        code == HTTP_BAD_REQUEST && step == Step.CONFIRM ->
            if (problemFields.any { it == NEW_PASSWORD_FIELD }) WeakPasswordException() else InvalidResetCodeException()
        else -> this
    }

    private companion object {
        const val HTTP_BAD_REQUEST = 400
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_NOT_FOUND = 404
        const val HTTP_METHOD_NOT_ALLOWED = 405
        const val HTTP_TOO_MANY_REQUESTS = 429
        const val NEW_PASSWORD_FIELD = "newPassword"
    }
}
