package com.olius.app.domain.entity

/** Os 3 tipos de ponto de coleta. */
enum class CollectionPointType(val label: String, val pluralLabel: String) {
    /** Ponto de Entrega Voluntária fixo (ecoponto). */
    PEV("PEV", "PEVs"),

    /** Estabelecimento que virou ponto de coleta. */
    ESTABLISHMENT("Estabelecimento", "Estabelecimentos"),

    /** Pessoa (cidadão) que virou ponto de coleta. */
    COLLECTOR("Coletor", "Coletores")
}

/** Tipos que aparecem para o usuário: mapa, busca e aba "Pontos PEV" da Home B2C. */
val VISIBLE_POINT_TYPES: List<CollectionPointType> = listOf(CollectionPointType.PEV)

/** Um ponto de coleta (pin do mapa e aba "Pontos PEV" da Home B2C). */
data class CollectionPoint(
    val id: String,
    val name: String,
    val type: CollectionPointType,
    val address: String,
    val openingHours: String,
    val isOpenNow: Boolean,
    val distanceKm: Double?,
    val acceptedMaterials: List<String>,
    val latitude: Double,
    val longitude: Double
)

/** Coordenadas de um endereço (geocodificação do "Vire Coletor"). */
data class GeoPoint(val latitude: Double, val longitude: Double)

/** "Vire Coletor" de quem já tem PEV aprovado ou inativo ([status] da Core). */
class PevAlreadyExistsException(val status: String) : Exception("PEV já existe: $status")

/** Retângulo visível do mapa (bounding box): os pins são buscados só dessa área. */
data class MapBounds(
    val south: Double,
    val west: Double,
    val north: Double,
    val east: Double
) {
    fun contains(latitude: Double, longitude: Double): Boolean =
        latitude in south..north && longitude in west..east
}

/** Item da lista do autocomplete ("loja do seu zé"): só o necessário para mover o mapa. */
data class PointSuggestion(
    val id: String,
    val name: String,
    val type: CollectionPointType,
    val address: String,
    val latitude: Double,
    val longitude: Double
)

/** Campo livre que a empresa escolheu publicar (tabela `ponto_info` proposta, só `publico = true`). */
data class PointInfo(val label: String, val value: String)

/** Ficha completa do ponto, buscada no toque (GET collection-points/{id}). */
data class CollectionPointDetails(
    val id: String,
    val name: String,
    val type: CollectionPointType,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val phone: String? = null,
    val whatsapp: String? = null,
    val cnpj: String? = null,
    val legalName: String? = null,
    val description: String? = null,
    val openingHours: String? = null,
    val isOpenNow: Boolean? = null,
    val acceptedMaterials: List<String> = emptyList(),
    val extraInfo: List<PointInfo> = emptyList()
)

private const val EARTH_RADIUS_KM = 6371.0

/** Distância em linha reta (fórmula de Haversine), sem depender do Android. */
fun distanceKmBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2).let { it * it } +
        Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2).let { it * it }
    return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a))
}

/** Formulário "Virando um Coletor". */
data class BecomeCollectorRequest(
    val cep: String,
    val street: String,
    val number: String,
    val neighborhood: String,
    val city: String,
    val state: String,
    val complement: String?,
    val openingHours: String
)

/** Solicitação que chega para o Coletor (aba "Solicitações" da Home B2B-Coletor). */
data class CollectorRequest(
    val id: String,
    val establishmentName: String,
    val dateLabel: String,
    val liters: Double
)

/** Card da aba "Dicas" da Home B2C. */
data class Tip(
    val id: String,
    val title: String,
    val description: String
)
