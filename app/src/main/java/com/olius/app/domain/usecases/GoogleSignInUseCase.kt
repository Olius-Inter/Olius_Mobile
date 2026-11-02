package com.olius.app.domain.usecases

import com.olius.app.domain.repository.UserRepository

/**
 * Recebe o ID token já obtido do Google (via Credential Manager, na camada
 * de apresentação — ver `presentation/auth/GoogleIdentityClient.kt`) e troca
 * pela sessão da Core (POST /auth/mobile/google). Conta nova → falha com
 * ProfileRequiredException, e a tela abre "Completar cadastro".
 */
class GoogleSignInUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(idToken: String): Result<Unit> =
        repository.signInWithGoogleIdToken(idToken)
}
