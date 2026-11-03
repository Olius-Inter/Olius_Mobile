package com.olius.app.data.repository

import com.olius.app.data.local.SessionStore
import com.olius.app.domain.entity.ProfileNotFoundException
import com.olius.app.domain.entity.ProfileUpdate
import com.olius.app.domain.entity.RegistrationAddress
import com.olius.app.domain.entity.UserProfile

/** Perfil só do aparelho, sem rede: devolve o que estiver salvo. */
class MockUserProfileRepository(store: SessionStore) : StoredUserProfileRepository(store) {
    override suspend fun fetchMyProfile(): Result<UserProfile> =
        store.load()?.let { Result.success(it) } ?: Result.failure(ProfileNotFoundException())

    /** Edição só no aparelho: atualiza o cache, como se a Core tivesse aceitado. */
    override suspend fun updateMyProfile(update: ProfileUpdate): Result<UserProfile> {
        val current = store.load() ?: return Result.failure(ProfileNotFoundException())
        val updated = current.copy(name = update.name, email = update.email, phone = update.telephone)
        store.save(updated)
        return Result.success(updated)
    }

    override suspend fun fetchMyAddress(): Result<RegistrationAddress?> = Result.success(null)

    override suspend fun fetchCollectorStatus(): Result<Boolean> =
        Result.success(store.load()?.isCollector == true)
}
