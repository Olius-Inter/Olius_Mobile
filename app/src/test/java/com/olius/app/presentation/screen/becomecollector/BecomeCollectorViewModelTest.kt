package com.olius.app.presentation.screen.becomecollector

import com.olius.app.MainDispatcherRule
import com.olius.app.data.repository.MockCollectionPointRepository
import com.olius.app.domain.entity.BecomeCollectorRequest
import com.olius.app.domain.entity.CepAddress
import com.olius.app.domain.repository.CepRepository
import com.olius.app.domain.repository.CollectionPointRepository
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class BecomeCollectorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val osasco = CepAddress("06010000", "SP", "Osasco", "Centro", "Rua Harmonia")

    /** Fake do CEP: responde [result] e guarda os CEPs consultados. */
    private class FakeCepRepository(var result: Result<CepAddress?>) : CepRepository {
        val lookups = mutableListOf<String>()
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun lookup(cep: String): Result<CepAddress?> {
            lookups += cep
            gate?.await()
            return result
        }
    }

    /** Guarda o request enviado, para conferir a UF. */
    private class RecordingRepository : CollectionPointRepository by MockCollectionPointRepository() {
        var sent: BecomeCollectorRequest? = null
        override suspend fun becomeCollector(request: BecomeCollectorRequest): Result<Unit> {
            sent = request
            return Result.success(Unit)
        }
    }

    private val cep = FakeCepRepository(Result.success(osasco))
    private val points = RecordingRepository()
    private val viewModel by lazy { BecomeCollectorViewModel(points, cep) }

    private fun fillRest() {
        viewModel.onFieldChange(CollectorField.NUMBER, "10")
        viewModel.onFieldChange(CollectorField.OPENING_HOURS, "Seg-Sex 10h - 16h")
    }

    @Test
    fun `nao tem campo UF na tela`() {
        assertFalse(CollectorField.entries.any { it.label == "UF" })
    }

    @Test
    fun `CEP aceita so digitos e consulta ao chegar em 8`() {
        viewModel.onFieldChange(CollectorField.CEP, "06010-00")
        assertTrue(cep.lookups.isEmpty())

        viewModel.onFieldChange(CollectorField.CEP, "06010-000x")
        assertEquals("06010000", viewModel.uiState.value.value(CollectorField.CEP))
        assertEquals(listOf("06010000"), cep.lookups)
    }

    @Test
    fun `CEP ok preenche UF escondida, cidade, bairro e rua e libera o envio`() {
        viewModel.onFieldChange(CollectorField.CEP, "06010000")
        fillRest()

        val state = viewModel.uiState.value
        assertEquals(CepStatus.FOUND, state.cepStatus)
        assertEquals("SP", state.state)
        assertEquals("Osasco", state.value(CollectorField.CITY))
        assertEquals("Centro", state.value(CollectorField.NEIGHBORHOOD))
        assertEquals("Rua Harmonia", state.value(CollectorField.STREET))
        assertNull(state.cepMessage)
        assertTrue(state.canSend)
    }

    @Test
    fun `campos preenchidos pelo CEP continuam editaveis`() {
        viewModel.onFieldChange(CollectorField.CEP, "06010000")
        viewModel.onFieldChange(CollectorField.STREET, "Rua Harmonia, fundos")

        assertEquals("Rua Harmonia, fundos", viewModel.uiState.value.value(CollectorField.STREET))
        assertEquals(CepStatus.FOUND, viewModel.uiState.value.cepStatus)
    }

    @Test
    fun `envio leva a UF que veio do CEP`() {
        viewModel.onFieldChange(CollectorField.CEP, "06010000")
        fillRest()
        viewModel.onSend()

        assertTrue(viewModel.uiState.value.isSent)
        assertEquals("SP", points.sent?.state)
        assertEquals("06010000", points.sent?.cep)
    }

    @Test
    fun `CEP inexistente mostra mensagem e bloqueia o envio`() {
        cep.result = Result.success(null)
        viewModel.onFieldChange(CollectorField.CEP, "99999999")
        viewModel.onFieldChange(CollectorField.STREET, "Rua X")
        viewModel.onFieldChange(CollectorField.NEIGHBORHOOD, "Centro")
        viewModel.onFieldChange(CollectorField.CITY, "Osasco")
        fillRest()

        val state = viewModel.uiState.value
        assertEquals(CepStatus.NOT_FOUND, state.cepStatus)
        assertEquals("CEP não encontrado. Confira os números.", state.cepMessage)
        assertFalse(state.canSend)
        viewModel.onSend()
        assertFalse(viewModel.uiState.value.isSent)
    }

    @Test
    fun `falha de rede mostra mensagem, bloqueia e permite tentar de novo`() {
        cep.result = Result.failure(IOException("sem internet"))
        viewModel.onFieldChange(CollectorField.CEP, "06010000")
        fillRest()

        assertEquals(CepStatus.NETWORK_ERROR, viewModel.uiState.value.cepStatus)
        assertTrue(viewModel.uiState.value.cepMessage!!.startsWith("Sem conexão"))
        assertFalse(viewModel.uiState.value.canSend)

        cep.result = Result.success(osasco)
        viewModel.onRetryCep()
        assertEquals(CepStatus.FOUND, viewModel.uiState.value.cepStatus)
        assertTrue(viewModel.uiState.value.canSend)
    }

    @Test
    fun `apagar um digito limpa a UF e bloqueia de novo`() {
        viewModel.onFieldChange(CollectorField.CEP, "06010000")
        fillRest()
        viewModel.onFieldChange(CollectorField.CEP, "0601000")

        assertEquals("", viewModel.uiState.value.state)
        assertEquals(CepStatus.IDLE, viewModel.uiState.value.cepStatus)
        assertFalse(viewModel.uiState.value.canSend)
    }

    @Test
    fun `enquanto consulta mostra carregando e nao envia`() {
        cep.gate = CompletableDeferred()
        viewModel.onFieldChange(CollectorField.CEP, "06010000")
        fillRest()

        assertEquals(CepStatus.LOADING, viewModel.uiState.value.cepStatus)
        assertEquals("Buscando endereço...", viewModel.uiState.value.cepMessage)
        assertFalse(viewModel.uiState.value.canSend)

        cep.gate!!.complete(Unit)
        assertEquals(CepStatus.FOUND, viewModel.uiState.value.cepStatus)
    }
}
