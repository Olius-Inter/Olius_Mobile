package com.olius.app.domain.entity

/** Indicadores do topo (Figma "Card/dash coletas") — iguais na Home e no Histórico. */
data class ImpactStats(
    val oilCollectedLiters: Double,
    val waterPreservedLiters: Double,
    val co2AvoidedKg: Double,
    val biodieselLiters: Double
)

/**
 * Status de uma coleta do estabelecimento (filtros do Histórico B2B).
 * Banco: PENDING = `collection_request` PENDING/APPROVED sem `collection`;
 * COLLECTED = `collection` com `record_status = 'RECORDED'`.
 */
enum class CollectionStatus(val label: String, val apiValue: String) {
    COLLECTED("Coletado", "COLLECTED"),
    PENDING("Pendente", "PENDING");

    companion object {
        /** "REGISTERED"/"PROCESSED" são do contrato antigo; desconhecido vira PENDING. */
        fun fromApi(value: String): CollectionStatus = when (value) {
            "COLLECTED", "PROCESSED" -> COLLECTED
            else -> PENDING
        }
    }
}

/** Uma linha da tabela "Data · Volume · Status" (Histórico de Coletas B2B). */
data class CollectionHistoryItem(
    val id: String,
    val dateLabel: String,
    val liters: Double,
    val status: CollectionStatus
)

data class EstablishmentHistory(
    val monthLabel: String,
    val stats: ImpactStats,
    val items: List<CollectionHistoryItem>
)

/** Card "64L descartados" do Histórico de Registro B2C (uma `delivery_pev`). */
data class DisposalRecord(
    val id: String,
    val liters: Double,
    val dateLabel: String,
    val waterPreservedLiters: Double,
    val co2AvoidedKg: Double,
    val biodieselKg: Double
)

data class CitizenHistory(
    val monthLabel: String,
    val stats: ImpactStats,
    val records: List<DisposalRecord>
)

/** Card "Solicitações de Coletas" da Home B2B (pedidos ainda em andamento). */
data class PickupRequestSummary(
    val id: String,
    val liters: Double,
    val dateLabel: String,
    val status: CollectionStatus
)
