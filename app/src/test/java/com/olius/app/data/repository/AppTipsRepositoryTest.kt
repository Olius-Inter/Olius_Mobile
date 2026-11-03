package com.olius.app.data.repository

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppTipsRepositoryTest {
    @Test
    fun `aba Dicas tem os 4 cards do Figma na ordem`() = runTest {
        val tips = AppTipsRepository().getTips().getOrThrow()

        assertEquals(
            listOf("Armazenamento correto", "Volume máximo", "Não descarte inadequadamente", "Tipo de óleo aceito"),
            tips.map { it.title }
        )
    }

    @Test
    fun `descricoes cabem em ate 3 linhas do card`() = runTest {
        AppTipsRepository().getTips().getOrThrow().forEach {
            assertTrue("${it.title}: ${it.description.length} caracteres", it.description.length <= 190)
        }
    }
}
