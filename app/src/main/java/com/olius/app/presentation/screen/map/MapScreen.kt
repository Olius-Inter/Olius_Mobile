package com.olius.app.presentation.screen.map

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.olius.app.R
import com.olius.app.data.local.SharedPrefsCollectorPromptStore
import com.olius.app.data.remote.AppContainer
import com.olius.app.domain.entity.CollectionPoint
import com.olius.app.domain.entity.CollectionPointDetails
import com.olius.app.domain.entity.CollectionPointType
import com.olius.app.domain.entity.PointSuggestion
import com.olius.app.presentation.components.BOTTOM_NAV_CONTENT_PADDING
import com.olius.app.presentation.components.OliusPageTitle
import com.olius.app.presentation.components.OliusTabScaffold
import com.olius.app.presentation.components.OliusTopBarState
import com.olius.app.presentation.theme.OliusAmarelo
import com.olius.app.presentation.theme.OliusAmareloBorda
import com.olius.app.presentation.theme.OliusAmareloFundoSuave
import com.olius.app.presentation.theme.OliusCampoBorda
import com.olius.app.presentation.theme.OliusMarromAviso
import com.olius.app.presentation.theme.OliusTextoPrimario
import com.olius.app.presentation.theme.OliusTextoPrimarioClaro
import com.olius.app.presentation.theme.OliusTextoSecundario
import com.olius.app.presentation.theme.OliusVerdeSeloTexto
import com.olius.app.presentation.theme.OliusSuperficie
import kotlinx.coroutines.launch
import java.util.Locale

private val MAP_HEIGHT = 320.dp
private val ClosedRed = Color(0xFFD64545)
private val WhatsAppGreen = Color(0xFF25D366)

/** Tela "Pontos de Coleta": mapa real (OpenStreetMap). */
@Composable
fun MapScreen(
    topBarState: OliusTopBarState,
    isCollector: Boolean = false,
    onBecomeCollector: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: MapViewModel = viewModel {
        MapViewModel(
            promptStore = SharedPrefsCollectorPromptStore(context),
            isCollector = isCollector,
            repository = AppContainer.collectionPointRepository
        )
    }
    val state by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    val locateUser: () -> Unit = {
        scope.launch {
            val location = currentDeviceLocation(context)
            if (location != null) viewModel.onUserLocation(location.latitude, location.longitude)
            else Toast.makeText(context, "Não foi possível obter sua localização. O GPS está ligado?", Toast.LENGTH_SHORT).show()
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) locateUser()
    }
    val onMyLocationClick: () -> Unit = {
        if (hasLocationPermission(context)) locateUser() else permissionLauncher.launch(LOCATION_PERMISSIONS)
    }
    LaunchedEffect(Unit) {
        if (hasLocationPermission(context) && state.userLocation == null) locateUser()
    }

    OliusTabScaffold(topBarState = topBarState) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(24.dp))
            OliusPageTitle(
                title = "Pontos de Coleta",
                subtitle = "Veja os pontos de coletas voluntárias e/ou vire um coletor voluntário."
            )
            AnimatedVisibility(
                visible = state.showBecomeCollectorBanner,
                exit = fadeOut() + shrinkVertically()
            ) {
                BecomeCollectorBanner(
                    onClick = onBecomeCollector,
                    onDismiss = viewModel::onDismissBanner,
                    modifier = Modifier.padding(top = 20.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            CollectionMap(
                state = state,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onSuggestionClick = { suggestion ->
                    focusManager.clearFocus()
                    viewModel.onSuggestionSelected(suggestion)
                },
                onPointClick = viewModel::onPointSelected,
                onViewportChanged = viewModel::onViewportChanged,
                onMyLocationClick = onMyLocationClick,
                onBecomeCollectorClick = onBecomeCollector.takeIf { state.canBecomeCollector }
            )
            if (MAP_FILTER_TYPES.size > 1) {
                Spacer(Modifier.height(12.dp))
                TypeFilterRow(activeTypes = state.activeTypes, onToggle = viewModel::onToggleType)
            }
            Spacer(Modifier.height(24.dp))
            Text("Pontos de Coleta próximos", fontSize = 14.sp, color = OliusTextoSecundario)
            Spacer(Modifier.height(12.dp))
            if (state.visiblePoints.isEmpty() && !state.isLoadingPoints) {
                Text(
                    "Nenhum ponto nesta área do mapa. Arraste o mapa ou busque pelo nome.",
                    fontSize = 13.sp,
                    color = OliusTextoSecundario,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
            state.visiblePoints.forEach { point ->
                CollectionPointCard(point = point, onClick = { viewModel.onPointSelected(point.id) })
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(BOTTOM_NAV_CONTENT_PADDING))
        }
    }

    if (state.isPointSheetOpen) {
        PointDetailsSheet(
            summary = state.selectedPoint,
            details = state.selectedDetails,
            isLoading = state.isLoadingDetails,
            error = state.detailsError,
            onDismiss = viewModel::onDismissPointDetails
        )
    }
}

@Composable
private fun BecomeCollectorBanner(onClick: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = OliusAmareloFundoSuave,
        border = BorderStroke(1.dp, OliusAmareloBorda)
    ) {
        Row(Modifier.padding(start = 14.dp, top = 10.dp, bottom = 14.dp, end = 4.dp)) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.ic_alert_circle),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Vire um ponto de Coleta", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OliusMarromAviso)
                }
                Spacer(Modifier.height(4.dp))
                Text("Vire um ponto de coleta e divulgue sua empresa!", fontSize = 13.sp, color = OliusMarromAviso)
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Image(painterResource(R.drawable.ic_close), contentDescription = "Fechar aviso", modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
private fun BecomeCollectorDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OliusSuperficie,
        icon = { Icon(Icons.Filled.VolunteerActivism, contentDescription = null, tint = OliusAmarelo) },
        title = { Text("Vire um ponto de coleta", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "Seu endereço aparece no mapa pra quem está por perto e você recebe o óleo usado " +
                    "da vizinhança. Nós combinamos a retirada quando atingir o mínimo para coleta.",
                fontSize = 14.sp,
                color = OliusTextoSecundario
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = OliusAmarelo, contentColor = Color.Black)
            ) { Text("Quero participar", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Agora não", color = OliusTextoSecundario) }
        }
    )
}

@Composable
private fun CollectionMap(
    state: MapUiState,
    onSearchQueryChange: (String) -> Unit,
    onSuggestionClick: (PointSuggestion) -> Unit,
    onPointClick: (String) -> Unit,
    onViewportChanged: (com.olius.app.domain.entity.MapBounds) -> Unit,
    onMyLocationClick: () -> Unit,
    onBecomeCollectorClick: (() -> Unit)?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(MAP_HEIGHT)
            .clip(RoundedCornerShape(14.dp))
            .border(BorderStroke(1.dp, OliusCampoBorda), RoundedCornerShape(14.dp))
    ) {
        OsmMapView(
            points = state.visiblePoints,
            selectedPointId = state.selectedPointId,
            userLocation = state.userLocation,
            cameraRequest = state.cameraRequest,
            onPointClick = onPointClick,
            onViewportChanged = onViewportChanged,
            modifier = Modifier.fillMaxSize()
        )

        Text(
            "© OpenStreetMap",
            fontSize = 9.sp,
            color = OliusTextoSecundario,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .background(Color.White.copy(alpha = 0.8f))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
                .fillMaxWidth(0.86f)
        ) {
            MapSearchField(
                value = state.searchQuery,
                isSearching = state.isSearching,
                onValueChange = onSearchQueryChange
            )
            if (state.suggestions.isNotEmpty()) {
                SuggestionList(suggestions = state.suggestions, onClick = onSuggestionClick)
            } else if (!state.isSearching && state.searchQuery.trim().length >= MIN_SEARCH_LENGTH) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = OliusSuperficie
                ) {
                    Text("Nenhum local encontrado", fontSize = 12.sp, color = OliusTextoSecundario, modifier = Modifier.padding(12.dp))
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier
                    .size(38.dp)
                    .shadow(2.dp, CircleShape)
                    .clickable(onClick = onMyLocationClick),
                shape = CircleShape,
                color = OliusSuperficie
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.MyLocation, contentDescription = "Minha localização", tint = OliusTextoPrimario, modifier = Modifier.size(20.dp))
                }
            }
            if (onBecomeCollectorClick != null) Button(
                onClick = onBecomeCollectorClick,
                modifier = Modifier.height(34.dp),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OliusAmarelo, contentColor = OliusTextoPrimarioClaro)
            ) {
                Image(painterResource(R.drawable.ic_vire_coletor), contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Vire Coletor", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun MapSearchField(value: String, isSearching: Boolean, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .shadow(2.dp, RoundedCornerShape(19.dp))
            .clip(RoundedCornerShape(19.dp))
            .background(OliusSuperficie)
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = OliusTextoSecundario, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text("Busque um ponto: ex. loja do seu Zé", fontSize = 12.sp, color = OliusTextoSecundario, maxLines = 1)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 13.sp, color = OliusTextoPrimario),
                modifier = Modifier.fillMaxWidth()
            )
        }
        when {
            isSearching -> CircularProgressIndicator(
                strokeWidth = 2.dp,
                color = OliusAmarelo,
                modifier = Modifier.padding(horizontal = 8.dp).size(16.dp)
            )
            value.isNotEmpty() -> IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Limpar busca", tint = OliusTextoSecundario, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/** Lista do autocomplete, por cima do mapa. */
@Composable
private fun SuggestionList(suggestions: List<PointSuggestion>, onClick: (PointSuggestion) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .shadow(3.dp, RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        color = OliusSuperficie
    ) {
        Column {
            suggestions.take(5).forEachIndexed { index, suggestion ->
                if (index > 0) HorizontalDivider(color = OliusCampoBorda)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClick(suggestion) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(painterResource(suggestion.type.iconRes), contentDescription = suggestion.type.label, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(suggestion.name, fontSize = 13.sp, color = OliusTextoPrimario, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(suggestion.address, fontSize = 11.sp, color = OliusTextoSecundario, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TypeFilterRow(activeTypes: Set<CollectionPointType>, onToggle: (CollectionPointType) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MAP_FILTER_TYPES.forEach { type ->
            val active = type in activeTypes
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (active) OliusAmareloFundoSuave else OliusSuperficie)
                    .border(BorderStroke(1.dp, if (active) OliusAmareloBorda else OliusCampoBorda), RoundedCornerShape(20.dp))
                    .clickable { onToggle(type) }
                    .padding(start = 6.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(painter = painterResource(type.iconRes), contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    type.pluralLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (active) OliusTextoPrimario else OliusTextoSecundario
                )
            }
        }
    }
}

@Composable
private fun CollectionPointCard(point: CollectionPoint, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = OliusSuperficie,
        border = BorderStroke(1.dp, OliusCampoBorda)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(OliusAmareloFundoSuave),
                contentAlignment = Alignment.Center
            ) {
                Image(painter = painterResource(R.drawable.map_point_selected), contentDescription = null, modifier = Modifier.size(28.dp))
                Image(
                    painter = painterResource(point.type.iconRes),
                    contentDescription = point.type.label,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(3.dp)
                        .size(16.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    point.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = OliusTextoPrimario,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    addressWithDistance(point.address, point.distanceKm),
                    fontSize = 12.sp,
                    color = OliusTextoSecundario,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                OpeningHours(point.openingHours, point.isOpenNow)
            }
        }
    }
}

@Composable
private fun OpeningHours(openingHours: String, isOpenNow: Boolean) {
    if (openingHours.isBlank()) return
    val color = if (isOpenNow) OliusVerdeSeloTexto else ClosedRed
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.AccessTime, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            if (isOpenNow) openingHours else "Fechado agora · $openingHours",
            fontSize = 12.sp,
            color = color
        )
    }
}

/** Abre na hora com o [summary] do pin e completa com os [details] quando a API responde. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PointDetailsSheet(
    summary: CollectionPoint?,
    details: CollectionPointDetails?,
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val type = details?.type ?: summary?.type
    val name = details?.name ?: summary?.name.orEmpty()
    val address = details?.address ?: summary?.address.orEmpty()
    val latitude = details?.latitude ?: summary?.latitude
    val longitude = details?.longitude ?: summary?.longitude
    val openingHours = details?.openingHours ?: summary?.openingHours
    val isOpenNow = details?.isOpenNow ?: summary?.isOpenNow ?: false
    val materials = details?.acceptedMaterials?.takeIf { it.isNotEmpty() } ?: summary?.acceptedMaterials.orEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = OliusSuperficie
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp)
        ) {
            if (type != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painter = painterResource(type.iconRes), contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(type.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OliusTextoSecundario)
                }
                Spacer(Modifier.height(8.dp))
            }
            Text(name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = OliusTextoPrimario)
            Spacer(Modifier.height(12.dp))
            if (address.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Place, contentDescription = null, tint = OliusTextoSecundario, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(addressWithDistance(address, summary?.distanceKm), fontSize = 13.sp, color = OliusTextoSecundario)
                }
                Spacer(Modifier.height(6.dp))
            }
            openingHours?.let { OpeningHours(it, isOpenNow) }

            Spacer(Modifier.height(18.dp))
            if (latitude != null && longitude != null) {
                Button(
                    onClick = { openDirections(context, latitude, longitude, name) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OliusAmarelo, contentColor = Color.Black)
                ) {
                    Icon(Icons.Filled.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Como chegar", fontWeight = FontWeight.Bold)
                }
            }
            if (details?.phone != null || details?.whatsapp != null) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    details.phone?.let { phone ->
                        ActionOutlinedButton("Ligar", Icons.Filled.Call, OliusTextoPrimario, Modifier.weight(1f)) { dial(context, phone) }
                    }
                    details.whatsapp?.let { number ->
                        ActionOutlinedButton("WhatsApp", Icons.AutoMirrored.Outlined.Chat, WhatsAppGreen, Modifier.weight(1f)) { openWhatsApp(context, number) }
                    }
                }
            }

            when {
                isLoading -> Row(Modifier.padding(top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(strokeWidth = 2.dp, color = OliusAmarelo, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Carregando informações do local...", fontSize = 12.sp, color = OliusTextoSecundario)
                }
                error != null -> Text(error, fontSize = 12.sp, color = ClosedRed, modifier = Modifier.padding(top = 20.dp))
            }

            if (details != null) {
                val rows = buildList {
                    details.phone?.let { add(InfoRow(Icons.Outlined.Phone, "Telefone", formatPhone(it))) }
                    details.cnpj?.let { add(InfoRow(Icons.Outlined.Badge, "CNPJ", formatCnpj(it), copyValue = it)) }
                    details.legalName?.let { add(InfoRow(Icons.Outlined.Storefront, "Razão social", it)) }
                    details.description?.let { add(InfoRow(Icons.Outlined.Info, "Sobre o local", it)) }
                    details.extraInfo.forEach { add(InfoRow(Icons.Outlined.Info, it.label, it.value)) }
                }
                if (rows.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider(color = OliusCampoBorda)
                    rows.forEach { row -> InfoLine(row) }
                }
            }

            if (materials.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                Text("O que recebe", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OliusTextoPrimario)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    materials.forEach { material ->
                        Text(
                            material,
                            fontSize = 12.sp,
                            color = OliusMarromAviso,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(OliusAmareloFundoSuave)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

private data class InfoRow(val icon: ImageVector, val label: String, val value: String, val copyValue: String? = null)

@Composable
private fun InfoLine(row: InfoRow) {
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(row.icon, contentDescription = null, tint = OliusTextoSecundario, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(row.label, fontSize = 11.sp, color = OliusTextoSecundario)
            Text(row.value, fontSize = 14.sp, color = OliusTextoPrimario)
        }
        row.copyValue?.let { value ->
            IconButton(onClick = { copyToClipboard(context, row.label, value) }) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = "Copiar ${row.label}", tint = OliusTextoSecundario, modifier = Modifier.size(18.dp))
            }
        }
    }
    HorizontalDivider(color = OliusCampoBorda)
}

@Composable
private fun ActionOutlinedButton(label: String, icon: ImageVector, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
        border = BorderStroke(1.dp, OliusCampoBorda)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = OliusTextoPrimario, fontSize = 13.sp, maxLines = 1)
    }
}

/** Rota sem API própria: abre o app de mapas instalado (Google Maps, Waze...). */
private fun openDirections(context: Context, latitude: Double, longitude: Double, name: String) {
    val intents = listOf(
        Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$latitude,$longitude")),
        Intent(Intent.ACTION_VIEW, Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(name)})")),
        Intent(Intent.ACTION_VIEW, Uri.parse("https://www.openstreetmap.org/directions?to=$latitude,$longitude"))
    )
    for (intent in intents) {
        try {
            context.startActivity(intent)
            return
        } catch (_: ActivityNotFoundException) {
        }
    }
    Toast.makeText(context, "Nenhum app de mapas encontrado", Toast.LENGTH_SHORT).show()
}

/** ACTION_DIAL abre o discador com o número preenchido: não precisa da permissão CALL_PHONE. */
private fun dial(context: Context, phone: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.filter(Char::isDigit)}")))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "Este aparelho não faz ligações", Toast.LENGTH_SHORT).show()
    }
}

private fun openWhatsApp(context: Context, number: String) {
    val digits = number.filter(Char::isDigit).let { if (it.startsWith("55")) it else "55$it" }
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits")))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "Não foi possível abrir o WhatsApp", Toast.LENGTH_SHORT).show()
    }
}

private fun copyToClipboard(context: Context, label: String, value: String) {
    context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(label, value))
    Toast.makeText(context, "$label copiado", Toast.LENGTH_SHORT).show()
}

private fun addressWithDistance(address: String, distanceKm: Double?): String =
    distanceKm?.let { "$address · ${formatDistance(it)}" } ?: address

private fun formatDistance(km: Double): String =
    if (km < 1) "${(km * 1000).toInt()} m" else String.format(Locale.forLanguageTag("pt-BR"), "%.1f km", km)

/** 12345678000190 -> 12.345.678/0001-90 */
internal fun formatCnpj(cnpj: String): String {
    val d = cnpj.filter(Char::isDigit)
    if (d.length != 14) return cnpj
    return "${d.substring(0, 2)}.${d.substring(2, 5)}.${d.substring(5, 8)}/${d.substring(8, 12)}-${d.substring(12)}"
}

/** 1132551234 -> (11) 3255-1234; 11999551234 -> (11) 99955-1234 */
internal fun formatPhone(phone: String): String {
    val d = phone.filter(Char::isDigit)
    return when (d.length) {
        10 -> "(${d.substring(0, 2)}) ${d.substring(2, 6)}-${d.substring(6)}"
        11 -> "(${d.substring(0, 2)}) ${d.substring(2, 7)}-${d.substring(7)}"
        else -> phone
    }
}
