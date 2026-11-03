package com.olius.app.domain.repository

import com.olius.app.domain.entity.ThemeMode
import kotlinx.coroutines.flow.StateFlow

interface AppPreferencesRepository {
    val themeMode: StateFlow<ThemeMode>

    fun setThemeMode(mode: ThemeMode)

    /** Chave "Notificações" do Perfil. */
    fun notificationsEnabled(): Boolean

    fun setNotificationsEnabled(enabled: Boolean)
}
