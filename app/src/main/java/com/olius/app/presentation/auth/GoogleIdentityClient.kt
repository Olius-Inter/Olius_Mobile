package com.olius.app.presentation.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.olius.app.R
import java.util.UUID

/**
 * Pede ao Credential Manager um ID token do Google pra conta escolhida no
 * dispositivo. Fica na camada de apresentação (não no ViewModel/domínio)
 * porque `CredentialManager.getCredential` precisa de um Context de Activity
 * — ver AUTH_02_LOGIN_GOOGLE.md, seção 6, pro porquê dessa fronteira.
 *
 * `R.string.default_web_client_id` é gerado automaticamente pelo plugin
 * `google-services` a partir do `google-services.json` (client_type 3) —
 * ver AUTH_02, seção 3.
 */
suspend fun requestGoogleIdToken(context: Context): String {
    val nonce = UUID.randomUUID().toString()

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setAutoSelectEnabled(false)
        .setServerClientId(context.getString(R.string.default_web_client_id))
        .setNonce(nonce)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    val credentialManager = CredentialManager.create(context)
    val result = credentialManager.getCredential(context, request)

    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
    return googleIdTokenCredential.idToken
}
