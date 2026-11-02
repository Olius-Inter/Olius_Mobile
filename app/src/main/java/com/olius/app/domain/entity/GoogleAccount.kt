package com.olius.app.domain.entity

/** Conta escolhida no seletor do Google (Credential Manager). */
data class GoogleAccount(
    val idToken: String,
    val email: String,
    val name: String?
) {
    override fun toString() = "GoogleAccount[redacted]"
}
