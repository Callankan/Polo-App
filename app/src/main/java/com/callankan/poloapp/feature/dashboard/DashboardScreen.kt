package com.callankan.poloapp.feature.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.rounded.CarCrash
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.TireRepair
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callankan.poloapp.data.repository.VehicleOverview
import com.callankan.poloapp.domain.ComponentStatus
import com.callankan.poloapp.domain.Deadline
import com.callankan.poloapp.domain.DeadlineState
import com.callankan.poloapp.domain.DebtSummary
import com.callankan.poloapp.domain.DueState
import com.callankan.poloapp.navigation.ComponentDetailRoute
import com.callankan.poloapp.navigation.DamageEditorRoute
import com.callankan.poloapp.navigation.DamagesRoute
import com.callankan.poloapp.navigation.ExpenseEditorRoute
import com.callankan.poloapp.navigation.FinanceRoute
import com.callankan.poloapp.navigation.FuelRoute
import com.callankan.poloapp.navigation.InsuranceRoute
import com.callankan.poloapp.navigation.ItvRoute
import com.callankan.poloapp.navigation.LoanEditorRoute
import com.callankan.poloapp.navigation.MaintenanceRoute
import com.callankan.poloapp.navigation.NoteEditorRoute
import com.callankan.poloapp.navigation.OdometerEditorRoute
import com.callankan.poloapp.navigation.RefuelEditorRoute
import com.callankan.poloapp.navigation.SettingsRoute
import com.callankan.poloapp.navigation.StatsRoute
import com.callankan.poloapp.navigation.TireEditorRoute
import com.callankan.poloapp.navigation.TiresRoute
import com.callankan.poloapp.navigation.VehiclesRoute
import com.callankan.poloapp.ui.components.AnimatedKm
import com.callankan.poloapp.ui.components.IconBadge
import com.callankan.poloapp.ui.components.LinearMeter
import com.callankan.poloapp.ui.components.MetricText
import com.callankan.poloapp.ui.components.PoloCard
import com.callankan.poloapp.ui.components.ProgressRing
import com.callankan.poloapp.ui.components.SectionHeader
import com.callankan.poloapp.ui.components.SpanishPlate
import com.callankan.poloapp.ui.components.Sparkline
import com.callankan.poloapp.ui.components.StatusPill
import com.callankan.poloapp.ui.components.color
import com.callankan.poloapp.ui.components.icon
import com.callankan.poloapp.ui.components.label
import com.callankan.poloapp.ui.format.Fmt
import com.callankan.poloapp.ui.illustration.CarIllustration
import com.callankan.poloapp.ui.theme.NumberStyles
import com.callankan.poloapp.ui.theme.PoloDimens
import com.callankan.poloapp.ui.theme.PoloTheme
import java.time.LocalTime
import kotlin.math.abs

@Composable
fun DashboardScreen(onNavigate: (Any) -> Unit, vm: DashboardViewModel = hiltViewModel()) {
    val overview by vm.overview.collectAsStateWithLifecycle()
    val data = overview
    if (data == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else {
        DashboardContent(data, onNavigate)
    }
}

@Composable
fun DashboardContent(o: VehicleOverview, onNavigate: (Any) -> Unit, animate: Boolean = true) {
    var visible by remember { mutableStateOf(!animate) }
    LaunchedEffect(Unit) { visible = true }
    val tips = buildTips(o)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = PoloDimens.screenPadding, end = PoloDimens.screenPadding, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Header(o, onNavigate) }
        item { Reveal(visible, 0) { HeroCard(o, onNavigate) } }
        item { Reveal(visible, 1) { QuickActions(onNavigate) } }
        item {
            Reveal(visible, 2) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DeadlineCard("ITV", Icons.Rounded.TaskAlt, o.itv, Modifier.weight(1f), emptyText = "Añade la fecha") { onNavigate(ItvRoute) }
                    DeadlineCard(
                        "Seguro", Icons.Rounded.Shield, o.insurance, Modifier.weight(1f),
                        subtitle = o.policy?.company, emptyText = "Añade tu póliza",
                    ) { onNavigate(InsuranceRoute) }
                }
            }
        }
        item { Reveal(visible, 3) { MaintenanceCard(o, onNavigate) } }
        item { Reveal(visible, 4) { DebtCard(o.primaryDebt, onNavigate) } }
        item { Reveal(visible, 5) { FuelCard(o, onNavigate) } }
        if (tips.isNotEmpty()) {
            item { SectionHeader("Para hoy", subtitle = "Pequeñas cosas que mantienen el coche al día") }
            items(tips, key = { it.title }) { tip ->
                PoloCard(onClick = { onNavigate(tip.route) }, contentPadding = PaddingValues(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(tip.icon, tip.color)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(tip.title, style = MaterialTheme.typography.titleSmall)
                            Text(tip.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        if (o.pinnedNotes.isNotEmpty()) {
            item { SectionHeader("Notas fijadas") }
            items(o.pinnedNotes, key = { "note${it.id}" }) { note ->
                PoloCard(onClick = { onNavigate(NoteEditorRoute(note.id)) }, contentPadding = PaddingValues(14.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Rounded.PushPin, null, tint = PoloTheme.colors.warning, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(note.title, style = MaterialTheme.typography.titleSmall)
                            if (note.body.isNotBlank()) {
                                Text(note.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Reveal(visible: Boolean, index: Int, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(420, delayMillis = 60 * index)) +
            slideInVertically(tween(480, delayMillis = 60 * index)) { it / 6 },
    ) { content() }
}

@Composable
private fun Header(o: VehicleOverview, onNavigate: (Any) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(Fmt.greeting(LocalTime.now().hour), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(o.vehicle.alias, style = MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        HeaderButton(Icons.Rounded.DirectionsCar, "Vehículos") { onNavigate(VehiclesRoute) }
        Spacer(Modifier.width(8.dp))
        HeaderButton(Icons.Rounded.Settings, "Ajustes") { onNavigate(SettingsRoute) }
    }
}

@Composable
private fun HeaderButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun HeroCard(o: VehicleOverview, onNavigate: (Any) -> Unit) {
    val colors = PoloTheme.colors
    val shape = MaterialTheme.shapes.extraLarge
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(colors.heroTop, colors.heroBottom)))
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(colors.heroGlow.copy(alpha = 0.42f), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.58f),
                        radius = size.width * 0.62f,
                    ),
                )
            }
            .clickable { onNavigate(OdometerEditorRoute()) },
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    SpanishPlate(o.vehicle.plate)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        listOf(o.vehicle.displayName, o.vehicle.version).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                ProgressRing(
                    progress = o.health.score / 100f,
                    color = o.health.level.color(),
                    size = 58.dp,
                    strokeWidth = 6.dp,
                    trackColor = Color.White.copy(alpha = 0.1f),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${o.health.score}", style = NumberStyles.small, color = Color.White)
                        Icon(Icons.Rounded.Favorite, null, tint = o.health.level.color(), modifier = Modifier.size(10.dp))
                    }
                }
            }
            CarIllustration(
                bodyColor = Color(o.vehicle.colorArgb.toInt()),
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
            )
            Text("Cuentakilómetros", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.6f))
            Row(verticalAlignment = Alignment.Bottom) {
                AnimatedKm(o.currentKm, color = Color.White, modifier = Modifier.alignByBaseline())
                Spacer(Modifier.width(6.dp))
                Text("km", style = NumberStyles.small, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.alignByBaseline())
            }
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                o.odometer.kmPerDay?.let { HeroChip("≈ ${Fmt.number(Math.round(it))} km/día") }
                HeroChip("Salud: ${o.health.level.label.lowercase()}")
                o.odometer.lastReadingDate?.let { HeroChip("Última lectura ${Fmt.since(it, o.today)}") }
            }
        }
    }
}

@Composable
private fun HeroChip(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = Color.White.copy(alpha = 0.85f),
        maxLines = 1,
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

private data class QuickAction(val label: String, val icon: ImageVector, val route: Any)

@Composable
private fun QuickActions(onNavigate: (Any) -> Unit) {
    val actions = listOf(
        QuickAction("Repostaje", Icons.Rounded.LocalGasStation, RefuelEditorRoute()),
        QuickAction("Gasto", Icons.Rounded.Receipt, ExpenseEditorRoute()),
        QuickAction("Nota", Icons.Rounded.StickyNote2, NoteEditorRoute()),
        QuickAction("Km", Icons.Rounded.Speed, OdometerEditorRoute()),
        QuickAction("Presión", Icons.Rounded.TireRepair, TireEditorRoute()),
        QuickAction("Daño", Icons.Rounded.CarCrash, DamageEditorRoute()),
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
        items(actions, key = { it.label }) { a ->
            Column(
                Modifier
                    .width(68.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { onNavigate(a.route) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(a.icon, null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(6.dp))
                Text(a.label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
        }
    }
}

@Composable
private fun DeadlineCard(
    title: String,
    icon: ImageVector,
    deadline: Deadline,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    emptyText: String,
    onClick: () -> Unit,
) {
    val color = deadline.state.color()
    PoloCard(modifier = modifier, onClick = onClick, contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        val days = deadline.daysLeft
        if (days == null) {
            Text(emptyText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(18.dp))
            StatusPill("Sin datos", color)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressRing(
                    progress = if (days < 0) 1f else 1f - (days / 365f).coerceIn(0f, 1f),
                    color = color,
                    size = 52.dp,
                    strokeWidth = 5.dp,
                ) {
                    Icon(if (deadline.state == DeadlineState.OK) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber, null, tint = color, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    MetricText(Fmt.number(abs(days)), if (abs(days) == 1L) "día" else "días", style = NumberStyles.large)
                    Text(
                        if (days < 0) "caducada" else "restantes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(Fmt.date(deadline.date), style = MaterialTheme.typography.labelLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun MaintenanceCard(o: VehicleOverview, onNavigate: (Any) -> Unit) {
    val next = o.nextMaintenance
    PoloCard(onClick = { onNavigate(MaintenanceRoute()) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Próximo mantenimiento", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (o.alerts.isNotEmpty()) StatusPill("${o.alerts.size} aviso${if (o.alerts.size > 1) "s" else ""}", PoloTheme.colors.warning, icon = Icons.Rounded.NotificationsActive)
        }
        Spacer(Modifier.height(14.dp))
        if (next == null) {
            Text(
                "Registra el último cambio de aceite, filtros o neumáticos para empezar a contar kilómetros.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            ComponentLine(next, prominent = true) { onNavigate(ComponentDetailRoute(next.component.id)) }
            o.components.filter { it !== next && (it.state == DueState.OVERDUE || it.state == DueState.SOON || it.state == DueState.OK) }
                .take(2)
                .forEach {
                    Spacer(Modifier.height(14.dp))
                    ComponentLine(it, prominent = false) { onNavigate(ComponentDetailRoute(it.component.id)) }
                }
        }
    }
}

@Composable
fun ComponentLine(s: ComponentStatus, prominent: Boolean, onClick: () -> Unit) {
    val color = s.state.color()
    Column(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(s.component.category.icon, color, size = if (prominent) 40.dp else 32.dp, iconSize = if (prominent) 20.dp else 16.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(s.component.name, style = if (prominent) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(componentSummary(s), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            StatusPill(s.state.label, color)
        }
        Spacer(Modifier.height(8.dp))
        LinearMeter(s.progress, color, height = if (prominent) 8.dp else 6.dp)
    }
}

fun componentSummary(s: ComponentStatus): String = when (s.state) {
    DueState.OVERDUE -> listOfNotNull(
        s.kmRemaining?.takeIf { it < 0 }?.let { "pasado ${Fmt.km(-it)}" },
        s.daysRemaining?.takeIf { it < 0 }?.let { "caducado por tiempo ${Fmt.relativeDays(it)}" },
    ).joinToString(" · ")
    DueState.SOON, DueState.OK -> listOfNotNull(
        s.kmRemaining?.let { "quedan ${Fmt.km(it)}" },
        s.estimatedDueDate?.let { (if (s.kmRemaining != null) "aprox. " else "vence el ") + Fmt.date(it) },
    ).joinToString(" · ")
    DueState.NEVER_DONE -> "Registra el último cambio para empezar a contar"
    DueState.NO_INTERVAL -> s.kmSinceInstall?.let { "${Fmt.km(it)} recorridos" } ?: "Sin intervalo definido"
}

@Composable
private fun DebtCard(debt: DebtSummary?, onNavigate: (Any) -> Unit) {
    val colors = PoloTheme.colors
    if (debt == null) {
        PoloCard(onClick = { onNavigate(LoanEditorRoute()) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Rounded.Savings, colors.violet)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Financiación", style = MaterialTheme.typography.titleMedium)
                    Text("Registra la deuda del coche y sigue cada pago.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        return
    }
    PoloCard(onClick = { onNavigate(FinanceRoute(0)) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Deuda · ${debt.loan.lender}", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (debt.isPaidOff) "¡Saldada por completo!" else "Pendiente",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ProgressRing(debt.progress, colors.violet, size = 46.dp, strokeWidth = 5.dp) {
                Text(Fmt.percent(debt.progress), style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.height(6.dp))
        MetricText(Fmt.moneyPlain(debt.remaining), "€", style = NumberStyles.large)
        Spacer(Modifier.height(10.dp))
        LinearMeter(debt.progress, colors.violet)
        Spacer(Modifier.height(10.dp))
        Row {
            Text("Pagado ${Fmt.moneyRound(debt.paid)} de ${Fmt.moneyRound(debt.initial)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            debt.projectedPayoff?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Rounded.TrendingDown, null, tint = colors.violet, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("fin ≈ ${Fmt.monthYear(it)}", style = MaterialTheme.typography.labelMedium, color = colors.violet)
                }
            }
        }
    }
}

@Composable
private fun FuelCard(o: VehicleOverview, onNavigate: (Any) -> Unit) {
    val fuelColor = Color(0xFFFF8A3D)
    PoloCard(onClick = { onNavigate(FuelRoute) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.LocalGasStation, fuelColor, size = 34.dp, iconSize = 18.dp)
            Spacer(Modifier.width(10.dp))
            Text("Consumo", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("Ver estadísticas", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { onNavigate(StatsRoute) })
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                MetricText(Fmt.twoDecimals(o.fuel.averageLitersPer100), "l/100 km", style = NumberStyles.large)
                Text("media real (lleno a lleno)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val series = o.fuel.segments.takeLast(10).map { it.litersPer100 }
            if (series.size >= 2) Sparkline(series, fuelColor, Modifier.width(110.dp).height(44.dp))
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniStat("Último €/l", Fmt.threeDecimals(o.fuel.lastPricePerLiter), Modifier.weight(1f))
            MiniStat("Gasto del mes", Fmt.moneyRound(o.monthSpend), Modifier.weight(1f))
            MiniStat("€ / 100 km", Fmt.twoDecimals(o.fuel.costPer100Km), Modifier.weight(1f))
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Text(value, style = NumberStyles.small, maxLines = 1)
    }
}

private data class Tip(val title: String, val subtitle: String, val icon: ImageVector, val color: Color, val route: Any)

@Composable
private fun buildTips(o: VehicleOverview): List<Tip> {
    val colors = PoloTheme.colors
    return buildList {
        o.dueReminders.forEach { add(Tip(it.title, "Recordatorio · ${Fmt.date(it.reminderDate)}", Icons.Rounded.EventNote, colors.info, NoteEditorRoute(it.id))) }
        val tire = o.lastTireCheck
        val tireDays = tire?.let { java.time.temporal.ChronoUnit.DAYS.between(it.date, o.today) }
        if (tireDays == null || tireDays > o.settings.pressureReminderDays) {
            add(
                Tip(
                    "Revisa la presión de los neumáticos",
                    if (tireDays == null) "Aún no has registrado ninguna comprobación" else "Última comprobación hace $tireDays días",
                    Icons.Rounded.TireRepair, colors.warning, if (tire == null) TireEditorRoute() else TiresRoute,
                ),
            )
        }
        if (o.openDamages.isNotEmpty()) {
            add(Tip("${o.openDamages.size} daño(s) sin reparar", o.openDamages.first().title, Icons.Rounded.CarCrash, colors.danger, DamagesRoute))
        }
        if (o.odometer.anomalies.isNotEmpty()) {
            add(Tip("Revisa el kilometraje", "Hay lecturas con menos km que otras anteriores", Icons.Rounded.WarningAmber, colors.danger, com.callankan.poloapp.navigation.OdometerHistoryRoute))
        }
    }
}
