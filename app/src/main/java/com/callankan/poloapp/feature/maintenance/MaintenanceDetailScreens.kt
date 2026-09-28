package com.callankan.poloapp.feature.maintenance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callankan.poloapp.data.model.MaintenanceCategory
import com.callankan.poloapp.data.model.PerformedBy
import com.callankan.poloapp.feature.common.AttachmentGallery
import com.callankan.poloapp.feature.common.rememberAttachmentOpener
import com.callankan.poloapp.feature.dashboard.componentSummary
import com.callankan.poloapp.navigation.ComponentEditorRoute
import com.callankan.poloapp.navigation.InstallationEditorRoute
import com.callankan.poloapp.navigation.MaintenanceDetailRoute
import com.callankan.poloapp.navigation.MaintenanceEditorRoute
import com.callankan.poloapp.navigation.PhotoViewerRoute
import com.callankan.poloapp.ui.components.DropdownField
import com.callankan.poloapp.ui.components.EditorScaffold
import com.callankan.poloapp.ui.components.FieldRow
import com.callankan.poloapp.ui.components.FormSection
import com.callankan.poloapp.ui.components.IconBadge
import com.callankan.poloapp.ui.components.InfoLine
import com.callankan.poloapp.ui.components.IntField
import com.callankan.poloapp.ui.components.LinearMeter
import com.callankan.poloapp.ui.components.ListScaffold
import com.callankan.poloapp.ui.components.MetricText
import com.callankan.poloapp.ui.components.PoloCard
import com.callankan.poloapp.ui.components.PoloTextField
import com.callankan.poloapp.ui.components.ProgressRing
import com.callankan.poloapp.ui.components.SectionHeader
import com.callankan.poloapp.ui.components.StatusPill
import com.callankan.poloapp.ui.components.SwitchRow
import com.callankan.poloapp.ui.components.color
import com.callankan.poloapp.ui.components.icon
import com.callankan.poloapp.ui.components.label
import com.callankan.poloapp.ui.format.Fmt
import com.callankan.poloapp.ui.theme.NumberStyles
import com.callankan.poloapp.ui.theme.PoloTheme
import com.callankan.poloapp.ui.components.DateField
import com.callankan.poloapp.ui.components.DecimalField
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material.icons.rounded.Call
import androidx.compose.ui.platform.LocalContext

@Composable
fun MaintenanceDetailScreen(onBack: () -> Unit, onNavigate: (Any) -> Unit, vm: MaintenanceDetailViewModel = hiltViewModel()) {
    val detail by vm.record.collectAsStateWithLifecycle()
    val photos by vm.photos.collectAsStateWithLifecycle()
    val open = rememberAttachmentOpener(vm.storage) { onNavigate(PhotoViewerRoute(it)) }
    val d = detail
    ListScaffold(
        title = "Intervención",
        onBack = onBack,
        actions = {
            d?.let { IconButton(onClick = { onNavigate(MaintenanceEditorRoute(it.record.id)) }) { Icon(Icons.Rounded.Edit, "Editar") } }
        },
    ) {
        if (d == null) return@ListScaffold
        val r = d.record
        item {
            PoloCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(r.category.icon, PoloTheme.colors.info, size = 48.dp, iconSize = 24.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(r.title, style = MaterialTheme.typography.titleLarge)
                        Text(r.category.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(16.dp))
                if (r.totalCost > 0) MetricText(Fmt.moneyPlain(r.totalCost), "€", style = NumberStyles.large)
                Spacer(Modifier.height(8.dp))
                InfoLine("Fecha", Fmt.longDate(r.date))
                InfoLine("Kilómetros", Fmt.km(r.odometerKm))
                InfoLine("Realizado", if (r.performedBy == PerformedBy.WORKSHOP) d.workshop?.name ?: "Taller" else "Por mi cuenta")
                r.partsCost?.let { InfoLine("Piezas", Fmt.money(it)) }
                r.laborCost?.let { InfoLine("Mano de obra", Fmt.money(it)) }
                if (r.invoiceNumber.isNotBlank()) InfoLine("Factura", r.invoiceNumber)
            }
        }
        if (d.installations.isNotEmpty()) {
            item { SectionHeader("Piezas cambiadas") }
            items(d.installations, key = { it.installation.id }) { i ->
                PoloCard(contentPadding = PaddingValues(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(i.component.category.icon, MaterialTheme.colorScheme.primary, size = 34.dp, iconSize = 18.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(i.component.name, style = MaterialTheme.typography.titleSmall)
                            val ref = listOf(i.installation.brand, i.installation.reference).filter { it.isNotBlank() }.joinToString(" · ")
                            if (ref.isNotBlank()) Text(ref, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        if (photos.isNotEmpty()) {
            item { SectionHeader("Facturas y fotos") }
            item { AttachmentGallery(photos, vm.storage, open) }
        }
        if (r.notes.isNotBlank()) {
            item { SectionHeader("Notas") }
            item { PoloCard { Text(r.notes, style = MaterialTheme.typography.bodyMedium) } }
        }
    }
}

@Composable
fun ComponentDetailScreen(onBack: () -> Unit, onNavigate: (Any) -> Unit, vm: ComponentDetailViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val pair = state
    val color = pair?.first?.state?.color() ?: MaterialTheme.colorScheme.primary
    val infoColor = PoloTheme.colors.info
    ListScaffold(
        title = pair?.first?.component?.name ?: "Pieza",
        onBack = onBack,
        actions = { IconButton(onClick = { onNavigate(ComponentEditorRoute(vm.componentId)) }) { Icon(Icons.Rounded.EditNote, "Editar pieza") } },
    ) {
        if (pair == null) return@ListScaffold
        val (s, currentKm) = pair
        item {
            PoloCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing(s.progress, color, size = 96.dp, strokeWidth = 9.dp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(s.component.category.icon, null, tint = color, modifier = Modifier.size(22.dp))
                            Text(Fmt.percent(s.progress.coerceAtMost(9.99f)), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Spacer(Modifier.width(18.dp))
                    Column(Modifier.weight(1f)) {
                        StatusPill(s.state.label, color)
                        Spacer(Modifier.height(8.dp))
                        Text("Lleva recorridos", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        MetricText(Fmt.kmPlain(s.kmSinceInstall), "km", style = NumberStyles.large)
                        s.daysSinceInstall?.let { Text("desde hace ${Fmt.duration((it / 30.44).toInt())}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
                Spacer(Modifier.height(16.dp))
                LinearMeter(s.progress, color)
                Spacer(Modifier.height(8.dp))
                Text(componentSummary(s), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                InfoLine("Intervalo", listOfNotNull(s.component.intervalKm?.let { Fmt.km(it) }, s.component.intervalMonths?.let { "$it meses" }).joinToString(" o ").ifEmpty { "Sin intervalo" })
                s.current?.let {
                    InfoLine("Instalada", "${Fmt.date(it.date)} · ${Fmt.km(it.odometerKm)}")
                    val ref = listOf(it.brand, it.reference).filter { t -> t.isNotBlank() }.joinToString(" · ")
                    if (ref.isNotBlank()) InfoLine("Marca / ref.", ref)
                }
                s.dueKm?.let { InfoLine("Próximo cambio", Fmt.km(it)) }
                s.dueDate?.let { InfoLine("Fecha límite", Fmt.date(it)) }
                s.estimatedDueDate?.let { InfoLine("Estimación a tu ritmo", Fmt.date(it), valueColor = infoColor) }
            }
        }
        if (s.component.hint.isNotBlank()) {
            item {
                PoloCard(containerColor = infoColor.copy(alpha = 0.1f)) {
                    Row {
                        Icon(Icons.Rounded.Info, null, tint = infoColor, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(s.component.hint, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { onNavigate(MaintenanceEditorRoute(componentId = s.component.id)) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Build, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Nuevo cambio")
                }
                OutlinedButton(onClick = { onNavigate(InstallationEditorRoute(s.component.id)) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Dato antiguo")
                }
            }
        }
        item { SectionHeader("Ciclo de vida", subtitle = "Cada instalación y cuántos km duró") }
        if (s.lifeSpans.isEmpty()) {
            item {
                Text(
                    "Aún no hay cambios registrados. Si sabes cuándo se cambió por última vez, añádelo como dato antiguo.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(s.lifeSpans, key = { it.installation.id }) { span ->
            val i = span.installation
            PoloCard(
                onClick = {
                    if (i.maintenanceId != null) onNavigate(MaintenanceDetailRoute(i.maintenanceId))
                    else onNavigate(InstallationEditorRoute(s.component.id, i.id))
                },
                contentPadding = PaddingValues(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Rounded.History, if (span.isActive) color else MaterialTheme.colorScheme.onSurfaceVariant, size = 34.dp, iconSize = 18.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (span.isActive) "Instalada ahora" else "${Fmt.date(i.date)} → ${Fmt.date(span.removedDate)}",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            "desde ${Fmt.km(i.odometerKm)}" + listOf(i.brand, i.reference).filter { it.isNotBlank() }.joinToString(" · ").let { if (it.isBlank()) "" else " · $it" },
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(Fmt.km(span.kmUsed(currentKm)), style = NumberStyles.small)
                        Text(if (span.isActive) "y sumando" else "duró", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun ComponentEditorScreen(onBack: () -> Unit, onDeleted: () -> Unit, vm: ComponentEditorViewModel = hiltViewModel()) {
    val f = vm.form
    EditorScaffold(
        title = if (vm.isEdit) "Editar pieza" else "Nueva pieza o tarea",
        onBack = onBack,
        onSave = { vm.save(onBack) },
        canSave = vm.canSave,
        onDelete = if (vm.isEdit) ({ vm.delete(onDeleted) }) else null,
        deleteMessage = "Se borrará la pieza del plan y todo su historial de cambios.",
    ) {
        PoloTextField(f.name, { v -> vm.update { it.copy(name = v) } }, "Nombre", placeholder = "p. ej. Aceite de motor")
        DropdownField(f.category, MaintenanceCategory.entries, "Categoría", { it.label }, { v -> vm.update { it.copy(category = v) } }, optionIcon = { it.icon }, leadingIcon = f.category.icon)
        FormSection("Intervalo · avisa con lo que llegue antes") {
            FieldRow {
                IntField(f.intervalKm, { v -> vm.update { it.copy(intervalKm = v) } }, "Cada", Modifier.weight(1f), suffix = "km")
                IntField(f.intervalMonths, { v -> vm.update { it.copy(intervalMonths = v) } }, "o cada", Modifier.weight(1f), suffix = "meses")
            }
            Text("Deja en blanco lo que no aplique. Sin intervalo, solo contará los km.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SwitchRow("Avisos activados", f.alerts, { v -> vm.update { it.copy(alerts = v) } }, subtitle = "Notificación cuando se acerque el cambio")
        PoloTextField(f.hint, { v -> vm.update { it.copy(hint = v) } }, "Consejo o nota", singleLine = false, minLines = 2)
        if (vm.isEdit) SwitchRow("Archivar", f.archived, { v -> vm.update { it.copy(archived = v) } }, subtitle = "Ocultar del plan sin borrar el historial")
    }
}

@Composable
fun InstallationEditorScreen(onBack: () -> Unit, vm: InstallationEditorViewModel = hiltViewModel()) {
    val f = vm.form
    EditorScaffold(
        title = vm.componentName.ifBlank { "Cambio de pieza" },
        onBack = onBack,
        onSave = { vm.save(onBack) },
        canSave = vm.canSave,
        onDelete = if (vm.isEdit && !vm.linkedToMaintenance) ({ vm.delete(onBack) }) else null,
    ) {
        Text(
            "Registra cuándo se cambió esta pieza aunque no tengas el resto de datos de la intervención.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FieldRow {
            DateField(f.date, { d -> d?.let { vm.update { s -> s.copy(date = it) } } }, "Fecha", Modifier.weight(1.2f))
            IntField(f.km, { v -> vm.update { it.copy(km = v) } }, "Km", Modifier.weight(1f), suffix = "km")
        }
        FieldRow {
            PoloTextField(f.brand, { v -> vm.update { it.copy(brand = v) } }, "Marca", Modifier.weight(1f))
            PoloTextField(f.reference, { v -> vm.update { it.copy(reference = v) } }, "Referencia", Modifier.weight(1f))
        }
        DecimalField(f.cost, { v -> vm.update { it.copy(cost = v) } }, "Coste de la pieza", suffix = "€")
        PoloTextField(f.notes, { v -> vm.update { it.copy(notes = v) } }, "Notas", singleLine = false, minLines = 2)
    }
}

@Composable
fun WorkshopEditorScreen(onBack: () -> Unit, vm: WorkshopEditorViewModel = hiltViewModel()) {
    val f = vm.form
    val usage by vm.usage.collectAsState(initial = 0)
    val context = LocalContext.current
    EditorScaffold(
        title = if (vm.isEdit) "Editar taller" else "Nuevo taller",
        onBack = onBack,
        onSave = { vm.save(onBack) },
        canSave = vm.canSave,
        onDelete = if (vm.isEdit) ({ vm.delete(onBack) }) else null,
        deleteMessage = if (usage > 0) "Tiene $usage intervenciones asociadas: se conservarán, pero sin taller." else "Esta acción no se puede deshacer.",
    ) {
        PoloTextField(f.name, { v -> vm.update { it.copy(name = v) } }, "Nombre")
        Row(verticalAlignment = Alignment.CenterVertically) {
            PoloTextField(f.phone, { v -> vm.update { it.copy(phone = v) } }, "Teléfono", Modifier.weight(1f), keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
            if (f.phone.isNotBlank()) {
                Spacer(Modifier.width(8.dp))
                FilledTonalIconButton(onClick = { dial(context, f.phone) }) { Icon(Icons.Rounded.Call, "Llamar") }
            }
        }
        PoloTextField(f.address, { v -> vm.update { it.copy(address = v) } }, "Dirección")
        FormSection("Tu valoración") {
            Row {
                (1..5).forEach { i ->
                    IconButton(onClick = { vm.update { it.copy(rating = if (it.rating == i) 0 else i) } }) {
                        Icon(
                            if (i <= f.rating) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                            "$i estrellas",
                            tint = if (i <= f.rating) PoloTheme.colors.warning else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
            }
        }
        PoloTextField(f.notes, { v -> vm.update { it.copy(notes = v) } }, "Notas", singleLine = false, minLines = 3)
        Spacer(Modifier.fillMaxWidth())
    }
}
