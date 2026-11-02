package com.olius.app.presentation.screen.PerfilType

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.olius.app.domain.entity.PhoneCountry
import com.olius.app.presentation.theme.OliusAmareloClaro
import com.olius.app.presentation.theme.OliusTextoPrimario
import com.olius.app.presentation.theme.OliusTextoSecundario
import com.olius.app.presentation.theme.OliusSuperficie
import java.text.Normalizer

/** Campo de telefone do cadastro: seletor de país ("🇧🇷 +55 ⌄") + número com a máscara do país. */
@Composable
internal fun OliusPhoneField(
    country: PhoneCountry,
    digits: String,
    formatted: String,
    error: String?,
    countries: () -> List<PhoneCountry>,
    onValueChange: (String) -> Unit,
    onSelectCountry: (PhoneCountry) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }

    TextField(
        value = digits,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Telefone", color = OliusTextoSecundario) },
        leadingIcon = {
            Text(
                text = "${country.flag} ${country.dialCodeLabel} ⌄",
                fontSize = 14.sp,
                color = OliusTextoPrimario,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showPicker = true }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        },
        isError = error != null,
        supportingText = error?.let { message -> { Text(message) } },
        singleLine = true,
        shape = RectangleShape,
        visualTransformation = PhoneMaskTransformation(formatted),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        colors = oliusFieldColors()
    )

    if (showPicker) {
        CountryPickerDialog(
            countries = countries(),
            selected = country,
            onSelect = {
                onSelectCountry(it)
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
}

/** Lista pesquisável de países: "🇧🇷 Brasil +55". */
@Composable
private fun CountryPickerDialog(
    countries: List<PhoneCountry>,
    selected: PhoneCountry,
    onSelect: (PhoneCountry) -> Unit,
    onDismiss: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(countries, query) { filterPhoneCountries(countries, query) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = OliusSuperficie) {
            Column(Modifier.padding(vertical = 20.dp)) {
                Text(
                    "Escolha o país",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = OliusTextoPrimario,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Buscar por nome ou DDI", color = OliusTextoSecundario) },
                    singleLine = true,
                    shape = RectangleShape,
                    colors = oliusFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(filtered, key = { it.isoCode }) { country ->
                        CountryRow(
                            country = country,
                            selected = country.isoCode == selected.isoCode,
                            onClick = { onSelect(country) }
                        )
                    }
                }
                if (filtered.isEmpty()) {
                    Text(
                        "Nenhum país encontrado",
                        color = OliusTextoSecundario,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CountryRow(country: PhoneCountry, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) OliusAmareloClaro.copy(alpha = 0.35f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(country.flag, fontSize = 20.sp)
        Text(country.name, color = OliusTextoPrimario, modifier = Modifier.weight(1f))
        Text(country.dialCodeLabel, color = OliusTextoSecundario)
    }
}

/** Busca do seletor: pelo nome (sem acento, sem diferença de maiúscula), pelo código ISO ("BR") ou pelo DDI ("55" ou "+55"). */
internal fun filterPhoneCountries(countries: List<PhoneCountry>, query: String): List<PhoneCountry> {
    val term = normalizeForSearch(query.trim())
    if (term.isEmpty()) return countries
    val dial = term.removePrefix("+")
    return countries.filter { country ->
        normalizeForSearch(country.name).contains(term) ||
            country.isoCode.equals(term, ignoreCase = true) ||
            (dial.isNotEmpty() && dial.all(Char::isDigit) && country.dialCode.toString().startsWith(dial))
    }
}

private fun normalizeForSearch(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase()

/** Máscara só visual: o campo guarda os dígitos e mostra [formatted] (o mesmo número com a formatação do país). */
internal data class PhoneMaskTransformation(private val formatted: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        if (formatted.filter(Char::isDigit) != digits) return TransformedText(text, OffsetMapping.Identity)

        val digitPositions = formatted.indices.filter { formatted[it].isDigit() }
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                if (offset <= 0) 0 else digitPositions[(offset - 1).coerceAtMost(digitPositions.lastIndex)] + 1

            override fun transformedToOriginal(offset: Int): Int = digitPositions.count { it < offset }
        }
        return TransformedText(AnnotatedString(formatted), mapping)
    }
}
