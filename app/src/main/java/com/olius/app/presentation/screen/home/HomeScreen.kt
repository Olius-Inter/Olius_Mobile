package com.olius.app.presentation.screen.home

import androidx.compose.foundation.layout.PaddingValues
import com.olius.app.domain.entity.CollectorRequest
import com.olius.app.domain.entity.PickupRequestSummary
import com.olius.app.domain.entity.Tip
import com.olius.app.presentation.components.OliusIconTab
import com.olius.app.presentation.components.OliusIconTabs
import com.olius.app.presentation.components.PevPointCard
import com.olius.app.presentation.theme.OliusLaranjaChipFundo
import com.olius.app.presentation.theme.OliusLaranjaVivo
import com.olius.app.presentation.theme.OliusSeloAvisoFundo
import com.olius.app.presentation.theme.OliusTextoCard
import com.olius.app.presentation.theme.OliusTextoCardSecundario
import com.olius.app.presentation.theme.OliusVermelhoDescartar
import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.graphics.Brush
import com.olius.app.domain.entity.ProfileType
import com.olius.app.presentation.theme.OliusLaranjaGradiente
import com.olius.app.presentation.theme.OliusTituloMarrom
import com.olius.app.presentation.theme.OliusTituloMarromClaro
import androidx.compose.animation.AnimatedVisibility
import android.provider.Settings
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.NotificationsNone
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
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
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
import com.olius.app.presentation.components.BOTTOM_NAV_CONTENT_PADDING
import com.olius.app.presentation.components.OliusTabScaffold
import com.olius.app.presentation.components.OliusTopBarState
import com.olius.app.presentation.theme.OliusAmarelo
import com.olius.app.presentation.theme.OliusTextoPrimarioClaro
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
import com.olius.app.presentation.theme.OliusSuperficie
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val SHEET_ENTER_DURATION_MS = 840
private const val SHEET_EXIT_DURATION_MS = 300

private val NOTIFICATIONS_LIST_HEIGHT = 440.dp

private const val STAT_COUNT_DURATION_MS = 2000

/** Tela Home. */
@Composable
fun HomeScreen(
    topBarState: OliusTopBarState,
    viewModel: HomeViewModel = viewModel(),
    onOpenQrCode: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val isPickupEnabled = viewModel.canRequestPickup(state)
    val countMemory = rememberSaveable(saver = StatCountMemory.Saver) { StatCountMemory() }

    OliusTabScaffold(topBarState = topBarState) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            if (state.isOffline) {
                Spacer(Modifier.height(24.dp))
            } else {
                Spacer(Modifier.height(24.dp))
                WelcomeSection(userName = state.userName, onQrCodeClick = onOpenQrCode.takeIf { state.showQrCode })
                Spacer(Modifier.height(20.dp))
                when {
                    state.profileType == ProfileType.CITIZEN -> CitizenHomeContent(
                        state = state,
                        countMemory = countMemory,
                        onRegisterOil = viewModel::onRegisterOilClick.takeIf { state.isCollector },
                        onTabSelected = viewModel::onCitizenTabSelected
                    )
                    else -> {
                        ActionButtonsRow(
                            isPickupEnabled = isPickupEnabled,
                            onRegisterOil = viewModel::onRegisterOilClick,
                            onRequestPickup = viewModel::onRequestPickupClick
                        )
                        if (state.isCollector) {
                            Spacer(Modifier.height(22.dp))
                            OliusIconTabs(
                                tabs = listOf(
                                    OliusIconTab(CollectorHomeTab.MY_DATA, "Meus Dados", R.drawable.ic_tab_my_data),
                                    OliusIconTab(CollectorHomeTab.REQUESTS, "Solicitações", R.drawable.ic_tab_requests)
                                ),
                                selected = state.collectorTab,
                                onSelect = viewModel::onCollectorTabSelected
                            )
                        }
                        if (state.isCollector && state.collectorTab == CollectorHomeTab.REQUESTS) {
                            Spacer(Modifier.height(16.dp))
                            CollectorRequestsList(
                                requests = state.collectorRequests,
                                onCollect = viewModel::onCollectRequest,
                                onDiscard = viewModel::onDiscardRequest
                            )
                        } else {
                            EstablishmentDashboard(state = state, isPickupEnabled = isPickupEnabled, countMemory = countMemory)
                        }
                    }
                }
            }
            Spacer(Modifier.height(BOTTOM_NAV_CONTENT_PADDING))
        }
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
}

/** Bottom sheet de Notificações. */
@Composable
fun HomeOverlaySheets(viewModel: HomeViewModel) {
    val state by viewModel.uiState.collectAsState()

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

@Composable
private fun WelcomeSection(userName: String, onQrCodeClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Olá, $userName!", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = OliusTextoPrimario)
            var hour by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
            LaunchedEffect(Unit) {
                while (true) {
                    delay(60_000)
                    hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                }
            }
            Text(greetingSubtitle(hour), fontSize = 14.sp, color = OliusTextoSecundario)
        }
        if (onQrCodeClick != null) {
            Button(
                onClick = onQrCodeClick,
                modifier = Modifier.height(42.dp),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OliusAmarelo, contentColor = OliusTituloMarromClaro)
            ) {
                Image(painterResource(R.drawable.ic_qr_code), contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(10.dp))
                Text("QR Code", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/** 5h–11h bom dia, 12h–17h boa tarde, 18h–4h boa noite (madrugada não é "bom dia"). */
internal fun greetingSubtitle(hour: Int): String = when (hour) {
    in 5..11 -> "Bom dia!"
    in 12..17 -> "Boa tarde!"
    else -> "Boa noite!"
}

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

@Composable
private fun StatsGrid(state: HomeUiState, countMemory: StatCountMemory, modifier: Modifier = Modifier) {
    ImpactStatsGrid(
        oilLiters = state.oilCollectedLiters,
        waterLiters = state.waterPreservedLiters,
        co2Kg = state.co2AvoidedKg,
        biodieselLiters = state.biodieselLiters,
        modifier = modifier,
        countMemory = countMemory
    )
}

@Composable
internal fun ImpactStatsGrid(
    oilLiters: Double,
    waterLiters: Double,
    co2Kg: Double,
    biodieselLiters: Double,
    modifier: Modifier = Modifier,
    countMemory: StatCountMemory? = null
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OilAndWaterStats(oilLiters, waterLiters, countMemory)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                iconRes = R.drawable.grass,
                label = "CO² Evitado",
                value = countingText("co2", co2Kg, compact = false, memory = countMemory),
                unit = "Kg",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                iconRes = R.drawable.fire,
                label = "Biodiesel",
                value = countingText("biodiesel", biodieselLiters, compact = false, memory = countMemory),
                unit = "L",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RowScope.OilAndWaterStats(oilLiters: Double, waterLiters: Double, countMemory: StatCountMemory?) {
    StatCard(
        iconRes = R.drawable.oil,
        label = "Óleo Coletado",
        value = countingText("oil", oilLiters, compact = false, memory = countMemory),
        unit = "L",
        modifier = Modifier.weight(1f)
    )
    StatCard(
        iconRes = R.drawable.water,
        label = "Água Preservada",
        value = countingText("water", waterLiters, compact = true, memory = countMemory),
        unit = "L",
        modifier = Modifier.weight(1f)
    )
}

/** Último valor que cada card já mostrou ("oil", "water", "co2", "biodiesel"). */
internal class StatCountMemory(initial: Map<String, Double> = emptyMap()) {
    private val shown = HashMap(initial)

    fun lastShown(key: String): Double = shown[key] ?: 0.0

    fun markShown(key: String, value: Double) {
        shown[key] = value
    }

    companion object {
        val Saver = Saver<StatCountMemory, HashMap<String, Double>>(
            save = { HashMap(it.shown) },
            restore = { StatCountMemory(it) }
        )
    }
}

/** Texto do card contando do último valor mostrado (0 na primeira vez) até [target]. */
@Composable
private fun countingText(key: String, target: Double, compact: Boolean, memory: StatCountMemory?): String {
    if (memory == null) return if (compact) formatCompact(target) else formatNumber(target)
    val context = LocalContext.current
    val animationsOff = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    var shown by remember(key) { mutableDoubleStateOf(memory.lastShown(key)) }
    LaunchedEffect(key, target) {
        if (!animationsOff && shown != target) {
            val from = shown
            Animatable(0f).animateTo(1f, tween(STAT_COUNT_DURATION_MS, easing = FastOutSlowInEasing)) {
                shown = from + (target - from) * value
            }
        }
        shown = target
        memory.markShown(key, target)
    }
    return formatCounting(shown, target, compact)
}

@Composable
private fun EstablishmentDashboard(state: HomeUiState, isPickupEnabled: Boolean, countMemory: StatCountMemory) {
    Spacer(Modifier.height(22.dp))
    Text(state.referenceMonthLabel, fontSize = 13.sp, color = OliusTextoSecundario)
    Spacer(Modifier.height(10.dp))
    StatsGrid(state = state, countMemory = countMemory)
    Spacer(Modifier.height(16.dp))
    OilLevelCard(state = state, isPickupEnabled = isPickupEnabled)
    if (state.pendingRequests.isNotEmpty()) {
        Spacer(Modifier.height(20.dp))
        PendingRequestsSection(requests = state.pendingRequests)
    }
    Spacer(Modifier.height(16.dp))
    SustainabilityLevelCard(state = state)
}

/** "Solicitações de Coletas": volume, data e chip "Pendente". */
@Composable
private fun PendingRequestsSection(requests: List<PickupRequestSummary>) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.ic_truck_dark), contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text("Solicitações de Coletas", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = OliusTextoCard)
    }
    Spacer(Modifier.height(10.dp))
    requests.forEach { request ->
        Surface(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            shape = RoundedCornerShape(10.dp),
            color = OliusSuperficie,
            border = BorderStroke(1.dp, OliusCampoBorda)
        ) {
            Row(Modifier.padding(horizontal = 21.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${formatNumber(request.liters)}L", fontSize = 18.sp, fontWeight = FontWeight.Light, color = OliusTextoCard)
                    Text(request.dateLabel, fontSize = 12.sp, color = OliusTextoCardSecundario)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(OliusLaranjaChipFundo)
                        .border(1.dp, OliusLaranjaVivo, RoundedCornerShape(10.dp))
                        .padding(horizontal = 18.dp, vertical = 4.dp)
                ) {
                    Text(request.status.label, fontSize = 12.sp, color = OliusLaranjaVivo)
                }
            }
        }
    }
}

/** Aba "Solicitações" do Coletor: Coletar ou Descartar. */
@Composable
private fun CollectorRequestsList(
    requests: List<CollectorRequest>,
    onCollect: (CollectorRequest) -> Unit,
    onDiscard: (CollectorRequest) -> Unit
) {
    if (requests.isEmpty()) {
        Text(
            "Nenhuma solicitação no momento.",
            fontSize = 13.sp,
            color = OliusTextoSecundario,
            modifier = Modifier.padding(vertical = 24.dp)
        )
    }
    requests.forEach { request ->
        Surface(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(9.dp),
            color = OliusSuperficie,
            border = BorderStroke(1.dp, OliusCampoBorda)
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                Row {
                    Column(Modifier.weight(1f)) {
                        Text(request.establishmentName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = OliusTextoCard)
                        Text(request.dateLabel, fontSize = 10.sp, color = OliusTextoCardSecundario)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(formatNumber(request.liters), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = OliusAmarelo)
                            Text("L", fontSize = 11.sp, color = OliusTextoCardSecundario)
                        }
                        Text("Óleo para coletar", fontSize = 10.sp, color = OliusTextoCardSecundario)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onCollect(request) },
                        modifier = Modifier.weight(1f).height(32.dp),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OliusAmarelo, contentColor = OliusTextoPrimarioClaro)
                    ) { Text("Coletar", fontSize = 12.sp) }
                    Button(
                        onClick = { onDiscard(request) },
                        modifier = Modifier.weight(1f).height(32.dp),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OliusVermelhoDescartar, contentColor = Color.White)
                    ) { Text("Descartar", fontSize = 12.sp) }
                }
            }
        }
    }
}

/** B2C: sem "Solicitar coleta", tanque, CO² e biodiesel (o cidadão entrega no PEV). */
@Composable
private fun CitizenHomeContent(
    state: HomeUiState,
    countMemory: StatCountMemory,
    onRegisterOil: (() -> Unit)?,
    onTabSelected: (CitizenHomeTab) -> Unit
) {
    CitizenBanner(onRegisterOil = onRegisterOil)
    if (state.currentLevelProgressLiters > 0.0) {
        Spacer(Modifier.height(16.dp))
        CitizenLevelCard(state = state)
    }
    Spacer(Modifier.height(22.dp))
    Text(state.referenceMonthLabel, fontSize = 13.sp, color = OliusTextoSecundario)
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OilAndWaterStats(state.oilCollectedLiters, state.waterPreservedLiters, countMemory)
    }
    Spacer(Modifier.height(20.dp))
    OliusIconTabs(
        tabs = listOf(
            OliusIconTab(CitizenHomeTab.PEV_POINTS, "Pontos PEV", R.drawable.ic_tab_pin),
            OliusIconTab(CitizenHomeTab.TIPS, "Dicas", R.drawable.ic_tab_book)
        ),
        selected = state.citizenTab,
        onSelect = onTabSelected
    )
    Spacer(Modifier.height(16.dp))
    when (state.citizenTab) {
        CitizenHomeTab.PEV_POINTS -> state.nearbyPoints.forEach { point ->
            PevPointCard(point = point, modifier = Modifier.padding(bottom = 16.dp))
        }
        CitizenHomeTab.TIPS -> state.tips.forEach { tip -> TipCard(tip) }
    }
}

@Composable
private fun TipCard(tip: Tip) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 9.dp),
        shape = RoundedCornerShape(10.dp),
        color = OliusSuperficie,
        border = BorderStroke(1.dp, OliusCampoBorda)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.ic_star_outline), contentDescription = null, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(8.dp))
                Text(tip.title, fontSize = 12.sp, color = OliusTextoCard)
            }
            Spacer(Modifier.height(6.dp))
            Text(tip.description, fontSize = 11.sp, color = OliusTextoCardSecundario)
        }
    }
}

/** [onRegisterOil] null = quem não é PEV: o banner só orienta a levar o óleo a um ponto. */
@Composable
private fun CitizenBanner(onRegisterOil: (() -> Unit)?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(OliusAmarelo, OliusLaranjaGradiente)))
            .padding(20.dp)
    ) {
        Text("Faça a Diferença Hoje!", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = OliusTituloMarromClaro)
        Spacer(Modifier.height(6.dp))
        Text(
            if (onRegisterOil != null) "Registre seu óleo e ajude a preservar milhares de litros de água"
            else "Leve seu óleo usado a um ponto de coleta e ajude a preservar milhares de litros de água",
            fontSize = 12.sp,
            color = OliusTituloMarromClaro
        )
        if (onRegisterOil == null) return@Column
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRegisterOil,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.6f), contentColor = OliusTituloMarromClaro)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Registrar óleo", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CitizenLevelCard(state: HomeUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = OliusSuperficie,
        border = BorderStroke(1.dp, OliusCampoBorda)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painter = painterResource(R.drawable.ic_level_silver), contentDescription = "Selo do nível", modifier = Modifier.size(48.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Nível de sustentabilidade", fontSize = 11.sp, color = OliusTextoSecundario)
                Text(
                    state.levelName ?: "lv. ${state.sustainabilityLevel}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = OliusTextoSecundario
                )
            }
            Text(
                "${formatNumber(state.currentLevelProgressLiters)}L",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = OliusTextoPrimario
            )
        }
    }
}

/** Ícone grande e apagado (`alpha`) vazando pelo canto inferior direito, por baixo do conteúdo. */
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
        color = OliusSuperficie,
        border = BorderStroke(1.dp, OliusCampoBorda)
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

@Composable
private fun OilLevelCard(state: HomeUiState, isPickupEnabled: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = OliusSuperficie,
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
                    if (state.showPickupReadyBadge && isPickupEnabled) {
                        ReadyForPickupBadge()
                    } else if (state.oilAccumulatedLiters < state.minimumForPickupLiters) {
                        LevelNotReachedBadge()
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
        Image(painterResource(R.drawable.ic_check_circle), contentDescription = null, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text("Pronto para coleta", color = OliusVerdeSeloTexto, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LevelNotReachedBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(OliusSeloAvisoFundo)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painterResource(R.drawable.ic_warning_circle), contentDescription = null, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text("Nível não atingido", color = OliusLaranjaVivo, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Tanque de óleo: a gota é CINZA quando o tanque está vazio e BRANCA conforme o amarelo sobe. */
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
            .background(OliusSuperficie)
            .border(BorderStroke(1.5.dp, OliusCampoBorda), RoundedCornerShape(14.dp))
    ) {
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

@Composable
private fun SustainabilityLevelCard(state: HomeUiState, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = OliusSuperficie,
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

@Composable
private fun RegisterOilDialog(
    volumeInput: String,
    estimatedWaterLiters: Double,
    estimatedCo2Kg: Double,
    onVolumeChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    CenteredDialog(onDismiss = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = OliusSuperficie, modifier = Modifier.fillMaxWidth()) {
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
                        Text(formatBr(estimatedWaterLiters, maxDecimals = 0), color = OliusIconeAgua, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Litros de água preservada", color = OliusTextoSecundario, fontSize = 11.sp)
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text(formatBr(estimatedCo2Kg, maxDecimals = 2), color = OliusIconeVerde, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("kg de CO₂ evitados", color = OliusTextoSecundario, fontSize = 11.sp, textAlign = TextAlign.End)
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

/** Popup centralizado (Registrar Óleo / Solicitar Coleta). */
@Composable
private fun CenteredDialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onDismiss
                )
                .padding(horizontal = 34.dp),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

/** Cores dos campos dos popups (Registrar Óleo / Solicitar Coleta). */
@Composable
private fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = OliusTextoPrimario,
    unfocusedTextColor = OliusTextoPrimario,
    cursorColor = OliusTextoPrimario,
    focusedBorderColor = OliusAmarelo,
    unfocusedBorderColor = OliusCampoBorda,
    focusedContainerColor = OliusSuperficie,
    unfocusedContainerColor = OliusSuperficie
)

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
                    tint = OliusTextoPrimario,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onValueChange((((value.toIntOrNull() ?: 0) + 1).toString())) }
                )
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Diminuir volume",
                    tint = OliusTextoPrimario,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable {
                            val next = (value.toIntOrNull() ?: 0) - 1
                            onValueChange(if (next < 0) "0" else next.toString())
                        }
                )
            }
        },
        colors = dialogFieldColors()
    )
}

@Composable
private fun RequestPickupDialog(
    state: HomeUiState,
    onVolumeChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    CenteredDialog(onDismiss = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = OliusSuperficie, modifier = Modifier.fillMaxWidth()) {
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
                    colors = dialogFieldColors()
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
        colors = dialogFieldColors()
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
    if (notifications.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(NOTIFICATIONS_LIST_HEIGHT),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Outlined.NotificationsNone,
                contentDescription = null,
                tint = OliusSheetTextoSecundario,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text("Sem notificações", color = OliusSheetTextoSecundario, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(NOTIFICATIONS_LIST_HEIGHT)
                .verticalScroll(rememberScrollState())
        ) {
            notifications.forEach { item ->
                NotificationRow(item = item, onAction = onAction)
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            }
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

internal fun formatNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString()
    else String.format(Locale.US, "%.1f", value)

internal fun formatCompact(value: Double): String {
    if (value < 1000) return formatNumber(value)
    return String.format(Locale.US, "%.1fK", value / 1000.0)
}

/** Número durante a contagem com o MESMO formato do valor final. */
internal fun formatCounting(value: Double, target: Double, compact: Boolean): String {
    val inThousands = compact && target >= 1000
    val decimals = if (inThousands || target != Math.floor(target)) 1 else 0
    val shown = if (inThousands) value / 1000.0 else value
    return String.format(Locale.US, "%.${decimals}f", shown) + if (inThousands) "K" else ""
}

/** Formato brasileiro: 25000.0 → "25.000", 3.0375 → "3,04". */
internal fun formatBr(value: Double, maxDecimals: Int): String =
    NumberFormat.getNumberInstance(PT_BR_LOCALE).apply {
        maximumFractionDigits = maxDecimals
        minimumFractionDigits = 0
    }.format(value)

private val PT_BR_LOCALE: Locale = Locale.forLanguageTag("pt-BR")

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun HomeScreenPreview() {
    HomeScreen(topBarState = OliusTopBarState())
}
