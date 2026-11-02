package com.olius.app.presentation.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.olius.app.R
import com.olius.app.domain.entity.GoogleAccount
import java.util.UUID

/** Pede ao Credential Manager um ID token do Google pra conta escolhida no dispositivo. */
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
    return GoogleAccount(idToken = credential.idToken, email = credential.id, name = credential.displayName)
}
