package com.olius.app.domain.usecases

import com.olius.app.domain.repository.UserRepository

/** Sai da conta: limpa a sessão do aparelho e avisa a Core (POST /auth/mobile/logout). */
class LogoutUseCase(private val repository: UserRepository) {
    operator fun invoke() = repository.signOut()
}
