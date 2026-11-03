package com.olius.app.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.olius.app.data.remote.AppContainer
import com.olius.app.presentation.screen.main.MainScaffoldScreen
import com.olius.app.presentation.screen.PerfilType.ForgotPasswordScreen
import com.olius.app.presentation.screen.PerfilType.PerfilTypeScreen
import com.olius.app.presentation.screen.error.ConnectionErrorScreen
import com.olius.app.presentation.screen.qrcode.QrCodeScreen
import com.olius.app.presentation.splash.SplashRoute

/** Saída da Splash e da Main quando o destino é o PerfilType. */
private val exitKeepingScreenUnderPerfilType:
    AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    if (targetState.destination.route == Routes.PerfilType.route) ExitTransition.None
    else fadeOut(tween(NAV_DEFAULT_FADE_MS))
}

private const val NAV_DEFAULT_FADE_MS = 700

@Composable
fun NavGraph(
    navController: NavHostController
) {
    LaunchedEffect(navController) {
        AppContainer.sessionExpired.collect {
            val current = navController.currentDestination?.route
            if (current != Routes.PerfilType.route && current != Routes.Splash.route) {
                navController.navigate(Routes.PerfilType.route) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.Splash.route
    ) {
        composable(Routes.Splash.route, exitTransition = exitKeepingScreenUnderPerfilType) {
            SplashRoute(
                onNavigateHome = {
                    navController.navigate(Routes.Main.route) {
                        popUpTo(Routes.Splash.route) { inclusive = true }
                    }
                },
                onNavigatePerfilType = {
                    navController.navigate(Routes.PerfilType.route) {
                        popUpTo(Routes.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.PerfilType.route) {
            PerfilTypeScreen(
                onNavigateHome = {
                    navController.navigate(Routes.Main.route) {
                        popUpTo(Routes.PerfilType.route) { inclusive = true }
                    }
                },
                onForgotPassword = {
                    navController.navigate(Routes.ForgotPassword.route)
                }
            )
        }
        composable(Routes.ForgotPassword.route) {
            ForgotPasswordScreen(
                onBack = { navController.popBackStack() },
                onPasswordReset = { navController.popBackStack() }
            )
        }
        composable(Routes.Main.route, exitTransition = exitKeepingScreenUnderPerfilType) {
            MainScaffoldScreen(
                onLogout = {
                    navController.navigate(Routes.PerfilType.route) {
                        popUpTo(Routes.Main.route) { inclusive = true }
                    }
                },
                onOpenQrCode = { navController.navigate(Routes.QrCode.route) },
                onConnectionError = {
                    navController.navigate(Routes.ConnectionError.route) {
                        popUpTo(Routes.Main.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(Routes.QrCode.route) {
            QrCodeScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ConnectionError.route) {
            ConnectionErrorScreen(
                onNavigateHome = {
                    navController.navigate(Routes.Main.route) {
                        popUpTo(Routes.ConnectionError.route) { inclusive = true }
                    }
                },
                onNavigatePerfilType = {
                    navController.navigate(Routes.PerfilType.route) {
                        popUpTo(Routes.ConnectionError.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
