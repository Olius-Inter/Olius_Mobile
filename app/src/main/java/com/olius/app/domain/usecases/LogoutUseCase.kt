package com.olius.app.domain.usecases

import com.olius.app.domain.repository.UserRepository

class LogoutUseCase(private val repository: UserRepository) {
    operator fun invoke() = repository.signOut()
}
