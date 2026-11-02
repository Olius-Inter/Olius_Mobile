package com.olius.app.data.repository

import com.olius.app.domain.entity.BecomeCollectorRequest
import com.olius.app.domain.entity.CitizenHistory
import com.olius.app.domain.entity.CollectionHistoryItem
import com.olius.app.domain.entity.CollectionPoint
import com.olius.app.domain.entity.CollectionPointDetails
import com.olius.app.domain.entity.CollectionPointType
import com.olius.app.domain.entity.MapBounds
import com.olius.app.domain.entity.PointInfo
import com.olius.app.domain.entity.PointSuggestion
import com.olius.app.domain.entity.CollectionStatus
import com.olius.app.domain.entity.CollectorRequest
import com.olius.app.domain.entity.DisposalRecord
import com.olius.app.domain.entity.EstablishmentHistory
import com.olius.app.domain.entity.ImpactStats
import com.olius.app.domain.entity.Tip
import com.olius.app.domain.repository.CollectionPointRepository
import com.olius.app.domain.repository.CollectorRepository
import com.olius.app.domain.repository.HistoryRepository
import com.olius.app.domain.repository.TipsRepository

/*
 * Dados das telas do Figma "Olius - Geral" enquanto o backend principal não
 * existe (ApiConfig.USE_MOCK_MAIN_BACKEND = true). Mesmo formato que a API devolve.
 */

private val FIGMA_STATS = ImpactStats(
    oilCollectedLiters = 50.0,
    waterPreservedLiters = 625_900.0,
    co2AvoidedKg = 7.5,
    biodieselLiters = 50.3
)

class MockHistoryRepository : HistoryRepository {

    override suspend fun getEstablishmentHistory(): Result<EstablishmentHistory> = Result.success(
        EstablishmentHistory(
            monthLabel = "Agosto de 2026",
            stats = FIGMA_STATS,
            items = listOf(
                CollectionHistoryItem("h-1", "23/12/2026", 50.0, CollectionStatus.PENDING),
                CollectionHistoryItem("h-2", "23/9/2026", 52.0, CollectionStatus.PENDING),
                CollectionHistoryItem("h-3", "10/8/2026", 48.0, CollectionStatus.COLLECTED),
                CollectionHistoryItem("h-4", "02/7/2026", 61.0, CollectionStatus.COLLECTED)
            )
        )
    )

    override suspend fun getCitizenHistory(): Result<CitizenHistory> = Result.success(
        CitizenHistory(
            monthLabel = "Agosto de 2026",
            stats = FIGMA_STATS,
            records = List(3) { index ->
                DisposalRecord(
                    id = "d-$index",
                    liters = 64.0,
                    dateLabel = "04 de Agosto de 2026",
                    waterPreservedLiters = 550_000.0,
                    co2AvoidedKg = 30.0,
                    biodieselKg = 50.0
                )
            }
        )
    )
}

/**
 * Pontos de exemplo em São Paulo, com coordenadas reais. Segue as mesmas
 * regras que o backend deve ter: pins por bounding box, autocomplete sem
 * acento (prefixo primeiro) e ficha completa só por id.
 */
class MockCollectionPointRepository : CollectionPointRepository {

    override suspend fun getNearbyPoints(): Result<List<CollectionPoint>> = Result.success(SAMPLE_POINTS)

    override suspend fun getPointsInArea(bounds: MapBounds): Result<List<CollectionPoint>> =
        Result.success(SAMPLE_POINTS.filter { bounds.contains(it.latitude, it.longitude) })

    override suspend fun searchPoints(query: String): Result<List<PointSuggestion>> {
        val q = normalize(query)
        if (q.length < MIN_SEARCH_LENGTH) return Result.success(emptyList())
        val matches = SAMPLE_POINTS
            .filter { normalize(it.name).contains(q) || normalize(it.address).contains(q) }
            .sortedByDescending { normalize(it.name).startsWith(q) }
            .take(MAX_SUGGESTIONS)
            .map { PointSuggestion(it.id, it.name, it.type, it.address, it.latitude, it.longitude) }
        return Result.success(matches)
    }

    override suspend fun getPointDetails(id: String): Result<CollectionPointDetails> {
        val point = SAMPLE_POINTS.firstOrNull { it.id == id }
            ?: return Result.failure(NoSuchElementException("Ponto $id não encontrado"))
        val extra = SAMPLE_DETAILS[id]
        return Result.success(
            CollectionPointDetails(
                id = point.id,
                name = point.name,
                type = point.type,
                address = point.address,
                latitude = point.latitude,
                longitude = point.longitude,
                openingHours = point.openingHours,
                isOpenNow = point.isOpenNow,
                acceptedMaterials = point.acceptedMaterials,
                phone = extra?.phone,
                whatsapp = extra?.whatsapp,
                cnpj = extra?.cnpj,
                legalName = extra?.legalName,
                description = extra?.description,
                extraInfo = extra?.extraInfo.orEmpty()
            )
        )
    }

    override suspend fun becomeCollector(request: BecomeCollectorRequest): Result<Unit> = Result.success(Unit)

    companion object {
        const val MIN_SEARCH_LENGTH = 3
        private const val MAX_SUGGESTIONS = 8

        /** Minúsculas e sem acento: "São" e "sao" casam (o backend usa unaccent + pg_trgm). */
        fun normalize(text: String): String =
            java.text.Normalizer.normalize(text.trim().lowercase(), java.text.Normalizer.Form.NFD)
                .replace(Regex("\\p{M}+"), "")

        val SAMPLE_POINTS: List<CollectionPoint> = listOf(
            CollectionPoint(
                id = "1", name = "SuperMercado Esperança - Vila Sônia", type = CollectionPointType.PEV,
                address = "R. Harmonia, Osasco", openingHours = "Seg-Sex 10h - 16h", isOpenNow = true, distanceKm = null,
                acceptedMaterials = listOf("Óleo de cozinha usado", "Garrafas PET"),
                latitude = -23.5329, longitude = -46.7916
            ),
            CollectionPoint(
                id = "2", name = "SuperMercado Extra - Vila Madalena", type = CollectionPointType.PEV,
                address = "R. Harmonia, Vila Madalena", openingHours = "Seg-Sex 5h - 9h", isOpenNow = false, distanceKm = null,
                acceptedMaterials = listOf("Óleo de cozinha usado"),
                latitude = -23.5558, longitude = -46.6912
            ),
            CollectionPoint(
                id = "3", name = "Ecoponto Sé", type = CollectionPointType.PEV,
                address = "Praça da Sé, Centro", openingHours = "Todos os dias 6h - 22h", isOpenNow = true, distanceKm = null,
                acceptedMaterials = listOf("Óleo de cozinha usado", "Recicláveis", "Eletrônicos"),
                latitude = -23.5503, longitude = -46.6339
            ),
            CollectionPoint(
                id = "4", name = "Ecoponto Liberdade", type = CollectionPointType.PEV,
                address = "R. Galvão Bueno, Liberdade", openingHours = "Seg-Sáb 7h - 19h", isOpenNow = false, distanceKm = null,
                acceptedMaterials = listOf("Óleo de cozinha usado", "Recicláveis"),
                latitude = -23.5587, longitude = -46.6358
            ),
            CollectionPoint(
                // PEV de cidadão: aparece como PEV, com o bairro no lugar do nome da pessoa.
                id = "5", name = "PEV Bom Retiro", type = CollectionPointType.PEV,
                address = "Região do Bom Retiro", openingHours = "Sáb 9h - 12h", isOpenNow = false, distanceKm = null,
                acceptedMaterials = listOf("Óleo de cozinha usado (em garrafa PET)"),
                latitude = -23.5268, longitude = -46.6405
            ),
            CollectionPoint(
                id = "6", name = "PEV Barra Funda", type = CollectionPointType.PEV,
                address = "Região da Barra Funda", openingHours = "Seg-Sex 18h - 21h", isOpenNow = true, distanceKm = null,
                acceptedMaterials = listOf("Óleo de cozinha usado (em garrafa PET)"),
                latitude = -23.5256, longitude = -46.6664
            ),
            CollectionPoint(
                id = "7", name = "Loja do Seu Zé", type = CollectionPointType.PEV,
                address = "R. Augusta, 1200 - Consolação", openingHours = "Seg-Sáb 8h - 20h", isOpenNow = true, distanceKm = null,
                acceptedMaterials = listOf("Óleo de cozinha usado"),
                latitude = -23.5546, longitude = -46.6577
            ),
            CollectionPoint(
                // Estabelecimento que não é PEV: a Core não devolve; está aqui para provar o filtro.
                id = "8", name = "Padaria Pão Quente", type = CollectionPointType.ESTABLISHMENT,
                address = "R. Cardeal Arcoverde, 500 - Pinheiros", openingHours = "Todos os dias 6h - 20h", isOpenNow = true, distanceKm = null,
                acceptedMaterials = emptyList(),
                latitude = -23.5610, longitude = -46.6820
            )
        )

        private val SAMPLE_DETAILS: Map<String, CollectionPointDetails> = mapOf(
            "1" to details(
                phone = "1136851200", whatsapp = "11987651200", cnpj = "12345678000190",
                legalName = "Esperança Comércio de Alimentos LTDA",
                description = "Ponto de coleta na entrada do mercado, ao lado dos caixas.",
                extraInfo = listOf(PointInfo("Estacionamento", "Gratuito para clientes"))
            ),
            "2" to details(
                phone = "1130309090", cnpj = "98765432000110", legalName = "Extra Supermercados S.A."
            ),
            "3" to details(
                phone = "156",
                description = "Ecoponto da Prefeitura. Leve o óleo em garrafa PET bem fechada.",
                extraInfo = listOf(PointInfo("Responsável", "Prefeitura de São Paulo"))
            ),
            "4" to details(phone = "156"),
            "7" to details(
                phone = "1132551234", whatsapp = "11999551234", cnpj = "11222333000144",
                legalName = "José da Silva Mercearia ME",
                description = "Mercearia de bairro. Troca 2 L de óleo usado por 1 sabão ecológico.",
                extraInfo = listOf(PointInfo("Ponto de referência", "Em frente à padaria"))
            )
        )

        private fun details(
            phone: String? = null,
            whatsapp: String? = null,
            cnpj: String? = null,
            legalName: String? = null,
            description: String? = null,
            extraInfo: List<PointInfo> = emptyList()
        ) = CollectionPointDetails(
            id = "", name = "", type = CollectionPointType.PEV, address = "", latitude = 0.0, longitude = 0.0,
            phone = phone, whatsapp = whatsapp, cnpj = cnpj, legalName = legalName,
            description = description, extraInfo = extraInfo
        )
    }
}

class MockCollectorRepository : CollectorRepository {

    private val requests = MutableList(5) { index -> CollectorRequest("r-$index", "KFC - Frangos", "04 de Agosto de 2026", 50.0) }

    override suspend fun getRequests(): Result<List<CollectorRequest>> = Result.success(requests.toList())

    override suspend fun collect(requestId: String): Result<Unit> {
        requests.removeAll { it.id == requestId }
        return Result.success(Unit)
    }

    override suspend fun discard(requestId: String): Result<Unit> = collect(requestId)
}

/** Textos do Figma ("Card- Dicas - B2C"). Podem continuar locais se não mudarem com frequência. */
class MockTipsRepository : TipsRepository {

    override suspend fun getTips(): Result<List<Tip>> = Result.success(
        listOf(
            Tip("t-1", "Armazenamento correto do Óleo", "Guarde em garrafas PET bem fechadas. Nunca misture com água."),
            Tip("t-2", "Volume mínimo", "Guarde em garrafas PET bem fechadas. Nunca misture com água."),
            Tip("t-3", "Tipos aceitos", "Guarde em garrafas PET bem fechadas. Nunca misture com água."),
            Tip("t-4", "Não descarte na pia!", "Guarde em garrafas PET bem fechadas. Nunca misture com água.")
        )
    )
}
