package com.olius.app.domain.repository

import com.olius.app.domain.entity.ProfileRegistration
import com.olius.app.domain.entity.UserProfile

/**
 * Perfil do usuário no backend principal. O backend identifica a pessoa pelo
 * token do Firebase (Bearer) e devolve o `users.id` e o `user_type` do Postgres.
 */
interface UserProfileRepository {

    /** Perfil salvo no aparelho (sem rede). null = ainda não sincronizou nenhuma vez. */
    fun cachedProfile(): UserProfile?

    /** GET /users/me. Result.failure(ProfileNotFoundException) quando ainda não existe. */
    suspend fun fetchMyProfile(): Result<UserProfile>

    /** POST /users: cria `users` + `establishment`/`citizens` e devolve o perfil com o id do banco. */
    suspend fun createProfile(registration: ProfileRegistration): Result<UserProfile>

    /** Guarda localmente (ex.: perfil provisório quando o backend está fora). */
    fun saveLocal(profile: UserProfile)

    /** Logout. */
    fun clear()
}
