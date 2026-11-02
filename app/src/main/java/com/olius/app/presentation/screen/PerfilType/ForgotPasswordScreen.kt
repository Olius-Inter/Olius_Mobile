package com.olius.app.presentation.screen.PerfilType

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.olius.app.R
import com.olius.app.presentation.theme.DecorativeBackground
import com.olius.app.presentation.theme.OliusAmarelo
import com.olius.app.presentation.theme.OliusCampoBorda
import com.olius.app.presentation.theme.OliusTextoPrimario
import com.olius.app.presentation.theme.OliusTextoSecundario

/**
 * Recuperar senha (ref_recuperar_senha.png / ref_recuperar_senha_nova.png):
 * e-mail -> código de 6 dígitos -> nova senha + confirmar senha. O card tem
 * a mesma altura dos cards de login/cadastro ([AUTH_SECONDARY_CARD_HEIGHT]).
 */
@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    onPasswordReset: () -> Unit,
    viewModel: ForgotPasswordViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ForgotPasswordEvent.PasswordReset -> {
                    Toast.makeText(context, "Senha alterada! Entre com a nova senha.", Toast.LENGTH_LONG).show()
                    onPasswordReset()
                }
            }
        }
    }

    val handleBack = { if (!viewModel.onBack()) onBack() }
    BackHandler(onBack = handleBack)

    Box(Modifier.fillMaxSize()) {
        DecorativeBackground(isRow = true)
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 16.dp, top = 16.dp)
                    .clickable(onClick = handleBack)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = OliusTextoPrimario, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Voltar", fontSize = 14.sp, color = OliusTextoPrimario)
            }
            Spacer(Modifier.weight(1f))
            OliusLogoRow(Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(HEADER_TO_CARD_GAP))
            AnimatedContent(
                targetState = state.step,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    (slideInHorizontally(tween(350)) { if (forward) it / 3 else -it / 3 } + fadeIn(tween(350))) togetherWith
                        fadeOut(tween(150))
                },
                label = "forgotPasswordStep"
            ) { step ->
                // A nova senha tem a lista de regras: só esse passo rola, se faltar altura.
                AuthCardScaffold(
                    modifier = Modifier.height(AUTH_SECONDARY_CARD_HEIGHT),
                    scrollable = step == ForgotPasswordStep.NEW_PASSWORD
                ) {
                    Text("Recuperar senha", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = OliusTextoPrimario)
                    Spacer(Modifier.height(8.dp))
                    when (step) {
                        ForgotPasswordStep.EMAIL -> EmailStep(state, viewModel::onEmailChange)
                        ForgotPasswordStep.CODE -> CodeStep(state, viewModel::onCodeChange, viewModel::onResendCode)
                        ForgotPasswordStep.NEW_PASSWORD -> NewPasswordStep(
                            state = state,
                            onNewPasswordChange = viewModel::onNewPasswordChange,
                            onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
                            onToggleNewPassword = viewModel::onToggleNewPasswordVisibility,
                            onToggleConfirmPassword = viewModel::onToggleConfirmPasswordVisibility
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    AuthErrorMessage(message = state.errorMessage)
                    Spacer(Modifier.height(40.dp))
                    ContinuarButton(
                        text = if (state.isLoading) "Aguarde..." else "Continuar",
                        onClick = viewModel::onContinue,
                        enabled = !state.isLoading
                    )
                }
            }
        }
    }
}

/** Logo + "Olius" em linha — o mesmo header dos cards de login/cadastro, sem a animação. */
@Composable
private fun OliusLogoRow(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        Image(
            painter = painterResource(R.drawable.logo_sem_fundo),
            contentDescription = "Olius",
            modifier = Modifier.size(HEADER_ROW_ICON_SIZE)
        )
        Spacer(Modifier.width(6.dp))
        Text("Olius", fontSize = HEADER_ROW_TEXT_SIZE, fontWeight = FontWeight.Bold, color = OliusAmarelo)
    }
}

@Composable
private fun StepSubtitle(text: String) {
    Text(
        text,
        fontSize = 12.sp,
        color = OliusTextoSecundario,
        lineHeight = 16.sp,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ColumnScope.EmailStep(state: ForgotPasswordUiState, onEmailChange: (String) -> Unit) {
    StepSubtitle("Digite o e-mail da sua conta. Vamos enviar um código de 6 dígitos para você trocar a senha.")
    Spacer(Modifier.height(20.dp))
    OliusTextField(
        value = state.email,
        onValueChange = onEmailChange,
        placeholder = "Email",
        leadingIconRes = R.drawable.ic_field_mail,
        keyboardType = KeyboardType.Email
    )
}

@Composable
private fun ColumnScope.CodeStep(state: ForgotPasswordUiState, onCodeChange: (String) -> Unit, onResend: () -> Unit) {
    // Mesmo texto com e sem conta: a tela não revela se o e-mail está cadastrado.
    StepSubtitle("Se houver uma conta com ${state.email}, enviamos um código.\nDigite o código de 6 dígitos abaixo para trocar a senha")
    Spacer(Modifier.height(48.dp))
    CodeInput(code = state.code, onCodeChange = onCodeChange)
    Spacer(Modifier.height(24.dp))
    ResendCodeLink(secondsLeft = state.resendSecondsLeft, enabled = !state.isLoading, onResend = onResend)
    state.infoMessage?.let {
        Spacer(Modifier.height(6.dp))
        Text(it, fontSize = 12.sp, color = OliusTextoSecundario, textAlign = TextAlign.Center)
    }
}

/** "Reenviar código": cinza com a contagem durante a espera, amarelo quando libera. */
@Composable
private fun ResendCodeLink(secondsLeft: Int, enabled: Boolean, onResend: () -> Unit) {
    if (secondsLeft > 0) {
        Text("Reenviar código em 0:%02d".format(secondsLeft), fontSize = 13.sp, color = OliusTextoSecundario)
    } else {
        Text(
            "Reenviar código",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = OliusAmarelo,
            modifier = Modifier
                .clickable(enabled = enabled, onClick = onResend)
                .padding(4.dp)
        )
    }
}

/**
 * Campo do código: um `BasicTextField` invisível que desenha 6 traços
 * (ref_recuperar_senha.png) com os dígitos já digitados em cima de cada um.
 */
@Composable
private fun ColumnScope.CodeInput(code: String, onCodeChange: (String) -> Unit) {
    BasicTextField(
        value = code,
        onValueChange = onCodeChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        textStyle = TextStyle(color = Color.Transparent),
        cursorBrush = SolidColor(Color.Transparent),
        modifier = Modifier.align(Alignment.CenterHorizontally),
        decorationBox = { innerField ->
            Box {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(RESET_CODE_LENGTH) { index ->
                        val digit = code.getOrNull(index)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                digit?.toString() ?: "",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = OliusTextoPrimario,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(32.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Box(
                                Modifier
                                    .width(32.dp)
                                    .height(2.dp)
                                    .background(if (index == code.length) OliusAmarelo else OliusCampoBorda)
                            )
                        }
                    }
                }
                // O campo de verdade fica por baixo, transparente — só recebe o toque/teclado.
                Box(Modifier.matchParentSize()) { innerField() }
            }
        }
    )
}

@Composable
private fun ColumnScope.NewPasswordStep(
    state: ForgotPasswordUiState,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onToggleNewPassword: () -> Unit,
    onToggleConfirmPassword: () -> Unit
) {
    StepSubtitle("Digite a nova senha do seu e-mail: ${state.email}")
    Spacer(Modifier.height(20.dp))
    OliusPasswordField(
        value = state.newPassword,
        onValueChange = onNewPasswordChange,
        visible = state.newPasswordVisible,
        onToggleVisible = onToggleNewPassword,
        placeholder = "Nova senha"
    )
    PasswordChecklist(state.passwordChecklist, visible = state.newPassword.isNotEmpty())
    Spacer(Modifier.height(AUTH_FIELD_SPACING))
    OliusPasswordField(
        value = state.confirmPassword,
        onValueChange = onConfirmPasswordChange,
        visible = state.confirmPasswordVisible,
        onToggleVisible = onToggleConfirmPassword,
        placeholder = "Confirmar senha"
    )
}
