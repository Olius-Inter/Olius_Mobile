package com.olius.app.presentation.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.olius.app.presentation.components.OliusBackButton
import com.olius.app.presentation.components.BOTTOM_NAV_CONTENT_PADDING
import com.olius.app.presentation.screen.PerfilType.AuthErrorMessage
import com.olius.app.presentation.screen.PerfilType.OliusPhoneField
import com.olius.app.presentation.theme.OliusAmarelo
import com.olius.app.presentation.theme.OliusFundoTela
import com.olius.app.presentation.theme.OliusMarromAviso
import com.olius.app.presentation.theme.OliusTextoPrimario
import com.olius.app.presentation.theme.OliusTextoSecundario
import com.olius.app.presentation.theme.OliusTituloMarrom

/** "Editar Perfil": só nome, e-mail e telefone com DDI. */
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onConnectionError: () -> Unit = {},
    viewModel: EditProfileViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.saved) { if (state.saved) onSaved() }
    LaunchedEffect(state.isOffline) { if (state.isOffline) onConnectionError() }

    Box(
        Modifier
            .fillMaxSize()
            .background(OliusFundoTela)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = BOTTOM_NAV_CONTENT_PADDING)
        ) {
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth()) {
                OliusBackButton(onClick = onBack)
                Text(
                    "Editar Perfil",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = OliusTituloMarrom,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 28.dp)
                )
            }
            Spacer(Modifier.height(28.dp))

            ProfileField("Nome Completo", state.name, viewModel::onNameChange, state.nameError)
            Spacer(Modifier.height(14.dp))
            ProfileField("E-mail", state.email, viewModel::onEmailChange, state.emailError, KeyboardType.Email)
            Spacer(Modifier.height(14.dp))
            Text("Número de Telefone", fontSize = 12.sp, color = OliusTextoSecundario)
            OliusPhoneField(
                country = state.phoneCountry,
                digits = state.phone,
                formatted = state.phoneFormatted,
                error = state.phoneError,
                countries = { viewModel.phoneCountries },
                onValueChange = viewModel::onPhoneChange,
                onSelectCountry = viewModel::onPhoneCountrySelected
            )
            Spacer(Modifier.height(20.dp))

            AuthErrorMessage(state.errorMessage)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = viewModel::onSaveClick,
                enabled = !state.isSaving,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OliusAmarelo,
                    contentColor = OliusMarromAviso,
                    disabledContainerColor = OliusAmarelo.copy(alpha = 0.6f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(color = OliusMarromAviso, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Text("ALTERAR", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = error?.let { message -> { Text(message) } },
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = OliusAmarelo,
            unfocusedBorderColor = OliusAmarelo.copy(alpha = 0.7f),
            focusedLabelColor = OliusTextoSecundario,
            unfocusedLabelColor = OliusTextoSecundario,
            focusedTextColor = OliusTextoPrimario,
            unfocusedTextColor = OliusTextoPrimario,
            cursorColor = OliusAmarelo
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
