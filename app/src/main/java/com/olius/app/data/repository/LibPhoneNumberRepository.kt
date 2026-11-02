package com.olius.app.data.repository

import com.olius.app.domain.entity.PhoneCountry
import com.olius.app.domain.repository.PhoneNumberRepository
import io.michaelrocks.libphonenumber.android.NumberParseException
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import java.text.Collator
import java.util.Locale

/**
 * [PhoneNumberRepository] sobre a libphonenumber (Google, Apache 2.0, offline,
 * sem chave). O [PhoneNumberUtil] é criado só no primeiro uso: no app ele lê os
 * metadados dos assets (precisa do Context); nos testes, de um MetadataLoader.
 */
class LibPhoneNumberRepository(
    utilProvider: () -> PhoneNumberUtil,
    private val displayLocale: Locale = Locale("pt", "BR")
) : PhoneNumberRepository {

    private val util by lazy(utilProvider)

    private val countries by lazy {
        val collator = Collator.getInstance(displayLocale)
        util.supportedRegions
            .map { iso ->
                PhoneCountry(
                    isoCode = iso,
                    name = Locale("", iso).getDisplayCountry(displayLocale).ifBlank { iso },
                    dialCode = util.getCountryCodeForRegion(iso)
                )
            }
            .sortedWith { a, b -> collator.compare(a.name, b.name) }
    }

    override fun countries(): List<PhoneCountry> = countries

    override fun formatAsYouType(isoCode: String, nationalDigits: String): String {
        val formatter = util.getAsYouTypeFormatter(isoCode)
        var formatted = ""
        nationalDigits.filter(Char::isDigit).forEach { formatted = formatter.inputDigit(it) }
        return formatted
    }

    override fun toE164Digits(isoCode: String, nationalDigits: String): String? {
        val digits = nationalDigits.filter(Char::isDigit)
        if (digits.isEmpty()) return null
        val number = try {
            util.parse(digits, isoCode)
        } catch (e: NumberParseException) {
            return null
        }
        if (!util.isValidNumberForRegion(number, isoCode)) return null
        val e164 = "${number.countryCode}${util.getNationalSignificantNumber(number)}"
        return e164.takeIf { it.length in CORE_PHONE_MIN_DIGITS..CORE_PHONE_MAX_DIGITS }
    }

    override fun fromE164Digits(e164Digits: String): Pair<PhoneCountry, String>? {
        val digits = e164Digits.filter(Char::isDigit)
        if (digits.isEmpty()) return null
        val number = try {
            util.parse("+$digits", null)
        } catch (e: NumberParseException) {
            return null
        }
        val iso = util.getRegionCodeForNumber(number) ?: util.getRegionCodeForCountryCode(number.countryCode)
        val country = countries.firstOrNull { it.isoCode == iso } ?: return null
        return country to util.getNationalSignificantNumber(number)
    }


    companion object {
        // Core: telephones = List<@Size(min=10,max=15) String> (IdentityRequests.java).
        const val CORE_PHONE_MIN_DIGITS = 10
        const val CORE_PHONE_MAX_DIGITS = 15
    }
}
