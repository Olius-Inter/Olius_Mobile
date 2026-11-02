package com.olius.app.presentation.screen.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.olius.app.domain.entity.CollectionStatus
import com.olius.app.domain.entity.DisposalRecord
import com.olius.app.domain.entity.ImpactStats
import com.olius.app.domain.entity.ProfileType
import com.olius.app.presentation.components.BOTTOM_NAV_CONTENT_PADDING
import com.olius.app.presentation.components.CollectionStatusChip
import com.olius.app.presentation.components.OliusPageTitle
import com.olius.app.presentation.components.OliusTabScaffold
import com.olius.app.presentation.components.OliusTopBarState
import com.olius.app.presentation.screen.home.ImpactStatsGrid
import com.olius.app.presentation.screen.home.formatNumber
import com.olius.app.presentation.theme.OliusAmarelo
import com.olius.app.presentation.theme.OliusAzulAgua
import com.olius.app.presentation.theme.OliusCampoBorda
import com.olius.app.presentation.theme.OliusCampoFundo
import com.olius.app.presentation.theme.OliusLaranjaBiodiesel
import com.olius.app.presentation.theme.OliusStatusNeutroFundo
import com.olius.app.presentation.theme.OliusStatusNeutroTexto
import com.olius.app.presentation.theme.OliusTextoCard
import com.olius.app.presentation.theme.OliusTextoCardSecundario
import com.olius.app.presentation.theme.OliusTextoSecundario
import com.olius.app.presentation.theme.OliusVerdeAberto
import com.olius.app.presentation.theme.OliusSuperficie
import java.util.Locale

/** Aba Histórico. */
private val PT_BR: Locale = Locale.forLanguageTag("pt-BR")

@Composable
fun HistoryScreen(
    topBarState: OliusTopBarState,
    onConnectionError: () -> Unit,
    viewModel: HistoryViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val isCitizen = state.profileType == ProfileType.CITIZEN

    OliusTabScaffold(topBarState = topBarState) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            if (state.isOffline) {
                LaunchedEffect(Unit) { onConnectionError() }
                return@Column
            }
            Spacer(Modifier.height(24.dp))
            OliusPageTitle(
                title = if (isCitizen) "Histórico de Registro" else "Histórico de Coletas",
                subtitle = "Todos os registros de óleo do seu estabelecimento"
            )
            Spacer(Modifier.height(16.dp))
            when {
                state.isLoading -> Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = OliusAmarelo)
                }
                state.errorMessage != null -> Text(
                    state.errorMessage!!,
                    color = OliusTextoSecundario,
                    modifier = Modifier.clickable(onClick = viewModel::load).padding(vertical = 24.dp)
                )
                isCitizen -> state.citizen?.let { history ->
                    MonthAndStats(history.monthLabel, history.stats)
                    Spacer(Modifier.height(28.dp))
                    history.records.forEach { DisposalRecordCard(it) }
                }
                else -> state.establishment?.let { history ->
                    MonthAndStats(history.monthLabel, history.stats)
                    Spacer(Modifier.height(28.dp))
                    StatusFilters(selected = state.statusFilter, onSelect = viewModel::onFilterSelected)
                    Spacer(Modifier.height(30.dp))
                    CollectionsTable(state)
                }
            }
            Spacer(Modifier.height(BOTTOM_NAV_CONTENT_PADDING))
        }
    }
}

@Composable
private fun MonthAndStats(monthLabel: String, stats: ImpactStats) {
    Text(monthLabel, fontSize = 13.sp, color = OliusTextoSecundario)
    Spacer(Modifier.height(10.dp))
    ImpactStatsGrid(
        oilLiters = stats.oilCollectedLiters,
        waterLiters = stats.waterPreservedLiters,
        co2Kg = stats.co2AvoidedKg,
        biodieselLiters = stats.biodieselLiters
    )
}

/** Filtros "Todos | Coletado | Pendente" (ordem do enum). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusFilters(selected: CollectionStatus?, onSelect: (CollectionStatus?) -> Unit) {
    val options: List<Pair<CollectionStatus?, String>> =
        listOf<Pair<CollectionStatus?, String>>(null to "Todos") + CollectionStatus.entries.map { it to it.label }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (status, label) ->
            val isSelected = status == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (isSelected) OliusAmarelo else Color.Transparent)
                    .border(2.dp, if (isSelected) OliusAmarelo else OliusCampoBorda, RoundedCornerShape(13.dp))
                    .clickable { onSelect(status) }
                    .padding(horizontal = 15.dp, vertical = 9.dp)
            ) {
                Text(
                    label,
                    fontSize = 16.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = if (isSelected) OliusStatusNeutroTexto else OliusTextoCardSecundario
                )
            }
        }
    }
}

@Composable
private fun CollectionsTable(state: HistoryUiState) {
    Surface(
        shape = RoundedCornerShape(13.dp),
        color = OliusSuperficie,
        border = BorderStroke(1.dp, OliusCampoBorda)
    ) {
        Column {
            TableRow(
                modifier = Modifier.background(OliusStatusNeutroFundo),
                date = { Text("Data", fontSize = 13.sp, color = OliusStatusNeutroTexto) },
                volume = { Text("Volume", fontSize = 13.sp, color = OliusStatusNeutroTexto) },
                status = { Text("Status", fontSize = 13.sp, color = OliusStatusNeutroTexto) }
            )
            if (state.filteredItems.isEmpty()) {
                Text(
                    "Nenhum registro com esse status.",
                    fontSize = 13.sp,
                    color = OliusTextoSecundario,
                    modifier = Modifier.padding(19.dp)
                )
            }
            state.filteredItems.forEach { item ->
                HorizontalDivider(color = OliusCampoBorda)
                TableRow(
                    date = { Text(item.dateLabel, fontSize = 13.sp, color = OliusStatusNeutroTexto) },
                    volume = { Text("${formatNumber(item.liters)}L", fontSize = 13.sp, color = OliusStatusNeutroTexto) },
                    status = { CollectionStatusChip(item.status) }
                )
            }
        }
    }
}

@Composable
private fun TableRow(
    date: @Composable () -> Unit,
    volume: @Composable () -> Unit,
    status: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 19.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f)) { date() }
        Box(Modifier.weight(0.8f), contentAlignment = Alignment.Center) { volume() }
        Box(Modifier.weight(1.1f), contentAlignment = Alignment.CenterEnd) { status() }
    }
}

@Composable
private fun DisposalRecordCard(record: DisposalRecord) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 15.dp),
        shape = RoundedCornerShape(9.dp),
        color = OliusSuperficie,
        border = BorderStroke(1.dp, OliusCampoBorda)
    ) {
        Column(Modifier.padding(horizontal = 15.dp, vertical = 15.dp)) {
            Row {
                Column(Modifier.weight(1f)) {
                    Text("${formatNumber(record.liters)}L descartados", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = OliusTextoCard)
                    Text(record.dateLabel, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = OliusTextoCardSecundario)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = OliusAzulAgua, fontSize = 14.sp)) {
                                append(String.format(PT_BR, "%,.0f", record.waterPreservedLiters))
                            }
                            withStyle(SpanStyle(color = OliusTextoCardSecundario, fontSize = 11.sp)) { append("L") }
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("Água preservada", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = OliusTextoCardSecundario)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                ImpactBox("CO₂ Evitado", "${formatNumber(record.co2AvoidedKg)}kg", OliusVerdeAberto, Modifier.weight(1f))
                ImpactBox("Biodiesel gerado", "${formatNumber(record.biodieselLiters)}L", OliusLaranjaBiodiesel, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ImpactBox(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(OliusCampoFundo)
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 10.sp, color = OliusTextoCardSecundario)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}
