package com.olius.app.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** true = tema escuro ativo. */
val LocalOliusDarkTheme = staticCompositionLocalOf { false }

private val OliusLightColorScheme = lightColorScheme(
    primary = OliusAmarelo,
    onPrimary = OliusTextoPrimarioClaro,
    secondary = OliusLaranja,
    onSecondary = Color.White,
    tertiary = OliusVerdeAberto,
    background = Color.White,
    onBackground = OliusTextoPrimarioClaro,
    surface = Color.White,
    onSurface = OliusTextoPrimarioClaro,
    surfaceVariant = OliusCampoFundoClaro,
    onSurfaceVariant = OliusTextoSecundarioClaro,
    outline = OliusCampoBordaClaro
)

private val OliusDarkColorScheme = darkColorScheme(
    primary = OliusAmarelo,
    onPrimary = OliusTextoPrimarioClaro,
    secondary = OliusLaranja,
    onSecondary = Color.White,
    tertiary = OliusVerdeAberto,
    background = OliusFundoEscuro,
    onBackground = OliusTextoPrimarioEscuro,
    surface = OliusSuperficieEscura,
    onSurface = OliusTextoPrimarioEscuro,
    surfaceVariant = OliusCampoFundoEscuro,
    onSurfaceVariant = OliusTextoSecundarioEscuro,
    outline = OliusCampoBordaEscura
)

/** Tema do app. */
@Composable
fun OliusTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalOliusDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) OliusDarkColorScheme else OliusLightColorScheme,
            typography = Typography,
            content = content
        )
    }
}
