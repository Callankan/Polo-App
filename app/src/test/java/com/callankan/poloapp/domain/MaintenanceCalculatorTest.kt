package com.callankan.poloapp.domain

import com.callankan.poloapp.data.db.entity.ComponentEntity
import com.callankan.poloapp.data.db.entity.PartInstallationEntity
import com.callankan.poloapp.data.model.MaintenanceCategory
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class MaintenanceCalculatorTest {
    private val today = LocalDate.of(2026, 9, 28)
    private val oil = ComponentEntity(id = 1, vehicleId = 1, name = "Aceite", category = MaintenanceCategory.OIL_SERVICE, intervalKm = 15_000, intervalMonths = 12)

    private fun install(km: Int, date: LocalDate, id: Long) =
        PartInstallationEntity(id = id, vehicleId = 1, componentId = 1, date = date, odometerKm = km)

    @Test
    fun `counts km since installation and remaining`() {
        val s = MaintenanceCalculator.status(oil, listOf(install(140_000, today.minusMonths(3), 1)), 146_000, today, 30.0, 1000, 30)
        assertEquals(6_000, s.kmSinceInstall)
        assertEquals(9_000, s.kmRemaining)
        assertEquals(DueState.OK, s.state)
        // 9.000 km a 30 km/día = 300 días, pero el límite de 12 meses llega antes
        assertEquals(today.minusMonths(3).plusMonths(12), s.estimatedDueDate)
    }

    @Test
    fun `overdue by time even with few km`() {
        val s = MaintenanceCalculator.status(oil, listOf(install(140_000, today.minusMonths(13), 1)), 142_000, today, null, 1000, 30)
        assertEquals(DueState.OVERDUE, s.state)
    }

    @Test
    fun `soon when inside the lead window`() {
        val s = MaintenanceCalculator.status(oil, listOf(install(140_000, today.minusMonths(2), 1)), 154_500, today, null, 1000, 30)
        assertEquals(DueState.SOON, s.state)
    }

    @Test
    fun `life spans are closed by the next installation`() {
        val spans = MaintenanceCalculator.lifeSpans(
            listOf(install(100_000, LocalDate.of(2024, 1, 1), 1), install(114_000, LocalDate.of(2025, 1, 1), 2)),
        )
        assertEquals(2, spans.size)
        assertEquals(114_000, spans[0].installation.odometerKm)
        assertEquals(true, spans[0].isActive)
        assertEquals(14_000, spans[1].kmUsed(150_000))
    }

    @Test
    fun `never done when there is no installation`() {
        val s = MaintenanceCalculator.status(oil, emptyList(), 150_000, today, null, 1000, 30)
        assertEquals(DueState.NEVER_DONE, s.state)
    }
}
