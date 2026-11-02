package com.olius.app.data.remote

import com.olius.app.BuildConfig

/**
 * ÚNICO lugar com URLs e endpoints do app. Para apontar para outro servidor,
 * troque só a base URL; para mudar uma rota, troque só a constante dela.
 * Ver API_00_CONEXAO_API.md e MAPA_PENDENCIAS.md.
 *
 * As duas base URLs vêm do `local.properties` (não versionado), via BuildConfig:
 * `OLIUS_CORE_BASE_URL` e `OLIUS_RANKING_BASE_URL`. Sem a chave, o padrão é o PC
 * visto do emulador (`10.0.2.2`). Trocou a URL? Rebuild do app. Ver MOBILE_EXPLICACAO_CONFIG_URLS.md.
 *
 * - [RANKING_BASE_URL]: API de Ranking e Conquistas (Java + Spring MVC + Redis,
 *   repositório `API_Redis_Olius`), porta padrão 8081.
 * - [MAIN_BASE_URL]: Core API (Java + Spring + Postgres, repositório
 *   `API_Olius/olius-api`), porta padrão 8080. Prefixo de todas as rotas: `/api/v1`.
 *   O chat da OlivIA também passa por ela: o app não chama a IA direto.
 *   Enquanto [USE_MOCK_MAIN_BACKEND] for true, as telas usam dados mockados.
 */
object ApiConfig {

    val RANKING_BASE_URL = withTrailingSlash(BuildConfig.OLIUS_RANKING_BASE_URL)
    val MAIN_BASE_URL = withTrailingSlash(BuildConfig.OLIUS_CORE_BASE_URL)

    /** As rotas não começam com "/", então a base precisa terminar com ela. */
    fun withTrailingSlash(url: String): String = if (url.endsWith("/")) url else "$url/"

    const val USE_MOCK_RANKING = false

    /**
     * Só os DADOS das telas (home, histórico, pontos de coleta...), que a Core
     * ainda não tem. Login, cadastro, sessão e /users/me vão SEMPRE para a
     * Core, com esta flag true ou false. Só passe para false depois do item 1
     * do MAPA_PENDENCIAS.md.
     */
    const val USE_MOCK_MAIN_BACKEND = true

    /**
     * Chat da OlivIA. false = POST [Main.CHAT] na Core (que repassa para a IA).
     * Enquanto a Core não tiver a rota, a resposta é 404 e a conversa mostra
     * "O assistente ainda não está disponível no servidor". true = respostas prontas.
     */
    const val USE_MOCK_OLIVIA = false

    /** "Fale conosco" do Perfil abre o app de e-mail para este endereço (e-mail oficial do Olius). */
    const val SUPPORT_EMAIL = "oliusjef26@gmail.com"

    /**
     * Botões de login com Google, Facebook e Microsoft. false = só e-mail e senha
     * da Core (decisão de 01/11/2026: a Core e o banco não terão as rotas a tempo,
     * e o Firebase não evita isso, porque a Core continua sem aceitar o token dele).
     * O fluxo do Google fica pronto para quando a Core tiver `auth/mobile/google`.
     */
    const val SOCIAL_LOGIN_ENABLED = false

    /** Só para testar as telas do Coletor/PEV com o backend em mock (o perfil mock nasce PEV). */
    const val DEBUG_MOCK_COLLECTOR = false

    const val CONNECT_TIMEOUT_MS = 10_000
    const val READ_TIMEOUT_MS = 15_000

    /** A IA (LLM + agentes) pode levar bem mais que as outras rotas. Igual ao timeout da Core com a IA. */
    const val CHAT_READ_TIMEOUT_MS = 60_000

    /** Quantos participantes o ranking pede (a API aceita 1..100). */
    const val RANKING_LIMIT = 10

    /**
     * API Redis (`API_Redis_Olius`): só o ranking anual por pontos.
     * Conquistas não vêm dela: B2B usa [Main.CERTIFICATE_PROGRESS] na Core; B2C ainda não tem rota.
     */
    object Ranking {
        /**
         * GET ?category=establishment|citizen&limit= → {year, updatedAt, myPosition, items}.
         * `myPosition` só vem com `Authorization: Bearer <access token da Core>`.
         */
        const val RANKING = "api/ranking"
    }

    /** ViaCEP (grátis, sem chave, https): preenche UF, cidade, bairro e rua no "Vire Coletor". */
    const val VIACEP_BASE_URL = "https://viacep.com.br/"

    object ViaCep {
        /** GET → {cep, logradouro, bairro, localidade, uf}; CEP inexistente → {"erro": true} */
        const val LOOKUP = "ws/{cep}/json/"
    }

    /**
     * Core API (`API_Olius/olius-api`). As marcadas "JÁ EXISTE" estão nos
     * controllers da Core (origin/main 93f60cf); as outras são o contrato que o
     * app espera e ainda precisam ser criadas. Inventário: MOBILE_EXPLICACAO_ROTAS.md.
     */
    object Main {
        // ---------------- JÁ EXISTE na Core ----------------

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

        /** GET (público) → [{id, name, description}] para o cadastro de estabelecimento */
        const val ESTABLISHMENT_TYPES = "api/v1/establishment-types"

        /**
         * GET (Bearer) → {id, name, email, userType, status, createdAt, telephones[], cpf, cnpj,
         * qrToken, establishmentTypeId, description, address}. Ainda NÃO devolve `isCollector`.
         */
        const val USERS_ME = "api/v1/users/me"

        /**
         * GET (Bearer de cidadão ou estabelecimento) → {id, status, createdAt, approvedAt, address}.
         * status: PENDING | APPROVED | REJECTED | INACTIVE; sem PEV → 404. "Coletor" = APPROVED.
         */
        const val PEVS_ME = "api/v1/pevs/me"

        /**
         * PATCH (Bearer) {address: {...}} (endereço completo, igual ao POST) → OwnPev. Só PENDING ou
         * REJECTED; outro estado → 409 BUSINESS_STATE_CONFLICT. Não muda o status.
         */
        const val PEVS_ME_ADDRESS = "api/v1/pevs/me/address"

        /** POST (Bearer, sem corpo) → OwnPev de volta em PENDING. Só REJECTED; outro estado → 409. */
        const val PEVS_ME_RESUBMISSIONS = "api/v1/pevs/me/resubmissions"

        /**
         * GET (Bearer de cidadão) ?page=&size= → {balance, movements: [{deliveryId, points, deliveryDate,
         * revision}], page, size, totalElements}. Histórico B2C. Ainda SEM os litros: pedido
         * `oilVolumeLiters` e `recordStatus` em cada movement (PEDIDOS_CORE_API.md 7.1).
         */
        const val USERS_ME_POINTS = "api/v1/users/me/points"

        /**
         * POST (Bearer de ESTABELECIMENTO, header `Idempotency-Key: <UUID>`) {estimatedVolumeLiters, observation?}
         * → 201 RequestView. Procedure create_collection_request; exige ciclo de assinatura vigente.
         * Todo estabelecimento pode pedir (não depende de ser PEV).
         */
        const val COLLECTION_REQUESTS = "api/v1/collection-requests"
        // GET em COLLECTION_REQUESTS (mesmo papel): ?status=PENDING|APPROVED|REJECTED|CANCELLED&page=&size= (size máx. 100)
        // → {items: [RequestView {id, status, estimatedVolumeLiters, requestAt, collectionId?...}], page, size, totalElements}.
        // Histórico B2B "Pendente" = PENDING/APPROVED com collectionId null.

        /**
         * POST (Bearer de cidadão ou estabelecimento) {address: {state, city, neighborhood, street,
         * number, cep, complement?, latitude?, longitude?}} → 201 OwnPev (nasce PENDING). Já tem PEV → 409 PEV_ALREADY_EXISTS.
         * "Vire Coletor". SEM horário de funcionamento: a Core recusa campo desconhecido (400).
         * GET (público) ?page=&size= → {items: [{id, address{..., latitude, longitude}}], page, size,
         * totalElements}: só APPROVED, de estabelecimento e de cidadão. É a fonte do mapa, da busca e dos
         * "Pontos PEV" (sem área nem busca na Core: o app filtra). Sem nome nem horário.
         */
        const val PEVS = "api/v1/pevs"

        /**
         * GET (Bearer de ESTABELECIMENTO) ?recordStatus=RECORDED|ANNULLED&result=&from=&until=&page=&size= (size máx. 100)
         * → {items: [CollectionView {id, requestId, collectedVolumeLiters, collectionDate, recordStatus, result...}],
         * page, size, totalElements}. Histórico B2B "Coletado" = recordStatus RECORDED.
         */
        const val COLLECTIONS = "api/v1/collections"

        /** GET (Bearer de ESTABELECIMENTO) → {points, successfulCollectionsCount, collectedVolumeLiters}. Topo do Histórico B2B. */
        const val ESTABLISHMENT_SCORE = "api/v1/establishments/me/score"

        /**
         * GET (Bearer de ESTABELECIMENTO) → {establishmentId, eligibleVolumeLiters, levels: [{levelId, name,
         * description, requiredLiters, reached, remainingLiters, certificateId, certificateStatus}]}.
         * Nível (Home) e aba Conquistas do estabelecimento. Litros acumulados, sem corte anual.
         * Cidadão: a Core não tem rota de nível/conquista (403 aqui).
         */
        const val CERTIFICATE_PROGRESS = "api/v1/establishments/me/certificate-progress"

        /*
         * Existem na Core e o app ainda NÃO usa (sem tela): GET establishments/me/environmental-summary,
         * GET certificate-levels, GET certificates[/{id}], POST qr/resolutions {qrToken} → {id, name},
         * POST/GET pevs/{pevId}/deliveries[/{deliveryId}] (o PEV lê o QR do cidadão e registra os litros),
         * GET billing/cycles/current/availability. Ver MOBILE_EXPLICACAO_ROTAS.md.
         */

        // ---------------- AINDA NÃO EXISTE na Core ----------------

        /*
         * "Editar Perfil": PATCH em USERS_ME {name, email, telephone (E.164 só com dígitos, ou null)}
         * → o mesmo Profile do GET. Hoje a Core só tem o GET, então responde 405.
         */

        /**
         * POST {idToken} → mesmos campos do login (MobileTokens). Conta que não
         * existe → 404 com `code` = PROFILE_REQUIRED (o app abre "Completar cadastro").
         */
        const val AUTH_MOBILE_GOOGLE = "api/v1/auth/mobile/google"

        /** "Completar cadastro" do Google: POST {idToken, name, email, cpf, telephones[]} (sem password) → MobileTokens */
        const val REGISTER_CITIZEN_GOOGLE = "api/v1/auth/registrations/citizens/google"

        /** POST {idToken, name, email, cnpj, typeId, description, address{...}, telephones[]} (sem password) → MobileTokens */
        const val REGISTER_ESTABLISHMENT_GOOGLE = "api/v1/auth/registrations/establishments/google"

        /**
         * Esqueci a senha (público, sem Bearer). Contrato em PEDIDO_ESQUECI_SENHA_CODIGO.md.
         * POST {email} → SEMPRE 202, exista a conta ou não (não revela contas).
         */
        const val PASSWORD_RESETS = "api/v1/auth/password-resets"

        /** POST {email, code (6 dígitos)} → 200 {resetToken, expiresAt}; código errado ou vencido → 400 */
        const val PASSWORD_RESETS_VERIFY = "api/v1/auth/password-resets/verify"

        /** POST {resetToken, newPassword} → 204; senha fora da regra → 400 */
        const val PASSWORD_RESETS_CONFIRM = "api/v1/auth/password-resets/confirm"

        /** GET → HomeDashboard (indicadores, tanque, perfil e notificações numa chamada só) */
        const val HOME_DASHBOARD = "api/v1/home"

        /** POST {litros} → HomeDashboard atualizado (estabelecimento; no B2C, só cidadão PEV) */
        const val OIL_REGISTRATIONS = "api/v1/oil-registrations"

        // Histórico B2B: sem rota própria. Usa COLLECTIONS + COLLECTION_REQUESTS + ESTABLISHMENT_SCORE (acima).
        // Água, CO₂ e biodiesel do topo ainda não vêm da Core (fórmulas iriam no GET HOME_DASHBOARD).

        // Histórico B2C: usa USERS_ME_POINTS (acima). Mapa, busca e "Pontos PEV": usam PEVS (acima).

        /**
         * GET → [{id, establishmentName, dateLabel, liters}]: solicitações que chegam para o Coletor (sem tabela no banco).
         * A Core tem GET pevs/{pevId}/deliveries (entregas recebidas pelo PEV), que não tem "Coletar/Descartar".
         */
        const val COLLECTOR_REQUESTS = "api/v1/collector/requests"

        /** POST → aceitar ("Coletar") / recusar ("Descartar") uma solicitação ({id}) */
        const val COLLECTOR_REQUEST_COLLECT = "api/v1/collector/requests/{id}/collect"
        const val COLLECTOR_REQUEST_DISCARD = "api/v1/collector/requests/{id}/discard"

        /**
         * Chat da OlivIA (Bearer). POST {message, conversationId?} → {reply, conversationId}.
         * Sem conversationId, a Core cria um e devolve; o app reenvia o mesmo nas próximas.
         * A Core repassa para a IA com o userId do token. IA fora → 503.
         */
        const val CHAT = "api/v1/chat"

        /** GET → [{id, title, description}]: cards da aba "Dicas" (Home B2C) */
        const val TIPS = "api/v1/tips"
    }
}
