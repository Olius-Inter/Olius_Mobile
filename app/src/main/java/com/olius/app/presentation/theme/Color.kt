package com.olius.app.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val OliusAmarelo = Color(0xFFFFC800)
val OliusAmareloClaro = Color(0xFFFFE566)

val OliusTextoPrimarioClaro = Color(0xFF1A1A1A)
val OliusTextoPrimarioEscuro = Color(0xFFF2F2F2)
val OliusTextoSecundarioClaro = Color(0xFF8A8A8A)
val OliusTextoSecundarioEscuro = Color(0xFFA8A8A8)
val OliusCampoFundoClaro = Color(0xFFF6F6F6)
val OliusCampoFundoEscuro = Color(0xFF2A2A2A)
val OliusCampoBordaClaro = Color(0xFFE3E3E3)
val OliusCampoBordaEscura = Color(0xFF3D3D3D)
/** Fundo das telas no escuro: quase preto, levemente quente (combina com o amarelo). */
val OliusFundoEscuro = Color(0xFF141210)
/** Cartões e campos no escuro (um pouco mais claros que o fundo). */
val OliusSuperficieEscura = Color(0xFF221F1B)
/** Títulos marrons das telas internas: no escuro viram creme. */
val OliusTituloMarromClaro = Color(0xFF3B2A1E)
val OliusTituloMarromEscuro = Color(0xFFF3E3C3)

private val isDark: Boolean
    @Composable @ReadOnlyComposable get() = LocalOliusDarkTheme.current

val OliusTextoPrimario: Color
    @Composable @ReadOnlyComposable get() = if (isDark) OliusTextoPrimarioEscuro else OliusTextoPrimarioClaro
val OliusTextoSecundario: Color
    @Composable @ReadOnlyComposable get() = if (isDark) OliusTextoSecundarioEscuro else OliusTextoSecundarioClaro
val OliusCampoFundo: Color
    @Composable @ReadOnlyComposable get() = if (isDark) OliusCampoFundoEscuro else OliusCampoFundoClaro
val OliusCampoBorda: Color
    @Composable @ReadOnlyComposable get() = if (isDark) OliusCampoBordaEscura else OliusCampoBordaClaro

/** Fundo das telas (branco no claro). */
val OliusFundoTela: Color
    @Composable @ReadOnlyComposable get() = if (isDark) OliusFundoEscuro else Color.White

val OliusSuperficie: Color
    @Composable @ReadOnlyComposable get() = if (isDark) OliusSuperficieEscura else Color.White

val OliusLaranja = Color(0xFFF57A04)
val OliusBotaoInativoFundo = Color(0xFFE3E3E3)
val OliusBotaoInativoTexto = Color(0xFF737A7B)

val OliusNavBarFundo = Color(0xFF676767)

val OliusIconeAgua = Color(0xFF4A90D9)
val OliusIconeVerde = Color(0xFF34A853)

val OliusVerdeSeloFundo = Color(0xFFDFF5E3)
val OliusVerdeSeloTexto = Color(0xFF2E9E52)

val OliusTanqueGotaVazia = Color(0xFFBFBFBF)

val OliusSheetFundo = Color(0xFF2B2B2B)
val OliusSheetTextoSecundario = Color(0xFFB0B0B0)
val OliusTituloMarrom: Color
    @Composable @ReadOnlyComposable get() = if (isDark) OliusTituloMarromEscuro else OliusTituloMarromClaro

val OliusAmareloFundoSuave: Color
    @Composable @ReadOnlyComposable get() = if (isDark) Color(0xFF3A3018) else Color(0xFFFFF3C4)
val OliusAmareloBorda = Color(0xFFFFD84D)
val OliusBalaoUsuario: Color
    @Composable @ReadOnlyComposable get() = if (isDark) Color(0xFF4A3B12) else Color(0xFFFFE9A0)
val OliusMarromAviso: Color
    @Composable @ReadOnlyComposable get() = if (isDark) OliusAmareloClaro else Color(0xFF7A5300)

val OliusSegmentoFundo: Color
    @Composable @ReadOnlyComposable get() = if (isDark) OliusCampoFundoEscuro else Color(0xFFEDEDED)
val OliusBotaoEscuro = Color(0xFF2B2B2B)

val OliusPrata = Color(0xFFBDBDBD)
val OliusAvatarPrataFundo = Color(0xFFBFD7F5)
val OliusAvatarPrataTexto = Color(0xFF2C5AA0)
val OliusBronze = Color(0xFFE8836B)
val OliusLaranjaGradiente = Color(0xFFFFA200)

val OliusMapaFundo = Color(0xFFEFF0EB)
val OliusMapaParque = Color(0xFFD4EBC6)
val OliusMapaAgua = Color(0xFFC9DDF2)
val OliusMapaAvenida = Color(0xFFFBE7A1)

val OliusStatusPendenteFundo = Color(0xFFFFD49F)
val OliusStatusPendenteTexto = Color(0xFFCD6400)
val OliusStatusNeutroFundo = Color(0xFFDFDFDF)
val OliusStatusNeutroTexto = Color(0xFF3C3C3C)
val OliusStatusAzulFundo = Color(0xFFDCEBFA)
val OliusStatusAzulTexto = Color(0xFF2C5AA0)
val OliusLaranjaVivo = Color(0xFFFD8C04)
val OliusLaranjaChipFundo = Color(0x38FFB153)
val OliusSeloAvisoFundo = Color(0xFFFFE88F)
val OliusPevIconeFundo = Color(0xFFFFF1BB)
val OliusPevIconeFundoFechado = Color(0x80FCCC03)
val OliusVerdeAberto = Color(0xFF27AE60)
val OliusVermelhoFechado = Color(0xFFFF0000)
val OliusAbaFundo: Color
    @Composable @ReadOnlyComposable get() = if (isDark) OliusCampoFundoEscuro else Color(0xFFF0F0F0)
val OliusTextoCard: Color
    @Composable @ReadOnlyComposable get() = if (isDark) Color(0xFFE6E6E6) else Color(0xFF323232)
val OliusTextoCardSecundario: Color
    @Composable @ReadOnlyComposable get() = if (isDark) Color(0xFFA0A0A0) else Color(0xFF848484)

val OliusAzulAgua = Color(0xFF3498DB)
val OliusLaranjaBiodiesel = Color(0xFFF05304)
val OliusVermelhoDescartar = Color(0xFFFF5757)
