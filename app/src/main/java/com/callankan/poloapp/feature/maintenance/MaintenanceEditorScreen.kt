package com.callankan.poloapp.feature.maintenance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callankan.poloapp.data.model.MaintenanceCategory
import com.callankan.poloapp.data.model.PerformedBy
import com.callankan.poloapp.feature.common.PhotoStrip
import com.callankan.poloapp.feature.common.rememberAttachmentOpener
import com.callankan.poloapp.navigation.PhotoViewerRoute
import com.callankan.poloapp.ui.components.DateField
import com.callankan.poloapp.ui.components.DecimalField
import com.callankan.poloapp.ui.components.DropdownField
import com.callankan.poloapp.ui.components.EditorScaffold
import com.callankan.poloapp.ui.components.FieldRow
import com.callankan.poloapp.ui.components.FormSection
import com.callankan.poloapp.ui.components.IntField
import com.callankan.poloapp.ui.components.PoloCard
import com.callankan.poloapp.ui.components.PoloTextField
import com.callankan.poloapp.ui.components.SegmentedTabs
import com.callankan.poloapp.ui.components.icon
import com.callankan.poloapp.ui.format.Fmt

private val titleSuggestions = listOf(
    "Cambio de aceite y filtro",
    "Revisión anual",
    "Cambio de pastillas de freno",
    "Cambio de neumáticos",
    "Cambio de batería",
    "Avería",
    "Diagnosis",
)

@Composable
fun MaintenanceEditorScreen(onBack: () -> Unit, onNavigate: (Any) -> Unit, vm: MaintenanceEditorViewModel = hiltViewModel()) {
    val f = vm.form
    val workshops by vm.workshops.collectAsStateWithLifecycle()
    var newWorkshopDialog by remember { mutableStateOf(false) }
    val openAttachment = rememberAttachmentOpener(vm.photos.storage) { onNavigate(PhotoViewerRoute(it)) }

    EditorScaffold(
        title = if (vm.isEdit) "Editar intervención" else "Nueva intervención",
        onBack = onBack,
        onSave = { vm.save(onBack) },
        canSave = vm.canSave,
        onDelete = if (vm.isEdit) ({ vm.delete(onBack) }) else null,
        deleteMessage = "Se borrará la intervención, sus piezas registradas y sus fotos.",
    ) {
        PoloTextField(f.title, { v -> vm.update { it.copy(title = v) } }, "¿Qué se hizo?", placeholder = "p. ej. Cambio de aceite y filtro")
        if (!vm.isEdit) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(titleSuggestions) { t -> AssistChip(onClick = { vm.update { it.copy(title = t) } }, label = { Text(t) }) }
            }
        }
        DropdownField(
            f.category, MaintenanceCategory.entries, "Categoría", { it.label }, { v -> vm.update { it.copy(category = v) } },
            optionIcon = { it.icon }, leadingIcon = f.category.icon,
        )
        FieldRow {
            DateField(f.date, { d -> d?.let { vm.update { s -> s.copy(date = it) } } }, "Fecha", Modifier.weight(1.2f))
            IntField(f.km, { v -> vm.update { it.copy(km = v) } }, "Km", Modifier.weight(1f), suffix = "km", leadingIcon = Icons.Rounded.Speed)
        }

        FormSection("¿Quién lo hizo?") {
            SegmentedTabs(PerformedBy.entries.map { it.label }, f.performedBy.ordinal, { i -> vm.update { it.copy(performedBy = PerformedBy.entries[i]) } })
            if (f.performedBy == PerformedBy.WORKSHOP) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DropdownField(
                        workshops.firstOrNull { it.id == f.workshopId }, workshops, "Taller", { it.name },
                        { w -> vm.update { it.copy(workshopId = w.id) } }, Modifier.weight(1f),
                        leadingIcon = Icons.Rounded.Storefront, placeholder = "Elige un taller",
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { newWorkshopDialog = true }) {
                        Icon(Icons.Rounded.Add, null)
                        Text("Nuevo")
                    }
                }
            }
        }

        FormSection("Piezas cambiadas · cuentan km desde hoy") {
            if (vm.components.isEmpty()) {
                Text("No hay piezas en el plan de mantenimiento.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                vm.components.forEach { c ->
                    val selected = f.parts.any { it.componentId == c.id }
                    FilterChip(
                        selected = selected,
                        onClick = { vm.togglePart(c.id) },
                        label = { Text(c.name) },
                        leadingIcon = { Icon(if (selected) Icons.Rounded.Check else c.category.icon, null, Modifier.size(18.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }
            f.parts.forEach { part ->
                val component = vm.components.firstOrNull { it.id == part.componentId } ?: return@forEach
                PoloCard {
                    Text(component.name, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.size(8.dp))
                    FieldRow {
                        PoloTextField(part.brand, { v -> vm.updatePart(part.componentId) { it.copy(brand = v) } }, "Marca", Modifier.weight(1f))
                        PoloTextField(part.reference, { v -> vm.updatePart(part.componentId) { it.copy(reference = v) } }, "Referencia", Modifier.weight(1f))
                    }
                }
            }
        }

        FormSection("Coste") {
            FieldRow {
                DecimalField(f.partsCost, { v -> vm.update { it.copy(partsCost = v) } }, "Piezas", Modifier.weight(1f), suffix = "€")
                DecimalField(f.laborCost, { v -> vm.update { it.copy(laborCost = v) } }, "Mano de obra", Modifier.weight(1f), suffix = "€")
            }
            DecimalField(
                f.totalCost, { v -> vm.update { it.copy(totalCost = v) } }, "Total", suffix = "€",
                placeholder = if (f.totalCost.isBlank()) vm.computedTotal?.let { Fmt.twoDecimals(it) } else null,
                supportingText = "Si lo dejas vacío se suma piezas + mano de obra",
            )
            PoloTextField(f.invoice, { v -> vm.update { it.copy(invoice = v) } }, "Nº de factura", leadingIcon = Icons.Rounded.Receipt)
        }

        PhotoStrip(vm.photos, openAttachment, title = "Facturas y fotos", allowPdf = true)
        PoloTextField(f.notes, { v -> vm.update { it.copy(notes = v) } }, "Notas", singleLine = false, minLines = 3)
        Column(Modifier.fillMaxWidth()) {}
    }

    if (newWorkshopDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { newWorkshopDialog = false },
            title = { Text("Nuevo taller") },
            text = { PoloTextField(name, { name = it }, "Nombre del taller") },
            confirmButton = {
                TextButton(onClick = {
                    vm.quickAddWorkshop(name)
                    newWorkshopDialog = false
                }, enabled = name.isNotBlank()) { Text("Añadir") }
            },
            dismissButton = { TextButton(onClick = { newWorkshopDialog = false }) { Text("Cancelar") } },
        )
    }
}
