package com.olius.app.domain.repository

/** Liga o passo 2 (código) ao passo 3 (nova senha) sem reenviar o código. */
data class PasswordResetTicket(val resetToken: String, val expiresAt: String?) {
    override fun toString() = "PasswordResetTicket[redacted]"
}

/** "Esqueci a senha" na Core, com código de 6 dígitos por e-mail. */
interface PasswordResetRepository {
    /** Pede o link de troca de senha por e-mail (fluxo da Core). */
    suspend fun requestResetLink(email: String): Result<Unit>

    /** Pede o código. */
    suspend fun requestCode(email: String): Result<Unit>

    suspend fun verifyCode(email: String, code: String): Result<PasswordResetTicket>

    suspend fun confirm(resetToken: String, newPassword: String): Result<Unit>
}
