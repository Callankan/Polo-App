package com.callankan.poloapp.feature.logbook

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CarCrash
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material.icons.rounded.TireRepair
import androidx.compose.material.icons.rounded.Toll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.callankan.poloapp.data.db.entity.TirePressureEntity
import com.callankan.poloapp.feature.common.AttachmentThumb
import com.callankan.poloapp.feature.common.PhotoItem
import com.callankan.poloapp.navigation.DamageEditorRoute
import com.callankan.poloapp.navigation.DocumentEditorRoute
import com.callankan.poloapp.navigation.NoteEditorRoute
import com.callankan.poloapp.navigation.TireEditorRoute
import com.callankan.poloapp.navigation.TripEditorRoute
import com.callankan.poloapp.navigation.VehicleEditorRoute
import com.callankan.poloapp.ui.components.EmptyState
import com.callankan.poloapp.ui.components.IconBadge
import com.callankan.poloapp.ui.components.ListScaffold
import com.callankan.poloapp.ui.components.MetricText
import com.callankan.poloapp.ui.components.PoloCard
import com.callankan.poloapp.ui.components.PoloFab
import com.callankan.poloapp.ui.components.SectionHeader
import com.callankan.poloapp.ui.components.StatTile
import com.callankan.poloapp.ui.components.StatusPill
import com.callankan.poloapp.ui.components.color
import com.callankan.poloapp.ui.components.icon
import com.callankan.poloapp.ui.format.Fmt
import com.callankan.poloapp.ui.illustration.CarTopView
import com.callankan.poloapp.ui.theme.NumberStyles
import com.callankan.poloapp.ui.theme.PoloTheme
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

// ---------------------------------------------------------------- Viajes

@Composable
fun TripsScreen(onBack: () -> Unit, onNavigate: (Any) -> Unit, vm: TripsViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    ListScaffold("Viajes largos", onBack, fab = { PoloFab("Viaje", Icons.Rounded.Add) { onNavigate(TripEditorRoute()) } }) {
        val s = state ?: return@ListScaffold
        if (s.trips.isEmpty()) {
            item { EmptyState(Icons.Rounded.Map, "Sin viajes", "Apunta tus trayectos largos, como Badalona – Madrid, con km, motivo y observaciones.", actionLabel = "Añadir viaje", onAction = { onNavigate(TripEditorRoute()) }) }
            return@ListScaffold
        }
        val year = LocalDate.now().year
        val costPerKm = s.fuel.costPer100Km?.div(100) ?: s.fuel.averageLitersPer100?.let { l -> s.fuel.averagePricePerLiter?.let { l * it / 100 } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Km en viajes $year", Fmt.kmPlain(s.trips.filter { it.date.year == year }.sumOf { it.distanceKm }), Modifier.weight(1f), unit = "km", icon = Icons.Rounded.Map, tint = PoloTheme.colors.info)
                StatTile("Más largo", Fmt.kmPlain(s.trips.maxOf { it.distanceKm }), Modifier.weight(1f), unit = "km", icon = Icons.AutoMirrored.Rounded.ArrowForward, tint = PoloTheme.colors.teal, caption = s.trips.maxBy { it.distanceKm }.destination)
            }
        }
        items(s.trips, key = { it.id }) { t ->
            PoloCard(onClick = { onNavigate(TripEditorRoute(t.id)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(Fmt.date(t.date), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(t.origin, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.padding(horizontal = 6.dp).size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(t.destination, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        }
                    }
                    MetricText(Fmt.kmPlain(t.distanceKm), "km", style = NumberStyles.medium)
                }
                if (t.purpose.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    StatusPill(t.purpose, PoloTheme.colors.info)
                }
                val fuelCost = costPerKm?.let { it * t.distanceKm }
                if (fuelCost != null || t.tolls != null) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        fuelCost?.let { Text("≈ ${Fmt.moneyRound(it)} de combustible", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        t.tolls?.let {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Toll, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(4.dp))
                                Text("${Fmt.money(it)} peajes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                if (t.notes.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(t.notes, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Notas

@Composable
fun NotesScreen(onBack: () -> Unit, onNavigate: (Any) -> Unit, vm: NotesViewModel = hiltViewModel()) {
    val notes by vm.notes.collectAsStateWithLifecycle()
    ListScaffold("Notas y recordatorios", onBack, fab = { PoloFab("Nota", Icons.Rounded.Add) { onNavigate(NoteEditorRoute()) } }) {
        val list = notes ?: return@ListScaffold
        if (list.isEmpty()) {
            item { EmptyState(Icons.Rounded.StickyNote2, "Sin notas", "Ruidos raros, cosas que comentar en el taller, recordatorios con fecha...", actionLabel = "Nueva nota", onAction = { onNavigate(NoteEditorRoute()) }) }
            return@ListScaffold
        }
        items(list, key = { it.id }) { n ->
            val today = LocalDate.now()
            val due = n.reminderDate?.let { !n.reminderDone && !it.isAfter(today) } == true
            PoloCard(
                onClick = { onNavigate(NoteEditorRoute(n.id)) },
                containerColor = if (n.pinned) PoloTheme.colors.warning.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(n.title, style = MaterialTheme.typography.titleSmall)
                        if (n.body.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(n.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 4, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    IconButton(onClick = { vm.togglePin(n) }) {
                        Icon(Icons.Rounded.PushPin, "Fijar", tint = if (n.pinned) PoloTheme.colors.warning else MaterialTheme.colorScheme.outline)
                    }
                }
                n.reminderDate?.let { date ->
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusPill(
                            "${Fmt.date(date)} · ${Fmt.since(date, today)}",
                            when {
                                n.reminderDone -> PoloTheme.colors.success
                                due -> PoloTheme.colors.danger
                                else -> PoloTheme.colors.info
                            },
                            icon = Icons.Rounded.NotificationsActive,
                        )
                        Spacer(Modifier.weight(1f))
                        Row(
                            Modifier.clip(MaterialTheme.shapes.small).clickable { vm.toggleDone(n) }.padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(if (n.reminderDone) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, null, tint = PoloTheme.colors.success, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (n.reminderDone) "Hecho" else "Marcar hecho", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Neumáticos

fun pressureColor(value: Double, recommended: Double?, ok: Color, warn: Color, danger: Color): Color = when {
    recommended == null -> ok
    abs(value - recommended) <= 0.1 + 1e-6 -> ok
    abs(value - recommended) <= 0.3 + 1e-6 -> warn
    else -> danger
}

@Composable
fun TiresScreen(onBack: () -> Unit, onNavigate: (Any) -> Unit, vm: TiresViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val c = PoloTheme.colors
    ListScaffold("Presión de neumáticos", onBack, fab = { PoloFab("He inflado", Icons.Rounded.TireRepair) { onNavigate(TireEditorRoute()) } }) {
        val s = state ?: return@ListScaffold
        val v = s.vehicle
        item {
            PoloCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Presión recomendada", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onNavigate(VehicleEditorRoute(v.id)) }) { Icon(Icons.Rounded.Edit, "Editar") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column {
                        Text("Delanteras", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        MetricText(Fmt.twoDecimals(v.tireFrontBar), "bar", style = NumberStyles.large)
                    }
                    Column {
                        Text("Traseras", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        MetricText(Fmt.twoDecimals(v.tireRearBar), "bar", style = NumberStyles.large)
                    }
                }
                if (v.tireFrontBar == null) {
                    Text("Añádela desde la etiqueta de la tapa del depósito.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        val last = s.checks.firstOrNull()
        if (last == null) {
            item { EmptyState(Icons.Rounded.TireRepair, "Sin comprobaciones", "Cada vez que infles las ruedas, apúntalo: se guarda el día y la hora.", actionLabel = "Registrar ahora", onAction = { onNavigate(TireEditorRoute()) }) }
            return@ListScaffold
        }
        item {
            val days = ChronoUnit.DAYS.between(last.date, LocalDate.now())
            PoloCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Última comprobación", style = MaterialTheme.typography.titleMedium)
                        Text("${Fmt.longDate(last.date)} · ${Fmt.time(last.minuteOfDay)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    StatusPill(if (days == 0L) "Hoy" else "Hace $days días", if (days > 30) c.warning else c.success)
                }
                Spacer(Modifier.height(12.dp))
                val values = listOf(last.frontLeft, last.frontRight, last.rearLeft, last.rearRight)
                val rec = listOf(v.tireFrontBar, v.tireFrontBar, v.tireRearBar, v.tireRearBar)
                CarTopView(
                    Color(v.colorArgb.toInt()),
                    Modifier.fillMaxWidth(0.62f).align(Alignment.CenterHorizontally),
                    wheelLabels = values.map { Fmt.twoDecimals(it) },
                    wheelColors = values.mapIndexed { i, p -> pressureColor(p, rec[i], c.success, c.warning, c.danger) },
                )
            }
        }
        item { SectionHeader("Historial") }
        items(s.checks, key = { it.id }) { t -> TireRow(t) { onNavigate(TireEditorRoute(t.id)) } }
    }
}

@Composable
private fun TireRow(t: TirePressureEntity, onClick: () -> Unit) {
    PoloCard(onClick = onClick, contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.TireRepair, PoloTheme.colors.teal, size = 36.dp, iconSize = 18.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("${Fmt.date(t.date)} · ${Fmt.time(t.minuteOfDay)}", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Del. ${Fmt.twoDecimals(t.frontLeft)} / ${Fmt.twoDecimals(t.frontRight)} · Tras. ${Fmt.twoDecimals(t.rearLeft)} / ${Fmt.twoDecimals(t.rearRight)} bar",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Daños

@Composable
fun DamagesScreen(onBack: () -> Unit, onNavigate: (Any) -> Unit, vm: DamagesViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    ListScaffold("Daños y golpes", onBack, fab = { PoloFab("Daño", Icons.Rounded.CarCrash) { onNavigate(DamageEditorRoute()) } }) {
        val s = state ?: return@ListScaffold
        if (s.damages.isEmpty()) {
            item { EmptyState(Icons.Rounded.CarCrash, "Sin daños registrados", "¿Un roce con una columna? Márcalo en el coche, añade fotos y sigue su reparación.", actionLabel = "Registrar daño", onAction = { onNavigate(DamageEditorRoute()) }) }
            return@ListScaffold
        }
        val open = s.damages.filter { !it.repaired }
        item {
            PoloCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CarTopView(
                        Color(s.vehicle.colorArgb.toInt()),
                        Modifier.weight(1f),
                        markers = s.damages.associate { it.zone to if (it.repaired) PoloTheme.colors.success else it.severity.color() },
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Column {
                            MetricText("${open.size}", "sin reparar", style = NumberStyles.large, color = if (open.isEmpty()) PoloTheme.colors.success else PoloTheme.colors.danger)
                        }
                        Column {
                            MetricText("${s.damages.size - open.size}", "reparados", style = NumberStyles.large)
                        }
                        Text(
                            "Coste de reparaciones: ${Fmt.moneyRound(s.damages.sumOf { it.repairCost ?: 0.0 })}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        items(s.damages, key = { it.id }) { d ->
            PoloCard(onClick = { onNavigate(DamageEditorRoute(d.id)) }, contentPadding = PaddingValues(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val thumb = s.thumbnails[d.id]
                    if (thumb != null) {
                        AttachmentThumb(PhotoItem(thumb.fileName, thumb.mimeType, thumb), vm.storage, Modifier.size(64.dp).clip(MaterialTheme.shapes.medium))
                    } else {
                        IconBadge(Icons.Rounded.CarCrash, d.severity.color(), size = 64.dp, iconSize = 28.dp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(d.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${Fmt.date(d.date)} · ${d.zone.label}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatusPill(d.severity.label, d.severity.color())
                            StatusPill(if (d.repaired) "Reparado" else "Pendiente", if (d.repaired) PoloTheme.colors.success else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Guantera digital

@Composable
fun DocumentsScreen(onBack: () -> Unit, onNavigate: (Any) -> Unit, vm: DocumentsViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    ListScaffold("Guantera digital", onBack, fab = { PoloFab("Documento", Icons.Rounded.Add) { onNavigate(DocumentEditorRoute()) } }) {
        val s = state ?: return@ListScaffold
        if (s.documents.isEmpty()) {
            item {
                EmptyState(
                    com.callankan.poloapp.data.model.DocumentType.REGISTRATION.icon, "Tu guantera, en el móvil",
                    "Fotografía el permiso de circulación, la ficha técnica, la póliza o el último informe de ITV.",
                    actionLabel = "Añadir documento", onAction = { onNavigate(DocumentEditorRoute()) },
                )
            }
            return@ListScaffold
        }
        items(s.documents, key = { it.id }) { d ->
            val files = s.attachments[d.id].orEmpty()
            val today = LocalDate.now()
            PoloCard(onClick = { onNavigate(DocumentEditorRoute(d.id)) }, contentPadding = PaddingValues(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val first = files.firstOrNull()
                    Box(Modifier.size(72.dp).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                        if (first != null && first.mimeType != "application/pdf") {
                            AsyncImage(model = vm.storage.file(first.fileName), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(72.dp))
                        } else {
                            Icon(d.type.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(d.title, style = MaterialTheme.typography.titleSmall)
                        Text(d.type.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatusPill("${files.size} archivo${if (files.size == 1) "" else "s"}", MaterialTheme.colorScheme.onSurfaceVariant)
                            d.expiryDate?.let { StatusPill("Caduca ${Fmt.since(it, today)}", if (it.isBefore(today)) PoloTheme.colors.danger else PoloTheme.colors.info) }
                        }
                    }
                }
            }
        }
    }
}
