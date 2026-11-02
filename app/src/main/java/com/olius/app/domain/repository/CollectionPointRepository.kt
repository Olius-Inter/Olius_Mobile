package com.olius.app.domain.repository

import com.olius.app.domain.entity.BecomeCollectorRequest
import com.olius.app.domain.entity.CollectionPoint
import com.olius.app.domain.entity.CollectionPointDetails
import com.olius.app.domain.entity.CollectorRequest
import com.olius.app.domain.entity.GeoPoint
import com.olius.app.domain.entity.MapBounds
import com.olius.app.domain.entity.PointSuggestion
import com.olius.app.domain.entity.Tip

/** Pontos de coleta (mapa e Home B2C) e o fluxo "Vire Coletor". */
interface CollectionPointRepository {
    /** Aba "Pontos PEV" da Home B2C. */
    suspend fun getNearbyPoints(): Result<List<CollectionPoint>>

    /** Pins da área visível do mapa (carrega por bounding box, não tudo de uma vez). */
    suspend fun getPointsInArea(bounds: MapBounds): Result<List<CollectionPoint>>

    /** Autocomplete da busca: pontos cujo nome/endereço combina com [query] (mín. 3 letras). */
    suspend fun searchPoints(query: String): Result<List<PointSuggestion>>

    /** Ficha completa, buscada só quando a pessoa toca no pin. */
    suspend fun getPointDetails(id: String): Result<CollectionPointDetails>

    /** Cria o `pev` com status PENDING; aparece no mapa depois da aprovação. */
    suspend fun becomeCollector(request: BecomeCollectorRequest): Result<Unit>
}

/** Endereço → coordenadas, para o PEV ter pin no mapa (a Core aceita o PEV sem elas). */
fun interface AddressGeocoder {
    /** null = não achou ou sem serviço; o PEV é enviado mesmo assim. */
    suspend fun locate(query: String): GeoPoint?

    companion object {
        val NONE = AddressGeocoder { null }
    }
}

/** Aba "Solicitações" de quem já é Coletor (estabelecimento com `is_pev`). */
interface CollectorRepository {
    suspend fun getRequests(): Result<List<CollectorRequest>>

    suspend fun collect(requestId: String): Result<Unit>

    suspend fun discard(requestId: String): Result<Unit>
}

/** Aba "Dicas" da Home B2C. */
interface TipsRepository {
    suspend fun getTips(): Result<List<Tip>>
}
