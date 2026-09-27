package com.olius.app.domain.usecases

import com.olius.app.domain.repository.UserRepository

/**
 * `operator fun invoke` deixa a classe "chamável como função":
 * `loginUseCase(email, senha)` em vez de `loginUseCase.execute(email, senha)`.
 */
class LoginUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> =
        repository.signInWithEmail(email, password)
}
