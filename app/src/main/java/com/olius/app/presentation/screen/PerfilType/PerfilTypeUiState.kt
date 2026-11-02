package com.olius.app.presentation.screen.PerfilType

import com.olius.app.domain.entity.BrazilianDocuments
import com.olius.app.domain.entity.EstablishmentType
import com.olius.app.domain.entity.GoogleAccount
import com.olius.app.domain.entity.PasswordRule
import com.olius.app.domain.entity.PhoneCountry
import com.olius.app.domain.entity.ProfileType
import com.olius.app.presentation.screen.becomecollector.CepStatus

enum class OnboardingStep {
    HEADER_INTRO,
    CHOOSE_PROFILE,
    LOGIN,
    REGISTER,
    /** Entrou com o Google e ainda não tem conta na Core: pede o que falta (sem senha). */
    COMPLETE_PROFILE
}

/** Campos do cadastro que mostram erro embaixo deles (o telefone usa [PerfilTypeUiState.registerPhoneError]). */
enum class RegisterField {
    NAME, EMAIL, PASSWORD, DOCUMENT, ESTABLISHMENT_TYPE, CEP, STREET, NUMBER, NEIGHBORHOOD, CITY
}

/** Carga da lista de tipos de estabelecimento (GET /establishment-types). */
enum class LoadStatus { IDLE, LOADING, LOADED, ERROR }

data class PerfilTypeUiState(
    val step: OnboardingStep = OnboardingStep.HEADER_INTRO,
    val selectedProfileType: ProfileType? = null,

    val loginEmail: String = "",
    val loginPassword: String = "",
    val loginPasswordVisible: Boolean = false,

    val registerName: String = "",
    val registerEmail: String = "",
    val registerPhoneCountry: PhoneCountry = PhoneCountry.BRAZIL,
    val registerPhone: String = "",
    val registerPhoneFormatted: String = "",
    val registerPhoneE164: String? = null,
    val registerPhoneError: String? = null,
    val registerPassword: String = "",
    val registerPasswordVisible: Boolean = false,

    val registerDocument: String = "",

    val establishmentTypes: List<EstablishmentType> = emptyList(),
    val establishmentTypesStatus: LoadStatus = LoadStatus.IDLE,
    val registerEstablishmentTypeId: String? = null,
    val registerCep: String = "",
    val registerState: String = "",
    val registerCity: String = "",
    val registerNeighborhood: String = "",
    val registerStreet: String = "",
    val registerNumber: String = "",
    val registerComplement: String = "",
    val cepStatus: CepStatus = CepStatus.IDLE,

    val registerFieldErrors: Map<RegisterField, String> = emptyMap(),

    val googleAccount: GoogleAccount? = null,

    val isAuthenticating: Boolean = false,

    val authErrorMessage: String? = null
) {
    /** Tipo usado no cadastro: o escolhido no onboarding (estabelecimento se nenhum). */
    val registrationType: ProfileType get() = selectedProfileType ?: ProfileType.ESTABLISHMENT

    /** Regras da senha (as mesmas da Core) e se cada uma já foi cumprida, para a lista na tela. */
    val passwordChecklist: List<Pair<PasswordRule, Boolean>>
        get() = PasswordRule.entries.map { it to it.isSatisfiedBy(registerPassword) }

    /** CPF ou CNPJ com a máscara, conforme o tipo. */
    val registerDocumentFormatted: String
        get() = if (registrationType == ProfileType.CITIZEN) BrazilianDocuments.formatCpf(registerDocument)
        else BrazilianDocuments.formatCnpj(registerDocument)

    val selectedEstablishmentType: EstablishmentType?
        get() = establishmentTypes.firstOrNull { it.id == registerEstablishmentTypeId }

    /** Texto embaixo do CEP: carregando ou erro da consulta. */
    val cepMessage: String?
        get() = when (cepStatus) {
            CepStatus.LOADING -> "Buscando endereço..."
            CepStatus.NOT_FOUND -> "CEP não encontrado. Confira os números."
            CepStatus.NETWORK_ERROR -> "Sem conexão para consultar o CEP."
            else -> null
        }

    fun fieldError(field: RegisterField): String? = registerFieldErrors[field]
}

/** Evento de navegação único. */
sealed class PerfilTypeNavigationEvent {
    object NavigateHome : PerfilTypeNavigationEvent()
}
