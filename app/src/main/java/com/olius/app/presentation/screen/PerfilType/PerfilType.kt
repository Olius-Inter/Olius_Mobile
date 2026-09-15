package com.olius.app.presentation.screen.PerfilType

import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.olius.app.R
import com.olius.app.presentation.theme.OliusAmarelo
import com.olius.app.presentation.theme.OliusAmareloClaro
import com.olius.app.presentation.theme.OliusCampoBorda
import com.olius.app.presentation.theme.OliusCampoFundo
import com.olius.app.presentation.theme.OliusTextoPrimario
import com.olius.app.presentation.theme.OliusTextoSecundario
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Quanto tempo o header fica centralizado (igual ao frame final da splash)
// antes de subir e virar linha. Ver seção "Como reposicionar" no markdown
// de documentação pra ajustar timings/posições.
private const val HEADER_INTRO_DURATION_MS = 1200L

/** Layout do header: só existem 2 estados, e a troca CENTERED->TOP acontece uma única vez. */
private enum class HeaderLayout { CENTERED, TOP }

// Tamanho da logo/texto "Olius" nos dois estados do header — ajustados pra
// bater com o tamanho da referencia_app.png (painel "SplashScreen II" pro
// estado centralizado, "Opção_conta" pro estado em linha). Mexa só nessas
// constantes pra redimensionar; ver markdown pra outras formas de ajustar
// posição/alinhamento.
private val HEADER_CENTERED_ICON_SIZE = 220.dp
private val HEADER_CENTERED_TEXT_SIZE = 90.sp
private val HEADER_ROW_ICON_SIZE = 75.dp
private val HEADER_ROW_TEXT_SIZE = 42.sp

// Diâmetro das bolinhas decorativas de fundo. O centro de cada uma fica
// exatamente em cima do canto da tela (ver DecorativeBackground) — como o
// Brush.radialGradient delas vai do centro (mais forte) até a borda do
// círculo (transparente), esse diâmetro/2 é o "raio" até onde a cor
// desvanece: quanto maior, mais a cor se espalha pela tela antes de sumir.
private val DECORATIVE_BLOB_SIZE = 520.dp

// Altura da linha fluida no card de escolha de tipo. Diminua pra ela ficar
// menos esticada verticalmente, aumente pra ocupar mais espaço — ver
// markdown pra outras formas de manipular esse tamanho.
private val TYPE_SELECTOR_FLUID_LINE_HEIGHT = 150.dp

// Altura do card de Login e do card de escolha de tipo — os dois usam o
// MESMO valor de propósito (pedido). O card de Cadastro NÃO usa essa
// constante: ele se autoajusta ao conteúdo (wrapContentHeight, sem
// scroll), porque tem mais campos que os outros dois. Se cortar algo no
// Login/escolha de tipo, aumente esse valor; se sobrar espaço em branco,
// diminua.
private val AUTH_SECONDARY_CARD_HEIGHT = 580.dp

// Distância entre o header (logo + "Olius") e o topo do card, quando o
// header já subiu e virou linha. Aumente/diminua à vontade.
private val HEADER_TO_CARD_GAP = 16.dp

// Espaço vertical entre um campo de input e o próximo, em Login/Cadastro —
// bem pequeno de propósito (igual referencia_input.png: quase não tem
// espaço entre eles, só a linha embaixo de cada um já separa visualmente).
private val AUTH_FIELD_SPACING = 2.dp

/**
 * Composable raiz do fluxo de onboarding: replica o fim da splash (Função 1),
 * escolha de tipo de perfil (Função 2) e os cards de login (Função 4) /
 * cadastro (Função 3). Tudo num arquivo só de propósito — ver o markdown de
 * documentação pra entender o porquê.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PerfilTypeScreen(
    onNavigateHome: () -> Unit,
    onForgotPassword: () -> Unit,
    viewModel: PerfilTypeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    PerfilTypeContent(
        uiState = uiState,
        onHeaderIntroFinished = viewModel::onHeaderIntroFinished,
        onSelectProfileType = viewModel::selectProfileType,
        onConfirmProfileType = viewModel::confirmProfileType,
        onUpdateLoginEmail = viewModel::updateLoginEmail,
        onUpdateLoginPassword = viewModel::updateLoginPassword,
        onToggleLoginPasswordVisibility = viewModel::toggleLoginPasswordVisibility,
        onUpdateRegisterName = viewModel::updateRegisterName,
        onUpdateRegisterEmail = viewModel::updateRegisterEmail,
        onUpdateRegisterBirthDate = viewModel::updateRegisterBirthDate,
        onUpdateRegisterPhone = viewModel::updateRegisterPhone,
        onUpdateRegisterPassword = viewModel::updateRegisterPassword,
        onToggleRegisterPasswordVisibility = viewModel::toggleRegisterPasswordVisibility,
        onNavigateHome = onNavigateHome,
        onForgotPassword = onForgotPassword,
        onGoToRegister = viewModel::goToRegister,
        onGoToLogin = viewModel::goToLogin
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PerfilTypeContent(
    uiState: PerfilTypeUiState,
    onHeaderIntroFinished: () -> Unit,
    onSelectProfileType: (ProfileType) -> Unit,
    onConfirmProfileType: () -> Unit,
    onUpdateLoginEmail: (String) -> Unit,
    onUpdateLoginPassword: (String) -> Unit,
    onToggleLoginPasswordVisibility: () -> Unit,
    onUpdateRegisterName: (String) -> Unit,
    onUpdateRegisterEmail: (String) -> Unit,
    onUpdateRegisterBirthDate: (String) -> Unit,
    onUpdateRegisterPhone: (String) -> Unit,
    onUpdateRegisterPassword: (String) -> Unit,
    onToggleRegisterPasswordVisibility: () -> Unit,
    onNavigateHome: () -> Unit,
    onForgotPassword: () -> Unit,
    onGoToRegister: () -> Unit,
    onGoToLogin: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(HEADER_INTRO_DURATION_MS)
        onHeaderIntroFinished()
    }

    val headerLayout = if (uiState.step == OnboardingStep.HEADER_INTRO) {
        HeaderLayout.CENTERED
    } else {
        HeaderLayout.TOP
    }

    SharedTransitionLayout {
        Box(Modifier.fillMaxSize()) {
            DecorativeBackground(isRow = headerLayout == HeaderLayout.TOP)

            AnimatedContent(
                targetState = headerLayout,
                transitionSpec = { fadeIn(tween(600)) togetherWith fadeOut(tween(400)) },
                label = "headerLayout"
            ) { layout ->
                when (layout) {
                    HeaderLayout.CENTERED -> Box(Modifier.fillMaxSize()) {
                        OnboardingHeader(
                            isRow = false,
                            sharedScope = this@SharedTransitionLayout,
                            animatedScope = this@AnimatedContent,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    HeaderLayout.TOP -> Column(Modifier.fillMaxSize()) {
                        // Espaço flexível fica em cima — empurra o grupo
                        // header+card pra baixo, sempre grudado no fundo da
                        // tela (o card não usa mais Alignment.BottomCenter
                        // interno pra isso, é essa Column que cuida).
                        Spacer(Modifier.weight(1f))
                        OnboardingHeader(
                            isRow = true,
                            sharedScope = this@SharedTransitionLayout,
                            animatedScope = this@AnimatedContent,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Spacer(Modifier.height(HEADER_TO_CARD_GAP))

                        AnimatedContent(
                            targetState = uiState.step,
                            modifier = Modifier.fillMaxWidth(),
                            transitionSpec = {
                                // +500ms no slide de entrada (pedido: card subindo mais devagar)
                                (slideInVertically(
                                    animationSpec = tween(840, easing = FastOutSlowInEasing)
                                ) { fullHeight -> fullHeight } + fadeIn(tween(350))) togetherWith
                                        fadeOut(tween(200))
                            },
                            label = "onboardingStep"
                        ) { step ->
                            when (step) {
                                OnboardingStep.CHOOSE_PROFILE -> ProfileTypeSelector(
                                    selected = uiState.selectedProfileType,
                                    onSelect = onSelectProfileType,
                                    onContinue = onConfirmProfileType
                                )

                                OnboardingStep.LOGIN -> LoginCard(
                                    uiState = uiState,
                                    onEmailChange = onUpdateLoginEmail,
                                    onPasswordChange = onUpdateLoginPassword,
                                    onToggleVisibility = onToggleLoginPasswordVisibility,
                                    onForgotPassword = onForgotPassword,
                                    onGoToRegister = onGoToRegister,
                                    onContinue = onNavigateHome
                                )

                                OnboardingStep.REGISTER -> RegisterCard(
                                    uiState = uiState,
                                    onNameChange = onUpdateRegisterName,
                                    onEmailChange = onUpdateRegisterEmail,
                                    onBirthDateChange = onUpdateRegisterBirthDate,
                                    onPhoneChange = onUpdateRegisterPhone,
                                    onPasswordChange = onUpdateRegisterPassword,
                                    onToggleVisibility = onToggleRegisterPasswordVisibility,
                                    onForgotPassword = onForgotPassword,
                                    onGoToLogin = onGoToLogin,
                                    onContinue = onNavigateHome
                                )

                                OnboardingStep.HEADER_INTRO -> Unit
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Função 1: logo + "Olius" — coluna centralizada (fim da splash) que
 * "morpha" suavemente pra uma linha no topo via shared elements.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun OnboardingHeader(
    isRow: Boolean,
    sharedScope: SharedTransitionScope,
    animatedScope: AnimatedContentScope,
    modifier: Modifier = Modifier
) {
    with(sharedScope) {
        if (isRow) {
            // Bottom (não CenterVertically) de propósito: ícone e texto ficam
            // "na mesma linha" (base alinhada), tipo display:flex sem
            // align-items:center — não o ícone centralizado com o texto.
            Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
                Image(
                    painter = painterResource(id = R.drawable.logo_sem_fundo),
                    contentDescription = "Olius",
                    modifier = Modifier
                        .size(HEADER_ROW_ICON_SIZE)
                        .sharedElement(
                            sharedContentState = rememberSharedContentState(key = "logoIcon"),
                            animatedVisibilityScope = animatedScope
                        )
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Olius",
                    fontSize = HEADER_ROW_TEXT_SIZE,
                    fontWeight = FontWeight.Bold,
                    color = OliusAmarelo,
                    modifier = Modifier.sharedElement(
                        sharedContentState = rememberSharedContentState(key = "logoText"),
                        animatedVisibilityScope = animatedScope
                    )
                )
            }
        } else {
            Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.logo_sem_fundo),
                    contentDescription = "Olius",
                    modifier = Modifier
                        .size(HEADER_CENTERED_ICON_SIZE)
                        .sharedElement(
                            sharedContentState = rememberSharedContentState(key = "logoIcon"),
                            animatedVisibilityScope = animatedScope
                        )
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Olius",
                    fontSize = HEADER_CENTERED_TEXT_SIZE,
                    fontWeight = FontWeight.Bold,
                    color = OliusAmarelo,
                    modifier = Modifier.sharedElement(
                        sharedContentState = rememberSharedContentState(key = "logoText"),
                        animatedVisibilityScope = animatedScope
                    )
                )
            }
        }
    }
}

/**
 * Fundo com dois blobs amarelos que trocam de lado (start<->end) em
 * sincronia com o header virando linha. O CENTRO de cada blob fica sempre
 * exatamente em cima de um canto da tela (top-start/top-end pro de cima,
 * bottom-start/bottom-end pro de baixo) — igual referencia_app.png, onde a
 * cor mais forte satura o canto e vai desvanecendo a partir dele.
 *
 * Usa gradient radial (Brush.radialGradient) em vez de "círculo sólido +
 * blur" — o `Modifier.blur()` do Compose só funciona em Android 12+
 * (API 31, via RenderEffect); abaixo disso ele não faz NADA (por isso
 * aumentar o valor do blur não tinha efeito nenhum). O gradient funciona
 * em qualquer versão do Android (é só matemática de cor, sem depender de
 * nenhuma API de renderização) e é o que realmente reproduz o visual
 * suave da referencia_app.png. Em API 31+ ainda soma um blur leve por
 * cima, só de bônus (ver `blurCompat` mais abaixo).
 *
 * Por que BoxWithConstraints + offset em vez de BiasAlignment: com
 * BiasAlignment, bias = ±1 deixa o círculo "encostado" na borda mas com o
 * CENTRO ainda a metade do tamanho dele pra dentro da tela (não no canto de
 * verdade) — pra empurrar o centro até o canto seria preciso um bias bem
 * maior que 1, e o valor certo muda dependendo do tamanho do círculo e da
 * tela (fácil de errar, foi o que causou a inconsistência do round
 * anterior). Calculando o offset manualmente a partir da largura/altura
 * reais da tela (`maxWidth`/`maxHeight`, via BoxWithConstraints), o centro
 * cai exatamente no canto sempre, não importa o tamanho do blob.
 */
@Composable
private fun DecorativeBackground(isRow: Boolean, modifier: Modifier = Modifier) {
    val bias by animateFloatAsState(
        targetValue = if (isRow) 1f else 0f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "blobBias"
    )

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        val half = DECORATIVE_BLOB_SIZE / 2
        // bias 0 -> centro no canto esquerdo (x=0); bias 1 -> centro no canto direito (x=maxWidth)
        val topBlobCenterX = lerp(0.dp, maxWidth, bias)
        val bottomBlobCenterX = lerp(maxWidth, 0.dp, bias)

        // Bolinha de cima: centro sempre em y=0 (borda de cima)
        GradientBlob(
            colors = listOf(
                OliusAmareloClaro.copy(alpha = 0.85f),
                OliusAmareloClaro.copy(alpha = 0.35f),
                Color.Transparent
            ),
            modifier = Modifier
                .size(DECORATIVE_BLOB_SIZE)
                .offset(x = topBlobCenterX - half, y = -half)
        )
        // Bolinha de baixo — sempre do lado oposto à de cima; centro em y=maxHeight
        GradientBlob(
            colors = listOf(OliusAmareloClaro.copy(alpha = 0.55f), Color.Transparent),
            modifier = Modifier
                .size(DECORATIVE_BLOB_SIZE)
                .offset(x = bottomBlobCenterX - half, y = maxHeight - half)
        )
    }
}

/**
 * Círculo com gradient radial (do centro pras bordas) + blur leve por
 * cima só em API 31+ (`blurCompat`, sem efeito nenhum abaixo disso, então
 * nem tenta — o gradient sozinho já cobre a aparência em qualquer versão).
 */
@Composable
private fun GradientBlob(colors: List<Color>, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(CircleShape)
            .background(Brush.radialGradient(colors))
            .blurCompat(40.dp)
    )
}

/**
 * `.blur()` normal em Android 12+ (API 31, `RenderEffect`); em versões
 * mais antigas o Compose não tem um jeito nativo/leve de fazer blur de
 * verdade (a alternativa seria RenderScript, que está deprecated, ou uma
 * lib externa tipo o Haze — ver seção "Blur nos círculos" no markdown de
 * documentação pra mais detalhes) — então abaixo de 31 simplesmente não
 * aplica nada, e o visual já fica correto só com o gradient acima.
 */
private fun Modifier.blurCompat(radius: Dp): Modifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) this.blur(radius) else this


/**
 * Função 2: card de escolha "Sou Estabelecimento" / "Sou Cidadão" +
 * linha fluida (Lottie) abaixo dos cards.
 */
@Composable
private fun ProfileTypeSelector(
    selected: ProfileType?,
    onSelect: (ProfileType) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Esse card não usa o AuthCardScaffold (que tem scroll) porque o texto/
    // botão precisam de padding lateral mas a linha fluida não (ela vai de
    // ponta a ponta do card) — por isso o padding horizontal fica só na
    // Column de dentro, não na de fora.
    //
    // Altura fixa em AUTH_SECONDARY_CARD_HEIGHT (mesma do LoginCard, de
    // propósito — pedido). Quem posiciona o card grudado embaixo da tela
    // agora é a Column de fora, em PerfilTypeContent — esse Surface só
    // precisa do próprio tamanho, sem Box/Alignment.BottomCenter.
    Surface(
        // Levemente mais estreito que a tela — pra estreitar mais, aumente
        // esse padding horizontal.
        modifier = modifier
            .fillMaxWidth()
            .height(AUTH_SECONDARY_CARD_HEIGHT)
            .padding(horizontal = 10.dp),
        color = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        shadowElevation = 22.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Text(
                    text = "Como você deseja utilizar o Olius?",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = OliusTextoPrimario,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))

                ProfileOptionCard(
                    title = "Sou Estabelecimento",
                    subtitle = "Restaurantes, Hotéis, escolas e cozinhas industriais etc",
                    icon = Icons.Filled.Business,
                    selected = selected == ProfileType.ESTABLISHMENT,
                    onClick = { onSelect(ProfileType.ESTABLISHMENT) }
                )
                Spacer(Modifier.height(12.dp))
                ProfileOptionCard(
                    title = "Sou Cidadão",
                    subtitle = "Pessoa física que deseja descartar óleo corretamente",
                    icon = Icons.Filled.Person,
                    selected = selected == ProfileType.CITIZEN,
                    onClick = { onSelect(ProfileType.CITIZEN) }
                )
                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = onContinue,
                    enabled = selected != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OliusAmarelo,
                        disabledContainerColor = OliusCampoFundo,
                        contentColor = Color.Black,
                        disabledContentColor = OliusTextoSecundario
                    )
                ) {
                    Text("Continuar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Image(
                        painter = painterResource(R.drawable.arrow),
                        contentDescription = "Icone de seta para continuar a próxima tela",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Linha fluida: fica FORA da Column com padding horizontal de
            // propósito, pra ir de ponta a ponta do card (sem o respiro de
            // 24dp que o texto/botão têm). Altura fixa e tunável em
            // TYPE_SELECTOR_FLUID_LINE_HEIGHT (menos esticada verticalmente
            // do que o weight(1f) que preenchia todo o espaço restante
            // ver markdown pra outras formas de manipular esse tamanho.
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.linhafluida))
            LottieAnimation(
                composition = composition,
                iterations = 1,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TYPE_SELECTOR_FLUID_LINE_HEIGHT)
                    .clipToBounds()
            )
        }
    }
}

@Composable
private fun ProfileOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background by animateColorAsState(
        targetValue = if (selected) OliusAmareloClaro.copy(alpha = 0.35f) else OliusCampoFundo,
        label = "cardBackground"
    )
    val border by animateColorAsState(
        targetValue = if (selected) OliusAmarelo else OliusCampoBorda,
        label = "cardBorder"
    )

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .border(BorderStroke(1.5.dp, border), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) OliusAmarelo else Color.White)
                    .border(BorderStroke(1.dp, OliusCampoBorda), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (selected) Color.Black else OliusTextoSecundario,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = OliusTextoPrimario)
                Text(subtitle, fontSize = 12.sp, color = OliusTextoSecundario)
            }
        }

        if (selected) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(OliusAmarelo),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
            }
        }
    }
}

/** Função 4: card de login. */
@Composable
private fun LoginCard(
    uiState: PerfilTypeUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    onForgotPassword: () -> Unit,
    onGoToRegister: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Altura fixa igual ao card de escolha de tipo (AUTH_SECONDARY_CARD_HEIGHT) — pedido.
    AuthCardScaffold(modifier = modifier.height(AUTH_SECONDARY_CARD_HEIGHT)) {
        Text(
            text = "Logue para entrar em sua Conta",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = OliusTextoPrimario
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Não tem uma conta? ",
                fontSize = 13.sp,
                color = OliusTextoSecundario
            )
            Text(
                "Cadastre-se",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = OliusAmarelo,
                modifier = Modifier.clickable(onClick = onGoToRegister)
            )
        }
        Spacer(Modifier.height(20.dp))

        OliusTextField(
            value = uiState.loginEmail,
            onValueChange = onEmailChange,
            placeholder = "Email",
            leadingIcon = Icons.Filled.Email,
            keyboardType = KeyboardType.Email
        )
        Spacer(Modifier.height(AUTH_FIELD_SPACING))
        OliusPasswordField(
            value = uiState.loginPassword,
            onValueChange = onPasswordChange,
            visible = uiState.loginPasswordVisible,
            onToggleVisible = onToggleVisibility
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Esqueceu sua senha?",
            fontSize = 12.sp,
            color = OliusTextoSecundario,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable(onClick = onForgotPassword)
        )
        Spacer(Modifier.height(20.dp))
        ContinuarButton(text = "Entrar", onClick = onContinue)
        Spacer(Modifier.height(20.dp))
        SocialIconsRow()
    }
}

/** Função 3: card de criar conta. */
@Composable
private fun RegisterCard(
    uiState: PerfilTypeUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onBirthDateChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    onForgotPassword: () -> Unit,
    onGoToLogin: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    AuthCardScaffold(modifier = modifier) {
        Text("Crie sua Conta", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = OliusTextoPrimario)
        Spacer(Modifier.height(4.dp))
        Row {
            Text("Já tem uma conta? ", fontSize = 13.sp, color = OliusTextoSecundario)
            Text(
                "Logue",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = OliusAmarelo,
                modifier = Modifier.clickable(onClick = onGoToLogin)
            )
        }
        Spacer(Modifier.height(20.dp))

        OliusTextField(
            value = uiState.registerName,
            onValueChange = onNameChange,
            placeholder = "Nome completo",
            leadingIcon = Icons.Filled.Person
        )
        Spacer(Modifier.height(AUTH_FIELD_SPACING))
        OliusTextField(
            value = uiState.registerEmail,
            onValueChange = onEmailChange,
            placeholder = "Email",
            leadingIcon = Icons.Filled.Email,
            keyboardType = KeyboardType.Email
        )
        Spacer(Modifier.height(AUTH_FIELD_SPACING))
        OliusDateField(value = uiState.registerBirthDate, onValueChange = onBirthDateChange)
        Spacer(Modifier.height(AUTH_FIELD_SPACING))
        OliusTextField(
            value = uiState.registerPhone,
            onValueChange = onPhoneChange,
            placeholder = "Telefone",
            leadingIcon = Icons.Filled.Phone,
            keyboardType = KeyboardType.Phone
        )
        Spacer(Modifier.height(AUTH_FIELD_SPACING))
        OliusPasswordField(
            value = uiState.registerPassword,
            onValueChange = onPasswordChange,
            visible = uiState.registerPasswordVisible,
            onToggleVisible = onToggleVisibility
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Esqueceu sua senha?",
            fontSize = 12.sp,
            color = OliusTextoSecundario,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable(onClick = onForgotPassword)
        )
        Spacer(Modifier.height(20.dp))
        ContinuarButton(text = "Continuar", onClick = onContinue)
        Spacer(Modifier.height(20.dp))
        SocialIconsRow()
    }
}

/**
 * Base branca com cantos superiores arredondados, comum aos cards de
 * login/cadastro. Sem scroll — quem chama passa a altura via `modifier`
 * (`LoginCard` passa `AUTH_SECONDARY_CARD_HEIGHT`; `RegisterCard` não passa
 * nenhuma, então cresce só o necessário pro conteúdo caber). Quem gruda o
 * card embaixo da tela é a Column de fora, em `PerfilTypeContent`.
 * `horizontalAlignment = CenterHorizontally` centraliza título/subtítulo
 * (que não usam fillMaxWidth) sem afetar campos/botão (que já usam
 * fillMaxWidth e ignoram o alinhamento do Column).
 */
@Composable
private fun AuthCardScaffold(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    // Sem scroll de propósito (pedido) — quem chama decide a altura pelo
    // `modifier`: LoginCard passa uma altura fixa (AUTH_SECONDARY_CARD_HEIGHT,
    // igual ao card de escolha de tipo); RegisterCard não passa altura
    // nenhuma, então o Surface cresce só o necessário pro conteúdo caber
    // (wrap content) — ele tem mais campos que os outros dois. Quem
    // posiciona o card grudado embaixo da tela é a Column de fora, em
    // PerfilTypeContent.
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        color = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

@Composable
private fun ContinuarButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = OliusAmarelo, contentColor = Color.Black)
    ) {
        Text(text, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(6.dp))
        // Placeholder pro ícone do botão — ver "Trocando os ícones sociais e a seta"
        // no markdown de documentação pra trocar por outra imagem depois.
        Image(painter = painterResource(R.drawable.arrow), contentDescription = null, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun SocialIconsRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
    ) {
        repeat(3) {
            SocialIconPlaceholder(onClick = { /* ver "Trocando os ícones sociais" no markdown */ })
        }
    }
}

/**
 * Placeholder visual pros botões de login social (Google/Apple/Facebook) do
 * design de referência: mesmo fundo/borda da imagem, com R.drawable.arrow
 * no lugar do ícone real. Ver o markdown de documentação pra trocar pela
 * imagem e destino (OAuth) reais de cada provedor.
 */
@Composable
private fun SocialIconPlaceholder(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(70.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(BorderStroke(1.dp, OliusCampoBorda), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(painter = painterResource(R.drawable.arrow), contentDescription = null, modifier = Modifier.size(18.dp))
    }
}

// Cor do texto que a pessoa DIGITA nos campos de input (não é o placeholder,
// que continua com OliusTextoSecundario, cinza). É a mesma cor usada nos
// títulos dos cards ("Crie sua Conta" etc) — troque só aqui pra mudar nos
// dois lugares de uma vez, ou troque só o valor de OliusTextoPrimario (em
// Color.kt) se quiser mudar em TODO o app de uma vez só.
private val AUTH_FIELD_TEXT_COLOR = OliusTextoPrimario

/**
 * Cores compartilhadas pelos 3 campos abaixo: sem fundo/caixa visível em
 * repouso (transparente, funde com o card), uma linha fina embaixo
 * (`indicatorColor`), e um fundo amarelo bem fraco quando o campo está em
 * foco (clicado) — igual referencia_input.png. `shape = RectangleShape`
 * tira o arredondado padrão do TextField (senão pareceria uma caixinha,
 * não só uma linha).
 */
@Composable
private fun oliusFieldColors() = TextFieldDefaults.colors(
    unfocusedTextColor = AUTH_FIELD_TEXT_COLOR,
    focusedTextColor = AUTH_FIELD_TEXT_COLOR,
    disabledTextColor = AUTH_FIELD_TEXT_COLOR,
    unfocusedContainerColor = Color.Transparent,
    focusedContainerColor = OliusAmareloClaro.copy(alpha = 0.18f),
    disabledContainerColor = Color.Transparent,
    unfocusedIndicatorColor = OliusCampoBorda,
    focusedIndicatorColor = OliusAmarelo
)

@Composable
private fun OliusTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = OliusTextoSecundario) },
        leadingIcon = { Icon(leadingIcon, contentDescription = null, tint = OliusAmarelo) },
        singleLine = true,
        shape = RectangleShape,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = oliusFieldColors()
    )
}

@Composable
private fun OliusPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Senha", color = OliusTextoSecundario) },
        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = OliusAmarelo) },
        trailingIcon = {
            IconButton(onClick = onToggleVisible) {
                Icon(
                    imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (visible) "Ocultar senha" else "Mostrar senha",
                    tint = OliusTextoSecundario
                )
            }
        },
        singleLine = true,
        shape = RectangleShape,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        colors = oliusFieldColors()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OliusDateField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }

    TextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        modifier = modifier
            .fillMaxWidth()
            .clickable { showPicker = true },
        placeholder = { Text("Data de nascimento", color = OliusTextoSecundario) },
        leadingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = OliusAmarelo) },
        trailingIcon = {
            IconButton(onClick = { showPicker = true }) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = "Abrir calendário", tint = OliusTextoSecundario)
            }
        },
        singleLine = true,
        shape = RectangleShape,
        colors = oliusFieldColors()
    )

    if (showPicker) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))
                        onValueChange(formatter.format(Date(millis)))
                    }
                    showPicker = false
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun PerfilTypeScreenPreview() {
    PerfilTypeContent(
        uiState = PerfilTypeUiState(step = OnboardingStep.CHOOSE_PROFILE),
        onHeaderIntroFinished = {},
        onSelectProfileType = {},
        onConfirmProfileType = {},
        onUpdateLoginEmail = {},
        onUpdateLoginPassword = {},
        onToggleLoginPasswordVisibility = {},
        onUpdateRegisterName = {},
        onUpdateRegisterEmail = {},
        onUpdateRegisterBirthDate = {},
        onUpdateRegisterPhone = {},
        onUpdateRegisterPassword = {},
        onToggleRegisterPasswordVisibility = {},
        onNavigateHome = {},
        onForgotPassword = {},
        onGoToRegister = {},
        onGoToLogin = {}
    )
}

@Preview(showBackground = true)
@Composable
private fun OliusTextFieldPreview() {
    var text by remember { mutableStateOf("") }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OliusTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = "Campo Vazio",
            leadingIcon = Icons.Filled.Email
        )
        OliusTextField(
            value = "Texto digitado",
            onValueChange = {},
            placeholder = "Placeholder",
            leadingIcon = Icons.Filled.Person
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun DecorativeBackgroundPreview() {
    Box(Modifier.fillMaxSize()) {
        DecorativeBackground(isRow = false)
        Text(
            "Olius",
            Modifier.align(Alignment.Center),
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun DecorativeBackgroundRowPreview() {
    Box(Modifier.fillMaxSize()) {
        DecorativeBackground(isRow = true)
        Text(
            "Olius",
            Modifier.align(Alignment.Center),
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}
