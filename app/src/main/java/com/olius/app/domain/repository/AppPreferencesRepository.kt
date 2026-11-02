package com.olius.app.domain.repository

import com.olius.app.domain.entity.ThemeMode
import kotlinx.coroutines.flow.StateFlow

/**
 * Preferências que ficam só no aparelho (não vão para a Core): tema e
 * notificações. O tema é um [StateFlow] para o app inteiro trocar na hora.
 */
interface AppPreferencesRepository {

    val themeMode: StateFlow<ThemeMode>

    fun setThemeMode(mode: ThemeMode)

    /** Chave "Notificações" do Perfil. Ainda não há push: só guarda a escolha. */
    fun notificationsEnabled(): Boolean

    fun setNotificationsEnabled(enabled: Boolean)
}
