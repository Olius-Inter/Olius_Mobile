package com.olius.app.data.local

import com.olius.app.data.local.db.UserProfileDao
import com.olius.app.data.local.db.UserProfileEntity
import com.olius.app.domain.entity.ProfileType
import com.olius.app.domain.entity.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionStoreTest {
    private val profile = UserProfile(
        id = "u-1",
        name = "Ana Lima",
        email = "ana@olius.app",
        profileType = ProfileType.CITIZEN,
        phone = "5511999990000",
        isCollector = true,
        qrToken = "qr-1"
    )

    /** DAO em memória: a tabela `user_profile` com uma linha só. */
    private class FakeDao : UserProfileDao {
        var row: UserProfileEntity? = null
        var loads = 0
        override fun load(): UserProfileEntity? { loads++; return row }
        override fun clear() { row = null }
        override fun upsert(profile: UserProfileEntity) { row = profile }
    }

    @Test
    fun `em memoria guarda e limpa o perfil`() {
        val store = InMemorySessionStore()
        assertNull(store.load())

        store.save(profile)
        assertEquals(profile, store.load())

        store.clear()
        assertNull(store.load())
    }

    @Test
    fun `room salva e carrega todos os campos e so le o disco uma vez`() {
        val dao = FakeDao()
        val store = RoomSessionStore(dao)
        assertNull(store.load())

        store.save(profile)

        assertEquals(UserProfileEntity.from(profile), dao.row)
        assertEquals(profile, store.load())
        assertEquals(profile, RoomSessionStore(dao).load())
        assertEquals(2, dao.loads)
    }

    @Test
    fun `tipo desconhecido no banco vira estabelecimento`() {
        val dao = FakeDao().apply { row = UserProfileEntity.from(profile).copy(profileType = "ADMIN") }

        assertEquals(ProfileType.ESTABLISHMENT, RoomSessionStore(dao).load()?.profileType)
    }

    @Test
    fun `clear apaga o perfil do banco`() {
        val dao = FakeDao()
        val store = RoomSessionStore(dao)
        store.save(profile)

        store.clear()

        assertNull(store.load())
        assertNull(dao.row)
    }
}
