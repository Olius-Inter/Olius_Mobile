package com.olius.app.presentation.screen.becomecollector

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.olius.app.presentation.components.OliusBackButton
import com.olius.app.presentation.components.BOTTOM_NAV_CONTENT_PADDING
import com.olius.app.presentation.components.OliusPageTitle
import com.olius.app.presentation.components.OliusTabScaffold
import com.olius.app.presentation.components.OliusTopBarState
import com.olius.app.presentation.theme.OliusAmarelo
import com.olius.app.presentation.theme.OliusBotaoInativoFundo
import com.olius.app.presentation.theme.OliusCampoBorda
import com.olius.app.presentation.theme.OliusTextoPrimario
import com.olius.app.presentation.theme.OliusTextoPrimarioClaro
import com.olius.app.presentation.theme.OliusTextoSecundario
import com.olius.app.presentation.theme.OliusVerdeSeloTexto
import com.olius.app.presentation.theme.OliusVermelhoFechado
import com.olius.app.presentation.theme.OliusSuperficie

/** "Virando um Coletor". */
@Composable
fun BecomeCollectorScreen(
    topBarState: OliusTopBarState,
    onDone: () -> Unit,
    viewModel: BecomeCollectorViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    OliusTabScaffold(topBarState = topBarState) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(12.dp))
            OliusBackButton(onClick = onDone)
            Spacer(Modifier.height(8.dp))
            OliusPageTitle(
                title = "Virando um Coletor",
                subtitle = "Se transforme em um coletor para que mais pessoas vejam e conheçam seu trabalho!"
            )
            Spacer(Modifier.height(20.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = OliusSuperficie,
                border = BorderStroke(1.dp, OliusCampoBorda)
            ) {
                Column(Modifier.padding(16.dp)) {
                    if (state.isSent) {
                        Text("Solicitação enviada!", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OliusVerdeSeloTexto)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Seu ponto passa por aprovação da equipe Olius e aparece no mapa assim que for aprovado.",
                            fontSize = 13.sp,
                            color = OliusTextoSecundario
                        )
                    } else {
                        CollectorField.entries.forEach { field ->
                            OutlinedTextField(
                                value = state.value(field),
                                onValueChange = { viewModel.onFieldChange(field, it) },
                                label = { Text(field.label, fontSize = 13.sp) },
                                singleLine = true,
                                isError = field == CollectorField.CEP &&
                                    (state.cepStatus == CepStatus.NOT_FOUND || state.cepStatus == CepStatus.NETWORK_ERROR),
                                supportingText = state.cepMessage?.takeIf { field == CollectorField.CEP }?.let { message ->
                                    { CepMessage(message, state.cepStatus, onRetry = viewModel::onRetryCep) }
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = if (field == CollectorField.CEP || field == CollectorField.NUMBER) KeyboardType.Number
                                    else KeyboardType.Text
                                ),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = OliusTextoPrimario,
                                    unfocusedTextColor = OliusTextoPrimario,
                                    focusedLabelColor = OliusTextoPrimario,
                                    unfocusedLabelColor = OliusTextoPrimario,
                                    cursorColor = OliusTextoPrimario,
                                    focusedBorderColor = OliusAmarelo,
                                    unfocusedBorderColor = OliusCampoBorda,
                                    focusedContainerColor = OliusSuperficie,
                                    unfocusedContainerColor = OliusSuperficie,
                                    errorTextColor = OliusTextoPrimario,
                                    errorLabelColor = OliusVermelhoFechado,
                                    errorCursorColor = OliusTextoPrimario,
                                    errorBorderColor = OliusVermelhoFechado,
                                    errorContainerColor = OliusSuperficie
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            )
                        }
                        state.errorMessage?.let {
                            Text(it, fontSize = 12.sp, color = OliusVermelhoFechado, modifier = Modifier.padding(bottom = 8.dp))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = if (state.isSent) onDone else viewModel::onSend,
                        enabled = state.isSent || state.canSend,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OliusAmarelo,
                            contentColor = OliusTextoPrimarioClaro,
                            disabledContainerColor = OliusBotaoInativoFundo,
                            disabledContentColor = OliusTextoPrimario
                        )
                    ) {
                        Text(
                            when {
                                state.isSent -> "Voltar ao mapa"
                                state.isSending -> "Enviando..."
                                else -> "Quero ser coletor"
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Spacer(Modifier.height(BOTTOM_NAV_CONTENT_PADDING))
        }
    }
}

/** Aviso embaixo do CEP. */
@Composable
private fun CepMessage(message: String, status: CepStatus, onRetry: () -> Unit) {
    val isError = status == CepStatus.NOT_FOUND || status == CepStatus.NETWORK_ERROR
    Column {
        Text(message, fontSize = 12.sp, color = if (isError) OliusVermelhoFechado else OliusTextoSecundario)
        if (status == CepStatus.NETWORK_ERROR) {
            Text(
                "Tentar de novo",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = OliusTextoPrimario,
                modifier = Modifier
                    .clickable(onClick = onRetry)
                    .padding(vertical = 4.dp)
            )
        }
    }
}
