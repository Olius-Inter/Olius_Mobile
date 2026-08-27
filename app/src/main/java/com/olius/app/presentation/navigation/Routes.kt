package com.olius.app.presentation.navigation

sealed class Routes(val route: String) {
    object Splash : Routes("splash")
    object PerfilType : Routes("perfilType")
    object Home : Routes("home")
    object ForgotPassword : Routes("forgotPassword")
}
