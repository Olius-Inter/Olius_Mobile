package com.olius.app.presentation.screen.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olius.app.data.local.CollectorPromptStore
import com.olius.app.domain.entity.CollectionPoint
import com.olius.app.domain.entity.CollectionPointType
import com.olius.app.domain.entity.MapBounds
import com.olius.app.domain.entity.PointSuggestion
import com.olius.app.domain.entity.distanceKmBetween
import com.olius.app.domain.repository.CollectionPointRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

val BECOME_COLLECTOR_PROMPT_INTERVAL_MILLIS: Long = TimeUnit.DAYS.toMillis(10)

/** Centro de São Paulo: onde o mapa abre enquanto o GPS não responde. */
const val DEFAULT_LATITUDE = -23.5505
const val DEFAULT_LONGITUDE = -46.6333
const val DEFAULT_ZOOM = 12.0
private const val FOCUS_ZOOM = 16.0

/** Área inicial (~ cidade de São Paulo) para a primeira carga, antes do mapa medir a tela. */
private val DEFAULT_BOUNDS = MapBounds(south = -23.75, west = -46.85, north = -23.35, east = -46.35)

/** Mínimo de letras para o autocomplete chamar a API. */
const val MIN_SEARCH_LENGTH = 3

/** Tela de Pontos de Coleta: mapa real (osmdroid/OpenStreetMap). */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class MapViewModel(
    private val promptStore: CollectorPromptStore,
    private val now: () -> Long = System::currentTimeMillis,
    /** Quem já é Coletor/PEV não vê o aviso nem o botão "Vire Coletor". */
    private val isCollector: Boolean = false,
    private val repository: CollectionPointRepository,
    /** Espera depois da última tecla antes de buscar (0 nos testes). */
    searchDebounceMillis: Long = 300
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        MapUiState(
            showBecomeCollectorBanner = !isCollector && shouldShowBecomeCollectorPrompt(),
            canBecomeCollector = !isCollector
        )
    )
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val searchQueries = MutableStateFlow("")
    private var pointsJob: Job? = null
    private var detailsJob: Job? = null

    init {
        onViewportChanged(DEFAULT_BOUNDS)

        searchQueries
            .debounce(searchDebounceMillis)
            .map { it.trim() }
            .distinctUntilChanged()
            .flatMapLatest { query ->
                if (query.length < MIN_SEARCH_LENGTH) flowOf(emptyList())
                else flow {
                    _uiState.update { it.copy(isSearching = true) }
                    emit(repository.searchPoints(query).getOrDefault(emptyList()).filter { it.type in MAP_FILTER_TYPES })
                }
            }
            .onEach { list -> _uiState.update { it.copy(suggestions = list, isSearching = false) } }
            .launchIn(viewModelScope)
    }

    /** Aparece uma vez (na primeira abertura do mapa) e depois só volta quando se passarem 10 dias desde a última vez que apareceu. */
    private fun shouldShowBecomeCollectorPrompt(): Boolean {
        val currentTime = now()
        val lastShown = promptStore.lastShownAtMillis()
        val show = lastShown == null || currentTime - lastShown >= BECOME_COLLECTOR_PROMPT_INTERVAL_MILLIS
        if (show) promptStore.markShown(currentTime)
        return show
    }

    /** O mapa parou de mexer: busca só os pins da área visível. */
    fun onViewportChanged(bounds: MapBounds) {
        pointsJob?.cancel()
        pointsJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPoints = true) }
            repository.getPointsInArea(bounds)
                .onSuccess { list -> _uiState.update { it.copy(points = withDistance(list, it.userLocation)) } }
            _uiState.update { it.copy(isLoadingPoints = false) }
        }
    }

    /** GPS respondeu: recalcula a distância de todos os pins e centraliza o mapa na pessoa. */
    fun onUserLocation(latitude: Double, longitude: Double, centerMap: Boolean = true) {
        val location = UserLocation(latitude, longitude)
        _uiState.update {
            it.copy(
                userLocation = location,
                points = withDistance(it.points, location),
                cameraRequest = if (centerMap) newCamera(latitude, longitude, it.cameraRequest, zoom = 14.0) else it.cameraRequest
            )
        }
    }

    private fun withDistance(points: List<CollectionPoint>, location: UserLocation?): List<CollectionPoint> =
        if (location == null) points
        else points.map { it.copy(distanceKm = distanceKmBetween(location.latitude, location.longitude, it.latitude, it.longitude)) }

    private fun newCamera(lat: Double, lng: Double, previous: CameraRequest?, zoom: Double = FOCUS_ZOOM) =
        CameraRequest(lat, lng, zoom, id = (previous?.id ?: 0L) + 1)

    fun onSearchQueryChange(value: String) {
        _uiState.update { it.copy(searchQuery = value) }
        searchQueries.value = value
    }

    /** Escolheu uma sugestão: limpa a busca, leva o mapa até o ponto e abre a ficha. */
    fun onSuggestionSelected(suggestion: PointSuggestion) {
        _uiState.update {
            it.copy(
                searchQuery = "",
                suggestions = emptyList(),
                cameraRequest = newCamera(suggestion.latitude, suggestion.longitude, it.cameraRequest)
            )
        }
        searchQueries.value = ""
        onPointSelected(suggestion.id)
    }

    /** Liga/desliga um tipo no filtro. */
    fun onToggleType(type: CollectionPointType) {
        if (type !in MAP_FILTER_TYPES) return
        _uiState.update { state ->
            val types = if (type in state.activeTypes) state.activeTypes - type else state.activeTypes + type
            if (types.isEmpty()) state else state.copy(activeTypes = types)
        }
    }

    /** Toque no pin/card: abre a ficha na hora com o resumo e busca o resto (telefone, CNPJ...). */
    fun onPointSelected(pointId: String) {
        detailsJob?.cancel()
        _uiState.update {
            it.copy(selectedPointId = pointId, selectedDetails = null, isLoadingDetails = true, detailsError = null)
        }
        detailsJob = viewModelScope.launch {
            repository.getPointDetails(pointId)
                .onSuccess { details -> _uiState.update { it.copy(selectedDetails = details, isLoadingDetails = false) } }
                .onFailure {
                    _uiState.update { it.copy(isLoadingDetails = false, detailsError = "Não foi possível carregar os detalhes.") }
                }
        }
    }

    fun onDismissPointDetails() {
        detailsJob?.cancel()
        _uiState.update {
            it.copy(selectedPointId = null, selectedDetails = null, isLoadingDetails = false, detailsError = null)
        }
    }

    fun onDismissBanner() {
        _uiState.update { it.copy(showBecomeCollectorBanner = false) }
    }

    fun onBecomeCollectorClick() {
        _uiState.update { it.copy(isBecomeCollectorDialogOpen = true) }
    }

    fun onDismissBecomeCollectorDialog() {
        _uiState.update { it.copy(isBecomeCollectorDialogOpen = false) }
    }

    /** Diálogo antigo; a tela agora navega direto para "Virando um Coletor" (BecomeCollectorScreen). */
    fun onConfirmBecomeCollector() {
        _uiState.update { it.copy(isBecomeCollectorDialogOpen = false, showBecomeCollectorBanner = false) }
    }
}
