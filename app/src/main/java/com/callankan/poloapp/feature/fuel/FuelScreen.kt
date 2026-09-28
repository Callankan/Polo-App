package com.callankan.poloapp.feature.fuel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Euro
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callankan.poloapp.domain.FuelStats
import com.callankan.poloapp.navigation.RefuelEditorRoute
import com.callankan.poloapp.navigation.StatsRoute
import com.callankan.poloapp.ui.components.ChartCard
import com.callankan.poloapp.ui.components.ChartColors
import com.callankan.poloapp.ui.components.EmptyState
import com.callankan.poloapp.ui.components.IconBadge
import com.callankan.poloapp.ui.components.LineChart
import com.callankan.poloapp.ui.components.PoloCard
import com.callankan.poloapp.ui.components.PoloFab
import com.callankan.poloapp.ui.components.SectionHeader
import com.callankan.poloapp.ui.components.StatTile
import com.callankan.poloapp.ui.components.StatusPill
import com.callankan.poloapp.ui.components.TabScaffold
import com.callankan.poloapp.ui.format.Fmt
import com.callankan.poloapp.ui.theme.NumberStyles
import com.callankan.poloapp.ui.theme.PoloTheme
import java.time.YearMonth

@Composable
fun FuelScreen(onNavigate: (Any) -> Unit, vm: FuelViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val s = state
    if (s == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    FuelContent(s, onNavigate)
}

@Composable
fun FuelContent(s: FuelUiState, onNavigate: (Any) -> Unit) {
    val stats = s.stats
    TabScaffold(
        title = "Combustible",
        subtitle = if (s.refuels.isEmpty()) "Registra repostajes para calcular tu consumo real"
        else "${s.refuels.size} repostajes · ${Fmt.km(stats.trackedDistanceKm)} con consumo medido",
        actions = { IconButton(onClick = { onNavigate(StatsRoute) }) { Icon(Icons.Rounded.BarChart, "Estadísticas") } },
        fab = { PoloFab("Repostar", Icons.Rounded.Add) { onNavigate(RefuelEditorRoute()) } },
    ) {
        if (s.refuels.isEmpty()) {
            item {
                EmptyState(
                    Icons.Rounded.LocalGasStation,
                    "Sin repostajes todavía",
                    "Anota fecha, km, litros y precio. Con dos depósitos llenos ya verás tu consumo medio real.",
                    actionLabel = "Añadir repostaje",
                    onAction = { onNavigate(RefuelEditorRoute()) },
                )
            }
            return@TabScaffold
        }
        item { ConsumptionHero(stats) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Precio medio", Fmt.threeDecimals(stats.averagePricePerLiter), Modifier.weight(1f), unit = "€/l", icon = Icons.Rounded.Euro, tint = ChartColors.fuel)
                StatTile("Coste", Fmt.twoDecimals(stats.costPer100Km), Modifier.weight(1f), unit = "€/100 km", icon = Icons.Rounded.Route, tint = PoloTheme.colors.info)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Total repostado", Fmt.number(Math.round(stats.totalLiters)), Modifier.weight(1f), unit = "l", icon = Icons.Rounded.LocalGasStation, tint = PoloTheme.colors.teal)
                StatTile("Total gastado", Fmt.number(Math.round(stats.totalCost)), Modifier.weight(1f), unit = "€", icon = Icons.Rounded.Euro, tint = PoloTheme.colors.violet)
            }
        }
        if (stats.segments.size >= 2) {
            item {
                ChartCard("Evolución del consumo", "l/100 km por tramo entre depósitos llenos") {
                    val segs = stats.segments.takeLast(12)
                    LineChart(
                        segs.map { it.litersPer100 },
                        ChartColors.fuel,
                        labels = listOf(Fmt.dayMonth(segs.first().endDate), Fmt.dayMonth(segs.last().endDate)),
                        valueLabel = { Fmt.twoDecimals(it) },
                        showReference = stats.averageLitersPer100,
                    )
                }
            }
        }
        if (s.refuels.size >= 3) {
            item {
                ChartCard("Precio del litro", "Últimos repostajes") {
                    val last = s.refuels.take(12).reversed()
                    LineChart(
                        last.map { it.pricePerLiter },
                        PoloTheme.colors.info,
                        labels = listOf(Fmt.dayMonth(last.first().date), Fmt.dayMonth(last.last().date)),
                        valueLabel = { Fmt.threeDecimals(it) },
                        height = 120.dp,
                    )
                }
            }
        }
        s.refuels.groupBy { YearMonth.from(it.date) }.forEach { (month, list) ->
            item(key = "m$month") {
                SectionHeader(Fmt.monthYear(month), subtitle = "${Fmt.liters(list.sumOf { it.liters })} · ${Fmt.money(list.sumOf { it.totalCost })}")
            }
            items(list, key = { it.id }) { r ->
                val segment = stats.segmentFor(r.id)
                PoloCard(onClick = { onNavigate(RefuelEditorRoute(r.id)) }, contentPadding = PaddingValues(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Rounded.LocalGasStation, if (r.fullTank) ChartColors.fuel else MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                listOf(Fmt.date(r.date), r.station).filter { it.isNotBlank() }.joinToString(" · "),
                                style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "${Fmt.km(r.odometerKm)} · ${Fmt.liters(r.liters)} · ${Fmt.pricePerLiter(r.pricePerLiter)}",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(Fmt.money(r.totalCost), style = NumberStyles.small)
                            Spacer(Modifier.height(4.dp))
                            when {
                                segment != null -> ConsumptionPill(segment.litersPer100, stats.averageLitersPer100)
                                !r.fullTank -> StatusPill("Parcial", MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsumptionPill(value: Double, average: Double?) {
    val color = when {
        average == null -> PoloTheme.colors.info
        value <= average * 0.97 -> PoloTheme.colors.success
        value >= average * 1.05 -> PoloTheme.colors.warning
        else -> PoloTheme.colors.info
    }
    StatusPill("${Fmt.twoDecimals(value)} l/100", color)
}

@Composable
private fun ConsumptionHero(stats: FuelStats) {
    PoloCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.Speed, ChartColors.fuel, size = 36.dp, iconSize = 18.dp)
            Spacer(Modifier.width(10.dp))
            Text("Consumo medio real", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(Fmt.twoDecimals(stats.averageLitersPer100), style = NumberStyles.hero, modifier = Modifier.alignByBaseline())
            Spacer(Modifier.width(8.dp))
            Text("l/100 km", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.alignByBaseline())
        }
        if (stats.averageLitersPer100 == null) {
            Text(
                "Necesitas dos repostajes con el depósito lleno para calcularlo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Mini("Último", stats.lastLitersPer100, Color.Unspecified)
            Mini("Mejor", stats.bestLitersPer100, PoloTheme.colors.success)
            Mini("Peor", stats.worstLitersPer100, PoloTheme.colors.warning)
        }
    }
}

@Composable
private fun Mini(label: String, value: Double?, color: Color) {
    Column(Modifier.padding(end = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(Fmt.twoDecimals(value), style = NumberStyles.small, color = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else color)
    }
}
