package com.olius.app.domain.repository

import com.olius.app.domain.entity.ProfileUpdate
import com.olius.app.domain.entity.UserProfile

/**
 * Perfil do usuário na Core. A Core identifica a pessoa pelo JWT dela
 * (Bearer) e devolve o `users.id` e o `user_type` do Postgres. O último
 * perfil carregado fica no aparelho (Room) para as telas abrirem sem rede.
 */
interface UserProfileRepository {

    /** Perfil salvo no aparelho (sem rede). null = ainda não carregou nenhuma vez. */
    fun cachedProfile(): UserProfile?

    /** GET /users/me; o resultado também atualiza o cache. */
    suspend fun fetchMyProfile(): Result<UserProfile>

    /**
     * PATCH /users/me {name, email, telephone} (ainda NÃO existe na Core).
     * Sucesso → perfil devolvido pela Core, já salvo no cache.
     */
    suspend fun updateMyProfile(update: ProfileUpdate): Result<UserProfile>

    /**
     * Coletor = GET /pevs/me com status APPROVED (sem PEV → false). O
     * resultado também vai para o cache (`isCollector`).
     */
    suspend fun fetchCollectorStatus(): Result<Boolean>

    /** Guarda localmente. */
    fun saveLocal(profile: UserProfile)

    /** Logout. */
    fun clear()
}
