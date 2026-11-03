package com.olius.app.presentation.screen.map

import com.olius.app.domain.entity.CollectionPoint
import com.olius.app.domain.entity.CollectionPointDetails
import com.olius.app.domain.entity.CollectionPointType
import com.olius.app.domain.entity.VISIBLE_POINT_TYPES
import com.olius.app.domain.entity.PointSuggestion

/** Posição (latitude/longitude) da pessoa, vinda do GPS. */
data class UserLocation(val latitude: Double, val longitude: Double)

data class CameraRequest(val latitude: Double, val longitude: Double, val zoom: Double, val id: Long)

/** Tipos que o mapa mostra e filtra: os mesmos do resto do app (só PEV). */
val MAP_FILTER_TYPES: List<CollectionPointType> = VISIBLE_POINT_TYPES

data class MapUiState(
    /** Pins da área visível (bounding box), já com a distância calculada pelo GPS. */
    val points: List<CollectionPoint> = emptyList(),
    val isLoadingPoints: Boolean = false,
    val searchQuery: String = "",
    /** Autocomplete: aparece depois de 3 letras e 300 ms sem digitar. */
    val suggestions: List<PointSuggestion> = emptyList(),
    val isSearching: Boolean = false,
    val activeTypes: Set<CollectionPointType> = MAP_FILTER_TYPES.toSet(),
    val selectedPointId: String? = null,
    /** Ficha completa do ponto tocado (telefone, CNPJ...), buscada só no toque. */
    val selectedDetails: CollectionPointDetails? = null,
    val isLoadingDetails: Boolean = false,
    val detailsError: String? = null,
    val userLocation: UserLocation? = null,
    val cameraRequest: CameraRequest? = null,
    val showBecomeCollectorBanner: Boolean = false,
    /** false para quem já é Coletor: some o botão "Vire Coletor" do mapa. */
    val canBecomeCollector: Boolean = true,
    val isBecomeCollectorDialogOpen: Boolean = false
) {
    /** Pontos que passam pelos filtros de tipo e busca, do mais perto pro mais longe. */
    val visiblePoints: List<CollectionPoint>
        get() {
            val query = searchQuery.trim()
            return points
                .filter { it.type in activeTypes }
                .filter {
                    query.isEmpty() ||
                        it.name.contains(query, ignoreCase = true) ||
                        it.address.contains(query, ignoreCase = true)
                }
                .sortedWith(compareBy(nullsLast()) { it.distanceKm })
        }

    val selectedPoint: CollectionPoint?
        get() = points.firstOrNull { it.id == selectedPointId }

    /** Tem algo para mostrar na ficha: o resumo do pin ou a ficha completa. */
    val isPointSheetOpen: Boolean
        get() = selectedPointId != null && (selectedPoint != null || selectedDetails != null || isLoadingDetails)
}
