package com.olius.app.domain.usecases

import com.olius.app.domain.repository.UserRepository

/**
 * Recebe o ID token já obtido do Google (via Credential Manager, na camada
 * de apresentação — ver `presentation/auth/GoogleIdentityClient.kt` e
 * AUTH_02_LOGIN_GOOGLE.md) e troca por uma sessão do Firebase.
 */
class GoogleSignInUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(idToken: String): Result<Unit> =
        repository.signInWithGoogleIdToken(idToken)
}
