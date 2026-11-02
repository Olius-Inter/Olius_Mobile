package com.olius.app.presentation.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.olius.app.R
import com.olius.app.domain.entity.GoogleAccount
import java.util.UUID

/**
 * Pede ao Credential Manager um ID token do Google pra conta escolhida no
 * dispositivo. Sem Firebase: o ID token vai direto para a Core
 * (POST /auth/mobile/google), que confere a assinatura com as chaves do Google. Fica na camada de apresentação (não no ViewModel/domínio)
 * porque `CredentialManager.getCredential` precisa de um Context de Activity
 * — ver AUTH_02_LOGIN_GOOGLE.md, seção 6, pro porquê dessa fronteira.
 *
 * `R.string.default_web_client_id` é gerado automaticamente pelo plugin
 * `google-services` a partir do `google-services.json` (client_type 3). É o
 * Client ID Web que a Core confere no `aud` do token. O plugin continua no
 * build só para isso (o app não usa mais nenhuma lib do Firebase).
 */
suspend fun requestGoogleAccount(context: Context): GoogleAccount {
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

    val credential = GoogleIdTokenCredential.createFrom(result.credential.data)
    // `id` do GoogleIdTokenCredential é o e-mail da conta.
    return GoogleAccount(idToken = credential.idToken, email = credential.id, name = credential.displayName)
}
