package com.callankan.poloapp.domain

import com.callankan.poloapp.data.db.entity.DamageEntity
import com.callankan.poloapp.data.model.DamageSeverity
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class HealthLevel(val label: String) {
    EXCELLENT("Excelente"),
    GOOD("Bueno"),
    FAIR("Mejorable"),
    ATTENTION("Necesita atención"),
}

data class HealthIssue(val title: String, val penalty: Int)

data class HealthScore(val score: Int, val level: HealthLevel, val issues: List<HealthIssue>)

/** Puntuación 0-100 del estado general del coche según vencimientos, mantenimiento y daños. */
object HealthScoreCalculator {
    fun compute(
        itv: Deadline,
        insurance: Deadline,
        components: List<ComponentStatus>,
        lastPressureCheck: LocalDate?,
        pressureReminderDays: Int,
        damages: List<DamageEntity>,
        odometerAnomalies: Int,
        today: LocalDate,
    ): HealthScore {
        val issues = mutableListOf<HealthIssue>()

        when (itv.state) {
            DeadlineState.EXPIRED -> issues += HealthIssue("ITV caducada", 30)
            DeadlineState.URGENT -> issues += HealthIssue("ITV a punto de caducar", 8)
            DeadlineState.SOON -> issues += HealthIssue("ITV en menos de un mes", 3)
            DeadlineState.UNKNOWN -> issues += HealthIssue("Fecha de ITV sin registrar", 8)
            DeadlineState.OK -> Unit
        }
        when (insurance.state) {
            DeadlineState.EXPIRED -> issues += HealthIssue("Seguro vencido", 30)
            DeadlineState.URGENT -> issues += HealthIssue("Seguro a punto de vencer", 8)
            DeadlineState.SOON -> issues += HealthIssue("Seguro vence en menos de un mes", 3)
            DeadlineState.UNKNOWN -> issues += HealthIssue("Seguro sin registrar", 8)
            DeadlineState.OK -> Unit
        }

        val overdue = components.count { it.state == DueState.OVERDUE }
        val soon = components.count { it.state == DueState.SOON }
        if (overdue > 0) issues += HealthIssue("$overdue mantenimiento(s) vencido(s)", minOf(40, overdue * 12))
        if (soon > 0) issues += HealthIssue("$soon mantenimiento(s) próximo(s)", minOf(12, soon * 3))

        val pressureAge = lastPressureCheck?.let { ChronoUnit.DAYS.between(it, today) }
        if (pressureAge == null || pressureAge > pressureReminderDays) {
            issues += HealthIssue("Revisar presión de neumáticos", 4)
        }

        val open = damages.filter { !it.repaired }
        if (open.isNotEmpty()) {
            val penalty = open.sumOf {
                when (it.severity) {
                    DamageSeverity.MINOR -> 1
                    DamageSeverity.MODERATE -> 4
                    DamageSeverity.SEVERE -> 10
                }.toInt()
            }
            issues += HealthIssue("${open.size} daño(s) sin reparar", minOf(15, penalty))
        }
        if (odometerAnomalies > 0) issues += HealthIssue("Incoherencias en el kilometraje", 5)

        val score = (100 - issues.sumOf { it.penalty }).coerceIn(0, 100)
        val level = when {
            score >= 85 -> HealthLevel.EXCELLENT
            score >= 70 -> HealthLevel.GOOD
            score >= 50 -> HealthLevel.FAIR
            else -> HealthLevel.ATTENTION
        }
        return HealthScore(score, level, issues.sortedByDescending { it.penalty })
    }
}
