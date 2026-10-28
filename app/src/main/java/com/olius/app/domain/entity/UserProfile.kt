package com.olius.app.domain.entity

/**
 * Tipo de conta escolhido no onboarding ("Tipo de conta" no Figma). Movido de
 * `presentation/screen/PerfilType` para o domínio: Home, Ranking e o backend
 * dependem dele, não só a tela de onboarding.
 *
 * [apiValue] = enum `user_type_t` do Postgres; [rankingCategory] = toggle do ranking.
 */
enum class ProfileType(val apiValue: String, val rankingCategory: RankingCategory) {
    ESTABLISHMENT("ESTABLISHMENT", RankingCategory.ESTABLISHMENT),
    CITIZEN("CITIZENS", RankingCategory.CITIZEN);

    companion object {
        fun fromApi(value: String): ProfileType = entries.first { it.apiValue == value }
    }
}

/**
 * Perfil guardado no Postgres (`users` + `establishment`/`citizens`).
 * [id] é o `users.id` (UUID) — NUNCA o uid do Firebase, que só serve para login.
 */
data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val profileType: ProfileType,
    /** true = perfil provisório criado offline; ainda falta enviar ao backend. */
    val pendingSync: Boolean = false,
    /** Só guardados enquanto [pendingSync], para o POST de criação feito depois. */
    val phone: String? = null,
    val birthDate: String? = null,
    /**
     * Estabelecimento que também é ponto de coleta (`establishment.is_pev`,
     * PEV aprovado). Vê as telas da seção "B2B-Coletor" do Figma.
     */
    val isCollector: Boolean = false,
    /** `user_qr_code.qr_token` — conteúdo do QR de identificação. */
    val qrToken: String? = null
)

/** Dados do formulário de cadastro enviados no POST de criação do perfil. */
data class ProfileRegistration(
    val profileType: ProfileType,
    val name: String,
    val email: String,
    val phone: String?,
    val birthDate: String?
)

/** HTTP 404 em GET /users/me: autenticado no Firebase, mas sem registro no Postgres ainda. */
class ProfileNotFoundException : Exception("Perfil ainda não cadastrado na API")
