package com.olius.app.presentation.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.olius.app.presentation.theme.OliusAmarelo

/**
 * Placeholder: ainda não existe uma Home real no projeto. Essa tela só
 * fecha a navegação depois do login/cadastro — substitua pelo fluxo real
 * quando ele existir.
 */
@Composable
fun HomeScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Bem-vindo ao Olius!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = OliusAmarelo
        )
    }
}
