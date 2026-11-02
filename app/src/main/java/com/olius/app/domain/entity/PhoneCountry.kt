package com.olius.app.domain.entity

/**
 * País do seletor de telefone do cadastro: código ISO (ex.: "BR"), nome em
 * português e DDI (ex.: 55). O DDI é o código do país; o DDD (11, 21...) faz
 * parte do número nacional.
 */
data class PhoneCountry(
    val isoCode: String,
    val name: String,
    val dialCode: Int
) {
    /** Bandeira em emoji, montada das 2 letras do ISO (sem imagens). */
    val flag: String
        get() = isoCode.uppercase()
            .filter { it in 'A'..'Z' }
            .map { String(Character.toChars(REGIONAL_INDICATOR_A + (it - 'A'))) }
            .joinToString("")

    /** Ex.: "+55". */
    val dialCodeLabel: String get() = "+$dialCode"

    companion object {
        private const val REGIONAL_INDICATOR_A = 0x1F1E6

        /** Padrão do cadastro. Fixo aqui para não depender da lib ao abrir a tela. */
        val BRAZIL = PhoneCountry(isoCode = "BR", name = "Brasil", dialCode = 55)
    }
}
