package com.olius.app.domain.repository

import com.olius.app.domain.entity.ProfileUpdate
import com.olius.app.domain.entity.UserProfile

/** Perfil do usuário na Core. */
interface UserProfileRepository {
    /** Perfil salvo no aparelho (sem rede). */
    fun cachedProfile(): UserProfile?

    /** GET /users/me; o resultado também atualiza o cache. */
    suspend fun fetchMyProfile(): Result<UserProfile>

    /** PATCH /users/me {name, email, telephone} (ainda NÃO existe na Core). */
    suspend fun updateMyProfile(update: ProfileUpdate): Result<UserProfile>

    /** Coletor = GET /pevs/me com status APPROVED (sem PEV → false). */
    suspend fun fetchCollectorStatus(): Result<Boolean>

    /** Guarda localmente. */
    fun saveLocal(profile: UserProfile)

    /** Logout. */
    fun clear()
}
