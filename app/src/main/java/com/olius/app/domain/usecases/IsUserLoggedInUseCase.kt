package com.olius.app.domain.usecases

import com.olius.app.domain.repository.UserRepository

/** Usado pela Splash pra decidir se pula o login (ver `SplashViewModel.kt`). */
class IsUserLoggedInUseCase(private val repository: UserRepository) {
    operator fun invoke(): Boolean = repository.isUserLoggedIn()
}
