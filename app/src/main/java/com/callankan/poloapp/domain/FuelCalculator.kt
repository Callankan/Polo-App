package com.callankan.poloapp.domain

import com.callankan.poloapp.data.db.entity.RefuelEntity
import java.time.LocalDate

/** Tramo entre dos llenados completos: la base del consumo real. */
data class ConsumptionSegment(
    val refuelId: Long,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val startKm: Int,
    val endKm: Int,
    val liters: Double,
    val cost: Double,
) {
    val distanceKm: Int get() = endKm - startKm
    val litersPer100: Double get() = liters / distanceKm * 100.0
    val costPer100: Double get() = cost / distanceKm * 100.0
}

data class FuelStats(
    val segments: List<ConsumptionSegment>,
    val averageLitersPer100: Double?,
    val lastLitersPer100: Double?,
    val bestLitersPer100: Double?,
    val worstLitersPer100: Double?,
    val averagePricePerLiter: Double?,
    val lastPricePerLiter: Double?,
    val totalLiters: Double,
    val totalCost: Double,
    val refuelCount: Int,
    val costPer100Km: Double?,
    val trackedDistanceKm: Int,
) {
    private val byRefuel = segments.associateBy { it.refuelId }
    fun segmentFor(refuelId: Long): ConsumptionSegment? = byRefuel[refuelId]

    companion object {
        val EMPTY = FuelStats(emptyList(), null, null, null, null, null, null, 0.0, 0.0, 0, null, 0)
    }
}

/**
 * Cálculo de consumo por el método "lleno a lleno": el consumo de un tramo es la suma de litros
 * repostados desde el último depósito lleno (sin incluirlo) hasta el siguiente lleno, dividida
 * entre los km recorridos. Los repostajes parciales se reparten en el tramo que los contiene.
 */
object FuelCalculator {
    private const val MIN_VALID = 1.5
    private const val MAX_VALID = 40.0

    fun compute(refuels: List<RefuelEntity>): FuelStats {
        if (refuels.isEmpty()) return FuelStats.EMPTY
        val sorted = refuels.sortedWith(compareBy<RefuelEntity>({ it.odometerKm }, { it.date }, { it.id }))
        val segments = mutableListOf<ConsumptionSegment>()
        var anchor: RefuelEntity? = null
        var liters = 0.0
        var cost = 0.0
        var broken = false
        for (r in sorted) {
            if (r.missedPrevious) broken = true
            liters += r.liters
            cost += r.totalCost
            if (r.fullTank) {
                val a = anchor
                if (a != null && !broken) {
                    val distance = r.odometerKm - a.odometerKm
                    if (distance > 0) {
                        val segment = ConsumptionSegment(r.id, a.date, r.date, a.odometerKm, r.odometerKm, liters, cost)
                        if (segment.litersPer100 in MIN_VALID..MAX_VALID) segments += segment
                    }
                }
                anchor = r
                liters = 0.0
                cost = 0.0
                broken = false
            }
        }
        val segLiters = segments.sumOf { it.liters }
        val segDistance = segments.sumOf { it.distanceKm }
        val segCost = segments.sumOf { it.cost }
        val totalLiters = refuels.sumOf { it.liters }
        val totalCost = refuels.sumOf { it.totalCost }
        return FuelStats(
            segments = segments,
            averageLitersPer100 = if (segDistance > 0) segLiters / segDistance * 100.0 else null,
            lastLitersPer100 = segments.lastOrNull()?.litersPer100,
            bestLitersPer100 = segments.minOfOrNull { it.litersPer100 },
            worstLitersPer100 = segments.maxOfOrNull { it.litersPer100 },
            averagePricePerLiter = if (totalLiters > 0) totalCost / totalLiters else null,
            lastPricePerLiter = sorted.last().pricePerLiter,
            totalLiters = totalLiters,
            totalCost = totalCost,
            refuelCount = refuels.size,
            costPer100Km = if (segDistance > 0) segCost / segDistance * 100.0 else null,
            trackedDistanceKm = segDistance,
        )
    }

    /** Dados dos de los tres valores (litros, €/l, total) calcula el tercero. */
    fun completeTriple(liters: Double?, pricePerLiter: Double?, total: Double?): Triple<Double?, Double?, Double?> = when {
        liters != null && pricePerLiter != null && total == null -> Triple(liters, pricePerLiter, liters * pricePerLiter)
        liters != null && total != null && pricePerLiter == null && liters > 0 -> Triple(liters, total / liters, total)
        pricePerLiter != null && total != null && liters == null && pricePerLiter > 0 -> Triple(total / pricePerLiter, pricePerLiter, total)
        else -> Triple(liters, pricePerLiter, total)
    }
}
