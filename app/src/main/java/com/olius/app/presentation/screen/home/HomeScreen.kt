package com.olius.app.presentation.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.olius.app.R
import com.olius.app.presentation.theme.DecorativeBackground
import com.olius.app.presentation.theme.OliusAmarelo
import com.olius.app.presentation.theme.OliusBotaoInativoFundo
import com.olius.app.presentation.theme.OliusBotaoInativoTexto
import com.olius.app.presentation.theme.OliusCampoBorda
import com.olius.app.presentation.theme.OliusCampoFundo
import com.olius.app.presentation.theme.OliusIconeAgua
import com.olius.app.presentation.theme.OliusIconeVerde
import com.olius.app.presentation.theme.OliusLaranja
import com.olius.app.presentation.theme.OliusNavBarFundo
import com.olius.app.presentation.theme.OliusSheetFundo
import com.olius.app.presentation.theme.OliusSheetTextoSecundario
import com.olius.app.presentation.theme.OliusTanqueGotaVazia
import com.olius.app.presentation.theme.OliusTextoPrimario
import com.olius.app.presentation.theme.OliusTextoSecundario
import com.olius.app.presentation.theme.OliusVerdeSeloFundo
import com.olius.app.presentation.theme.OliusVerdeSeloTexto
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Duração da animação de entrada dos bottom sheets (Perfil/Notificações) —
// igual ao slide dos cards de login/cadastro em PerfilType.kt
// (HEADER_INTRO_DURATION_MS/AnimatedContent de lá usa 840ms com a mesma
// easing). Ver documentação em HOME_SCREEN.md pra reaproveitar em outro lugar.
private const val SHEET_ENTER_DURATION_MS = 840
private const val SHEET_EXIT_DURATION_MS = 300

/**
 * Tela Home (ver referencia_home.png / ref_home_cell_1.png / ref_home_cell_2.png).
 * Só monta a UI a partir do HomeUiState; toda a lógica de negócio (registrar
 * óleo, solicitar coleta, notificações, logout) fica no HomeViewModel — a
 * maior parte das funções lá são placeholders vazios de propósito (ver
 * HOME_SCREEN.md).
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onLogout: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val isPickupEnabled = viewModel.canRequestPickup(state)

    Box(modifier = Modifier.fillMaxSize()) {
        DecorativeBackground(isRow = true)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            HomeTopBar(
                hasUnreadNotifications = state.hasUnreadNotifications,
                onNotificationsClick = viewModel::onNotificationsClick,
                onProfileClick = viewModel::onProfileClick
            )
            Spacer(Modifier.height(24.dp))
            WelcomeSection(userName = state.userName)
            Spacer(Modifier.height(20.dp))
            ActionButtonsRow(
                isPickupEnabled = isPickupEnabled,
                onRegisterOil = viewModel::onRegisterOilClick,
                onRequestPickup = viewModel::onRequestPickupClick
            )
            Spacer(Modifier.height(22.dp))
            Text(state.referenceMonthLabel, fontSize = 13.sp, color = OliusTextoSecundario)
            Spacer(Modifier.height(10.dp))
            StatsGrid(state = state)
            Spacer(Modifier.height(16.dp))
            OilLevelCard(state = state)
            Spacer(Modifier.height(16.dp))
            SustainabilityLevelCard(state = state)
            // Espaço extra no final: o conteúdo agora rola por baixo do menu
            // inferior (efeito de vidro — ver MainScaffoldScreen.kt), então
            // esse respiro evita que o último card fique colado embaixo dele.
            Spacer(Modifier.height(110.dp))
        }

        if (state.isRegisterOilDialogOpen) {
            val volume = state.registerOilVolumeInput.toDoubleOrNull() ?: 0.0
            RegisterOilDialog(
                volumeInput = state.registerOilVolumeInput,
                estimatedWaterLiters = viewModel.estimateWaterPreserved(volume),
                estimatedCo2Kg = viewModel.estimateCo2Avoided(volume),
                onVolumeChange = viewModel::onRegisterOilVolumeChange,
                onConfirm = viewModel::onConfirmRegisterOil,
                onDismiss = viewModel::onDismissRegisterOilDialog
            )
        }

        if (state.isRequestPickupDialogOpen) {
            RequestPickupDialog(
                state = state,
                onVolumeChange = viewModel::onRequestPickupVolumeChange,
                onDateChange = viewModel::onRequestPickupDateChange,
                onNotesChange = viewModel::onRequestPickupNotesChange,
                onConfirm = viewModel::onConfirmRequestPickup,
                onDismiss = viewModel::onDismissRequestPickupDialog
            )
        }

        if (state.isProfileSheetOpen) {
            SlideUpDialog(onDismissRequest = viewModel::onDismissProfileSheet) { animatedDismiss ->
                ProfileSheetContent(
                    fullName = state.fullName,
                    accountSubtitle = state.accountSubtitle,
                    onAddAccount = viewModel::onAddAccountClick,
                    onLogout = {
                        viewModel.onLogoutClick()
                        onLogout()
                    },
                    onClose = animatedDismiss
                )
            }
        }

        if (state.isNotificationsSheetOpen) {
            SlideUpDialog(onDismissRequest = viewModel::onDismissNotificationsSheet) { animatedDismiss ->
                NotificationsSheetContent(
                    filter = state.notificationFilter,
                    notifications = state.filteredNotifications,
                    onFilterChange = viewModel::onNotificationFilterChange,
                    onAction = viewModel::onNotificationActionClick,
                    onClose = animatedDismiss
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Header: logo + "Home" + notificações + avatar
// ---------------------------------------------------------------------------

@Composable
private fun HomeTopBar(
    hasUnreadNotifications: Boolean,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.logo_sem_fundo),
            contentDescription = "Olius",
            modifier = Modifier.size(30.dp)
        )
        Text(
            text = "Home",
            fontSize = 14.sp,
            color = OliusTextoSecundario,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onNotificationsClick) {
            Image(
                painter = painterResource(
                    if (hasUnreadNotifications) R.drawable.notification_true else R.drawable.notification_false
                ),
                contentDescription = "Notificações",
                modifier = Modifier.size(22.dp)
            )
        }
        ProfileAvatar(onClick = onProfileClick)
    }
}

/**
 * TODO: trocar pelo avatar real do usuário (ex.: Coil + `profileImageUrl`)
 * quando existir integração com o backend. Por enquanto usa um placeholder
 * com iniciais/ícone, igual referência (círculo amarelo).
 */
@Composable
private fun ProfileAvatar(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(OliusAmarelo)
            .border(BorderStroke(2.dp, OliusAmarelo), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.Person, contentDescription = "Perfil", tint = Color.Black, modifier = Modifier.size(18.dp))
    }
}

// ---------------------------------------------------------------------------
// Saudação
// ---------------------------------------------------------------------------

@Composable
private fun WelcomeSection(userName: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text("Olá, $userName!", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = OliusTextoPrimario)
        Text(greetingSubtitle(), fontSize = 14.sp, color = OliusTextoSecundario)
    }
}

private fun greetingSubtitle(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Bom dia!"
        hour < 18 -> "Boa tarde!"
        else -> "Boa noite!"
    }
}

// ---------------------------------------------------------------------------
// Botões de ação: Registrar óleo / Solicitar coleta
// ---------------------------------------------------------------------------

/**
 * Cores iguais às de ref_home_cell_1.png / referencia_home.png: "Registrar
 * óleo" sempre amarelo; "Solicitar coleta" fica LARANJA quando habilitado e
 * CINZA quando não (ver [HomeViewModel.canRequestPickup]).
 */
@Composable
private fun ActionButtonsRow(
    isPickupEnabled: Boolean,
    onRegisterOil: () -> Unit,
    onRequestPickup: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onRegisterOil,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OliusAmarelo, contentColor = Color.Black)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Registrar óleo", fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        Button(
            onClick = onRequestPickup,
            enabled = isPickupEnabled,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = OliusLaranja,
                contentColor = Color.White,
                disabledContainerColor = OliusBotaoInativoFundo,
                disabledContentColor = OliusBotaoInativoTexto
            )
        ) {
            Image(
                painter = painterResource(R.drawable.truck),
                contentDescription = null,
                colorFilter = ColorFilter.tint(if (isPickupEnabled) Color.White else OliusBotaoInativoTexto),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text("Solicitar coleta", fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

// ---------------------------------------------------------------------------
// Grid 2x2 de indicadores
// ---------------------------------------------------------------------------

@Composable
private fun StatsGrid(state: HomeUiState, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                iconRes = R.drawable.water,
                label = "Água Preservada",
                value = formatCompact(state.waterPreservedLiters),
                unit = "L",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                iconRes = R.drawable.oil,
                label = "Óleo Coletado",
                value = formatNumber(state.oilCollectedLiters),
                unit = "L",
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                iconRes = R.drawable.grass,
                label = "CO² Evitado",
                value = formatNumber(state.co2AvoidedKg),
                unit = "Kg",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                iconRes = R.drawable.fire,
                label = "Biodiesel",
                value = formatNumber(state.biodieselLiters),
                unit = "L",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Ícone grande e apagado (`alpha`) vazando pelo canto inferior direito, por
 * baixo do conteúdo — igual às referências (ref_home_cell_1.png): cada card
 * tem uma marca d'água do próprio ícone, bem maior e bem mais clara que o
 * ícone pequeno ao lado do rótulo. O `Surface` do card recorta (`shape`)
 * a parte que vaza pra fora, então não precisa de clip manual aqui.
 */
@Composable
private fun StatCard(
    iconRes: Int,
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Box {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 16.dp, y = 16.dp)
                    .size(88.dp)
                    .alpha(0.18f)
            )
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painter = painterResource(iconRes), contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(label, fontSize = 12.sp, color = OliusTextoSecundario, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = OliusTextoPrimario)
                    Text(
                        unit,
                        fontSize = 13.sp,
                        color = OliusTextoSecundario,
                        modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Nível de óleo acumulado (tanque + selo "Pronto para coleta")
// ---------------------------------------------------------------------------

@Composable
private fun OilLevelCard(state: HomeUiState, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, OliusCampoBorda)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Nível de óleo acumulado", fontSize = 13.sp, color = OliusTextoSecundario)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OilTankGauge(fraction = state.oilTankFraction)
                Spacer(Modifier.width(20.dp))
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            formatNumber(state.oilAccumulatedLiters),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = OliusTextoPrimario
                        )
                        Text(
                            "L",
                            fontSize = 15.sp,
                            color = OliusTextoSecundario,
                            modifier = Modifier.padding(start = 2.dp, bottom = 3.dp)
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Mínimo para coleta ${formatNumber(state.minimumForPickupLiters)}L",
                        fontSize = 12.sp,
                        color = OliusTextoSecundario
                    )
                    Spacer(Modifier.height(10.dp))
                    if (state.showPickupReadyBadge) {
                        ReadyForPickupBadge()
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadyForPickupBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(OliusVerdeSeloFundo)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = OliusVerdeSeloTexto, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text("Pronto para coleta", color = OliusVerdeSeloTexto, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Tanque de óleo (ver oil_drop.png): a gota é CINZA quando o tanque está
 * vazio e BRANCA conforme o amarelo sobe — se estiver na metade, metade da
 * gota (a parte "dentro" do amarelo) fica branca e a outra metade cinza.
 * Isso é feito desenhando a gota cinza por baixo e uma cópia branca por
 * cima, recortada (clipToBounds) na mesma fração do preenchimento.
 */
@Composable
private fun OilTankGauge(fraction: Float, modifier: Modifier = Modifier) {
    val animatedFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "oilTankFill"
    )

    Box(
        modifier = modifier
            .size(width = 84.dp, height = 116.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(BorderStroke(1.5.dp, OliusCampoBorda), RoundedCornerShape(14.dp))
    ) {
        // Preenchimento amarelo subindo de baixo pra cima, de acordo com o nível.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(animatedFraction)
                .align(Alignment.BottomCenter)
                .background(OliusAmarelo)
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(42.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.oil_drop),
                contentDescription = null,
                colorFilter = ColorFilter.tint(OliusTanqueGotaVazia),
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(animatedFraction)
                    .align(Alignment.BottomCenter)
                    .clipToBounds()
            ) {
                Image(
                    painter = painterResource(R.drawable.oil_drop),
                    contentDescription = "Nível de óleo acumulado",
                    colorFilter = ColorFilter.tint(Color.White),
                    modifier = Modifier
                        .size(42.dp)
                        .align(Alignment.BottomCenter)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Nível de sustentabilidade (selo + barra de progresso)
// ---------------------------------------------------------------------------

@Composable
private fun SustainabilityLevelCard(state: HomeUiState, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, OliusCampoBorda)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Nível de sustentabilidade", fontSize = 13.sp, color = OliusTextoSecundario)
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(OliusNavBarFundo),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.level),
                        contentDescription = "Nível de sustentabilidade",
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Text("lv. ${state.sustainabilityLevel}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = OliusTextoPrimario)
            }
            Spacer(Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${formatNumber(state.currentLevelProgressLiters)}L coletados", fontSize = 11.sp, color = OliusTextoSecundario)
                Text("${formatNumber(state.nextLevelTargetLiters)}L para o próximo nível", fontSize = 11.sp, color = OliusTextoSecundario)
            }
            Spacer(Modifier.height(10.dp))
            LevelProgressBar(
                fraction = state.levelProgressFraction,
                targetLabel = "${formatNumber(state.nextLevelTargetLiters)}L"
            )
        }
    }
}

/**
 * Barra bem mais grossa que uma `LinearProgressIndicator` comum, igual à
 * referência (referencia_home.png) — e com o rótulo do total (`targetLabel`,
 * ex.: "100L") escrito DENTRO da própria barra, alinhado à direita, em vez
 * de só no texto acima.
 */
@Composable
private fun LevelProgressBar(fraction: Float, targetLabel: String, modifier: Modifier = Modifier) {
    val animatedFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "levelProgress"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(30.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(OliusCampoFundo)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedFraction)
                .clip(RoundedCornerShape(15.dp))
                .background(OliusAmarelo)
        )
        Text(
            targetLabel,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = OliusTextoSecundario,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// Popup: Registrar Óleo (ver ref_register_oil.png)
// ---------------------------------------------------------------------------

@Composable
private fun RegisterOilDialog(
    volumeInput: String,
    estimatedWaterLiters: Double,
    estimatedCo2Kg: Double,
    onVolumeChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White, modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
            Column(Modifier.padding(24.dp)) {
                Text(
                    "Registrar Óleo",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = OliusTextoPrimario,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(22.dp))
                Text("Volume de Óleo (Litros)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OliusTextoPrimario)
                Spacer(Modifier.height(8.dp))
                NumericStepperField(value = volumeInput, onValueChange = onVolumeChange)
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(OliusCampoFundo)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(formatThousands(estimatedWaterLiters), color = OliusIconeAgua, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Litros de água preservada", color = OliusTextoSecundario, fontSize = 11.sp)
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text(formatThousands(estimatedCo2Kg), color = OliusIconeVerde, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Kg de CO² evitados", color = OliusTextoSecundario, fontSize = 11.sp, textAlign = TextAlign.End)
                    }
                }
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OliusAmarelo, contentColor = Color.Black)
                ) {
                    Text("Registrar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun NumericStepperField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> if (input.isEmpty() || input.all { it.isDigit() }) onValueChange(input) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        trailingIcon = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.KeyboardArrowUp,
                    contentDescription = "Aumentar volume",
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onValueChange((((value.toIntOrNull() ?: 0) + 1).toString())) }
                )
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Diminuir volume",
                    modifier = Modifier
                        .size(18.dp)
                        .clickable {
                            val next = (value.toIntOrNull() ?: 0) - 1
                            onValueChange(if (next < 0) "0" else next.toString())
                        }
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = OliusAmarelo,
            unfocusedBorderColor = OliusCampoBorda,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        )
    )
}

// ---------------------------------------------------------------------------
// Popup: Solicitar Coleta (ver ref_solicitar_coleta.png)
// ---------------------------------------------------------------------------

@Composable
private fun RequestPickupDialog(
    state: HomeUiState,
    onVolumeChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White, modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
            Column(Modifier.padding(24.dp)) {
                Box(Modifier.fillMaxWidth()) {
                    Text(
                        "Solicitar Coleta",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = OliusTextoPrimario,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterEnd)) {
                        Icon(Icons.Filled.Close, contentDescription = "Fechar", tint = OliusTextoSecundario)
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text("Volume Estimado (Litros)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OliusTextoPrimario)
                Spacer(Modifier.height(8.dp))
                NumericStepperField(value = state.requestPickupVolumeInput, onValueChange = onVolumeChange)
                Spacer(Modifier.height(16.dp))
                Text("Data Preferencial", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OliusTextoPrimario)
                Spacer(Modifier.height(8.dp))
                BorderedDateField(value = state.requestPickupDateInput, onValueChange = onDateChange)
                Spacer(Modifier.height(16.dp))
                Text("Observações", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OliusTextoPrimario)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.requestPickupNotesInput,
                    onValueChange = onNotesChange,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OliusAmarelo,
                        unfocusedBorderColor = OliusCampoBorda,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OliusAmarelo, contentColor = Color.Black)
                ) {
                    Text("Registrar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BorderedDateField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var showPicker by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        modifier = modifier
            .fillMaxWidth()
            .clickable { showPicker = true },
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        trailingIcon = {
            IconButton(onClick = { showPicker = true }) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = "Abrir calendário", tint = OliusTextoSecundario)
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = OliusAmarelo,
            unfocusedBorderColor = OliusCampoBorda,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        )
    )

    if (showPicker) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))
                        onValueChange(formatter.format(Date(millis)))
                    }
                    showPicker = false
                }) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancelar") } }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

// ---------------------------------------------------------------------------
// Bottom sheet genérico animado de baixo pra cima (Perfil/Notificações) —
// mesma animação de entrada usada pelos cards de login/cadastro em
// PerfilType.kt (slideInVertically + fadeIn, tween ~840ms, FastOutSlowInEasing).
// Implementado com Dialog (não ModalBottomSheet padrão) pra reaproveitar
// exatamente essa animação e cobrir a tela inteira, inclusive por cima do
// menu inferior fixo — ver HOME_SCREEN.md.
// ---------------------------------------------------------------------------

@Composable
private fun SlideUpDialog(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.(animatedDismiss: () -> Unit) -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { visible = true }

    val animatedDismiss: () -> Unit = {
        visible = false
        scope.launch {
            delay(SHEET_EXIT_DURATION_MS.toLong())
            onDismissRequest()
        }
    }

    Dialog(onDismissRequest = animatedDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400)),
                exit = fadeOut(tween(250))
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = animatedDismiss
                        )
                )
            }
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(
                    animationSpec = tween(SHEET_ENTER_DURATION_MS, easing = FastOutSlowInEasing)
                ) { fullHeight -> fullHeight } + fadeIn(tween(350)),
                exit = slideOutVertically(animationSpec = tween(SHEET_EXIT_DURATION_MS)) { fullHeight -> fullHeight } + fadeOut(tween(200)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    color = OliusSheetFundo,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 22.dp, vertical = 24.dp)
                    ) {
                        content(animatedDismiss)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Bottom sheet: Perfil / Configurações (ver ref_perfil.png)
// ---------------------------------------------------------------------------

@Composable
private fun ProfileSheetContent(
    fullName: String,
    accountSubtitle: String,
    onAddAccount: () -> Unit,
    onLogout: () -> Unit,
    onClose: () -> Unit
) {
    Box(Modifier.fillMaxWidth()) {
        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Fechar", tint = Color.White)
        }
        Text(
            "Configurações",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.align(Alignment.Center)
        )
    }
    Spacer(Modifier.height(24.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(OliusAmarelo),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Person, contentDescription = null, tint = Color.Black, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(fullName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(accountSubtitle, color = OliusSheetTextoSecundario, fontSize = 11.sp)
        }
    }
    Spacer(Modifier.height(20.dp))
    HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
    Spacer(Modifier.height(20.dp))
    Text("Outras Contas", color = OliusSheetTextoSecundario, fontSize = 12.sp)
    Spacer(Modifier.height(12.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onAddAccount)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text("Adicionar conta", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(
                "Crie ou adicione uma nova conta Olius para gerir melhor seu descarte",
                color = OliusSheetTextoSecundario,
                fontSize = 11.sp
            )
        }
    }
    Spacer(Modifier.height(32.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .clickable(onClick = onLogout)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Sair do aplicativo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

// ---------------------------------------------------------------------------
// Bottom sheet: Notificações (ver ref_notification.png)
// ---------------------------------------------------------------------------

@Composable
private fun NotificationsSheetContent(
    filter: NotificationFilter,
    notifications: List<NotificationItem>,
    onFilterChange: (NotificationFilter) -> Unit,
    onAction: (NotificationItem) -> Unit,
    onClose: () -> Unit
) {
    Box(Modifier.fillMaxWidth()) {
        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Fechar", tint = Color.White)
        }
        Text(
            "Notificações",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.align(Alignment.Center)
        )
    }
    Spacer(Modifier.height(18.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FilterChipButton("Todas", filter == NotificationFilter.TODAS) { onFilterChange(NotificationFilter.TODAS) }
        FilterChipButton("Coletas", filter == NotificationFilter.COLETAS) { onFilterChange(NotificationFilter.COLETAS) }
        FilterChipButton("Frotas", filter == NotificationFilter.FROTAS) { onFilterChange(NotificationFilter.FROTAS) }
    }
    Spacer(Modifier.height(16.dp))
    HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 440.dp)
            .verticalScroll(rememberScrollState())
    ) {
        notifications.forEach { item ->
            NotificationRow(item = item, onAction = onAction)
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
        }
    }
}

@Composable
private fun FilterChipButton(text: String, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) OliusAmarelo else Color.White.copy(alpha = 0.08f)
    val foreground = if (selected) Color.Black else Color.White
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text, color = foreground, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun NotificationRow(item: NotificationItem, onAction: (NotificationItem) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f))
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text(item.description, color = OliusSheetTextoSecundario, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            if (item.hasAction) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .border(BorderStroke(1.dp, OliusAmarelo), RoundedCornerShape(20.dp))
                        .clickable { onAction(item) }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Conferir", color = OliusAmarelo, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(6.dp))
            }
            Text(item.timeAgo, color = OliusSheetTextoSecundario, fontSize = 10.sp)
        }
    }
}

// ---------------------------------------------------------------------------
// Formatação numérica — usa ponto decimal (não vírgula) de propósito, pra
// bater visualmente com as referências ("625.9K", "50.3"), mesmo com o resto
// do app em pt-BR. Ver HOME_SCREEN.md.
// ---------------------------------------------------------------------------

private fun formatNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString()
    else String.format(Locale.US, "%.1f", value)

private fun formatCompact(value: Double): String {
    if (value < 1000) return formatNumber(value)
    return String.format(Locale.US, "%.1fK", value / 1000.0)
}

private fun formatThousands(value: Double): String = String.format(Locale.US, "%.3f", value)

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun HomeScreenPreview() {
    HomeScreen()
}
