package com.olius.app.data.local

import android.content.Context
import android.content.SharedPreferences
import com.olius.app.domain.entity.ProfileType
import com.olius.app.domain.entity.UserProfile
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionStoreTest {

    private val profile = UserProfile(
        id = "u-1",
        name = "Ana Lima",
        email = "ana@olius.app",
        profileType = ProfileType.CITIZEN,
        pendingSync = true,
        phone = "11999990000",
        birthDate = "2000-01-31",
        isCollector = true,
        qrToken = "qr-1"
    )

    private val values = mutableMapOf<String, Any?>()
    private val editor = mockk<SharedPreferences.Editor>(relaxed = true) {
        every { putString(any(), any()) } answers { values[firstArg()] = secondArg<String?>(); self as SharedPreferences.Editor }
        every { putBoolean(any(), any()) } answers { values[firstArg()] = secondArg<Boolean>(); self as SharedPreferences.Editor }
        every { clear() } answers { values.clear(); self as SharedPreferences.Editor }
    }
    private val prefs = mockk<SharedPreferences> {
        every { edit() } returns editor
        every { getString(any(), any()) } answers { values[firstArg()] as String? ?: secondArg() }
        every { getBoolean(any(), any()) } answers { values[firstArg()] as Boolean? ?: secondArg() }
    }
    private val context = mockk<Context> {
        every { applicationContext } returns this
        every { getSharedPreferences(any(), Context.MODE_PRIVATE) } returns prefs
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
    fun `shared preferences salva e carrega todos os campos`() {
        val store = SharedPrefsSessionStore(context)
        assertNull(store.load())

        store.save(profile)

        assertEquals(profile, store.load())
        verify { editor.apply() }
    }

    @Test
    fun `tipo desconhecido no aparelho vira estabelecimento`() {
        values["profile_type"] = "ADMIN"

        val loaded = SharedPrefsSessionStore(context).load()

        assertEquals(UserProfile("", "", "", ProfileType.ESTABLISHMENT), loaded)
    }

    @Test
    fun `clear apaga a sessao`() {
        val store = SharedPrefsSessionStore(context)
        store.save(profile)

        store.clear()

        assertNull(store.load())
    }
}
