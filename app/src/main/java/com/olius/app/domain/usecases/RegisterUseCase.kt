package com.olius.app.domain.usecases

import com.olius.app.domain.entity.AccountRegistration
import com.olius.app.domain.repository.UserRepository

/** Cadastro na Core (POST /auth/registrations/citizens ou /establishments). */
class RegisterUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(registration: AccountRegistration, password: String): Result<Unit> =
        repository.register(registration, password)
}
