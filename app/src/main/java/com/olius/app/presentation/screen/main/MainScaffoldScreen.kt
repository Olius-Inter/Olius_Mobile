package com.olius.app.presentation.screen.main

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.olius.app.R
import com.olius.app.presentation.screen.home.HomeScreen
import com.olius.app.presentation.theme.OliusNavBarFundo
import com.olius.app.presentation.theme.OliusTextoSecundario

/** As 5 abas do menu inferior fixo (ver ref_home_cell_1.png/ref_home_cell_2.png). */
enum class BottomNavTab(val route: String, val label: String) {
    HOME("home_tab", "Home"),
    HISTORY("history_tab", "Histórico"),
    SERVICES("services_tab", "Serviços"),
    CHAT("chat_tab", "Chat"),
    PIGGY_BANK("piggy_tab", "Cofrinho")
}

private data class NavTabIcons(val regular: Int, val selected: Int)

private val tabIcons = mapOf(
    BottomNavTab.HOME to NavTabIcons(R.drawable.home, R.drawable.home_selected),
    BottomNavTab.HISTORY to NavTabIcons(R.drawable.history, R.drawable.history_selected),
    BottomNavTab.SERVICES to NavTabIcons(R.drawable.vector, R.drawable.vector_selected),
    BottomNavTab.CHAT to NavTabIcons(R.drawable.chat, R.drawable.chat_selected),
    BottomNavTab.PIGGY_BANK to NavTabIcons(R.drawable.pig, R.drawable.pig_selected)
)

// Raio do blur do vidro do menu inferior — só tem efeito em API 31+ (ver
// comentário em OliusBottomNavBar). Aumente pra um vidro mais "fosco".
private const val NAV_BAR_GLASS_BLUR_RADIUS = 28f

/**
 * Casca do app pós-login: menu inferior fixo (nunca sai da tela, mesmo com
 * popups/bottom sheets abertos por cima do conteúdo — ver HOME_SCREEN.md)
 * + um NavHost próprio pra trocar de aba sem perder o menu. Só a aba Home
 * tem tela de verdade por enquanto; as outras 4 são placeholders "Em breve"
 * (ver [PlaceholderTabScreen]) até existirem.
 *
 * Não usa mais `Scaffold` de propósito: pro efeito de vidro do menu inferior
 * funcionar, o conteúdo precisa continuar sendo desenhado POR BAIXO do menu
 * (não empurrado pra cima por um padding), pra ter algo de verdade pra
 * "borrar" atrás do vidro — ver [OliusBottomNavBar].
 */
@Composable
fun MainScaffoldScreen(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Camada onde o conteúdo de trás (a tela ativa) é "gravado" a cada frame,
    // só pra ser redesenhada (borrada) por baixo do menu inferior — ver
    // OliusBottomNavBar. O conteúdo em si continua sendo desenhado
    // normalmente também (drawContent() roda nos dois lugares).
    val contentLayer = rememberGraphicsLayer()

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = BottomNavTab.HOME.route,
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    contentLayer.record { this@drawWithContent.drawContent() }
                    drawContent()
                }
        ) {
            composable(BottomNavTab.HOME.route) { HomeScreen(onLogout = onLogout) }
            composable(BottomNavTab.HISTORY.route) { PlaceholderTabScreen(title = "Histórico") }
            composable(BottomNavTab.SERVICES.route) { PlaceholderTabScreen(title = "Serviços") }
            composable(BottomNavTab.CHAT.route) { PlaceholderTabScreen(title = "Chat") }
            composable(BottomNavTab.PIGGY_BANK.route) { PlaceholderTabScreen(title = "Cofrinho") }
        }

        OliusBottomNavBar(
            contentLayer = contentLayer,
            currentRoute = currentRoute,
            onTabSelected = { tab ->
                if (tab.route != currentRoute) {
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/**
 * Menu inferior fixo com efeito de vidro (glassmorphism): em vez de um fundo
 * cinza sólido, redesenha por baixo de si mesmo — borrado e com uma leve
 * tonalidade escura por cima — o que já está atrás dele na [contentLayer]
 * (a tela ativa, capturada em `MainScaffoldScreen`). O borrão de verdade
 * (`RenderEffect`/`BlurEffect`) só existe a partir do Android 12 (API 31);
 * abaixo disso cai pra um fundo escuro translúcido só (sem borrão), do
 * mesmo jeito que `blurCompat` faz em DecorativeBackground.kt.
 *
 * A troca de cor animada dos ícones continua sendo um crossfade entre o
 * ícone normal e o "_selected" (os PNGs já vêm coloridos).
 */
@Composable
private fun OliusBottomNavBar(
    contentLayer: GraphicsLayer,
    currentRoute: String?,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    var barPositionInRoot by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            contentLayer.renderEffect = BlurEffect(NAV_BAR_GLASS_BLUR_RADIUS, NAV_BAR_GLASS_BLUR_RADIUS)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { barPositionInRoot = it.positionInRoot() }
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .drawWithContent {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    translate(left = -barPositionInRoot.x, top = -barPositionInRoot.y) {
                        drawLayer(contentLayer)
                    }
                    drawRect(OliusNavBarFundo.copy(alpha = 0.42f))
                } else {
                    drawRect(OliusNavBarFundo.copy(alpha = 0.94f))
                }
                drawContent()
            }
            .navigationBarsPadding()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavTab.entries.forEach { tab ->
            BottomNavItem(
                tab = tab,
                selected = currentRoute == tab.route,
                onClick = { onTabSelected(tab) }
            )
        }
    }
}

@Composable
private fun BottomNavItem(tab: BottomNavTab, selected: Boolean, onClick: () -> Unit) {
    val icons = tabIcons.getValue(tab)
    val regularAlpha by animateFloatAsState(if (selected) 0f else 1f, tween(250), label = "regularAlpha")
    val selectedAlpha by animateFloatAsState(if (selected) 1f else 0f, tween(250), label = "selectedAlpha")
    val scale by animateFloatAsState(if (selected) 1.12f else 1f, tween(250), label = "iconScale")

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(icons.regular),
            contentDescription = tab.label,
            modifier = Modifier
                .size(24.dp)
                .alpha(regularAlpha)
                .scale(scale)
        )
        Image(
            painter = painterResource(icons.selected),
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .alpha(selectedAlpha)
                .scale(scale)
        )
    }
}

/**
 * Placeholder pras 4 abas que ainda não têm tela real (Histórico, Serviços,
 * Chat, Cofrinho) — só pra deixar a navegação/menu inferior funcionando de
 * ponta a ponta. Substitua pela tela de verdade quando ela existir.
 */
@Composable
private fun PlaceholderTabScreen(title: String) {
    Box(Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
        Text("$title em breve", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OliusTextoSecundario)
    }
}
