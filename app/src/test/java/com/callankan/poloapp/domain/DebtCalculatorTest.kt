package com.callankan.poloapp.domain

import com.callankan.poloapp.data.db.entity.LoanEntity
import com.callankan.poloapp.data.db.entity.LoanPaymentEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class DebtCalculatorTest {
    @Test
    fun `simulation with fixed monthly payment`() {
        val sim = DebtCalculator.simulate(1000.0, 300.0, YearMonth.of(2026, 10))!!
        assertEquals(4, sim.months)
        assertEquals(YearMonth.of(2027, 1), sim.payoffMonth)
        assertEquals(100.0, sim.lastPayment, 0.001)
        assertEquals(1000.0, sim.totalPaid, 0.001)
    }

    @Test
    fun `extra payments shorten the plan`() {
        val sim = DebtCalculator.simulate(1000.0, 300.0, YearMonth.of(2026, 10), mapOf(YearMonth.of(2026, 10) to 400.0))!!
        assertEquals(2, sim.months)
        assertEquals(0.0, sim.rows.last().remainingAfter, 0.001)
    }

    @Test
    fun `zero monthly payment cannot be simulated`() {
        assertNull(DebtCalculator.simulate(1000.0, 0.0, YearMonth.of(2026, 10)))
    }

    @Test
    fun `monthly amount needed for a target date`() {
        val monthly = DebtCalculator.monthlyForTarget(1200.0, YearMonth.of(2026, 1), YearMonth.of(2026, 12))
        assertEquals(100.0, monthly!!, 0.001)
    }

    @Test
    fun `summary projects payoff from the real pace`() {
        val loan = LoanEntity(id = 1, vehicleId = 1, lender = "Mamá", initialAmount = 3000.0, startDate = LocalDate.of(2026, 1, 1))
        val payments = listOf(
            LoanPaymentEntity(1, 1, LocalDate.of(2026, 2, 1), 300.0),
            LoanPaymentEntity(2, 1, LocalDate.of(2026, 5, 1), 600.0),
        )
        val summary = DebtCalculator.summarize(loan, payments, LocalDate.of(2026, 6, 15))
        assertEquals(900.0, summary.paid, 0.001)
        assertEquals(2100.0, summary.remaining, 0.001)
        assertEquals(150.0, summary.averageMonthly!!, 0.001)
        assertEquals(YearMonth.of(2027, 8), summary.projectedPayoff)
        assertEquals(0.3f, summary.progress, 0.001f)
    }
}
