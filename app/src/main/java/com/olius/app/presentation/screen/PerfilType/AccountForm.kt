package com.olius.app.presentation.screen.PerfilType

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olius.app.R
import com.olius.app.domain.entity.PasswordRule
import com.olius.app.domain.entity.PhoneCountry
import com.olius.app.domain.entity.ProfileType
import com.olius.app.presentation.components.OliusSegmentedControl
import com.olius.app.presentation.screen.becomecollector.CepStatus
import com.olius.app.presentation.theme.OliusAmarelo
import com.olius.app.presentation.theme.OliusTextoPrimario
import com.olius.app.presentation.theme.OliusTextoSecundario
import com.olius.app.presentation.theme.OliusVerdeAberto

/**
 * Ações dos campos que a Core exige no cadastro (CPF/CNPJ, tipo, endereço) e
 * do "Completar cadastro" do Google. Agrupadas para não inflar ainda mais a
 * lista de parâmetros do PerfilTypeContent. Padrão vazio = preview.
 */
internal class AccountFormActions(
    val onSelectProfileType: (ProfileType) -> Unit = {},
    val onDocumentChange: (String) -> Unit = {},
    val onSelectEstablishmentType: (String) -> Unit = {},
    val onRetryEstablishmentTypes: () -> Unit = {},
    val onCepChange: (String) -> Unit = {},
    val onRetryCep: () -> Unit = {},
    val onStreetChange: (String) -> Unit = {},
    val onNumberChange: (String) -> Unit = {},
    val onNeighborhoodChange: (String) -> Unit = {},
    val onCityChange: (String) -> Unit = {},
    val onComplementChange: (String) -> Unit = {},
    val onCompleteGoogleProfile: () -> Unit = {}
)

/** CPF (cidadão) ou CNPJ + tipo + endereço (estabelecimento). */
@Composable
internal fun ColumnScope.AccountDocumentFields(uiState: PerfilTypeUiState, actions: AccountFormActions) {
    val isCitizen = uiState.registrationType == ProfileType.CITIZEN
    OliusTextField(
        value = uiState.registerDocument,
        onValueChange = actions.onDocumentChange,
        placeholder = if (isCitizen) "CPF" else "CNPJ",
        leadingIconRes = if (isCitizen) R.drawable.ic_field_user else R.drawable.ic_profile_establishment,
        keyboardType = KeyboardType.Number,
        error = uiState.fieldError(RegisterField.DOCUMENT),
        visualTransformation = PhoneMaskTransformation(uiState.registerDocumentFormatted)
    )
    if (!isCitizen) {
        Spacer(Modifier.height(AUTH_FIELD_SPACING))
        EstablishmentFields(uiState, actions)
    }
}

@Composable
private fun EstablishmentFields(uiState: PerfilTypeUiState, actions: AccountFormActions) {
    EstablishmentTypeField(uiState, actions)
    Spacer(Modifier.height(AUTH_FIELD_SPACING))
    OliusTextField(
        value = uiState.registerCep,
        onValueChange = actions.onCepChange,
        placeholder = "CEP",
        leadingIconRes = null,
        keyboardType = KeyboardType.Number,
        // Erro de validação vale mais que o status da consulta.
        error = uiState.fieldError(RegisterField.CEP) ?: uiState.cepMessage.takeIf { uiState.cepStatus != CepStatus.LOADING },
        visualTransformation = PhoneMaskTransformation(formatCep(uiState.registerCep))
    )
    if (uiState.cepStatus == CepStatus.LOADING) HelperText(uiState.cepMessage.orEmpty())
    if (uiState.cepStatus == CepStatus.NETWORK_ERROR) LinkText("Tentar de novo", actions.onRetryCep)
    Spacer(Modifier.height(AUTH_FIELD_SPACING))
    OliusTextField(
        value = uiState.registerStreet,
        onValueChange = actions.onStreetChange,
        placeholder = "Rua",
        leadingIconRes = null,
        error = uiState.fieldError(RegisterField.STREET)
    )
    Spacer(Modifier.height(AUTH_FIELD_SPACING))
    Row(Modifier.fillMaxWidth()) {
        OliusTextField(
            value = uiState.registerNumber,
            onValueChange = actions.onNumberChange,
            placeholder = "Número",
            leadingIconRes = null,
            error = uiState.fieldError(RegisterField.NUMBER),
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        OliusTextField(
            value = uiState.registerComplement,
            onValueChange = actions.onComplementChange,
            placeholder = "Complemento",
            leadingIconRes = null,
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(AUTH_FIELD_SPACING))
    OliusTextField(
        value = uiState.registerNeighborhood,
        onValueChange = actions.onNeighborhoodChange,
        placeholder = "Bairro",
        leadingIconRes = null,
        error = uiState.fieldError(RegisterField.NEIGHBORHOOD)
    )
    Spacer(Modifier.height(AUTH_FIELD_SPACING))
    OliusTextField(
        value = uiState.registerCity,
        onValueChange = actions.onCityChange,
        placeholder = if (uiState.registerState.isEmpty()) "Cidade" else "Cidade (${uiState.registerState})",
        leadingIconRes = null,
        error = uiState.fieldError(RegisterField.CITY)
    )
}

/** Tipo de estabelecimento: lista da Core (GET /establishment-types) num menu. */
@Composable
private fun EstablishmentTypeField(uiState: PerfilTypeUiState, actions: AccountFormActions) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val placeholder = when (uiState.establishmentTypesStatus) {
        LoadStatus.LOADING -> "Carregando tipos..."
        LoadStatus.ERROR -> "Não foi possível carregar os tipos"
        else -> "Tipo de estabelecimento"
    }
    Box(Modifier.fillMaxWidth()) {
        TextField(
            value = uiState.selectedEstablishmentType?.name.orEmpty(),
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = OliusTextoSecundario) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            isError = uiState.fieldError(RegisterField.ESTABLISHMENT_TYPE) != null,
            supportingText = uiState.fieldError(RegisterField.ESTABLISHMENT_TYPE)?.let { message -> { Text(message) } },
            singleLine = true,
            shape = RectangleShape,
            colors = oliusFieldColors()
        )
        // O TextField só leitura não repassa o toque: uma camada por cima abre o menu.
        Box(
            Modifier
                .matchParentSize()
                .clickable {
                    if (uiState.establishmentTypesStatus == LoadStatus.ERROR) actions.onRetryEstablishmentTypes()
                    else expanded = uiState.establishmentTypes.isNotEmpty()
                }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            uiState.establishmentTypes.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.name) },
                    onClick = {
                        actions.onSelectEstablishmentType(type.id)
                        expanded = false
                    }
                )
            }
        }
    }
    if (uiState.establishmentTypesStatus == LoadStatus.ERROR) LinkText("Tentar de novo", actions.onRetryEstablishmentTypes)
}

/**
 * As regras da senha da Core, cada uma marcada quando cumprida. Só aparece
 * depois que a pessoa começa a digitar ([visible] = senha não vazia).
 */
@Composable
internal fun PasswordChecklist(checklist: List<Pair<PasswordRule, Boolean>>, visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        PasswordRules(checklist)
    }
}

@Composable
private fun PasswordRules(checklist: List<Pair<PasswordRule, Boolean>>) {
    Column(Modifier.fillMaxWidth().padding(top = 6.dp, start = 4.dp)) {
        checklist.forEach { (rule, ok) ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 1.dp)) {
                Icon(
                    imageVector = if (ok) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = if (ok) "Cumprida" else "Pendente",
                    tint = if (ok) OliusVerdeAberto else OliusTextoSecundario,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(rule.message, fontSize = 12.sp, color = if (ok) OliusTextoPrimario else OliusTextoSecundario)
            }
        }
    }
}

/**
 * "Completar cadastro": quem entrou com o Google e ainda não tem conta na
 * Core. Pede tipo de conta, telefone (com DDI), CPF ou CNPJ/tipo/endereço.
 * Sem senha: a conta é do Google. O e-mail vem do Google e não muda.
 */
@Composable
internal fun CompleteProfileCard(
    uiState: PerfilTypeUiState,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    phoneCountries: () -> List<PhoneCountry>,
    onSelectPhoneCountry: (PhoneCountry) -> Unit,
    onCancel: () -> Unit,
    actions: AccountFormActions,
    modifier: Modifier = Modifier
) {
    AuthCardScaffold(modifier = modifier, scrollable = true) {
        Text("Complete seu cadastro", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = OliusTextoPrimario)
        Spacer(Modifier.height(4.dp))
        Text(
            "Entrando com ${uiState.googleAccount?.email.orEmpty()}",
            fontSize = 13.sp,
            color = OliusTextoSecundario
        )
        Spacer(Modifier.height(16.dp))
        OliusSegmentedControl(
            options = listOf(ProfileType.ESTABLISHMENT, ProfileType.CITIZEN),
            selected = uiState.registrationType,
            label = { if (it == ProfileType.CITIZEN) "Cidadão" else "Estabelecimento" },
            onSelect = actions.onSelectProfileType,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OliusTextField(
            value = uiState.registerName,
            onValueChange = onNameChange,
            placeholder = if (uiState.registrationType == ProfileType.CITIZEN) "Nome completo" else "Nome do estabelecimento",
            leadingIconRes = R.drawable.ic_field_user,
            error = uiState.fieldError(RegisterField.NAME)
        )
        Spacer(Modifier.height(AUTH_FIELD_SPACING))
        OliusPhoneField(
            country = uiState.registerPhoneCountry,
            digits = uiState.registerPhone,
            formatted = uiState.registerPhoneFormatted,
            error = uiState.registerPhoneError,
            countries = phoneCountries,
            onValueChange = onPhoneChange,
            onSelectCountry = onSelectPhoneCountry
        )
        Spacer(Modifier.height(AUTH_FIELD_SPACING))
        AccountDocumentFields(uiState = uiState, actions = actions)
        Spacer(Modifier.height(12.dp))
        AuthErrorMessage(message = uiState.authErrorMessage)
        Spacer(Modifier.height(8.dp))
        ContinuarButton(
            text = if (uiState.isAuthenticating) "Concluindo..." else "Concluir cadastro",
            onClick = actions.onCompleteGoogleProfile,
            enabled = !uiState.isAuthenticating
        )
        Spacer(Modifier.height(12.dp))
        LinkText("Voltar para o login", onCancel)
    }
}

@Composable
private fun HelperText(text: String) {
    Text(text, fontSize = 12.sp, color = OliusTextoSecundario, modifier = Modifier.fillMaxWidth().padding(start = 16.dp))
}

@Composable
private fun LinkText(text: String, onClick: () -> Unit) {
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = OliusAmarelo,
        modifier = Modifier.padding(vertical = 4.dp).clickable(onClick = onClick)
    )
}

/** 00000-000 enquanto a pessoa digita. */
internal fun formatCep(digits: String): String =
    if (digits.length <= 5) digits else digits.substring(0, 5) + "-" + digits.substring(5)
