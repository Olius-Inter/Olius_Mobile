package com.olius.app.domain.usecases

import com.olius.app.domain.entity.UserProfile
import com.olius.app.domain.repository.UserProfileRepository

/** Roda logo depois do login/cadastro na Core e descobre QUEM é a pessoa (GET /users/me: id, nome, tipo...). */
class SyncUserProfileUseCase(private val repository: UserProfileRepository) {
    suspend operator fun invoke(): UserProfile? =
        repository.fetchMyProfile().getOrNull() ?: repository.cachedProfile()
}
