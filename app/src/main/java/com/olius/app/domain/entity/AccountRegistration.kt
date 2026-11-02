package com.olius.app.domain.entity

/**
 * Dados do cadastro na Core (IdentityRequests.CitizenRegistration /
 * EstablishmentRegistration), sem a senha: o cadastro com Google
 * ("Completar cadastro") manda os mesmos campos, só que sem senha.
 *
 * A data de nascimento NÃO entra: o banco não tem a coluna e a Core recusa
 * campos desconhecidos (PEDIDOS_BANCO 1.1).
 */
sealed interface AccountRegistration {
    val name: String
    val email: String
    /** Telefones em E.164 só com dígitos (ex.: 5511999998888). Pode ser vazia. */
    val telephones: List<String>

    data class Citizen(
        override val name: String,
        override val email: String,
        override val telephones: List<String>,
        /** 11 dígitos, sem máscara. */
        val cpf: String
    ) : AccountRegistration

    data class Establishment(
        override val name: String,
        override val email: String,
        override val telephones: List<String>,
        /** 14 dígitos, sem máscara. */
        val cnpj: String,
        /** `establishment_type.id` (UUID) vindo de GET /establishment-types. */
        val typeId: String,
        val address: RegistrationAddress,
        val description: String? = null
    ) : AccountRegistration
}

/** `address` do cadastro de estabelecimento (IdentityRequests.AddressInput). */
data class RegistrationAddress(
    /** UF com 2 letras maiúsculas. */
    val state: String,
    val city: String,
    val neighborhood: String,
    val street: String,
    val number: String,
    /** 8 dígitos, sem máscara. */
    val cep: String,
    val complement: String? = null
)
