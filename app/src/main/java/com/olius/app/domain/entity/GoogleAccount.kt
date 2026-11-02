package com.olius.app.domain.entity

/**
 * Conta escolhida no seletor do Google (Credential Manager). O [idToken] vai
 * para a Core; [email] e [name] só pré-preenchem o "Completar cadastro".
 */
data class GoogleAccount(
    val idToken: String,
    val email: String,
    val name: String?
) {
    override fun toString() = "GoogleAccount[redacted]"
}
