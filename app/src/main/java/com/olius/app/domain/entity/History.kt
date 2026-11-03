package com.olius.app.domain.entity

/** Indicadores do topo — iguais na Home e no Histórico. */
data class ImpactStats(
    val oilCollectedLiters: Double,
    val waterPreservedLiters: Double,
    val co2AvoidedKg: Double,
    val biodieselLiters: Double
) {
    companion object {
        /** Água, CO2 e biodiesel calculados no app a partir dos litros de óleo ([OilImpact]). */
        fun fromOilLiters(oilLiters: Double) = ImpactStats(
            oilCollectedLiters = oilLiters,
            waterPreservedLiters = OilImpact.waterPreservedLiters(oilLiters),
            co2AvoidedKg = OilImpact.co2AvoidedKg(oilLiters),
            biodieselLiters = OilImpact.biodieselLiters(oilLiters)
        )
    }
}

/** Impacto ambiental de reciclar o óleo, calculado no app. */
object OilImpact {
    /** 1 L de óleo contamina até 25 mil litros de água (Sabesp). */
    const val WATER_LITERS_PER_OIL_LITER = 25_000.0

    /** Rendimento conservador (93–95% em laboratório). */
    const val BIODIESEL_LITERS_PER_OIL_LITER = 0.9

    /** 2,25 kg de CO2 por litro de biodiesel (BioPower) × 0,9 = 2,025 por litro de óleo. */
    const val CO2_KG_PER_OIL_LITER = 2.025

    fun waterPreservedLiters(oilLiters: Double) = oilLiters * WATER_LITERS_PER_OIL_LITER
    fun biodieselLiters(oilLiters: Double) = oilLiters * BIODIESEL_LITERS_PER_OIL_LITER
    fun co2AvoidedKg(oilLiters: Double) = oilLiters * CO2_KG_PER_OIL_LITER
}

/** Status de uma coleta do estabelecimento (filtros do Histórico B2B). */
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
    val biodieselLiters: Double
)

data class CitizenHistory(
    val monthLabel: String,
    val stats: ImpactStats,
    val records: List<DisposalRecord>
)

data class PickupRequestSummary(
    val id: String,
    val liters: Double,
    val dateLabel: String,
    val status: CollectionStatus
)
