package com.olius.app.domain.usecases

import com.olius.app.domain.repository.UserRepository

/** Recebe o ID token já obtido do Google e troca pela sessão da Core (POST /auth/mobile/google). */
class GoogleSignInUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(idToken: String): Result<Unit> =
        repository.signInWithGoogleIdToken(idToken)
}
