package com.callankan.poloapp.domain

import com.callankan.poloapp.data.db.entity.ComponentEntity
import com.callankan.poloapp.data.db.entity.PartInstallationEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.max

enum class DueState { OVERDUE, SOON, OK, NEVER_DONE, NO_INTERVAL }

/** Vida útil de una instalación concreta: de su montaje al siguiente cambio (o hasta hoy). */
data class LifeSpan(
    val installation: PartInstallationEntity,
    val removedKm: Int?,
    val removedDate: LocalDate?,
) {
    fun kmUsed(currentKm: Int): Int = max(0, (removedKm ?: currentKm) - installation.odometerKm)
    val isActive: Boolean get() = removedKm == null
}

data class ComponentStatus(
    val component: ComponentEntity,
    val current: PartInstallationEntity?,
    val lifeSpans: List<LifeSpan>,
    val kmSinceInstall: Int?,
    val daysSinceInstall: Long?,
    val dueKm: Int?,
    val dueDate: LocalDate?,
    val kmRemaining: Int?,
    val daysRemaining: Long?,
    /** Fracción de vida consumida (puede superar 1 si está vencida). */
    val progress: Float,
    val state: DueState,
    /** Fecha estimada del próximo cambio combinando km/día medios y el intervalo de tiempo. */
    val estimatedDueDate: LocalDate?,
) {
    val hasInterval: Boolean get() = component.intervalKm != null || component.intervalMonths != null
}

object MaintenanceCalculator {

    fun statuses(
        components: List<ComponentEntity>,
        installations: List<PartInstallationEntity>,
        currentKm: Int,
        today: LocalDate,
        kmPerDay: Double?,
        leadKm: Int,
        leadDays: Int,
    ): List<ComponentStatus> {
        val byComponent = installations.groupBy { it.componentId }
        return components.filter { !it.archived }
            .map { status(it, byComponent[it.id].orEmpty(), currentKm, today, kmPerDay, leadKm, leadDays) }
            .sortedWith(urgencyComparator)
    }

    val urgencyComparator: Comparator<ComponentStatus> = compareBy<ComponentStatus> { it.state.ordinal }
        .thenByDescending { it.progress }
        .thenBy { it.component.sortOrder }

    fun lifeSpans(installations: List<PartInstallationEntity>): List<LifeSpan> {
        val sorted = installations.sortedWith(compareBy({ it.odometerKm }, { it.date }, { it.id }))
        return sorted.mapIndexed { i, inst ->
            val next = sorted.getOrNull(i + 1)
            LifeSpan(inst, next?.odometerKm, next?.date)
        }.reversed()
    }

    fun status(
        component: ComponentEntity,
        installations: List<PartInstallationEntity>,
        currentKm: Int,
        today: LocalDate,
        kmPerDay: Double?,
        leadKm: Int,
        leadDays: Int,
    ): ComponentStatus {
        val spans = lifeSpans(installations)
        val current = spans.firstOrNull()?.installation
        val hasInterval = component.intervalKm != null || component.intervalMonths != null
        if (current == null) {
            return ComponentStatus(
                component, null, spans, null, null, null, null, null, null, 0f,
                if (hasInterval) DueState.NEVER_DONE else DueState.NO_INTERVAL, null,
            )
        }
        val kmSince = max(0, currentKm - current.odometerKm)
        val daysSince = max(0, ChronoUnit.DAYS.between(current.date, today))
        val dueKm = component.intervalKm?.let { current.odometerKm + it }
        val dueDate = component.intervalMonths?.let { current.date.plusMonths(it.toLong()) }
        val kmRemaining = dueKm?.let { it - currentKm }
        val daysRemaining = dueDate?.let { ChronoUnit.DAYS.between(today, it) }

        val kmFraction = component.intervalKm?.let { kmSince.toFloat() / it } ?: 0f
        val timeFraction = dueDate?.let {
            val total = ChronoUnit.DAYS.between(current.date, it).coerceAtLeast(1)
            daysSince.toFloat() / total
        } ?: 0f
        val progress = max(kmFraction, timeFraction)

        val state = when {
            !hasInterval -> DueState.NO_INTERVAL
            (kmRemaining != null && kmRemaining < 0) || (daysRemaining != null && daysRemaining < 0) -> DueState.OVERDUE
            (kmRemaining != null && kmRemaining <= leadKm) || (daysRemaining != null && daysRemaining <= leadDays) -> DueState.SOON
            else -> DueState.OK
        }

        val byKm = if (kmRemaining != null && kmPerDay != null && kmPerDay > 0.5) {
            today.plusDays(ceil(max(0, kmRemaining) / kmPerDay).toLong())
        } else null
        val estimated = listOfNotNull(byKm, dueDate).minOrNull()

        return ComponentStatus(
            component, current, spans, kmSince, daysSince, dueKm, dueDate,
            kmRemaining, daysRemaining, progress, state, estimated,
        )
    }
}
