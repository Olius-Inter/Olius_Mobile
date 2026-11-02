package com.olius.app.presentation.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieAnimatable
import com.airbnb.lottie.compose.rememberLottieComposition
import com.olius.app.R
import com.olius.app.presentation.theme.OliusFundoTela

/** Dono do [SplashViewModel]: toca a animação e. */
@Composable
fun SplashRoute(
    onNavigateHome: () -> Unit,
    onNavigatePerfilType: () -> Unit,
    viewModel: SplashViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        when (uiState) {
            SplashUiState.NavigateHome -> onNavigateHome()
            SplashUiState.NavigatePerfilType -> onNavigatePerfilType()
            SplashUiState.Loading -> Unit
        }
    }

    MainScreen(onFinished = viewModel::onSplashAnimationFinished)
}

private const val SPLASH_ZOOM = 1.2f

/** Toca a animação da splash uma única vez e chama [onFinished] exatamente quando ela termina. */
@Composable
fun MainScreen(onFinished: () -> Unit) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.splashscreen))
    val animatable = rememberLottieAnimatable()

    LaunchedEffect(composition) {
        val loadedComposition = composition ?: return@LaunchedEffect
        animatable.animate(loadedComposition, iterations = 1)
        onFinished()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OliusFundoTela),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LottieAnimation(
            composition = composition,
            progress = { animatable.progress },
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .scale(SPLASH_ZOOM)
        )
    }
}
