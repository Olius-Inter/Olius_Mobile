package com.olius.app.data.local

import android.content.Context
import com.olius.app.domain.entity.ProfileType
import com.olius.app.domain.entity.UserProfile

/**
 * Guarda o perfil (id do Postgres + tipo) no aparelho: abrir o app não precisa
 * chamar a API para saber se é estabelecimento ou cidadão. O login em si
 * continua com o Firebase, que mantém a sessão (token renovado sozinho).
 */
interface SessionStore {
    fun load(): UserProfile?
    fun save(profile: UserProfile)
    fun clear()
}

class InMemorySessionStore : SessionStore {
    private var profile: UserProfile? = null
    override fun load() = profile
    override fun save(profile: UserProfile) { this.profile = profile }
    override fun clear() { profile = null }
}

class SharedPrefsSessionStore(context: Context) : SessionStore {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun load(): UserProfile? {
        val type = prefs.getString(KEY_TYPE, null) ?: return null
        return UserProfile(
            id = prefs.getString(KEY_ID, "").orEmpty(),
            name = prefs.getString(KEY_NAME, "").orEmpty(),
            email = prefs.getString(KEY_EMAIL, "").orEmpty(),
            profileType = runCatching { ProfileType.valueOf(type) }.getOrDefault(ProfileType.ESTABLISHMENT),
            pendingSync = prefs.getBoolean(KEY_PENDING, false),
            phone = prefs.getString(KEY_PHONE, null),
            birthDate = prefs.getString(KEY_BIRTH, null),
            isCollector = prefs.getBoolean(KEY_COLLECTOR, false),
            qrToken = prefs.getString(KEY_QR, null)
        )
    }

    override fun save(profile: UserProfile) {
        prefs.edit()
            .putString(KEY_ID, profile.id)
            .putString(KEY_NAME, profile.name)
            .putString(KEY_EMAIL, profile.email)
            .putString(KEY_TYPE, profile.profileType.name)
            .putBoolean(KEY_PENDING, profile.pendingSync)
            .putString(KEY_PHONE, profile.phone)
            .putString(KEY_BIRTH, profile.birthDate)
            .putBoolean(KEY_COLLECTOR, profile.isCollector)
            .putString(KEY_QR, profile.qrToken)
            .apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREFS_NAME = "olius_session"
        const val KEY_ID = "user_id"
        const val KEY_NAME = "name"
        const val KEY_EMAIL = "email"
        const val KEY_TYPE = "profile_type"
        const val KEY_PENDING = "pending_sync"
        const val KEY_PHONE = "phone"
        const val KEY_BIRTH = "birth_date"
        const val KEY_COLLECTOR = "is_collector"
        const val KEY_QR = "qr_token"
    }
}
