package com.callankan.poloapp.feature.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Euro
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.callankan.poloapp.data.db.entity.OdometerEvent
import com.callankan.poloapp.data.db.entity.VehicleEntity
import com.callankan.poloapp.data.repository.ActiveVehicle
import com.callankan.poloapp.data.repository.ComplianceRepository
import com.callankan.poloapp.data.repository.FinanceRepository
import com.callankan.poloapp.data.repository.FuelRepository
import com.callankan.poloapp.data.repository.LogbookRepository
import com.callankan.poloapp.data.repository.MaintenanceRepository
import com.callankan.poloapp.data.repository.VehicleRepository
import com.callankan.poloapp.domain.CostAnalyzer
import com.callankan.poloapp.domain.CostEntry
import com.callankan.poloapp.domain.CostGroup
import com.callankan.poloapp.domain.FuelCalculator
import com.callankan.poloapp.domain.FuelStats
import com.callankan.poloapp.domain.OdometerAnalyzer
import com.callankan.poloapp.ui.components.BarSeries
import com.callankan.poloapp.ui.components.ChartCard
import com.callankan.poloapp.ui.components.ChartColors
import com.callankan.poloapp.ui.components.DonutChart
import com.callankan.poloapp.ui.components.EmptyState
import com.callankan.poloapp.ui.components.LineChart
import com.callankan.poloapp.ui.components.ListScaffold
import com.callankan.poloapp.ui.components.PoloCard
import com.callankan.poloapp.ui.components.SegmentedTabs
import com.callankan.poloapp.ui.components.StackedBarChart
import com.callankan.poloapp.ui.components.StatTile
import com.callankan.poloapp.ui.components.color
import com.callankan.poloapp.ui.format.Fmt
import com.callankan.poloapp.ui.theme.NumberStyles
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class StatsData(
    val vehicle: VehicleEntity,
    val costs: List<CostEntry>,
    val events: List<OdometerEvent>,
    val fuel: FuelStats,
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    active: ActiveVehicle,
    fuel: FuelRepository,
    maintenance: MaintenanceRepository,
    finance: FinanceRepository,
    compliance: ComplianceRepository,
    logbook: LogbookRepository,
    vehicles: VehicleRepository,
) : ViewModel() {
    @Suppress("UNCHECKED_CAST")
    val state: StateFlow<StatsData?> = active.vehicle.flatMapLatest { v ->
        combine(
            listOf(
                fuel.observe(v.id), maintenance.observeRecords(v.id), finance.observeExpenses(v.id),
                compliance.observeItv(v.id), compliance.observeInsurance(v.id), logbook.observeDamages(v.id),
                logbook.observeTrips(v.id), vehicles.observeOdometerEvents(v.id),
            ),
        ) { a ->
            val refuels = a[0] as List<com.callankan.poloapp.data.db.entity.RefuelEntity>
            StatsData(
                vehicle = v,
                costs = CostAnalyzer.entries(
                    refuels,
                    (a[1] as List<com.callankan.poloapp.data.db.dao.MaintenanceWithDetails>).map { it.record },
                    a[2] as List<com.callankan.poloapp.data.db.entity.ExpenseEntity>,
                    a[3] as List<com.callankan.poloapp.data.db.entity.ItvInspectionEntity>,
                    a[4] as List<com.callankan.poloapp.data.db.entity.InsurancePolicyEntity>,
                    a[5] as List<com.callankan.poloapp.data.db.entity.DamageEntity>,
                    a[6] as List<com.callankan.poloapp.data.db.entity.TripEntity>,
                ),
                events = a[7] as List<OdometerEvent>,
                fuel = FuelCalculator.compute(refuels),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** Km recorridos en cada uno de los últimos [months] meses a partir de las lecturas. */
fun kmPerMonth(events: List<OdometerEvent>, months: Int, today: LocalDate): List<Pair<YearMonth, Int?>> {
    val sorted = events.sortedBy { it.date }
    fun maxUntil(date: LocalDate): Int? = sorted.filter { !it.date.isAfter(date) }.maxOfOrNull { it.km }
    val end = YearMonth.from(today)
    return (months - 1 downTo 0).map { i ->
        val m = end.minusMonths(i.toLong())
        val atEnd = maxUntil(m.atEndOfMonth())
        val atStart = maxUntil(m.minusMonths(1).atEndOfMonth())
        m to if (atEnd != null && atStart != null) (atEnd - atStart).coerceAtLeast(0) else null
    }
}

@Composable
fun StatsScreen(onBack: () -> Unit, vm: StatsViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var period by rememberSaveable { mutableIntStateOf(0) }
    ListScaffold("Estadísticas", onBack) {
        val s = state ?: return@ListScaffold
        statsContent(s, period) { period = it }
    }
}

fun androidx.compose.foundation.lazy.LazyListScope.statsContent(s: StatsData, period: Int, onPeriod: (Int) -> Unit) {
    val today = LocalDate.now()
    item { SegmentedTabs(listOf("Últimos 12 meses", "Desde la compra"), period, onPeriod) }
    if (s.costs.isEmpty()) {
        item { EmptyState(Icons.Rounded.Euro, "Aún no hay datos", "Registra repostajes, mantenimientos y gastos para ver tus estadísticas.") }
        return
    }
    val from = if (period == 0) YearMonth.from(today).minusMonths(11).atDay(1) else (s.vehicle.purchaseDate ?: s.costs.minOf { it.date })
    val inPeriod = s.costs.filter { !it.date.isBefore(from) }
    val total = inPeriod.sumOf { it.amount }
    val months = (ChronoUnit.MONTHS.between(YearMonth.from(from), YearMonth.from(today)) + 1).coerceAtLeast(1)
    val analysis = OdometerAnalyzer.analyze(s.events, s.vehicle.purchaseKm)
    val kmStart = s.events.filter { !it.date.isAfter(from) }.maxOfOrNull { it.km }
        ?: s.events.minByOrNull { it.date }?.km
    val kmDriven = if (analysis.currentKm != null && kmStart != null) (analysis.currentKm - kmStart).coerceAtLeast(0) else null

    item {
        PoloCard {
            Text("Coste total del coche", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(Fmt.money(total), style = NumberStyles.hero)
            Text("en ${Fmt.duration(months.toInt())}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("Al mes", Fmt.moneyRound(total / months), Modifier.weight(1f), icon = Icons.Rounded.CalendarMonth, tint = ChartColors.other)
            StatTile("Por km", kmDriven?.takeIf { it > 0 }?.let { Fmt.twoDecimals(total / it) } ?: "—", Modifier.weight(1f), unit = "€/km", icon = Icons.Rounded.Route, tint = ChartColors.fuel)
            StatTile("Recorridos", Fmt.kmPlain(kmDriven), Modifier.weight(1f), unit = "km", icon = Icons.Rounded.Speed, tint = ChartColors.maintenance)
        }
    }
    item {
        val monthly = CostAnalyzer.monthly(s.costs, 12, today)
        ChartCard("Gasto mensual", "Últimos 12 meses") {
            StackedBarChart(
                values = monthly.map { m -> CostGroup.entries.map { m.byGroup[it] ?: 0.0 } },
                series = CostGroup.entries.map { BarSeries(it.label, it.color) },
                barLabels = monthly.map { Fmt.shortMonth(it.month).take(1) },
            )
        }
    }
    val breakdown = CostAnalyzer.breakdown(inPeriod)
    item {
        ChartCard("¿En qué se va el dinero?") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DonutChart(breakdown.map { it.second to it.first.color }, size = 140.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(Fmt.moneyRound(total), style = NumberStyles.small)
                        Text("total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    breakdown.take(7).forEach { (kind, v) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(kind.color))
                            Spacer(Modifier.width(6.dp))
                            Text(kind.label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f), maxLines = 1)
                            Text(Fmt.percent((v / total).toFloat()), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
    val km = kmPerMonth(s.events, 12, today)
    if (km.count { it.second != null } >= 2) {
        item {
            ChartCard("Kilómetros por mes") {
                StackedBarChart(
                    values = km.map { listOf((it.second ?: 0).toDouble()) },
                    series = listOf(BarSeries("Km", ChartColors.maintenance)),
                    barLabels = km.map { Fmt.shortMonth(it.first).take(1) },
                    height = 130.dp,
                )
            }
        }
    }
    if (s.fuel.segments.size >= 2) {
        item {
            ChartCard("Consumo", "Media ${Fmt.consumption(s.fuel.averageLitersPer100)}") {
                LineChart(s.fuel.segments.takeLast(20).map { it.litersPer100 }, ChartColors.fuel, valueLabel = { Fmt.twoDecimals(it) }, showReference = s.fuel.averageLitersPer100, height = 130.dp)
            }
        }
    }
}
