package com.olius.app.domain.usecases

import com.olius.app.domain.repository.UserRepository

/** `operator fun invoke` deixa a classe "chamável como função". */
class LoginUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> =
        repository.signInWithEmail(email, password)
}
