package com.olius.app.presentation.screen.PerfilType

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.ColorFilter
import com.olius.app.domain.entity.PhoneCountry
import com.olius.app.domain.entity.ProfileType
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.graphics.painter.Painter
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.olius.app.R
import com.olius.app.data.remote.ApiConfig
import com.olius.app.presentation.auth.requestGoogleAccount
import com.olius.app.presentation.theme.OliusAmarelo
import com.olius.app.presentation.theme.OliusAmareloClaro
import com.olius.app.presentation.theme.OliusCampoBorda
import com.olius.app.presentation.theme.OliusCampoFundo
import com.olius.app.presentation.theme.DecorativeBackground
import com.olius.app.presentation.theme.OliusTextoPrimario
import com.olius.app.presentation.theme.OliusTextoSecundario
import com.olius.app.presentation.theme.OliusSuperficie
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Quanto tempo o header fica centralizado (igual ao frame final da splash)
// antes de subir e virar linha. Ver seção "Como reposicionar" no markdown
// de documentação pra ajustar timings/posições.
private const val HEADER_INTRO_DURATION_MS = 1200L

// Fade do "Olius" grande enquanto o pequeno (escalado) entra por cima dele.
private const val HEADER_TEXT_EXIT_FADE_MS = 300

/** Layout do header: só existem 2 estados, e a troca CENTERED->TOP acontece uma única vez. */
private enum class HeaderLayout { CENTERED, TOP }

// Tamanho da logo/texto "Olius" nos dois estados do header — ajustados pra
// bater com o tamanho da referencia_app.png (painel "SplashScreen II" pro
// estado centralizado, "Opção_conta" pro estado em linha). Mexa só nessas
// constantes pra redimensionar; ver markdown pra outras formas de ajustar
// posição/alinhamento.
private val HEADER_CENTERED_ICON_SIZE = 210.dp
private val HEADER_CENTERED_TEXT_SIZE = 85.sp
internal val HEADER_ROW_ICON_SIZE = 75.dp
internal val HEADER_ROW_TEXT_SIZE = 42.sp

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
internal val AUTH_SECONDARY_CARD_HEIGHT = 580.dp

// Distância entre o header (logo + "Olius") e o topo do card, quando o
// header já subiu e virou linha. Aumente/diminua à vontade.
internal val HEADER_TO_CARD_GAP = 16.dp

/** Distância mínima entre a barra de status e o logo quando o card ocupa a tela toda. */
private val HEADER_MIN_TOP_SPACE = 40.dp

// Espaço vertical entre um campo de input e o próximo, em Login/Cadastro —
// bem pequeno de propósito (igual ref_input.png: quase não tem
// espaço entre eles, só a linha embaixo de cada um já separa visualmente).
internal val AUTH_FIELD_SPACING = 2.dp

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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // LaunchedEffect(Unit): a chave "Unit" nunca muda, então este bloco roda
    // uma vez quando a tela entra em composição e fica escutando o Flow até
    // sair de composição — é o jeito recomendado de coletar um Flow de
    // evento único dentro do Compose (ver AUTH_01, seção 6).
    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                PerfilTypeNavigationEvent.NavigateHome -> onNavigateHome()
            }
        }
    }

    val onGoogleSignInClick: () -> Unit = {
        coroutineScope.launch {
            try {
                val account = requestGoogleAccount(context)
                viewModel.signInWithGoogle(account)
            } catch (e: GetCredentialCancellationException) {
                // Cancelamento não é erro — a pessoa só fechou o seletor de conta.
                viewModel.onGoogleSignInCancelled()
            } catch (e: GetCredentialException) {
                viewModel.onGoogleSignInCancelled()
            }
        }
    }

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
        phoneCountries = { viewModel.phoneCountries },
        onSelectPhoneCountry = viewModel::selectPhoneCountry,
        onUpdateRegisterPassword = viewModel::updateRegisterPassword,
        onToggleRegisterPasswordVisibility = viewModel::toggleRegisterPasswordVisibility,
        onLoginContinue = viewModel::login,
        onRegisterContinue = viewModel::register,
        onGoogleSignInClick = onGoogleSignInClick,
        onForgotPassword = onForgotPassword,
        onGoToRegister = viewModel::goToRegister,
        onGoToLogin = viewModel::goToLogin,
        accountForm = AccountFormActions(
            onSelectProfileType = viewModel::selectProfileType,
            onDocumentChange = viewModel::updateRegisterDocument,
            onSelectEstablishmentType = viewModel::selectEstablishmentType,
            onRetryEstablishmentTypes = viewModel::retryEstablishmentTypes,
            onCepChange = viewModel::updateRegisterCep,
            onRetryCep = viewModel::retryCep,
            onStreetChange = viewModel::updateRegisterStreet,
            onNumberChange = viewModel::updateRegisterNumber,
            onNeighborhoodChange = viewModel::updateRegisterNeighborhood,
            onCityChange = viewModel::updateRegisterCity,
            onComplementChange = viewModel::updateRegisterComplement,
            onCompleteGoogleProfile = viewModel::completeGoogleProfile
        )
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
    phoneCountries: () -> List<PhoneCountry>,
    onSelectPhoneCountry: (PhoneCountry) -> Unit,
    onUpdateRegisterPassword: (String) -> Unit,
    onToggleRegisterPasswordVisibility: () -> Unit,
    onLoginContinue: () -> Unit,
    onRegisterContinue: () -> Unit,
    onGoogleSignInClick: () -> Unit,
    onForgotPassword: () -> Unit,
    onGoToRegister: () -> Unit,
    onGoToLogin: () -> Unit,
    accountForm: AccountFormActions
) {
    LaunchedEffect(Unit) {
        delay(HEADER_INTRO_DURATION_MS)
        onHeaderIntroFinished()
    }

    // Carregados aqui, durante a pausa do HEADER_INTRO (nada anima), e não no
    // quadro em que o header vira linha: a logo é decodificada uma vez só (as
    // duas pontas usam o mesmo painter) e a linha fluida já chega parseada no
    // card, sem recompor no meio da transição.
    val logoPainter = painterResource(id = R.drawable.logo_sem_fundo)
    val fluidLineComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.linhafluida))

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
                            logoPainter = logoPainter,
                            sharedScope = this@SharedTransitionLayout,
                            animatedScope = this@AnimatedContent,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    HeaderLayout.TOP -> Column(
                        Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            // Com o teclado aberto a Column encolhe e o card rola
                            // por dentro, em vez de ficar escondido atrás do teclado.
                            .imePadding()
                    ) {
                        // Espaço mínimo: com o card de cadastro (alto) o logo
                        // não cola no topo da tela.
                        Spacer(Modifier.height(HEADER_MIN_TOP_SPACE))
                        // Espaço flexível fica em cima — empurra o grupo
                        // header+card pra baixo, sempre grudado no fundo da
                        // tela (o card não usa mais Alignment.BottomCenter
                        // interno pra isso, é essa Column que cuida).
                        Spacer(Modifier.weight(1f))
                        OnboardingHeader(
                            isRow = true,
                            logoPainter = logoPainter,
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
                                    fluidLineComposition = fluidLineComposition,
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
                                    onContinue = onLoginContinue,
                                    onGoogleSignInClick = onGoogleSignInClick
                                )

                                OnboardingStep.REGISTER -> RegisterCard(
                                    uiState = uiState,
                                    onNameChange = onUpdateRegisterName,
                                    onEmailChange = onUpdateRegisterEmail,
                                    onBirthDateChange = onUpdateRegisterBirthDate,
                                    onPhoneChange = onUpdateRegisterPhone,
                                    phoneCountries = phoneCountries,
                                    onSelectPhoneCountry = onSelectPhoneCountry,
                                    onPasswordChange = onUpdateRegisterPassword,
                                    onToggleVisibility = onToggleRegisterPasswordVisibility,
                                    onForgotPassword = onForgotPassword,
                                    onGoToLogin = onGoToLogin,
                                    onContinue = onRegisterContinue,
                                    onGoogleSignInClick = onGoogleSignInClick,
                                    accountForm = accountForm
                                )

                                OnboardingStep.COMPLETE_PROFILE -> CompleteProfileCard(
                                    uiState = uiState,
                                    onNameChange = onUpdateRegisterName,
                                    onPhoneChange = onUpdateRegisterPhone,
                                    phoneCountries = phoneCountries,
                                    onSelectPhoneCountry = onSelectPhoneCountry,
                                    onCancel = onGoToLogin,
                                    actions = accountForm
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
    logoPainter: Painter,
    sharedScope: SharedTransitionScope,
    animatedScope: AnimatedContentScope,
    modifier: Modifier = Modifier
) {
    with(sharedScope) {
        val iconModifier = Modifier.sharedElement(
            sharedContentState = rememberSharedContentState(key = "logoIcon"),
            animatedVisibilityScope = animatedScope
        )
        // Texto: sharedBounds + scaleToBounds, não sharedElement. O
        // sharedElement remede o filho a cada quadro com o tamanho animado,
        // então o "Olius" (fontSize diferente nas duas pontas) pulava direto
        // pro tamanho final e era re-diagramado a cada quadro. Aqui o texto é
        // medido uma vez e só escalado (camada gráfica) até o novo tamanho.
        // O de saída some por baixo do de entrada (iguais, só escalados).
        val textModifier = Modifier.sharedBounds(
            sharedContentState = rememberSharedContentState(key = "logoText"),
            animatedVisibilityScope = animatedScope,
            enter = EnterTransition.None,
            exit = fadeOut(tween(HEADER_TEXT_EXIT_FADE_MS)),
            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
        )
        if (isRow) {
            // Bottom (não CenterVertically) de propósito: ícone e texto ficam
            // "na mesma linha" (base alinhada), tipo display:flex sem
            // align-items:center — não o ícone centralizado com o texto.
            Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
                Image(
                    painter = logoPainter,
                    contentDescription = "Olius",
                    modifier = Modifier
                        .size(HEADER_ROW_ICON_SIZE)
                        .then(iconModifier)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Olius",
                    fontSize = HEADER_ROW_TEXT_SIZE,
                    fontWeight = FontWeight.Bold,
                    color = OliusAmarelo,
                    modifier = textModifier
                )
            }
        } else {
            Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = logoPainter,
                    contentDescription = "Olius",
                    modifier = Modifier
                        .size(HEADER_CENTERED_ICON_SIZE)
                        .then(iconModifier)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Olius",
                    fontSize = HEADER_CENTERED_TEXT_SIZE,
                    fontWeight = FontWeight.Bold,
                    color = OliusAmarelo,
                    modifier = textModifier
                )
            }
        }
    }
}

/**
 * Função 2: card de escolha "Sou Estabelecimento" / "Sou Cidadão" +
 * linha fluida (Lottie) abaixo dos cards.
 */
@Composable
private fun ProfileTypeSelector(
    fluidLineComposition: LottieComposition?,
    selected: ProfileType?,
    onSelect: (ProfileType) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    // O conteúdo do card (textos, cards, botão, Lottie) entra um quadro depois
    // do Surface. Este card nasce no mesmo quadro em que o header vira linha;
    // dividir a composição em dois quadros evita um quadro longo bem no começo
    // da animação. Não aparece: o card ainda está com alpha ~0 (fadeIn).
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        showContent = true
    }

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
        color = OliusSuperficie,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        shadowElevation = 22.dp
    ) {
        if (showContent) Column(modifier = Modifier.fillMaxSize()) {
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
                    iconRes = R.drawable.ic_profile_establishment,
                    selected = selected == ProfileType.ESTABLISHMENT,
                    onClick = { onSelect(ProfileType.ESTABLISHMENT) }
                )
                Spacer(Modifier.height(12.dp))
                ProfileOptionCard(
                    title = "Sou Cidadão",
                    subtitle = "Pessoa física que deseja descartar óleo corretamente",
                    iconRes = R.drawable.ic_profile_citizen,
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
            // A composição vem pronta de PerfilTypeContent (pré-carregada).
            LottieAnimation(
                composition = fluidLineComposition,
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
    @DrawableRes iconRes: Int,
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
                    .background(if (selected) OliusAmarelo else OliusSuperficie)
                    .border(BorderStroke(1.dp, OliusCampoBorda), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Ícones do Figma (frame "Tipo de conta"), não do Material.
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    colorFilter = if (selected) ColorFilter.tint(Color.Black) else null,
                    modifier = Modifier.size(18.dp)
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
    onGoogleSignInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var passwordFocused by remember { mutableStateOf(false) }
    ScrollToEndWhileFocused(scrollState, passwordFocused)
    // Altura fixa igual ao card de escolha de tipo (AUTH_SECONDARY_CARD_HEIGHT) — pedido.
    // Rola só quando o teclado abre e o card não cabe mais.
    AuthCardScaffold(
        modifier = modifier.height(AUTH_SECONDARY_CARD_HEIGHT),
        scrollable = true,
        scrollState = scrollState
    ) {
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
            leadingIconRes = R.drawable.ic_field_mail,
            keyboardType = KeyboardType.Email
        )
        Spacer(Modifier.height(AUTH_FIELD_SPACING))
        OliusPasswordField(
            value = uiState.loginPassword,
            onValueChange = onPasswordChange,
            visible = uiState.loginPasswordVisible,
            onToggleVisible = onToggleVisibility,
            onFocusChange = { passwordFocused = it }
        )
        AuthCardFooter(
            uiState = uiState,
            buttonText = if (uiState.isAuthenticating) "Entrando..." else "Entrar",
            onForgotPassword = onForgotPassword,
            onContinue = onContinue,
            onGoogleSignInClick = onGoogleSignInClick
        )
    }
}

/**
 * Rodapé comum aos cards de login e cadastro: "Esqueceu sua senha?", mensagem
 * de erro, botão principal e login social. Precisa estar dentro de uma
 * Column (usa `align`) — ver [AuthCardScaffold].
 */
@Composable
private fun ColumnScope.AuthCardFooter(
    uiState: PerfilTypeUiState,
    buttonText: String,
    onForgotPassword: () -> Unit,
    onContinue: () -> Unit,
    onGoogleSignInClick: () -> Unit
) {
    Spacer(Modifier.height(8.dp))
    Text(
        "Esqueceu sua senha?",
        fontSize = 12.sp,
        color = OliusTextoSecundario,
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .clickable(onClick = onForgotPassword)
    )
    Spacer(Modifier.height(12.dp))
    AuthErrorMessage(message = uiState.authErrorMessage)
    Spacer(Modifier.height(8.dp))
    ContinuarButton(
        text = buttonText,
        onClick = onContinue,
        enabled = !uiState.isAuthenticating
    )
    if (ApiConfig.SOCIAL_LOGIN_ENABLED) {
        Spacer(Modifier.height(20.dp))
        SocialIconsRow(onGoogleClick = onGoogleSignInClick, enabled = !uiState.isAuthenticating)
    }
}

/** Mostra a mensagem de erro de autenticação, se houver — ver AUTH_01, seção 5/6. */
@Composable
internal fun AuthErrorMessage(message: String?, modifier: Modifier = Modifier) {
    if (message != null) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = modifier.fillMaxWidth()
        )
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
    phoneCountries: () -> List<PhoneCountry>,
    onSelectPhoneCountry: (PhoneCountry) -> Unit,
    onPasswordChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    onForgotPassword: () -> Unit,
    onGoToLogin: () -> Unit,
    onContinue: () -> Unit,
    onGoogleSignInClick: () -> Unit,
    accountForm: AccountFormActions,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var passwordFocused by remember { mutableStateOf(false) }
    ScrollToEndWhileFocused(scrollState, passwordFocused)
    AuthCardScaffold(modifier = modifier, scrollable = true, scrollState = scrollState) {
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
            placeholder = if (uiState.registrationType == ProfileType.CITIZEN) "Nome completo" else "Nome do estabelecimento",
            leadingIconRes = R.drawable.ic_field_user,
            error = uiState.fieldError(RegisterField.NAME)
        )
        Spacer(Modifier.height(AUTH_FIELD_SPACING))
        OliusTextField(
            value = uiState.registerEmail,
            onValueChange = onEmailChange,
            placeholder = "Email",
            leadingIconRes = R.drawable.ic_field_mail,
            keyboardType = KeyboardType.Email,
            error = uiState.fieldError(RegisterField.EMAIL)
        )
        Spacer(Modifier.height(AUTH_FIELD_SPACING))
        OliusDateField(value = uiState.registerBirthDate, onValueChange = onBirthDateChange)
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
        // CPF (cidadão) ou CNPJ + tipo + endereço (estabelecimento): o que a Core exige.
        AccountDocumentFields(uiState = uiState, actions = accountForm)
        Spacer(Modifier.height(AUTH_FIELD_SPACING))
        OliusPasswordField(
            value = uiState.registerPassword,
            onValueChange = onPasswordChange,
            visible = uiState.registerPasswordVisible,
            onToggleVisible = onToggleVisibility,
            error = uiState.fieldError(RegisterField.PASSWORD),
            onFocusChange = { passwordFocused = it }
        )
        PasswordChecklist(uiState.passwordChecklist, visible = uiState.registerPassword.isNotEmpty())
        AuthCardFooter(
            uiState = uiState,
            buttonText = if (uiState.isAuthenticating) "Continuando..." else "Continuar",
            onForgotPassword = onForgotPassword,
            onContinue = onContinue,
            onGoogleSignInClick = onGoogleSignInClick
        )
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
internal fun AuthCardScaffold(
    modifier: Modifier = Modifier,
    // Cadastro e "Completar cadastro" (CPF/CNPJ, tipo e endereço) passam da
    // altura da tela: só eles rolam. Login e escolha de tipo continuam fixos.
    scrollable: Boolean = false,
    scrollState: ScrollState = rememberScrollState(),
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
        color = OliusSuperficie,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (scrollable) Modifier.verticalScroll(scrollState) else Modifier)
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

/**
 * Enquanto [focused] (campo de senha), rola o card até o fim: "Esqueceu sua
 * senha?" e o botão ficam à vista acima do teclado. Reage ao `maxValue`
 * porque ele cresce enquanto o teclado ainda está abrindo.
 */
@Composable
private fun ScrollToEndWhileFocused(scrollState: ScrollState, focused: Boolean) {
    LaunchedEffect(focused, scrollState.maxValue) {
        if (focused) scrollState.animateScrollTo(scrollState.maxValue)
    }
}

@Composable
internal fun ContinuarButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        // Desabilitado (enquanto entra/cadastra) mantém as MESMAS cores: sem
        // isso o Material usa as cores "desabilitadas" do tema, e o texto fica
        // branco quando o celular está em modo escuro.
        colors = ButtonDefaults.buttonColors(
            containerColor = OliusAmarelo,
            contentColor = Color.Black,
            disabledContainerColor = OliusAmarelo,
            disabledContentColor = Color.Black
        )
    ) {
        Text(text, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(6.dp))
        // Placeholder pro ícone do botão — ver "Trocando os ícones sociais e a seta"
        // no markdown de documentação pra trocar por outra imagem depois.
        Image(painter = painterResource(R.drawable.arrow), contentDescription = null, modifier = Modifier.size(16.dp))
    }
}

/**
 * Login social: Google, Facebook e Microsoft. Escondido enquanto
 * [ApiConfig.SOCIAL_LOGIN_ENABLED] for false. Só o Google já faz login de
 * verdade ([onGoogleClick] — ver AUTH_02_LOGIN_GOOGLE.md); Facebook e
 * Microsoft mostram "em breve" até serem implementados (AUTH_11).
 *
 * Tamanhos diferentes de propósito: o PNG do Google ocupa a imagem inteira,
 * os do Facebook e da Microsoft já vêm com margem interna — assim os três
 * ficam do mesmo tamanho visualmente.
 */
@Composable
private fun SocialIconsRow(
    onGoogleClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val comingSoon = { provider: String ->
        Toast.makeText(context, "Login com $provider em breve", Toast.LENGTH_SHORT).show()
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
    ) {
        SocialLoginButton(
            iconRes = R.drawable.google,
            iconSize = 28.dp,
            contentDescription = "Entrar com o Google",
            enabled = enabled,
            onClick = onGoogleClick
        )
        SocialLoginButton(
            iconRes = R.drawable.facebook,
            iconSize = 42.dp,
            contentDescription = "Entrar com o Facebook",
            enabled = enabled,
            onClick = { comingSoon("Facebook") }
        )
        SocialLoginButton(
            iconRes = R.drawable.microsoft,
            iconSize = 40.dp,
            contentDescription = "Entrar com a Microsoft",
            enabled = enabled,
            onClick = { comingSoon("Microsoft") }
        )
    }
}

/** Botão quadrado de login social (fundo branco + borda), igual ao design de referência. */
@Composable
private fun SocialLoginButton(
    iconRes: Int,
    iconSize: Dp,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .size(70.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(OliusSuperficie)
            .border(BorderStroke(1.dp, OliusCampoBorda), RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(painter = painterResource(iconRes), contentDescription = contentDescription, modifier = Modifier.size(iconSize))
    }
}

// Cor do texto que a pessoa DIGITA nos campos de input (não é o placeholder,
// que continua com OliusTextoSecundario, cinza). É a mesma cor usada nos
// títulos dos cards ("Crie sua Conta" etc) — troque só aqui pra mudar nos
// dois lugares de uma vez, ou troque só o valor de OliusTextoPrimario (em
// Color.kt) se quiser mudar em TODO o app de uma vez só.
private val AUTH_FIELD_TEXT_COLOR: Color
    @Composable @ReadOnlyComposable get() = OliusTextoPrimario

/**
 * Cores compartilhadas pelos 3 campos abaixo: sem fundo/caixa visível em
 * repouso (transparente, funde com o card), uma linha fina embaixo
 * (`indicatorColor`), e um fundo amarelo bem fraco quando o campo está em
 * foco (clicado) — igual referencia_input.png. `shape = RectangleShape`
 * tira o arredondado padrão do TextField (senão pareceria uma caixinha,
 * não só uma linha).
 */
@Composable
internal fun oliusFieldColors() = TextFieldDefaults.colors(
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
internal fun OliusTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    /** Ícone do Figma (drawable); null = sem ícone. O telefone usa o OliusPhoneField (PhoneInput.kt). */
    @DrawableRes leadingIconRes: Int?,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    /** Mensagem embaixo do campo (ex.: "CPF inválido"); null = sem erro. */
    error: String? = null,
    /** Máscara só visual (CPF, CNPJ, CEP): [value] continua só com os dígitos. */
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = OliusTextoSecundario) },
        leadingIcon = leadingIconRes?.let { icon ->
            { Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(16.dp)) }
        },
        isError = error != null,
        supportingText = error?.let { message -> { Text(message) } },
        singleLine = true,
        shape = RectangleShape,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = oliusFieldColors()
    )
}

@Composable
internal fun OliusPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Senha",
    error: String? = null,
    onFocusChange: (Boolean) -> Unit = {}
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { onFocusChange(it.isFocused) },
        placeholder = { Text(placeholder, color = OliusTextoSecundario) },
        leadingIcon = { Image(painterResource(R.drawable.ic_field_lock), contentDescription = null, modifier = Modifier.size(16.dp)) },
        trailingIcon = {
            // O Figma só tem o olho cortado; com a senha visível ele fica amarelo.
            IconButton(onClick = onToggleVisible) {
                Image(
                    painter = painterResource(R.drawable.ic_eye_off),
                    contentDescription = if (visible) "Ocultar senha" else "Mostrar senha",
                    colorFilter = if (visible) ColorFilter.tint(OliusAmarelo) else null,
                    modifier = Modifier.size(16.dp)
                )
            }
        },
        isError = error != null,
        supportingText = error?.let { message -> { Text(message) } },
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
        leadingIcon = { Image(painterResource(R.drawable.ic_field_calendar), contentDescription = null, modifier = Modifier.size(16.dp)) },
        trailingIcon = {
            IconButton(onClick = { showPicker = true }) {
                Image(painterResource(R.drawable.ic_field_calendar_end), contentDescription = "Abrir calendário", modifier = Modifier.size(16.dp))
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
        phoneCountries = { emptyList() },
        onSelectPhoneCountry = {},
        onUpdateRegisterPassword = {},
        onToggleRegisterPasswordVisibility = {},
        onLoginContinue = {},
        onRegisterContinue = {},
        onGoogleSignInClick = {},
        onForgotPassword = {},
        onGoToRegister = {},
        onGoToLogin = {},
        accountForm = AccountFormActions()
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
            leadingIconRes = R.drawable.ic_field_mail
        )
        OliusTextField(
            value = "Texto digitado",
            onValueChange = {},
            placeholder = "Placeholder",
            leadingIconRes = R.drawable.ic_field_user
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
