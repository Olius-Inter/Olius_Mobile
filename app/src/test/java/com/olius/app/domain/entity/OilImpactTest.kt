package com.olius.app.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Test

class OilImpactTest {
    @Test
    fun `agua CO2 e biodiesel saem dos litros de oleo`() {
        val stats = ImpactStats.fromOilLiters(2.0)

        assertEquals(2.0, stats.oilCollectedLiters, 0.0)
        assertEquals(50_000.0, stats.waterPreservedLiters, 0.0)
        assertEquals(1.8, stats.biodieselLiters, 1e-9)
        assertEquals(4.05, stats.co2AvoidedKg, 1e-9)
    }

    @Test
    fun `sem oleo tudo fica zerado`() {
        assertEquals(ImpactStats(0.0, 0.0, 0.0, 0.0), ImpactStats.fromOilLiters(0.0))
    }
}
