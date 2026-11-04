package com.olius.app.data.remote

import com.olius.app.BuildConfig

/** ÚNICO lugar com URLs e endpoints do app. */
object ApiConfig {
    val RANKING_BASE_URL = withTrailingSlash(BuildConfig.OLIUS_RANKING_BASE_URL)
    val MAIN_BASE_URL = withTrailingSlash(BuildConfig.OLIUS_CORE_BASE_URL)

    /** As rotas não começam com "/", então a base precisa terminar com ela. */
    fun withTrailingSlash(url: String): String = if (url.endsWith("/")) url else "$url/"

    /** Forms que o PEV ou o motorista abre ao escanear o QR do usuário (`?qrToken=`). */
    const val FORMS_BASE_URL = "https://olius-frontend-forms-segundo.onrender.com/"

    /** "Fale conosco" do Perfil abre o app de e-mail para este endereço (e-mail oficial do Olius). */
    const val SUPPORT_EMAIL = "oliusjef26@gmail.com"

    /** Botões de login com Google, Facebook e Microsoft. */
    const val SOCIAL_LOGIN_ENABLED = false

    /** "Esqueci a senha". */
    const val PASSWORD_RESET_WITH_CODE = false

    const val CONNECT_TIMEOUT_MS = 10_000
    const val READ_TIMEOUT_MS = 15_000

    /** A IA (LLM + agentes) pode levar bem mais que as outras rotas. */
    const val CHAT_READ_TIMEOUT_MS = 60_000

    /** Quantos participantes o ranking pede (a API aceita 1..100). */
    const val RANKING_LIMIT = 10

    /** API Redis (`API_Redis_Olius`): só o ranking anual por pontos. */
    object Ranking {
        /** GET ?category=establishment|citizen&limit= → {year, updatedAt, myPosition, items}. */
        const val RANKING = "api/ranking"
    }

    /** ViaCEP (grátis, sem chave, https): preenche UF, cidade, bairro e rua no "Vire Coletor". */
    const val VIACEP_BASE_URL = "https://viacep.com.br/"

    object ViaCep {
        /** GET → {cep, logradouro, bairro, localidade, uf}; CEP inexistente → {"erro": true} */
        const val LOOKUP = "ws/{cep}/json/"
    }

    /** Core API (`API_Olius/olius-api`). */
    object Main {
        /** POST {email, password} → {accessToken, tokenType, expiresIn, expiresAt, refreshToken, refreshExpiresAt} */
        const val AUTH_MOBILE_LOGIN = "api/v1/auth/mobile/login"

        /** POST {refreshToken} → mesmos campos do login (o refresh antigo deixa de valer) */
        const val AUTH_MOBILE_REFRESH = "api/v1/auth/mobile/refresh"

        /** POST (Bearer) → 204: encerra a sessão atual */
        const val AUTH_MOBILE_LOGOUT = "api/v1/auth/mobile/logout"

        /** POST {name, email, password, cpf, telephones[]} → 201 {id, userType} (depois o app faz o login) */
        const val REGISTER_CITIZEN = "api/v1/auth/registrations/citizens"

        /** POST {name, email, password, cnpj, typeId, description, address{...}, telephones[]} → 201 {id, userType} */
        const val REGISTER_ESTABLISHMENT = "api/v1/auth/registrations/establishments"

        /** Esqueci a senha (público). */
        const val PASSWORD_RESET_REQUESTS = "api/v1/auth/password-reset-requests"

        /** GET (público) → [{id, name, description}] para o cadastro de estabelecimento */
        const val ESTABLISHMENT_TYPES = "api/v1/establishment-types"

        /** GET (Bearer) → {id, name, email, userType, status, createdAt, telephones[], cpf, cnpj, qrToken, establishmentTypeId, description, address}. */
        const val USERS_ME = "api/v1/users/me"

        /** GET (Bearer de cidadão ou estabelecimento) → {id, status, createdAt, approvedAt, address}. */
        const val PEVS_ME = "api/v1/pevs/me"

        /** PATCH (Bearer) {address: {...}} (endereço completo, igual ao POST) → OwnPev. */
        const val PEVS_ME_ADDRESS = "api/v1/pevs/me/address"

        /** POST (Bearer, sem corpo) → OwnPev de volta em PENDING. */
        const val PEVS_ME_RESUBMISSIONS = "api/v1/pevs/me/resubmissions"

        /** GET (Bearer de cidadão) ?page=&size= → {balance, movements: [{deliveryId, points, deliveryDate, revision}], page, size, totalElements}. */
        const val USERS_ME_POINTS = "api/v1/users/me/points"

        const val USERS_ME_DELIVERIES = "api/v1/users/me/deliveries"

        /** POST (Bearer de ESTABELECIMENTO, header `Idempotency-Key: <UUID>`) {estimatedVolumeLiters, observation?} → 201 RequestView. */
        const val COLLECTION_REQUESTS = "api/v1/collection-requests"

        const val PEVS = "api/v1/pevs"

        const val COLLECTIONS = "api/v1/collections"

        /** GET (Bearer de ESTABELECIMENTO) → {points, successfulCollectionsCount, collectedVolumeLiters}. */
        const val ESTABLISHMENT_SCORE = "api/v1/establishments/me/score"

        const val CERTIFICATE_PROGRESS = "api/v1/establishments/me/certificate-progress"

        /** POST {idToken} → mesmos campos do login (MobileTokens). */
        const val AUTH_MOBILE_GOOGLE = "api/v1/auth/mobile/google"

        /** "Completar cadastro" do Google: POST {idToken, name, email, cpf, telephones[]} (sem password) → MobileTokens */
        const val REGISTER_CITIZEN_GOOGLE = "api/v1/auth/registrations/citizens/google"

        /** POST {idToken, name, email, cnpj, typeId, description, address{...}, telephones[]} (sem password) → MobileTokens */
        const val REGISTER_ESTABLISHMENT_GOOGLE = "api/v1/auth/registrations/establishments/google"

        /** Esqueci a senha COM CÓDIGO (só com [PASSWORD_RESET_WITH_CODE]). */
        const val PASSWORD_RESETS = "api/v1/auth/password-resets"

        /** POST {email, code (6 dígitos)} → 200 {resetToken, expiresAt}; código errado ou vencido → 400 */
        const val PASSWORD_RESETS_VERIFY = "api/v1/auth/password-resets/verify"

        /** POST {resetToken, newPassword} → 204; senha fora da regra → 400 */
        const val PASSWORD_RESETS_CONFIRM = "api/v1/auth/password-resets/confirm"

        /** GET → HomeDashboard (indicadores, tanque, perfil e notificações numa chamada só) */
        const val HOME_DASHBOARD = "api/v1/home"

        /** POST {litros} → HomeDashboard atualizado (estabelecimento; no B2C, só cidadão PEV) */
        const val OIL_REGISTRATIONS = "api/v1/oil-registrations"

        /** GET → [{id, establishmentName, dateLabel, liters}]. */
        const val COLLECTOR_REQUESTS = "api/v1/collector/requests"

        /** POST → aceitar ("Coletar") / recusar ("Descartar") uma solicitação ({id}) */
        const val COLLECTOR_REQUEST_COLLECT = "api/v1/collector/requests/{id}/collect"
        const val COLLECTOR_REQUEST_DISCARD = "api/v1/collector/requests/{id}/discard"

        /** Chat da OlivIA (Bearer). */
        const val CHAT = "api/v1/chat"
    }
}
