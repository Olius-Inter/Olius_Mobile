package com.olius.app.domain.repository

import com.olius.app.domain.entity.PhoneCountry

/** DDI, máscara e validação de telefone por país (hoje a libphonenumber, offline). */
interface PhoneNumberRepository {
    /** Todos os países com DDI, em ordem alfabética do nome em português. */
    fun countries(): List<PhoneCountry>

    /** Número com a máscara do país, do jeito que aparece enquanto a pessoa digita. */
    fun formatAsYouType(isoCode: String, nationalDigits: String): String

    /** Número em E.164 só com dígitos (ex.: "5511999998888") quando ele é válido para o país e cabe na Core (10 a 15 dígitos); senão null. */
    fun toE164Digits(isoCode: String, nationalDigits: String): String?

    /** Caminho inverso (telefone salvo na Core → campo com DDI). */
    fun fromE164Digits(e164Digits: String): Pair<PhoneCountry, String>?
}

