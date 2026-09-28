package com.callankan.poloapp.domain

import com.callankan.poloapp.data.db.entity.OdometerEvent
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class OdometerAnalysis(
    val currentKm: Int?,
    val lastReadingDate: LocalDate?,
    val kmPerDay: Double?,
    /** Lecturas con menos km que otra lectura anterior en el tiempo. */
    val anomalies: List<OdometerEvent>,
    val eventCount: Int,
) {
    val kmPerYear: Int? get() = kmPerDay?.let { (it * 365).toInt() }

    companion object {
        val EMPTY = OdometerAnalysis(null, null, null, emptyList(), 0)
    }
}

object OdometerAnalyzer {
    private const val MIN_SPAN_DAYS = 14L

    fun analyze(events: List<OdometerEvent>, fallbackKm: Int? = null): OdometerAnalysis {
        if (events.isEmpty()) return OdometerAnalysis.EMPTY.copy(currentKm = fallbackKm)
        val sorted = events.sortedWith(compareBy<OdometerEvent>({ it.date }, { it.km }))
        val top = sorted.maxWith(compareBy<OdometerEvent>({ it.km }, { it.date }))

        val anomalies = mutableListOf<OdometerEvent>()
        var runningMax = Int.MIN_VALUE
        var runningDate: LocalDate? = null
        for (e in sorted) {
            if (e.km < runningMax && runningDate != null && e.date.isAfter(runningDate)) anomalies += e
            if (e.km > runningMax) {
                runningMax = e.km
                runningDate = e.date
            }
        }

        return OdometerAnalysis(
            currentKm = maxOf(top.km, fallbackKm ?: 0),
            lastReadingDate = sorted.last().date,
            kmPerDay = rate(sorted, top),
            anomalies = anomalies,
            eventCount = events.size,
        )
    }

    /** Media de km/día del último año (o de todo el historial si es más corto). */
    private fun rate(sorted: List<OdometerEvent>, top: OdometerEvent): Double? {
        val windowStart = top.date.minusDays(365)
        val first = sorted.firstOrNull { !it.date.isBefore(windowStart) && it.km < top.km }
            ?: sorted.firstOrNull { it.km < top.km }
            ?: return null
        val days = ChronoUnit.DAYS.between(first.date, top.date)
        if (days < MIN_SPAN_DAYS) {
            val oldest = sorted.first()
            val span = ChronoUnit.DAYS.between(oldest.date, top.date)
            if (span < MIN_SPAN_DAYS || top.km <= oldest.km) return null
            return (top.km - oldest.km).toDouble() / span
        }
        return (top.km - first.km).toDouble() / days
    }
}
