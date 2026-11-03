package com.olius.app.domain.usecases

import com.olius.app.domain.entity.EstablishmentType
import com.olius.app.domain.repository.UserRepository

/** Lista do campo "Tipo de estabelecimento" no cadastro. */
class GetEstablishmentTypesUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(): Result<List<EstablishmentType>> = repository.establishmentTypes()
}
