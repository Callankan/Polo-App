package com.callankan.poloapp.feature.logbook

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callankan.poloapp.data.model.CarZone
import com.callankan.poloapp.data.model.DamageSeverity
import com.callankan.poloapp.data.model.DocumentType
import com.callankan.poloapp.feature.common.PhotoStrip
import com.callankan.poloapp.feature.common.rememberAttachmentOpener
import com.callankan.poloapp.navigation.PhotoViewerRoute
import com.callankan.poloapp.ui.components.ChoiceChips
import com.callankan.poloapp.ui.components.DateField
import com.callankan.poloapp.ui.components.DecimalField
import com.callankan.poloapp.ui.components.DropdownField
import com.callankan.poloapp.ui.components.EditorScaffold
import com.callankan.poloapp.ui.components.FieldRow
import com.callankan.poloapp.ui.components.FormSection
import com.callankan.poloapp.ui.components.IntField
import com.callankan.poloapp.ui.components.PickerField
import com.callankan.poloapp.ui.components.PoloCard
import com.callankan.poloapp.ui.components.PoloTextField
import com.callankan.poloapp.ui.components.SwitchRow
import com.callankan.poloapp.ui.components.icon
import com.callankan.poloapp.ui.format.Fmt
import com.callankan.poloapp.ui.illustration.CarTopView

private val purposes = listOf("Vacaciones", "Familia", "Trabajo", "Escapada", "Mudanza")

@Composable
fun TripEditorScreen(onBack: () -> Unit, vm: TripEditorViewModel = hiltViewModel()) {
    val f = vm.form
    EditorScaffold(
        title = if (vm.isEdit) "Editar viaje" else "Nuevo viaje",
        onBack = onBack,
        onSave = { vm.save(onBack) },
        canSave = vm.canSave,
        onDelete = if (vm.isEdit) ({ vm.delete(onBack) }) else null,
    ) {
        FieldRow {
            PoloTextField(f.origin, { v -> vm.update { it.copy(origin = v) } }, "Origen", Modifier.weight(1f), placeholder = "Badalona", capitalization = KeyboardCapitalization.Words)
            PoloTextField(f.destination, { v -> vm.update { it.copy(destination = v) } }, "Destino", Modifier.weight(1f), placeholder = "Madrid", capitalization = KeyboardCapitalization.Words)
        }
        DateField(f.date, { d -> d?.let { vm.update { s -> s.copy(date = it) } } }, "Fecha")
        FormSection("Kilómetros") {
            FieldRow {
                IntField(f.startKm, { v -> vm.update { it.copy(startKm = v) } }, "Al salir", Modifier.weight(1f), suffix = "km")
                IntField(f.endKm, { v -> vm.update { it.copy(endKm = v) } }, "Al llegar", Modifier.weight(1f), suffix = "km")
            }
            IntField(
                f.distance, { v -> vm.update { it.copy(distance = v) } }, "Distancia recorrida", suffix = "km",
                placeholder = vm.distance?.let { Fmt.number(it) },
                supportingText = "Se calcula sola con los km de salida y llegada",
            )
        }
        FormSection("Motivo") {
            ChoiceChips(purposes, f.purpose.takeIf { it in purposes }, { it }, { v -> vm.update { it.copy(purpose = v) } })
            PoloTextField(f.purpose, { v -> vm.update { it.copy(purpose = v) } }, "Propósito del viaje")
        }
        DecimalField(f.tolls, { v -> vm.update { it.copy(tolls = v) } }, "Peajes", suffix = "€")
        PoloTextField(f.notes, { v -> vm.update { it.copy(notes = v) } }, "Observaciones", singleLine = false, minLines = 3, placeholder = "Tráfico, consumo, incidencias...")
    }
}

@Composable
fun NoteEditorScreen(onBack: () -> Unit, vm: NoteEditorViewModel = hiltViewModel()) {
    val f = vm.form
    EditorScaffold(
        title = if (vm.isEdit) "Editar nota" else "Nueva nota",
        onBack = onBack,
        onSave = { vm.save(onBack) },
        canSave = vm.canSave,
        onDelete = if (vm.isEdit) ({ vm.delete(onBack) }) else null,
    ) {
        PoloTextField(f.title, { v -> vm.update { it.copy(title = v) } }, "Título")
        PoloTextField(f.body, { v -> vm.update { it.copy(body = v) } }, "Nota", singleLine = false, minLines = 6)
        SwitchRow("Fijar en el inicio", f.pinned, { v -> vm.update { it.copy(pinned = v) } }, subtitle = "Aparecerá en el dashboard")
        FormSection("Recordatorio") {
            DateField(f.reminder, { d -> vm.update { it.copy(reminder = d) } }, "Avisarme el día", allowClear = true, supportingText = "Recibirás una notificación ese día")
            if (f.reminder != null) SwitchRow("Hecho", f.done, { v -> vm.update { it.copy(done = v) } })
        }
    }
}

@Composable
fun TireEditorScreen(onBack: () -> Unit, vm: TireEditorViewModel = hiltViewModel()) {
    val f = vm.form
    val context = LocalContext.current
    EditorScaffold(
        title = if (vm.isEdit) "Editar comprobación" else "Presión de neumáticos",
        onBack = onBack,
        onSave = { vm.save(onBack) },
        canSave = vm.canSave,
        onDelete = if (vm.isEdit) ({ vm.delete(onBack) }) else null,
    ) {
        Text("Se guarda el día y la hora en que lo hiciste.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FieldRow {
            DateField(f.date, { d -> d?.let { vm.update { s -> s.copy(date = it) } } }, "Día", Modifier.weight(1.3f))
            PickerField(
                Fmt.time(f.minute), "Hora", {
                    TimePickerDialog(context, { _, h, m -> vm.update { it.copy(minute = h * 60 + m) } }, f.minute / 60, f.minute % 60, true).show()
                },
                Modifier.weight(1f), leadingIcon = Icons.Rounded.Schedule,
            )
        }
        if (vm.recommendedFront != null || vm.recommendedRear != null) {
            AssistChip(
                onClick = vm::useRecommended,
                label = { Text("Usar la recomendada (${Fmt.twoDecimals(vm.recommendedFront)} / ${Fmt.twoDecimals(vm.recommendedRear)} bar)") },
                leadingIcon = { Icon(Icons.Rounded.AutoFixHigh, null, Modifier) },
                colors = AssistChipDefaults.assistChipColors(leadingIconContentColor = MaterialTheme.colorScheme.primary),
            )
        }
        SwitchRow("Misma presión en cada eje", f.sameAxle, { v -> vm.update { it.copy(sameAxle = v) } })
        if (f.sameAxle) {
            FieldRow {
                DecimalField(f.fl, { v -> vm.update { it.copy(fl = v) } }, "Delanteras", Modifier.weight(1f), suffix = "bar")
                DecimalField(f.rl, { v -> vm.update { it.copy(rl = v) } }, "Traseras", Modifier.weight(1f), suffix = "bar")
            }
        } else {
            FieldRow {
                DecimalField(f.fl, { v -> vm.update { it.copy(fl = v) } }, "Del. izquierda", Modifier.weight(1f), suffix = "bar")
                DecimalField(f.fr, { v -> vm.update { it.copy(fr = v) } }, "Del. derecha", Modifier.weight(1f), suffix = "bar")
            }
            FieldRow {
                DecimalField(f.rl, { v -> vm.update { it.copy(rl = v) } }, "Tras. izquierda", Modifier.weight(1f), suffix = "bar")
                DecimalField(f.rr, { v -> vm.update { it.copy(rr = v) } }, "Tras. derecha", Modifier.weight(1f), suffix = "bar")
            }
        }
        IntField(f.km, { v -> vm.update { it.copy(km = v) } }, "Km (opcional)", suffix = "km", leadingIcon = Icons.Rounded.Speed)
        PoloTextField(f.notes, { v -> vm.update { it.copy(notes = v) } }, "Notas", placeholder = "p. ej. Gasolinera, en frío", singleLine = false, minLines = 2)
    }
}

@Composable
fun DamageEditorScreen(onBack: () -> Unit, onNavigate: (Any) -> Unit, vm: DamageEditorViewModel = hiltViewModel()) {
    val f = vm.form
    val workshops by vm.workshops.collectAsStateWithLifecycle()
    val open = rememberAttachmentOpener(vm.photos.storage) { onNavigate(PhotoViewerRoute(it)) }
    EditorScaffold(
        title = if (vm.isEdit) "Editar daño" else "Nuevo daño",
        onBack = onBack,
        onSave = { vm.save(onBack) },
        canSave = vm.canSave,
        onDelete = if (vm.isEdit) ({ vm.delete(onBack) }) else null,
    ) {
        PoloTextField(f.title, { v -> vm.update { it.copy(title = v) } }, "¿Qué ha pasado?", placeholder = "p. ej. Rayón con una columna")
        FormSection("¿Dónde? Toca la zona en el coche") {
            PoloCard {
                CarTopView(
                    Color(vm.bodyColor.toInt()),
                    Modifier.fillMaxWidth(0.55f).align(androidx.compose.ui.Alignment.CenterHorizontally),
                    selected = f.zone,
                    selectionColor = MaterialTheme.colorScheme.primary,
                    onZoneClick = { z -> vm.update { it.copy(zone = z) } },
                )
            }
            DropdownField(f.zone, CarZone.entries, "Zona", { it.label }, { z -> vm.update { it.copy(zone = z) } }, placeholder = "Elige la zona")
        }
        FormSection("Gravedad") {
            ChoiceChips(DamageSeverity.entries, f.severity, { it.label }, { v -> vm.update { it.copy(severity = v) } })
        }
        FieldRow {
            DateField(f.date, { d -> d?.let { vm.update { s -> s.copy(date = it) } } }, "Fecha", Modifier.weight(1.2f))
            IntField(f.km, { v -> vm.update { it.copy(km = v) } }, "Km", Modifier.weight(1f), suffix = "km")
        }
        PhotoStrip(vm.photos, open, title = "Fotos del daño")
        PoloTextField(f.description, { v -> vm.update { it.copy(description = v) } }, "Descripción", singleLine = false, minLines = 3)
        FormSection("Reparación") {
            SwitchRow("Reparado", f.repaired, { v -> vm.update { it.copy(repaired = v) } })
            if (f.repaired) {
                DateField(f.repairedDate, { d -> vm.update { it.copy(repairedDate = d) } }, "Fecha de reparación")
                DecimalField(f.repairCost, { v -> vm.update { it.copy(repairCost = v) } }, "Coste", suffix = "€")
                DropdownField(workshops.firstOrNull { it.id == f.workshopId }, workshops, "Taller", { it.name }, { w -> vm.update { it.copy(workshopId = w.id) } }, leadingIcon = Icons.Rounded.Storefront, placeholder = "Opcional")
            }
            SwitchRow("Parte al seguro", f.insuranceClaim, { v -> vm.update { it.copy(insuranceClaim = v) } })
        }
    }
}

@Composable
fun DocumentEditorScreen(onBack: () -> Unit, onNavigate: (Any) -> Unit, vm: DocumentEditorViewModel = hiltViewModel()) {
    val f = vm.form
    val open = rememberAttachmentOpener(vm.photos.storage) { onNavigate(PhotoViewerRoute(it)) }
    EditorScaffold(
        title = if (vm.isEdit) "Documento" else "Nuevo documento",
        onBack = onBack,
        onSave = { vm.save(onBack) },
        canSave = true,
        onDelete = if (vm.isEdit) ({ vm.delete(onBack) }) else null,
    ) {
        ChoiceChips(DocumentType.entries, f.type, { it.label }, { v -> vm.update { it.copy(type = v) } }, optionIcon = { it.icon })
        PoloTextField(f.title, { v -> vm.update { it.copy(title = v) } }, "Título", placeholder = f.type.label)
        PhotoStrip(vm.photos, open, title = "Fotos o PDF", allowPdf = true)
        DateField(f.expiry, { d -> vm.update { it.copy(expiry = d) } }, "Caducidad (opcional)", allowClear = true)
        PoloTextField(f.notes, { v -> vm.update { it.copy(notes = v) } }, "Notas", singleLine = false, minLines = 2)
    }
}
