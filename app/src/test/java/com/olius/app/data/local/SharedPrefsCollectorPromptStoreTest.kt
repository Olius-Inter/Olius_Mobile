package com.olius.app.data.local

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SharedPrefsCollectorPromptStoreTest {

    private val editor = mockk<SharedPreferences.Editor>(relaxed = true)
    private val prefs = mockk<SharedPreferences> {
        every { edit() } returns editor
    }
    private val store = SharedPrefsCollectorPromptStore(prefs)

    @Test
    fun `sem registro devolve null`() {
        every { prefs.contains(any()) } returns false

        assertNull(store.lastShownAtMillis())
    }

    @Test
    fun `devolve o horario salvo`() {
        every { prefs.contains(any()) } returns true
        every { prefs.getLong(any(), any()) } returns 1234L

        assertEquals(1234L, store.lastShownAtMillis())
    }

    @Test
    fun `markShown salva o horario`() {
        every { editor.putLong(any(), any()) } returns editor

        store.markShown(5678L)

        verify { editor.putLong(any(), 5678L) }
        verify { editor.apply() }
    }

    @Test
    fun `construtor com Context usa as preferencias do app`() {
        val context = mockk<Context>()
        every { context.applicationContext } returns context
        every { context.getSharedPreferences(any(), Context.MODE_PRIVATE) } returns prefs
        every { prefs.contains(any()) } returns false

        assertNull(SharedPrefsCollectorPromptStore(context).lastShownAtMillis())
    }
}
