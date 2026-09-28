package com.callankan.poloapp.domain

import com.callankan.poloapp.data.db.entity.DamageEntity
import com.callankan.poloapp.data.db.entity.ExpenseEntity
import com.callankan.poloapp.data.db.entity.InsurancePolicyEntity
import com.callankan.poloapp.data.db.entity.ItvInspectionEntity
import com.callankan.poloapp.data.db.entity.MaintenanceEntity
import com.callankan.poloapp.data.db.entity.RefuelEntity
import com.callankan.poloapp.data.db.entity.TripEntity
import com.callankan.poloapp.data.model.ExpenseCategory
import java.time.LocalDate
import java.time.YearMonth

enum class CostGroup(val label: String) { FUEL("Combustible"), MAINTENANCE("Taller"), OTHER("Otros") }

enum class CostKind(val label: String, val group: CostGroup) {
    FUEL("Combustible", CostGroup.FUEL),
    MAINTENANCE("Mantenimiento", CostGroup.MAINTENANCE),
    DAMAGE("Daños", CostGroup.MAINTENANCE),
    INSURANCE("Seguro", CostGroup.OTHER),
    ITV("ITV", CostGroup.OTHER),
    TAX("Impuesto de circulación", CostGroup.OTHER),
    PARKING("Parking", CostGroup.OTHER),
    TOLL("Peajes", CostGroup.OTHER),
    WASH("Lavado y limpieza", CostGroup.OTHER),
    FINE("Multas", CostGroup.OTHER),
    ACCESSORIES("Accesorios", CostGroup.OTHER),
    PAPERWORK("Gestiones", CostGroup.OTHER),
    OTHER("Otros", CostGroup.OTHER),
}

data class CostEntry(val date: LocalDate, val amount: Double, val kind: CostKind)

data class MonthCost(val month: YearMonth, val byGroup: Map<CostGroup, Double>) {
    val total: Double get() = byGroup.values.sum()
}

object CostAnalyzer {
    fun entries(
        refuels: List<RefuelEntity>,
        maintenance: List<MaintenanceEntity>,
        expenses: List<ExpenseEntity>,
        itv: List<ItvInspectionEntity>,
        insurance: List<InsurancePolicyEntity>,
        damages: List<DamageEntity>,
        trips: List<TripEntity>,
    ): List<CostEntry> = buildList {
        refuels.forEach { add(CostEntry(it.date, it.totalCost, CostKind.FUEL)) }
        maintenance.forEach { if (it.totalCost > 0) add(CostEntry(it.date, it.totalCost, CostKind.MAINTENANCE)) }
        expenses.forEach { add(CostEntry(it.date, it.amount, kindOf(it.category))) }
        itv.forEach { i -> i.cost?.takeIf { it > 0 }?.let { add(CostEntry(i.date, it, CostKind.ITV)) } }
        insurance.forEach { p -> p.premium?.takeIf { it > 0 }?.let { add(CostEntry(p.startDate, it, CostKind.INSURANCE)) } }
        damages.forEach { d ->
            d.repairCost?.takeIf { it > 0 }?.let { add(CostEntry(d.repairedDate ?: d.date, it, CostKind.DAMAGE)) }
        }
        trips.forEach { t -> t.tolls?.takeIf { it > 0 }?.let { add(CostEntry(t.date, it, CostKind.TOLL)) } }
    }

    fun kindOf(category: ExpenseCategory): CostKind = when (category) {
        ExpenseCategory.TAX -> CostKind.TAX
        ExpenseCategory.PARKING -> CostKind.PARKING
        ExpenseCategory.TOLL -> CostKind.TOLL
        ExpenseCategory.WASH -> CostKind.WASH
        ExpenseCategory.FINE -> CostKind.FINE
        ExpenseCategory.ACCESSORIES -> CostKind.ACCESSORIES
        ExpenseCategory.PAPERWORK -> CostKind.PAPERWORK
        ExpenseCategory.OTHER -> CostKind.OTHER
    }

    /** Totales de los últimos [months] meses (incluido el actual), rellenando meses vacíos. */
    fun monthly(entries: List<CostEntry>, months: Int, today: LocalDate): List<MonthCost> {
        val end = YearMonth.from(today)
        val start = end.minusMonths((months - 1).toLong())
        val grouped = entries.filter { !YearMonth.from(it.date).isBefore(start) && !YearMonth.from(it.date).isAfter(end) }
            .groupBy { YearMonth.from(it.date) }
        return (0 until months).map { i ->
            val m = start.plusMonths(i.toLong())
            val list = grouped[m].orEmpty()
            MonthCost(m, CostGroup.entries.associateWith { g -> list.filter { it.kind.group == g }.sumOf { it.amount } })
        }
    }

    fun breakdown(entries: List<CostEntry>): List<Pair<CostKind, Double>> =
        entries.groupBy { it.kind }.map { (k, v) -> k to v.sumOf { it.amount } }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
}
