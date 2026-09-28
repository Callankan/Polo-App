package com.callankan.poloapp.domain

import com.callankan.poloapp.data.db.entity.LoanEntity
import com.callankan.poloapp.data.db.entity.LoanPaymentEntity
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

data class DebtSummary(
    val loan: LoanEntity,
    val paid: Double,
    val remaining: Double,
    val paymentsCount: Int,
    val lastPayment: LoanPaymentEntity?,
    /** Media mensual real desde el inicio del préstamo. */
    val averageMonthly: Double?,
    /** Mes estimado de liquidación si sigues al ritmo actual. */
    val projectedPayoff: YearMonth?,
) {
    val initial: Double get() = loan.initialAmount
    val progress: Float get() = if (initial <= 0) 1f else (paid / initial).toFloat().coerceIn(0f, 1f)
    val isPaidOff: Boolean get() = remaining <= 0.005
}

data class DebtScheduleRow(
    val index: Int,
    val month: YearMonth,
    val payment: Double,
    val extra: Double,
    val remainingAfter: Double,
)

data class DebtSimulation(
    val rows: List<DebtScheduleRow>,
    val payoffMonth: YearMonth,
    val months: Int,
    val lastPayment: Double,
    val totalPaid: Double,
)

/** Préstamo sin intereses: cálculo de saldo pendiente, ritmo real y simulaciones de pago. */
object DebtCalculator {
    private const val MAX_MONTHS = 600

    fun summarize(loan: LoanEntity, payments: List<LoanPaymentEntity>, today: LocalDate): DebtSummary {
        val paid = payments.sumOf { it.amount }
        val remaining = max(0.0, loan.initialAmount - paid)
        val last = payments.maxWithOrNull(compareBy({ it.date }, { it.id }))
        val months = monthsBetween(YearMonth.from(loan.startDate), YearMonth.from(today)) + 1
        val average = if (payments.isNotEmpty()) paid / max(1, months) else null
        val projected = if (average != null && average > 0 && remaining > 0) {
            YearMonth.from(today).plusMonths(ceil(remaining / average).toLong())
        } else null
        return DebtSummary(loan, paid, remaining, payments.size, last, average, projected)
    }

    /**
     * Simula pagos mensuales fijos (más pagos extra puntuales) empezando en [start].
     * Devuelve null si la cuota no es positiva o se superan 50 años.
     */
    fun simulate(
        remaining: Double,
        monthly: Double,
        start: YearMonth,
        extras: Map<YearMonth, Double> = emptyMap(),
    ): DebtSimulation? {
        if (remaining <= 0) return DebtSimulation(emptyList(), start, 0, 0.0, 0.0)
        if (monthly <= 0 && extras.values.sum() < remaining) return null
        val rows = mutableListOf<DebtScheduleRow>()
        var balance = remaining
        var month = start
        var index = 0
        while (balance > 0.005 && index < MAX_MONTHS) {
            val extra = min(extras[month] ?: 0.0, balance)
            balance -= extra
            val payment = min(monthly, balance)
            balance -= payment
            index++
            rows += DebtScheduleRow(index, month, payment, extra, max(0.0, balance))
            month = month.plusMonths(1)
        }
        if (balance > 0.005) return null
        val last = rows.last()
        return DebtSimulation(
            rows = rows,
            payoffMonth = last.month,
            months = rows.size,
            lastPayment = last.payment + last.extra,
            totalPaid = rows.sumOf { it.payment + it.extra },
        )
    }

    /** Cuota mensual necesaria para terminar en [target] (incluido) empezando en [start]. */
    fun monthlyForTarget(remaining: Double, start: YearMonth, target: YearMonth): Double? {
        val months = monthsBetween(start, target) + 1
        if (months <= 0 || remaining <= 0) return null
        return ceil(remaining / months * 100) / 100
    }

    fun monthsBetween(from: YearMonth, to: YearMonth): Int = ChronoUnit.MONTHS.between(from, to).toInt()
}
