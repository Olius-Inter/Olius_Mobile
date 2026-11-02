package com.olius.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.olius.app.domain.entity.ThemeMode
import com.olius.app.domain.repository.AppPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Tema e notificações em `SharedPreferences` próprias (`olius_prefs`). */
class SharedPrefsAppPreferences(private val prefs: SharedPreferences) : AppPreferencesRepository {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    private val theme = MutableStateFlow(ThemeMode.fromStored(prefs.getString(KEY_THEME, null)))
    override val themeMode: StateFlow<ThemeMode> = theme.asStateFlow()

    override fun setThemeMode(mode: ThemeMode) {
        prefs.edit { putString(KEY_THEME, mode.name) }
        theme.value = mode
    }

    override fun notificationsEnabled(): Boolean = prefs.getBoolean(KEY_NOTIFICATIONS, true)

    override fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_NOTIFICATIONS, enabled) }
    }

    private companion object {
        const val PREFS_NAME = "olius_prefs"
        const val KEY_THEME = "theme_mode"
        const val KEY_NOTIFICATIONS = "notifications_enabled"
    }
}

/** Mesmas preferências só na memória (testes e antes do `AppContainer.init`). */
class InMemoryAppPreferences(
    theme: ThemeMode = ThemeMode.DEFAULT,
    private var notifications: Boolean = true
) : AppPreferencesRepository {

    private val themeFlow = MutableStateFlow(theme)
    override val themeMode: StateFlow<ThemeMode> = themeFlow.asStateFlow()

    override fun setThemeMode(mode: ThemeMode) { themeFlow.value = mode }

    override fun notificationsEnabled() = notifications

    override fun setNotificationsEnabled(enabled: Boolean) { notifications = enabled }
}
