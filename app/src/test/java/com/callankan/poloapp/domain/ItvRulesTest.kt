package com.callankan.poloapp.domain

import com.callankan.poloapp.data.model.ItvResult
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ItvRulesTest {
    private val polo = LocalDate.of(2013, 6, 15)

    @Test
    fun `car older than ten years has annual inspection`() {
        val next = ItvRules.nextDueDate(polo, LocalDate.of(2026, 3, 10), ItvResult.FAVORABLE, previousDue = null)
        assertEquals(LocalDate.of(2027, 3, 10), next)
    }

    @Test
    fun `passing within 30 days before expiry keeps the original date`() {
        val next = ItvRules.nextDueDate(polo, LocalDate.of(2026, 6, 1), ItvResult.FAVORABLE, LocalDate.of(2026, 6, 20))
        assertEquals(LocalDate.of(2027, 6, 20), next)
    }

    @Test
    fun `car between four and ten years goes every two years`() {
        val next = ItvRules.nextDueDate(LocalDate.of(2020, 1, 1), LocalDate.of(2026, 1, 10), ItvResult.FAVORABLE, null)
        assertEquals(LocalDate.of(2028, 1, 10), next)
    }

    @Test
    fun `unfavourable result gives two months`() {
        val next = ItvRules.nextDueDate(polo, LocalDate.of(2026, 3, 10), ItvResult.UNFAVORABLE, null)
        assertEquals(LocalDate.of(2026, 5, 10), next)
    }
}
