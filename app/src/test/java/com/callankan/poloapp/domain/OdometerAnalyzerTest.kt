package com.callankan.poloapp.domain

import com.callankan.poloapp.data.db.entity.OdometerEvent
import com.callankan.poloapp.data.model.OdometerSource
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class OdometerAnalyzerTest {
    private fun e(date: LocalDate, km: Int) = OdometerEvent(1, date, km, OdometerSource.REFUEL, km.toLong())

    @Test
    fun `current km and daily average`() {
        val start = LocalDate.of(2026, 1, 1)
        val a = OdometerAnalyzer.analyze(listOf(e(start, 140_000), e(start.plusDays(100), 143_000)))
        assertEquals(143_000, a.currentKm)
        assertEquals(30.0, a.kmPerDay!!, 0.001)
        assertEquals(0, a.anomalies.size)
    }

    @Test
    fun `detects readings that go backwards in time`() {
        val start = LocalDate.of(2026, 1, 1)
        val a = OdometerAnalyzer.analyze(listOf(e(start, 140_000), e(start.plusDays(10), 141_000), e(start.plusDays(20), 139_000)))
        assertEquals(1, a.anomalies.size)
        assertEquals(139_000, a.anomalies.single().km)
    }
}
