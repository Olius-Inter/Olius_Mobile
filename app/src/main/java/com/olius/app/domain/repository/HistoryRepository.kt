package com.olius.app.domain.repository

import com.olius.app.domain.entity.CitizenHistory
import com.olius.app.domain.entity.EstablishmentHistory

/** Aba Histórico. Uma chamada por tela: indicadores + lista juntos. */
interface HistoryRepository {

    /** B2B: "Histórico de Coletas" (collection_request / collection). */
    suspend fun getEstablishmentHistory(): Result<EstablishmentHistory>

    /** B2C: "Histórico de Registro" (delivery_pev). */
    suspend fun getCitizenHistory(): Result<CitizenHistory>
}
