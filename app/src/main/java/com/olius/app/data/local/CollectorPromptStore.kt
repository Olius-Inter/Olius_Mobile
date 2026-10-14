package com.olius.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Guarda quando o aviso "Vire um ponto de Coleta" (tela de Mapa) apareceu
 * pela última vez — é o que permite mostrar ele uma vez e só de novo depois
 * de 10 dias (ver `MapViewModel`).
 */
interface CollectorPromptStore {
    fun lastShownAtMillis(): Long?
    fun markShown(atMillis: Long)
}

class SharedPrefsCollectorPromptStore(
    private val prefs: SharedPreferences
) : CollectorPromptStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    override fun lastShownAtMillis(): Long? =
        if (prefs.contains(KEY_LAST_SHOWN)) prefs.getLong(KEY_LAST_SHOWN, 0L) else null

    override fun markShown(atMillis: Long) {
        prefs.edit { putLong(KEY_LAST_SHOWN, atMillis) }
    }

    private companion object {
        const val PREFS_NAME = "olius_map_prefs"
        const val KEY_LAST_SHOWN = "become_collector_prompt_last_shown"
    }
}
