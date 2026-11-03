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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

val DECORATIVE_BLOB_SIZE = 520.dp

/** Fundo com dois blobs amarelos que trocam de lado (start<->end) em sincronia com `isRow`. */
@Composable
fun DecorativeBackground(isRow: Boolean, modifier: Modifier = Modifier) {
    val bias = animateFloatAsState(
        targetValue = if (isRow) 1f else 0f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "blobBias"
    )

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(OliusFundoTela)
    ) {
        val widthPx = constraints.maxWidth
        val heightPx = constraints.maxHeight

        GradientBlob(
            colors = listOf(
                OliusAmareloClaro.copy(alpha = 0.85f),
                OliusAmareloClaro.copy(alpha = 0.35f),
                Color.Transparent
            ),
            modifier = Modifier
                .size(DECORATIVE_BLOB_SIZE)
                .offset {
                    val half = (DECORATIVE_BLOB_SIZE / 2).roundToPx()
                    IntOffset(x = (widthPx * bias.value).roundToInt() - half, y = -half)
                }
        )
        GradientBlob(
            colors = listOf(OliusAmareloClaro.copy(alpha = 0.55f), Color.Transparent),
            modifier = Modifier
                .size(DECORATIVE_BLOB_SIZE)
                .offset {
                    val half = (DECORATIVE_BLOB_SIZE / 2).roundToPx()
                    IntOffset(x = (widthPx * (1f - bias.value)).roundToInt() - half, y = heightPx - half)
                }
        )
    }
}

@Composable
private fun GradientBlob(colors: List<Color>, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(CircleShape)
            .background(Brush.radialGradient(colors))
            .blurCompat(40.dp)
    )
}

/** `.blur()` normal em Android 12+ (API 31, `RenderEffect`). */
private fun Modifier.blurCompat(radius: Dp): Modifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) this.blur(radius) else this
