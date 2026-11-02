package com.olius.app.presentation.screen.history

import com.olius.app.MainDispatcherRule
import com.olius.app.data.remote.ApiException
import com.olius.app.data.repository.MockHistoryRepository
import com.olius.app.domain.entity.CitizenHistory
import com.olius.app.domain.entity.CollectionStatus
import com.olius.app.domain.entity.EstablishmentHistory
import com.olius.app.domain.entity.ProfileType
import com.olius.app.domain.repository.HistoryRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class HistoryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `estabelecimento carrega o historico de coletas`() {
        val state = HistoryViewModel(ProfileType.ESTABLISHMENT, MockHistoryRepository()).uiState.value

        assertFalse(state.isLoading)
        assertNotNull(state.establishment)
        assertNull(state.citizen)
    }

    @Test
    fun `cidadao carrega o historico de registro`() {
        val state = HistoryViewModel(ProfileType.CITIZEN, MockHistoryRepository()).uiState.value

        assertNotNull(state.citizen)
        assertNull(state.establishment)
    }

    @Test
    fun `filtro de status e local e Todos volta a lista inteira`() {
        val viewModel = HistoryViewModel(ProfileType.ESTABLISHMENT, MockHistoryRepository())
        val total = viewModel.uiState.value.filteredItems.size

        viewModel.onFilterSelected(CollectionStatus.PENDING)
        assertTrue(viewModel.uiState.value.filteredItems.all { it.status == CollectionStatus.PENDING })

        viewModel.onFilterSelected(null)
        assertEquals(total, viewModel.uiState.value.filteredItems.size)
    }

    @Test
    fun `filtros sao so Coletado e Pendente nessa ordem`() {
        assertEquals(listOf(CollectionStatus.COLLECTED, CollectionStatus.PENDING), CollectionStatus.entries)
        assertEquals(listOf("Coletado", "Pendente"), CollectionStatus.entries.map { it.label })
    }

    @Test
    fun `cada filtro mostra so o seu status`() {
        val viewModel = HistoryViewModel(ProfileType.ESTABLISHMENT, MockHistoryRepository())

        CollectionStatus.entries.forEach { status ->
            viewModel.onFilterSelected(status)
            val items = viewModel.uiState.value.filteredItems
            assertTrue(items.isNotEmpty())
            assertTrue(items.all { it.status == status })
        }
    }

    @Test
    fun `status antigos e desconhecidos da API viram Coletado ou Pendente`() {
        assertEquals(CollectionStatus.COLLECTED, CollectionStatus.fromApi("COLLECTED"))
        assertEquals(CollectionStatus.COLLECTED, CollectionStatus.fromApi("PROCESSED"))
        assertEquals(CollectionStatus.PENDING, CollectionStatus.fromApi("PENDING"))
        assertEquals(CollectionStatus.PENDING, CollectionStatus.fromApi("REGISTERED"))
        assertEquals(CollectionStatus.PENDING, CollectionStatus.fromApi("QUALQUER"))
        assertEquals(CollectionStatus.PENDING, CollectionStatus.fromApi(""))
    }

    @Test
    fun `sem internet mostra Sem conexao`() {
        val offline = object : HistoryRepository {
            override suspend fun getEstablishmentHistory(): Result<EstablishmentHistory> = Result.failure(IOException())
            override suspend fun getCitizenHistory(): Result<CitizenHistory> = Result.failure(IOException())
        }

        val state = HistoryViewModel(ProfileType.ESTABLISHMENT, offline).uiState.value

        assertTrue(state.isOffline)
        assertNull(state.errorMessage)
    }

    @Test
    fun `erro 4xx da API nao abre a tela Sem conexao`() {
        val rejected = object : HistoryRepository {
            override suspend fun getEstablishmentHistory(): Result<EstablishmentHistory> =
                Result.failure(ApiException(422, "regra"))
            override suspend fun getCitizenHistory(): Result<CitizenHistory> = Result.failure(ApiException(422, "regra"))
        }

        val state = HistoryViewModel(ProfileType.ESTABLISHMENT, rejected).uiState.value

        assertFalse(state.isOffline)
        assertNotNull(state.errorMessage)
    }
}
