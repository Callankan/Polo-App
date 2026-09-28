package com.callankan.poloapp.feature.maintenance

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callankan.poloapp.data.db.dao.MaintenanceWithDetails
import com.callankan.poloapp.data.db.entity.WorkshopEntity
import com.callankan.poloapp.data.model.PerformedBy
import com.callankan.poloapp.domain.DueState
import com.callankan.poloapp.feature.dashboard.ComponentLine
import com.callankan.poloapp.navigation.ComponentDetailRoute
import com.callankan.poloapp.navigation.ComponentEditorRoute
import com.callankan.poloapp.navigation.MaintenanceDetailRoute
import com.callankan.poloapp.navigation.MaintenanceEditorRoute
import com.callankan.poloapp.navigation.WorkshopEditorRoute
import com.callankan.poloapp.ui.components.EmptyState
import com.callankan.poloapp.ui.components.IconBadge
import com.callankan.poloapp.ui.components.PoloCard
import com.callankan.poloapp.ui.components.PoloFab
import com.callankan.poloapp.ui.components.SectionHeader
import com.callankan.poloapp.ui.components.SegmentedTabs
import com.callankan.poloapp.ui.components.StatTile
import com.callankan.poloapp.ui.components.StatusPill
import com.callankan.poloapp.ui.components.TabScaffold
import com.callankan.poloapp.ui.components.color
import com.callankan.poloapp.ui.components.icon
import com.callankan.poloapp.ui.format.Fmt
import com.callankan.poloapp.ui.theme.NumberStyles
import com.callankan.poloapp.ui.theme.PoloTheme

@Composable
fun MaintenanceScreen(initialTab: Int, onNavigate: (Any) -> Unit, vm: MaintenanceViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val s = state
    if (s == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    MaintenanceContent(s, initialTab, onNavigate)
}

@Composable
fun MaintenanceContent(s: MaintenanceUiState, initialTab: Int, onNavigate: (Any) -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }
    val context = LocalContext.current
    val components = s.overview.components
    TabScaffold(
        title = "Taller",
        subtitle = "${Fmt.km(s.overview.currentKm)} · ${components.size} piezas y tareas en seguimiento",
        header = { SegmentedTabs(listOf("Plan", "Historial", "Talleres"), tab, { tab = it }) },
        fab = {
            AnimatedContent(tab == 2, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "fab") { workshops ->
                if (workshops) PoloFab("Taller", Icons.Rounded.Add) { onNavigate(WorkshopEditorRoute()) }
                else PoloFab("Intervención", Icons.Rounded.Build) { onNavigate(MaintenanceEditorRoute()) }
            }
        },
    ) {
        when (tab) {
            0 -> planTab(s, onNavigate)
            1 -> historyTab(s, onNavigate)
            else -> workshopsTab(s.workshops, s.records, context, onNavigate)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.planTab(s: MaintenanceUiState, onNavigate: (Any) -> Unit) {
    val components = s.overview.components
    item(key = "summary") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryTile("Vencidos", components.count { it.state == DueState.OVERDUE }, DueState.OVERDUE, Modifier.weight(1f))
            SummaryTile("Pronto", components.count { it.state == DueState.SOON }, DueState.SOON, Modifier.weight(1f))
            SummaryTile("Al día", components.count { it.state == DueState.OK }, DueState.OK, Modifier.weight(1f))
        }
    }
    if (components.isEmpty()) {
        item {
            EmptyState(Icons.Rounded.Handyman, "Plan vacío", "Añade piezas o tareas con su intervalo para recibir avisos.", actionLabel = "Añadir pieza", onAction = { onNavigate(ComponentEditorRoute()) })
        }
    }
    items(components, key = { "c${it.component.id}" }) { status ->
        PoloCard(contentPadding = PaddingValues(14.dp)) {
            ComponentLine(status, prominent = true) { onNavigate(ComponentDetailRoute(status.component.id)) }
        }
    }
    item(key = "addComponent") {
        TextButton(onClick = { onNavigate(ComponentEditorRoute()) }) {
            Icon(Icons.Rounded.PlaylistAdd, null)
            Spacer(Modifier.width(8.dp))
            Text("Añadir pieza o tarea al plan")
        }
    }
}

@Composable
private fun SummaryTile(label: String, count: Int, state: DueState, modifier: Modifier) {
    PoloCard(modifier, contentPadding = PaddingValues(14.dp)) {
        Text("$count", style = NumberStyles.large, color = if (count > 0) state.color() else MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.historyTab(s: MaintenanceUiState, onNavigate: (Any) -> Unit) {
    val records = s.records
    if (records.isEmpty()) {
        item {
            EmptyState(
                Icons.Rounded.Build, "Sin intervenciones",
                "Registra revisiones, cambios de piezas y averías. Es la base del informe para vender el coche.",
                actionLabel = "Añadir intervención", onAction = { onNavigate(MaintenanceEditorRoute()) },
            )
        }
        return
    }
    item(key = "totals") {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("Invertido en taller", Fmt.moneyRound(records.sumOf { it.record.totalCost }), Modifier.weight(1f), icon = Icons.Rounded.Build, tint = PoloTheme.colors.info)
            StatTile(
                "Intervenciones", "${records.size}", Modifier.weight(1f),
                caption = "${records.count { it.record.performedBy == PerformedBy.WORKSHOP }} en taller · ${records.count { it.record.performedBy == PerformedBy.DIY }} por tu cuenta",
                icon = Icons.Rounded.Handyman, tint = PoloTheme.colors.teal,
            )
        }
    }
    records.groupBy { it.record.date.year }.forEach { (year, list) ->
        item(key = "y$year") { SectionHeader("$year", subtitle = "${list.size} ${if (list.size == 1) "intervención" else "intervenciones"} · ${Fmt.money(list.sumOf { it.record.totalCost })}") }
        items(list, key = { "r${it.record.id}" }) { RecordCard(it) { onNavigate(MaintenanceDetailRoute(it.record.id)) } }
    }
}

@Composable
fun RecordCard(d: MaintenanceWithDetails, onClick: () -> Unit) {
    val r = d.record
    PoloCard(onClick = onClick, contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            IconBadge(r.category.icon, PoloTheme.colors.info)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(r.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    "${Fmt.date(r.date)} · ${Fmt.km(r.odometerKm)}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                StatusPill(
                    if (r.performedBy == PerformedBy.WORKSHOP) d.workshop?.name ?: "Taller" else "Por mi cuenta",
                    if (r.performedBy == PerformedBy.WORKSHOP) PoloTheme.colors.info else PoloTheme.colors.teal,
                    icon = if (r.performedBy == PerformedBy.WORKSHOP) Icons.Rounded.Storefront else Icons.Rounded.Handyman,
                )
            }
            if (r.totalCost > 0) Text(Fmt.money(r.totalCost), style = NumberStyles.small)
        }
        if (d.installations.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                d.installations.forEach { StatusPill(it.component.name, MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.workshopsTab(
    workshops: List<WorkshopEntity>,
    records: List<MaintenanceWithDetails>,
    context: Context,
    onNavigate: (Any) -> Unit,
) {
    if (workshops.isEmpty()) {
        item {
            EmptyState(Icons.Rounded.Storefront, "Tu agenda de talleres", "Guarda los talleres de confianza con su teléfono y tu valoración.", actionLabel = "Añadir taller", onAction = { onNavigate(WorkshopEditorRoute()) })
        }
        return
    }
    items(workshops, key = { "w${it.id}" }) { w ->
        val visits = records.filter { it.record.workshopId == w.id }
        PoloCard(onClick = { onNavigate(WorkshopEditorRoute(w.id)) }, contentPadding = PaddingValues(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Rounded.Storefront, PoloTheme.colors.info)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(w.name, style = MaterialTheme.typography.titleSmall)
                    if (w.address.isNotBlank()) Text(w.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(5) { i ->
                            Icon(Icons.Rounded.Star, null, tint = if (i < w.rating) PoloTheme.colors.warning else MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(14.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${visits.size} visita${if (visits.size == 1) "" else "s"} · ${Fmt.moneyRound(visits.sumOf { it.record.totalCost })}",
                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (w.phone.isNotBlank()) {
                    FilledTonalIconButton(onClick = { dial(context, w.phone) }) { Icon(Icons.Rounded.Call, "Llamar") }
                }
            }
        }
    }
}

fun dial(context: Context, phone: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.filter { it.isDigit() || it == '+' }}"))) }
}
