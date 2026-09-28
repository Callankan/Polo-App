package com.callankan.poloapp.domain

import com.callankan.poloapp.data.db.entity.RefuelEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class FuelCalculatorTest {
    private var nextId = 1L
    private fun refuel(km: Int, liters: Double, total: Double, full: Boolean = true, missed: Boolean = false, day: Int = 1) =
        RefuelEntity(
            id = nextId++, vehicleId = 1, date = LocalDate.of(2026, 1, 1).plusDays(day.toLong()),
            odometerKm = km, liters = liters, totalCost = total, pricePerLiter = total / liters,
            fullTank = full, missedPrevious = missed,
        )

    @Test
    fun `full to full consumption`() {
        val stats = FuelCalculator.compute(
            listOf(refuel(100_000, 40.0, 60.0, day = 1), refuel(100_600, 36.0, 54.0, day = 10)),
        )
        assertEquals(1, stats.segments.size)
        assertEquals(6.0, stats.averageLitersPer100!!, 0.001)
        assertEquals(1.5, stats.averagePricePerLiter!!, 0.001)
        assertEquals(9.0, stats.costPer100Km!!, 0.001)
    }

    @Test
    fun `partial refuels are accumulated into the next full tank`() {
        val stats = FuelCalculator.compute(
            listOf(
                refuel(10_000, 40.0, 60.0, day = 1),
                refuel(10_300, 10.0, 15.0, full = false, day = 5),
                refuel(10_800, 38.0, 57.0, day = 12),
            ),
        )
        assertEquals(1, stats.segments.size)
        assertEquals(48.0 / 800 * 100, stats.averageLitersPer100!!, 0.001)
    }

    @Test
    fun `missed previous refuel invalidates the segment`() {
        val stats = FuelCalculator.compute(
            listOf(
                refuel(10_000, 40.0, 60.0, day = 1),
                refuel(10_900, 20.0, 30.0, missed = true, day = 10),
                refuel(11_500, 36.0, 54.0, day = 20),
            ),
        )
        assertEquals(1, stats.segments.size)
        assertEquals(6.0, stats.segments.single().litersPer100, 0.001)
    }

    @Test
    fun `no consumption with a single refuel`() {
        val stats = FuelCalculator.compute(listOf(refuel(1_000, 30.0, 45.0)))
        assertNull(stats.averageLitersPer100)
        assertEquals(1, stats.refuelCount)
    }

    @Test
    fun `completes the missing value of the triple`() {
        val (l, p, t) = FuelCalculator.completeTriple(40.0, null, 62.0)
        assertEquals(40.0, l!!, 0.0)
        assertEquals(1.55, p!!, 0.0001)
        assertEquals(62.0, t!!, 0.0)
    }
}
