package com.olius.app.domain.repository

/**
 * Liga o passo 2 (código) ao passo 3 (nova senha) sem reenviar o código.
 * [expiresAt] (ISO-8601) só informa; quem decide se venceu é a Core.
 */
data class PasswordResetTicket(val resetToken: String, val expiresAt: String?) {
    override fun toString() = "PasswordResetTicket[redacted]"
}

/**
 * "Esqueci a senha" na Core, com código de 6 dígitos por e-mail. Falhas:
 * InvalidResetCodeException, WeakPasswordException, TooManyAttemptsException,
 * AuthUnavailableException (rota ainda não publicada) ou a IOException (sem rede).
 */
interface PasswordResetRepository {

    /** Pede o código. Sucesso não diz se a conta existe (a Core responde 202 sempre). */
    suspend fun requestCode(email: String): Result<Unit>

    suspend fun verifyCode(email: String, code: String): Result<PasswordResetTicket>

    suspend fun confirm(resetToken: String, newPassword: String): Result<Unit>
}
