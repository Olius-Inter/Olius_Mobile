package com.olius.app.presentation.screen.map

import com.olius.app.data.repository.MockCollectionPointRepository
import com.olius.app.MainDispatcherRule
import com.olius.app.domain.entity.CollectionPointType
import com.olius.app.domain.entity.MapBounds
import com.olius.app.data.local.CollectorPromptStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

class MapViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakePromptStore(var lastShown: Long? = null) : CollectorPromptStore {
        override fun lastShownAtMillis(): Long? = lastShown
        override fun markShown(atMillis: Long) {
            lastShown = atMillis
        }
    }

    private val now = TimeUnit.DAYS.toMillis(100)
    private val store = FakePromptStore()

    private fun viewModel(currentTime: Long = now) =
        MapViewModel(promptStore = store, now = { currentTime }, repository = MockCollectionPointRepository(), searchDebounceMillis = 0)

    @Test
    fun `aviso aparece na primeira vez e marca quando apareceu`() {
        val vm = viewModel()

        assertTrue(vm.uiState.value.showBecomeCollectorBanner)
        assertEquals(now, store.lastShown)
    }

    @Test
    fun `aviso nao volta antes de 10 dias`() {
        store.lastShown = now - TimeUnit.DAYS.toMillis(9)

        val vm = viewModel()

        assertFalse(vm.uiState.value.showBecomeCollectorBanner)
        assertEquals(now - TimeUnit.DAYS.toMillis(9), store.lastShown)
    }

    @Test
    fun `aviso volta depois de 10 dias`() {
        store.lastShown = now - BECOME_COLLECTOR_PROMPT_INTERVAL_MILLIS

        assertTrue(viewModel().uiState.value.showBecomeCollectorBanner)
        assertEquals(now, store.lastShown)
    }

    @Test
    fun `aviso aparece so uma vez mesmo abrindo o mapa de novo no mesmo dia`() {
        viewModel()

        assertFalse(viewModel(now + TimeUnit.HOURS.toMillis(1)).uiState.value.showBecomeCollectorBanner)
    }

    @Test
    fun `fechar o aviso esconde ele`() {
        val vm = viewModel()

        vm.onDismissBanner()

        assertFalse(vm.uiState.value.showBecomeCollectorBanner)
    }

    @Test
    fun `confirmar virar coletor fecha o dialogo e o aviso`() {
        val vm = viewModel()

        vm.onBecomeCollectorClick()
        assertTrue(vm.uiState.value.isBecomeCollectorDialogOpen)

        vm.onConfirmBecomeCollector()
        assertFalse(vm.uiState.value.isBecomeCollectorDialogOpen)
        assertFalse(vm.uiState.value.showBecomeCollectorBanner)
    }

    @Test
    fun `cancelar virar coletor so fecha o dialogo`() {
        val vm = viewModel()

        vm.onBecomeCollectorClick()
        vm.onDismissBecomeCollectorDialog()

        assertFalse(vm.uiState.value.isBecomeCollectorDialogOpen)
        assertTrue(vm.uiState.value.showBecomeCollectorBanner)
    }

    @Test
    fun `lista mostra so PEVs ordenados pela distancia`() {
        val points = viewModel().uiState.value.visiblePoints

        assertEquals(MAP_FILTER_TYPES.toSet(), points.map { it.type }.toSet())
        assertEquals(points.sortedWith(compareBy(nullsLast()) { it.distanceKm }), points)
    }

    @Test
    fun `filtro tem so PEV e nunca fica desligado`() {
        val vm = viewModel()
        assertEquals(setOf(CollectionPointType.PEV), vm.uiState.value.activeTypes)

        vm.onToggleType(CollectionPointType.PEV)

        assertEquals(setOf(CollectionPointType.PEV), vm.uiState.value.activeTypes)
    }

    @Test
    fun `estabelecimento que nao e PEV some do mapa e nao pode ser ligado`() {
        val vm = viewModel()
        assertTrue(vm.uiState.value.points.any { it.type == CollectionPointType.ESTABLISHMENT })

        vm.onToggleType(CollectionPointType.ESTABLISHMENT)
        vm.onToggleType(CollectionPointType.COLLECTOR)

        assertEquals(setOf(CollectionPointType.PEV), vm.uiState.value.activeTypes)
        assertTrue(vm.uiState.value.visiblePoints.all { it.type == CollectionPointType.PEV })
    }

    @Test
    fun `busca do autocomplete nao sugere estabelecimento sem PEV`() {
        val vm = viewModel()

        vm.onSearchQueryChange("padaria pao")

        assertTrue(vm.uiState.value.suggestions.isEmpty())
    }

    @Test
    fun `busca filtra por nome ou endereco sem diferenciar maiusculas`() {
        val vm = viewModel()

        vm.onSearchQueryChange("  ecoponto ")
        assertTrue(vm.uiState.value.visiblePoints.all { it.type == CollectionPointType.PEV })

        vm.onSearchQueryChange("osasco")
        assertTrue(vm.uiState.value.visiblePoints.all { it.address.contains("Osasco") })

        vm.onSearchQueryChange("nada com esse nome")
        assertTrue(vm.uiState.value.visiblePoints.isEmpty())
    }

    @Test
    fun `selecionar um ponto abre o resumo e carrega a ficha completa`() {
        val vm = viewModel()
        val point = vm.uiState.value.visiblePoints.first { it.id == "1" }

        vm.onPointSelected(point.id)
        val state = vm.uiState.value
        assertEquals(point, state.selectedPoint)
        assertTrue(state.isPointSheetOpen)
        assertFalse(state.isLoadingDetails)
        assertEquals("12345678000190", state.selectedDetails?.cnpj)
        assertNotNull(state.selectedDetails?.phone)

        vm.onDismissPointDetails()
        assertNull(vm.uiState.value.selectedPoint)
        assertNull(vm.uiState.value.selectedDetails)
        assertFalse(vm.uiState.value.isPointSheetOpen)
    }

    @Test
    fun `PEV de cidadao nao expoe telefone nem CNPJ`() {
        val vm = viewModel()

        vm.onPointSelected("5")

        val details = vm.uiState.value.selectedDetails!!
        assertNull(details.phone)
        assertNull(details.cnpj)
    }

    @Test
    fun `ponto inexistente mostra erro na ficha`() {
        val vm = viewModel()

        vm.onPointSelected("nao-existe")

        assertNotNull(vm.uiState.value.detailsError)
    }

    @Test
    fun `mapa carrega so os pins da area visivel`() {
        val vm = viewModel()

        vm.onViewportChanged(MapBounds(south = -23.565, west = -46.645, north = -23.545, east = -46.625))

        assertEquals(setOf("3", "4"), vm.uiState.value.points.map { it.id }.toSet())
    }

    @Test
    fun `gps calcula a distancia e ordena do mais perto para o mais longe`() {
        val vm = viewModel()

        vm.onUserLocation(-23.5503, -46.6339)

        val points = vm.uiState.value.visiblePoints
        assertEquals("3", points.first().id)
        assertTrue(points.first().distanceKm!! < 0.01)
        assertEquals(points.sortedBy { it.distanceKm }, points)
        assertNotNull(vm.uiState.value.cameraRequest)
    }

    @Test
    fun `autocomplete so busca a partir de 3 letras`() {
        val vm = viewModel()

        vm.onSearchQueryChange("lo")
        assertTrue(vm.uiState.value.suggestions.isEmpty())

        vm.onSearchQueryChange("loja do seu")
        assertEquals("Loja do Seu Zé", vm.uiState.value.suggestions.first().name)
    }

    @Test
    fun `autocomplete ignora acento e maiusculas`() {
        val vm = viewModel()

        vm.onSearchQueryChange("SONIA")

        assertEquals("1", vm.uiState.value.suggestions.single().id)
    }

    @Test
    fun `escolher sugestao move o mapa e abre a ficha`() {
        val vm = viewModel()
        vm.onSearchQueryChange("loja do seu")
        val suggestion = vm.uiState.value.suggestions.first()

        vm.onSuggestionSelected(suggestion)

        val state = vm.uiState.value
        assertEquals("", state.searchQuery)
        assertTrue(state.suggestions.isEmpty())
        assertEquals(suggestion.latitude, state.cameraRequest!!.latitude, 0.0)
        assertEquals(suggestion.id, state.selectedPointId)
        assertEquals("Loja do Seu Zé", state.selectedDetails?.name)
    }

    @Test
    fun `coletor nao ve o aviso nem o botao Vire Coletor`() {
        val state = MapViewModel(promptStore = store, now = { now }, isCollector = true, repository = MockCollectionPointRepository(), searchDebounceMillis = 0).uiState.value

        assertFalse(state.showBecomeCollectorBanner)
        assertFalse(state.canBecomeCollector)
    }
}
