package com.olius.app.domain.usecases

import com.olius.app.domain.entity.UserProfile
import com.olius.app.domain.repository.UserProfileRepository

/**
 * Roda logo depois do login/cadastro na Core e descobre QUEM é a pessoa
 * (GET /users/me: id, nome, tipo...). O perfil fica no cache do aparelho
 * (Room), então as telas não esperam a rede na próxima vez.
 *
 * Sem rede (ou erro), devolve o que estiver no cache; null se nunca carregou.
 * Nunca derruba o login: a Home tenta de novo quando abrir.
 */
class SyncUserProfileUseCase(private val repository: UserProfileRepository) {

    suspend operator fun invoke(): UserProfile? =
        repository.fetchMyProfile().getOrNull() ?: repository.cachedProfile()
}
