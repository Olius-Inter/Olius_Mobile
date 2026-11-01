package com.olius.app.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olius.app.R
import com.olius.app.domain.entity.CollectionPoint
import com.olius.app.domain.entity.CollectionStatus
import com.olius.app.presentation.theme.OliusAbaFundo
import com.olius.app.presentation.theme.OliusAmarelo
import com.olius.app.presentation.theme.OliusCampoBorda
import com.olius.app.presentation.theme.OliusPevIconeFundo
import com.olius.app.presentation.theme.OliusPevIconeFundoFechado
import com.olius.app.presentation.theme.OliusStatusPendenteFundo
import com.olius.app.presentation.theme.OliusStatusPendenteTexto
import com.olius.app.presentation.theme.OliusTextoCard
import com.olius.app.presentation.theme.OliusTextoCardSecundario
import com.olius.app.presentation.theme.OliusTextoSecundario
import com.olius.app.presentation.theme.OliusTituloMarrom
import com.olius.app.presentation.theme.OliusTituloMarromClaro
import com.olius.app.presentation.theme.OliusVerdeAberto
import com.olius.app.presentation.theme.OliusVerdeSeloFundo
import com.olius.app.presentation.theme.OliusVerdeSeloTexto
import com.olius.app.presentation.theme.OliusVermelhoFechado
import com.olius.app.presentation.theme.OliusSuperficie

/** Uma aba com ícone (Figma: "Meus Dados | Solicitações" e "Pontos PEV | Dicas"). */
data class OliusIconTab<T>(val value: T, val label: String, val iconRes: Int)

/**
 * Abas com ícone num trilho cinza, a selecionada em branco. O ícone não
 * selecionado fica acinzentado (o Figma usa a mesma arte com cor apagada).
 */
@Composable
fun <T> OliusIconTabs(tabs: List<OliusIconTab<T>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .background(OliusAbaFundo)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        tabs.forEach { tab ->
            val isSelected = tab.value == selected
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (isSelected) OliusSuperficie else Color.Transparent)
                    .clickable { onSelect(tab.value) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(tab.iconRes),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(if (isSelected) OliusTextoCard else OliusTextoCardSecundario),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    tab.label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) OliusTextoCard else OliusTextoCardSecundario
                )
            }
        }
    }
}

/**
 * Conteúdo da tela "Sem conexão" (Figma "Mobile / Erro", frame 391 x 865).
 * Medidas do Figma: título em y=123, texto 276 dp, ilustração de 150 dp
 * centralizada e botão de 300 x 43 dp. Quem usa é a ConnectionErrorScreen.
 */
@Composable
fun NoConnectionContent(onBackToStart: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        // 123 dp do topo do frame menos os ~44 dp da status bar do Figma.
        Spacer(Modifier.height(79.dp))
        Text("Sem conexão", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = OliusTituloMarrom, lineHeight = 32.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            "Ops... Estamos com falhas na conexão. Reinicie a página ou entre no aplicativo novamente.",
            fontSize = 14.sp,
            lineHeight = 16.sp,
            color = OliusTextoSecundario,
            modifier = Modifier.widthIn(max = 276.dp)
        )
        Spacer(Modifier.height(97.dp))
        UnpluggedIllustration(Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(17.dp))
        Button(
            onClick = onBackToStart,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .widthIn(max = 300.dp)
                .fillMaxWidth()
                .height(43.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OliusAmarelo, contentColor = Color.Black)
        ) { Text("Voltar ao início", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(24.dp))
    }
}

/**
 * Ilustração do Figma: círculo amarelo de 150 dp com um plugue desconectado
 * (dois pinos de 5 x 15 dp) e o fio descendo até perto do botão. Desenhada
 * em Canvas, em dp, numa área de 150 x 244 dp (y 306 a 550 no Figma).
 */
@Composable
private fun UnpluggedIllustration(modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .size(width = 150.dp, height = 244.dp)
            .semantics { contentDescription = "Tomada desconectada" }
    ) {
        val u = 1.dp.toPx()
        val center = 75f * u
        drawCircle(color = OliusAmarelo, radius = center, center = Offset(center, center))

        // Faíscas acima dos pinos: o "desligado". Marrom fixo: fica sobre o amarelo nos dois temas.
        val spark = 3f * u
        drawLine(OliusTituloMarromClaro, Offset(center, 24f * u), Offset(center, 36f * u), spark, StrokeCap.Round)
        drawLine(OliusTituloMarromClaro, Offset(52f * u, 30f * u), Offset(58f * u, 40f * u), spark, StrokeCap.Round)
        drawLine(OliusTituloMarromClaro, Offset(98f * u, 30f * u), Offset(92f * u, 40f * u), spark, StrokeCap.Round)

        // Pinos (5 x 15 dp) + corpo do plugue + pescoço.
        drawRoundRect(OliusTituloMarromClaro, Offset(60.5f * u, 50f * u), Size(5f * u, 15f * u), CornerRadius(1.5f * u))
        drawRoundRect(OliusTituloMarromClaro, Offset(84.5f * u, 50f * u), Size(5f * u, 15f * u), CornerRadius(1.5f * u))
        drawRoundRect(OliusTituloMarromClaro, Offset(55f * u, 65f * u), Size(40f * u, 32f * u), CornerRadius(7f * u))
        drawRoundRect(OliusTituloMarromClaro, Offset(68f * u, 96f * u), Size(14f * u, 11f * u), CornerRadius(3f * u))

        // Fio: sai do plugue, cruza a borda do círculo e desce em "S".
        val cord = Path().apply {
            moveTo(center, 106f * u)
            lineTo(center, 140f * u)
            cubicTo(center, 175f * u, 105f * u, 178f * u, 105f * u, 204f * u)
            cubicTo(105f * u, 230f * u, center, 226f * u, center, 242f * u)
        }
        drawPath(cord, OliusTituloMarromClaro, style = Stroke(width = 4f * u, cap = StrokeCap.Round))
    }
}

/** Chip de status da tabela do Histórico B2B ("Pendente" laranja, "Coletado" verde). */
@Composable
fun CollectionStatusChip(status: CollectionStatus, modifier: Modifier = Modifier) {
    val (background, text) = when (status) {
        CollectionStatus.PENDING -> OliusStatusPendenteFundo to OliusStatusPendenteTexto
        CollectionStatus.COLLECTED -> OliusVerdeSeloFundo to OliusVerdeSeloTexto
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(23.dp))
            .background(background)
            .padding(horizontal = 14.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(status.label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = text)
    }
}

/** Card "Pontos PEV" (Figma "Card - Pontos PEV Disponível/Indisponível - B2C"). */
@Composable
fun PevPointCard(point: CollectionPoint, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(10.dp),
        color = OliusSuperficie,
        border = BorderStroke(1.dp, OliusCampoBorda)
    ) {
        // Sem horário (a Core ainda não tem o campo) o ponto não aparece como fechado.
        val open = point.isOpenNow || point.openingHours.isBlank()
        Row(Modifier.padding(horizontal = 15.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(59.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (open) OliusPevIconeFundo else OliusPevIconeFundoFechado),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painterResource(if (open) R.drawable.ic_pev_pin_open else R.drawable.ic_pev_pin_closed),
                    contentDescription = null,
                    modifier = Modifier.size(width = 27.dp, height = 35.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(point.name, fontSize = 12.sp, color = OliusTextoCard, maxLines = 1)
                Text(point.address, fontSize = 10.sp, color = OliusTextoCardSecundario, maxLines = 1)
                if (point.openingHours.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "⏱️ ${point.openingHours}",
                        fontSize = 10.sp,
                        color = if (point.isOpenNow) OliusVerdeAberto else OliusVermelhoFechado
                    )
                }
            }
        }
    }
}
