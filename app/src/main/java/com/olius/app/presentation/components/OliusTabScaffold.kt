package com.olius.app.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olius.app.presentation.theme.DecorativeBackground
import com.olius.app.presentation.theme.OliusTextoSecundario
import com.olius.app.presentation.theme.OliusTituloMarrom

// Respiro no fim das telas roláveis: o conteúdo passa por baixo do menu
// inferior de vidro (ver MainScaffoldScreen.kt), então o último item precisa
// dessa folga pra não ficar escondido atrás dele.
val BOTTOM_NAV_CONTENT_PADDING = 110.dp

// Distância entre o fim do conteúdo fixo (ex.: campo do chat) e o topo do
// menu inferior — o menu tem 44dp de ícone + 2x10dp de padding.
val BOTTOM_NAV_HEIGHT = 76.dp

/**
 * Base das abas pós-login: fundo decorativo amarelo + header (logo, sino,
 * avatar). O [content] decide se rola ou não — o chat, por exemplo, precisa
 * do campo de texto fixo embaixo.
 */
@Composable
fun OliusTabScaffold(
    topBarState: OliusTopBarState,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        DecorativeBackground(isRow = true)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            OliusTopBar(state = topBarState)
            content()
        }
    }
}

/** Título + subtítulo das telas internas (ref_map.png, ref_chat.png). */
@Composable
fun OliusPageTitle(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = OliusTituloMarrom, lineHeight = 30.sp)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, fontSize = 13.sp, color = OliusTextoSecundario, lineHeight = 17.sp)
    }
}
