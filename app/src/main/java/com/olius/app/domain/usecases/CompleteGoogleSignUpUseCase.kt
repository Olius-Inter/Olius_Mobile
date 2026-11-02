package com.olius.app.domain.usecases

import com.olius.app.domain.entity.AccountRegistration
import com.olius.app.domain.repository.UserRepository

/**
 * "Completar cadastro" de quem entrou com o Google e ainda não tem conta na
 * Core: envia o ID token + os dados do cadastro (sem senha) e já volta logado.
 */
class CompleteGoogleSignUpUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(idToken: String, registration: AccountRegistration): Result<Unit> =
        repository.completeGoogleRegistration(idToken, registration)
}
