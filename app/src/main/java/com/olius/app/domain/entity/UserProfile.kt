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
 * Perfil da Core (GET /users/me: `users` + `establishment`/`citizens`),
 * guardado no cache do aparelho (Room). [id] é o `users.id` (UUID).
 */
data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val profileType: ProfileType,
    /** Primeiro telefone do cadastro (E.164 só com dígitos), se houver. */
    val phone: String? = null,
    /**
     * Estabelecimento que também é ponto de coleta (`establishment.is_pev`,
     * PEV aprovado). Vê as telas da seção "B2B-Coletor" do Figma.
     */
    val isCollector: Boolean = false,
    /** `user_qr_code.qr_token` — conteúdo do QR de identificação. */
    val qrToken: String? = null
)

/** Ainda não há perfil carregado (nem no cache). */
class ProfileNotFoundException : Exception("Perfil ainda não carregado")
