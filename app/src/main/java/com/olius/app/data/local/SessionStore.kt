package com.olius.app.data.local

import com.olius.app.data.local.db.UserProfileDao
import com.olius.app.data.local.db.UserProfileEntity
import com.olius.app.domain.entity.UserProfile

/** Guarda o perfil da Core (id + tipo + nome...) no aparelho. */
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

/** Perfil no SQLite (Room, tabela `user_profile`), com uma cópia em memória: o disco só é lido na primeira vez. */
class RoomSessionStore(private val dao: UserProfileDao) : SessionStore {
    private val lock = Any()
    private var loaded = false
    private var cached: UserProfile? = null

    override fun load(): UserProfile? = synchronized(lock) {
        if (!loaded) {
            cached = dao.load()?.toDomain()
            loaded = true
        }
        cached
    }

    override fun save(profile: UserProfile) = synchronized(lock) {
        dao.replace(UserProfileEntity.from(profile))
        cached = profile
        loaded = true
    }

    override fun clear() = synchronized(lock) {
        dao.clear()
        cached = null
        loaded = true
    }
}
