package com.olius.app.presentation.screen.ranking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.olius.app.R
import com.olius.app.presentation.components.BOTTOM_NAV_CONTENT_PADDING
import com.olius.app.presentation.components.OliusSegmentedControl
import com.olius.app.presentation.components.OliusTabScaffold
import com.olius.app.presentation.components.OliusTopBarState
import com.olius.app.presentation.theme.OliusAmarelo
import com.olius.app.presentation.theme.OliusAmareloClaro
import com.olius.app.presentation.theme.OliusAvatarPrataFundo
import com.olius.app.presentation.theme.OliusAvatarPrataTexto
import com.olius.app.presentation.theme.OliusBronze
import com.olius.app.presentation.theme.OliusCampoBorda
import com.olius.app.presentation.theme.OliusLaranjaGradiente
import com.olius.app.presentation.theme.OliusPrata
import com.olius.app.presentation.theme.OliusTextoPrimario
import com.olius.app.presentation.theme.OliusTextoSecundario
import com.olius.app.presentation.theme.OliusTituloMarrom
import java.util.Locale

/** Cores de cada lugar do pódio: borda do card, fundo e texto do avatar. */
private class PodiumStyle(val medal: Int, val border: Color, val avatarBackground: Color, val avatarText: Color)

private fun podiumStyle(position: Int): PodiumStyle = when (position) {
    1 -> PodiumStyle(R.drawable.podio_1, OliusAmarelo, OliusAmarelo, Color.White)
    2 -> PodiumStyle(R.drawable.podio_2, OliusPrata, OliusAvatarPrataFundo, OliusAvatarPrataTexto)
    else -> PodiumStyle(R.drawable.podio_3, OliusBronze, OliusBronze, Color.White)
}

/**
 * Ranking de Recicladores (ref_ranking.png / ref_ranking_2.png / ref_ranking_b2b.png):
 * banner com a posição da pessoa, seletores de categoria e período, abas
 * Competições/Conquistas, pódio e a lista completa.
 */
@Composable
fun RankingScreen(topBarState: OliusTopBarState, viewModel: RankingViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    OliusTabScaffold(topBarState = topBarState) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(20.dp))
            RankingBanner(currentUser = state.currentUser)
            Spacer(Modifier.height(20.dp))
            OliusSegmentedControl(
                options = RankingCategory.entries,
                selected = state.category,
                label = { it.label },
                onSelect = viewModel::onCategorySelected
            )
            Spacer(Modifier.height(10.dp))
            OliusSegmentedControl(
                options = RankingPeriod.entries,
                selected = state.period,
                label = { it.label },
                onSelect = viewModel::onPeriodSelected
            )
            Spacer(Modifier.height(20.dp))
            RankingTabs(selected = state.tab, onSelect = viewModel::onTabSelected)
            Spacer(Modifier.height(24.dp))

            when (state.tab) {
                RankingTab.COMPETITIONS -> {
                    Text(state.periodLabel, fontSize = 14.sp, color = OliusTextoSecundario)
                    Spacer(Modifier.height(16.dp))
                    Podium(entries = state.podium)
                    Spacer(Modifier.height(28.dp))
                    RankingList(entries = state.entries)
                }

                RankingTab.ACHIEVEMENTS -> AchievementsComingSoon()
            }
            Spacer(Modifier.height(BOTTOM_NAV_CONTENT_PADDING))
        }
    }
}

// ---------------------------------------------------------------------------
// Banner "Ranking de Recicladores"
// ---------------------------------------------------------------------------

@Composable
private fun RankingBanner(currentUser: RankingEntry?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(OliusAmarelo, OliusLaranjaGradiente)))
    ) {
        Icon(
            Icons.Outlined.EmojiEvents,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.3f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 16.dp)
                .size(150.dp)
        )
        Column(Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
            Text("Ranking de Recicladores", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = OliusTituloMarrom)
            Spacer(Modifier.height(4.dp))
            Text("Veja quem está fazendo mais pelo planeta este mês.", fontSize = 12.sp, color = OliusTituloMarrom)
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(OliusAmareloClaro.copy(alpha = 0.85f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(painter = painterResource(R.drawable.podio_1), contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    currentUser?.let { "Sua posição: #${it.position} - ${formatLiters(it.liters)}" }
                        ?: "Você ainda não pontuou neste ranking",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OliusTituloMarrom
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Abas Competições / Conquistas
// ---------------------------------------------------------------------------

@Composable
private fun RankingTabs(selected: RankingTab, onSelect: (RankingTab) -> Unit) {
    Column {
        Row {
            RankingTabItem("Competições", R.drawable.podio_2, selected == RankingTab.COMPETITIONS) {
                onSelect(RankingTab.COMPETITIONS)
            }
            Spacer(Modifier.width(8.dp))
            RankingTabItem("Conquistas", R.drawable.ranking_selected, selected == RankingTab.ACHIEVEMENTS) {
                onSelect(RankingTab.ACHIEVEMENTS)
            }
        }
        HorizontalDivider(color = OliusCampoBorda)
    }
}

@Composable
private fun RankingTabItem(label: String, iconRes: Int, selected: Boolean, onClick: () -> Unit) {
    Column(Modifier.width(IntrinsicTabWidth).clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(painter = painterResource(iconRes), contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) OliusAmarelo else OliusTextoPrimario
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (selected) OliusAmarelo else Color.Transparent)
        )
    }
}

private val IntrinsicTabWidth: Dp = 120.dp

@Composable
private fun AchievementsComingSoon() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(painter = painterResource(R.drawable.ranking_selected), contentDescription = null, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text("Conquistas em breve", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OliusTextoPrimario)
        Spacer(Modifier.height(4.dp))
        Text(
            "Aqui você vai ver as medalhas que ganhou reciclando.",
            fontSize = 13.sp,
            color = OliusTextoSecundario,
            textAlign = TextAlign.Center
        )
    }
}

// ---------------------------------------------------------------------------
// Pódio (2º, 1º, 3º)
// ---------------------------------------------------------------------------

@Composable
private fun Podium(entries: List<RankingEntry>) {
    // Ordem visual do pódio: 2º à esquerda, 1º no meio (mais alto), 3º à direita.
    val ordered = listOfNotNull(entries.getOrNull(1), entries.getOrNull(0), entries.getOrNull(2))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        ordered.forEach { entry ->
            PodiumCard(entry = entry, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PodiumCard(entry: RankingEntry, modifier: Modifier = Modifier) {
    val style = podiumStyle(entry.position)
    val isFirst = entry.position == 1
    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        Surface(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(if (isFirst) 2.dp else 1.5.dp, style.border),
            shadowElevation = if (isFirst) 8.dp else 0.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = if (isFirst) 20.dp else 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(painter = painterResource(style.medal), contentDescription = "${entry.position}º lugar", modifier = Modifier.size(22.dp))
                Spacer(Modifier.height(8.dp))
                InitialsAvatar(entry.initials, style, size = if (isFirst) 50.dp else 44.dp)
                Spacer(Modifier.height(8.dp))
                Text(
                    entry.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = OliusTextoPrimario,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(entry.city, fontSize = 10.sp, color = OliusTextoSecundario, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(10.dp))
                Text(formatLiters(entry.liters), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OliusAmarelo)
            }
        }
        if (entry.isCurrentUser) {
            Text(
                "Você",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = OliusTituloMarrom,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(OliusAmarelo)
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun InitialsAvatar(initials: String, style: PodiumStyle, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(style.avatarBackground)
            .border(BorderStroke(1.dp, style.border), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(initials, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = style.avatarText)
    }
}

// ---------------------------------------------------------------------------
// Lista completa
// ---------------------------------------------------------------------------

@Composable
private fun RankingList(entries: List<RankingEntry>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, OliusCampoBorda)
    ) {
        Column {
            entries.forEachIndexed { index, entry ->
                if (index > 0) HorizontalDivider(color = OliusCampoBorda)
                RankingRow(entry)
            }
        }
    }
}

@Composable
private fun RankingRow(entry: RankingEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (entry.isCurrentUser) OliusAmareloClaro.copy(alpha = 0.15f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (entry.position <= 3) {
            val style = podiumStyle(entry.position)
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(style.border.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Image(painter = painterResource(style.medal), contentDescription = "${entry.position}º lugar", modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(entry.name, fontSize = 14.sp, color = OliusTextoPrimario, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(entry.city, fontSize = 11.sp, color = OliusTextoSecundario, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(formatLiters(entry.liters), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OliusAmarelo)
    }
}

private fun formatLiters(liters: Double): String =
    if (liters % 1.0 == 0.0) "${liters.toInt()}L" else String.format(Locale.US, "%.1fL", liters)
