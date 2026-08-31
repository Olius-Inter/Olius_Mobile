package com.olius.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.olius.app.presentation.screen.main.MainScaffoldScreen
import com.olius.app.presentation.screen.PerfilType.ForgotPasswordScreen
import com.olius.app.presentation.screen.PerfilType.PerfilTypeScreen
import com.olius.app.presentation.splash.MainScreen

@Composable
fun NavGraph(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Routes.Splash.route
    ) {
        composable(Routes.Splash.route) {
            MainScreen(
                onFinished = {
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
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.Main.route) {
            MainScaffoldScreen(
                onLogout = {
                    navController.navigate(Routes.PerfilType.route) {
                        popUpTo(Routes.Main.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
