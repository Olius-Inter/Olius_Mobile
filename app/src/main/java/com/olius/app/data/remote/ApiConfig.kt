package com.olius.app.data.remote

/**
 * ÚNICO lugar com URLs e endpoints do app. Para apontar para outro servidor,
 * troque só a base URL; para mudar uma rota, troque só a constante dela.
 * Ver API_00_CONEXAO_API.md e MAPA_PENDENCIAS.md.
 *
 * - [RANKING_BASE_URL]: API de Ranking e Conquistas (Java + Spring MVC + Redis,
 *   pasta `DB_Segundo/redis-api`). `10.0.2.2` é o "localhost" do PC visto de
 *   dentro do emulador; 8081 é a porta da API (local e docker-compose).
 * - [MAIN_BASE_URL]: Core API (Java + Spring + Postgres, repositório
 *   `API_Olius/olius-api`), porta padrão 8080. Prefixo de todas as rotas: `/api/v1`.
 *   Enquanto [USE_MOCK_MAIN_BACKEND] for true, as telas usam dados mockados.
 */
object ApiConfig {

    const val RANKING_BASE_URL = "http://10.0.2.2:8081/"
    const val MAIN_BASE_URL = "http://10.0.2.2:8080/"

    const val USE_MOCK_RANKING = false

    /**
     * A Core já tem cadastro, login e /users/me, mas autentica com JWT próprio
     * (não Firebase) e ainda não tem home, histórico nem pontos de coleta.
     * Só passe para false depois do item 1 do MAPA_PENDENCIAS.md.
     */
    const val USE_MOCK_MAIN_BACKEND = true

    /**
     * Página web aberta quando alguém escaneia o QR de identificação. O app
     * gera "QR_WEB_BASE_URL + token" (token = user_qr_code.qr_token).
     * PROVISÓRIO: troque pelo domínio real quando o site existir.
     */
    const val QR_WEB_BASE_URL = "https://olius.app/coleta/"

    /** Só para testar as telas do Coletor/PEV com o backend em mock (o perfil mock nasce PEV). */
    const val DEBUG_MOCK_COLLECTOR = false

    const val CONNECT_TIMEOUT_MS = 10_000
    const val READ_TIMEOUT_MS = 15_000

    /** Quantos participantes o ranking pede (a API aceita 1..100). */
    const val RANKING_LIMIT = 10

    /**
     * API Redis (`DB_Segundo/redis-api`): só o ranking anual por pontos.
     * Conquistas não vêm dela (vão ficar no Room).
     */
    object Ranking {
        /**
         * GET ?category=establishment|citizen&limit= → {year, updatedAt, myPosition, items}.
         * `myPosition` só vem com `Authorization: Bearer <access token da Core>`.
         */
        const val RANKING = "api/ranking"
    }

    /**
     * Core API (`API_Olius/olius-api`). As marcadas "JÁ EXISTE" estão nos
     * controllers da Core hoje (IdentityController / AuthenticationController);
     * as outras são o contrato que o app espera e ainda precisam ser criadas.
     */
    object Main {
        // ---------------- JÁ EXISTE na Core ----------------

        /** POST {email, password} → {accessToken, tokenType, expiresIn, expiresAt, refreshToken, refreshExpiresAt} */
        const val AUTH_MOBILE_LOGIN = "api/v1/auth/mobile/login"

        /** POST {refreshToken} → mesmos campos do login (o refresh antigo deixa de valer) */
        const val AUTH_MOBILE_REFRESH = "api/v1/auth/mobile/refresh"

        /** POST (Bearer) → 204: encerra a sessão atual */
        const val AUTH_MOBILE_LOGOUT = "api/v1/auth/mobile/logout"

        /** POST {name, email, password, cpf, telephones[]} → 201 {id, userType} */
        const val REGISTER_CITIZEN = "api/v1/auth/registrations/citizens"

        /** POST {name, email, password, cnpj, typeId, description, address{...}, telephones[]} → 201 {id, userType} */
        const val REGISTER_ESTABLISHMENT = "api/v1/auth/registrations/establishments"

        /** GET (público) → [{id, name, description}] para o cadastro de estabelecimento */
        const val ESTABLISHMENT_TYPES = "api/v1/establishment-types"

        /**
         * GET (Bearer) → {id, name, email, userType, status, createdAt, telephones[], cpf, cnpj,
         * qrToken, establishmentTypeId, description, address}. Ainda NÃO devolve `isCollector`.
         */
        const val USERS_ME = "api/v1/users/me"

        // ---------------- AINDA NÃO EXISTE na Core ----------------

        /** POST {userType, name, email, phone, birthDate}: só fazia sentido com Firebase; na Core o perfil nasce no cadastro. */
        const val USERS = "api/v1/users"

        /** GET → HomeDashboard (indicadores, tanque, perfil e notificações numa chamada só) */
        const val HOME_DASHBOARD = "api/v1/home"

        /** POST {litros} → HomeDashboard atualizado (só PEV) */
        const val OIL_REGISTRATIONS = "api/v1/oil-registrations"

        /** POST {estimatedVolumeLiters, scheduledAt, observation, idempotencyKey} → procedure create_collection_request (só PEV) */
        const val COLLECTION_REQUESTS = "api/v1/collection-requests"

        /** GET → EstablishmentHistory (B2B: indicadores + tabela de coletas) */
        const val HISTORY_ESTABLISHMENT = "api/v1/history/collections"

        /** GET → CitizenHistory (B2C: indicadores + registros de entrega no PEV) */
        const val HISTORY_CITIZEN = "api/v1/history/deliveries"

        /** GET ?minLat=&minLng=&maxLat=&maxLng= → pins da área visível; POST → "Vire Coletor" (cria pev PENDING) */
        const val COLLECTION_POINTS = "api/v1/collection-points"

        /** GET ?q= (mín. 3 letras) → [{id, name, type, address, latitude, longitude}] */
        const val COLLECTION_POINTS_AUTOCOMPLETE = "api/v1/collection-points/autocomplete"

        /** GET → ficha completa (telefone, CNPJ, horário, campos públicos) */
        const val COLLECTION_POINT_DETAILS = "api/v1/collection-points/{id}"

        /** GET → solicitações que chegam para o Coletor */
        const val COLLECTOR_REQUESTS = "api/v1/collector/requests"

        /** POST → aceitar ("Coletar") / recusar ("Descartar") uma solicitação ({id}) */
        const val COLLECTOR_REQUEST_COLLECT = "api/v1/collector/requests/{id}/collect"
        const val COLLECTOR_REQUEST_DISCARD = "api/v1/collector/requests/{id}/discard"

        /** GET → cards da aba "Dicas" (Home B2C) */
        const val TIPS = "api/v1/tips"
    }
}
