package com.callankan.poloapp.domain

import com.callankan.poloapp.data.db.entity.ItvInspectionEntity
import com.callankan.poloapp.data.model.ItvResult
import java.time.LocalDate
import java.time.Period

/**
 * Periodicidad de la ITV para turismos de uso particular en España (RD 920/2017):
 * - Hasta 4 años: exento (primera inspección a los 4 años).
 * - De 4 a 10 años: cada 2 años.
 * - Más de 10 años: anual.
 * Si se pasa en los 30 días previos al vencimiento, la siguiente fecha cuenta desde el vencimiento.
 * Con resultado desfavorable o negativo hay 2 meses para volver.
 */
object ItvRules {
    fun intervalYears(registration: LocalDate?, at: LocalDate): Int {
        if (registration == null) return 1
        val age = Period.between(registration, at).years
        return if (age < 10) 2 else 1
    }

    fun nextDueDate(
        registration: LocalDate?,
        inspectionDate: LocalDate,
        result: ItvResult,
        previousDue: LocalDate?,
    ): LocalDate {
        if (!result.passed) return inspectionDate.plusMonths(2)
        val base = if (
            previousDue != null &&
            !inspectionDate.isBefore(previousDue.minusDays(30)) &&
            !inspectionDate.isAfter(previousDue)
        ) previousDue else inspectionDate
        return base.plusYears(intervalYears(registration, base).toLong())
    }

    fun firstInspection(registration: LocalDate): LocalDate = registration.plusYears(4)

    /** Fecha de la próxima ITV según lo registrado (última inspección > fecha manual > primera ITV). */
    fun currentDueDate(
        inspections: List<ItvInspectionEntity>,
        manualDueDate: LocalDate?,
        registration: LocalDate?,
        today: LocalDate,
    ): LocalDate? {
        val latest = inspections.maxWithOrNull(compareBy({ it.date }, { it.id }))
        if (latest != null) return latest.nextDueDate
        if (manualDueDate != null) return manualDueDate
        if (registration != null && firstInspection(registration).isAfter(today)) return firstInspection(registration)
        return null
    }

    fun describeFrequency(registration: LocalDate?, today: LocalDate): String {
        if (registration == null) return "Indica la fecha de matriculación para calcular la periodicidad."
        val age = Period.between(registration, today).years
        return when {
            age < 4 -> "Tu coche tiene $age años: la primera ITV toca a los 4 años."
            age < 10 -> "Tu coche tiene $age años: ITV cada 2 años hasta cumplir 10."
            else -> "Tu coche tiene $age años: la ITV es anual."
        }
    }
}
