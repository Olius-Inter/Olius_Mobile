package com.olius.app.presentation.theme

import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp

// Diâmetro das bolinhas decorativas de fundo. O centro de cada uma fica
// exatamente em cima do canto da tela (ver DecorativeBackground) — como o
// Brush.radialGradient delas vai do centro (mais forte) até a borda do
// círculo (transparente), esse diâmetro/2 é o "raio" até onde a cor
// desvanece: quanto maior, mais a cor se espalha pela tela antes de sumir.
val DECORATIVE_BLOB_SIZE = 520.dp

/**
 * Fundo com dois blobs amarelos que trocam de lado (start<->end) em
 * sincronia com `isRow` — igual referencia_app.png/referencia_home.png, onde a
 * cor mais forte satura o canto e vai desvanecendo a partir dele. Compartilhado
 * entre o fluxo de login/cadastro (PerfilType.kt) e a Home (HomeScreen.kt) —
 * é o MESMO fundo nas duas telas, de propósito.
 *
 * Usa gradient radial (Brush.radialGradient) em vez de "círculo sólido +
 * blur" — o `Modifier.blur()` do Compose só funciona em Android 12+
 * (API 31, via RenderEffect); abaixo disso ele não faz NADA. O gradient
 * funciona em qualquer versão do Android e é o que realmente reproduz o
 * visual suave da referência. Em API 31+ ainda soma um blur leve por cima,
 * só de bônus (ver `blurCompat` mais abaixo).
 *
 * Por que BoxWithConstraints + offset em vez de BiasAlignment: com
 * BiasAlignment, bias = ±1 deixa o círculo "encostado" na borda mas com o
 * CENTRO ainda a metade do tamanho dele pra dentro da tela (não no canto de
 * verdade) — pra empurrar o centro até o canto seria preciso um bias bem
 * maior que 1, e o valor certo muda dependendo do tamanho do círculo e da
 * tela. Calculando o offset manualmente a partir da largura/altura reais da
 * tela (`maxWidth`/`maxHeight`, via BoxWithConstraints), o centro cai
 * exatamente no canto sempre, não importa o tamanho do blob.
 */
@Composable
fun DecorativeBackground(isRow: Boolean, modifier: Modifier = Modifier) {
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
 * verdade — então abaixo de 31 simplesmente não aplica nada, e o visual já
 * fica correto só com o gradient acima.
 */
private fun Modifier.blurCompat(radius: Dp): Modifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) this.blur(radius) else this
