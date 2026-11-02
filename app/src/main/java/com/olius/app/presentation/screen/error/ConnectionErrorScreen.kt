package com.olius.app.presentation.screen.error

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.olius.app.presentation.components.NoConnectionContent
import com.olius.app.presentation.theme.DecorativeBackground

/**
 * Tela cheia "Sem conexão" (rota `error` do NavGraph), sem header nem menu
 * inferior, como no Figma. Abre quando a Home ou o Histórico não conseguem
 * falar com o servidor (IOException: sem internet, timeout, servidor fora).
 */
@Composable
fun ConnectionErrorScreen(
    onNavigateHome: () -> Unit,
    onNavigatePerfilType: () -> Unit,
    viewModel: ConnectionErrorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        when (uiState) {
            ConnectionErrorUiState.NavigateHome -> onNavigateHome()
            ConnectionErrorUiState.NavigatePerfilType -> onNavigatePerfilType()
            ConnectionErrorUiState.Idle -> Unit
        }
    }

    Box(Modifier.fillMaxSize()) {
        DecorativeBackground(isRow = true)
        NoConnectionContent(
            onBackToStart = viewModel::onBackToStartClick,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 29.dp)
        )
    }
}
