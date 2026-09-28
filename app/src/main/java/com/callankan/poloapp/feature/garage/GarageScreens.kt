package com.callankan.poloapp.feature.garage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callankan.poloapp.data.db.entity.SpecEntity
import com.callankan.poloapp.data.model.FuelType
import com.callankan.poloapp.data.model.OdometerSource
import com.callankan.poloapp.data.model.TimingDrive
import com.callankan.poloapp.data.preset.Presets
import com.callankan.poloapp.navigation.OdometerEditorRoute
import com.callankan.poloapp.navigation.OnboardingRoute
import com.callankan.poloapp.navigation.VehicleEditorRoute
import com.callankan.poloapp.ui.components.ChartCard
import com.callankan.poloapp.ui.components.ChoiceChips
import com.callankan.poloapp.ui.components.DateField
import com.callankan.poloapp.ui.components.DecimalField
import com.callankan.poloapp.ui.components.EditorScaffold
import com.callankan.poloapp.ui.components.FieldRow
import com.callankan.poloapp.ui.components.FormSection
import com.callankan.poloapp.ui.components.IconBadge
import com.callankan.poloapp.ui.components.InfoLine
import com.callankan.poloapp.ui.components.IntField
import com.callankan.poloapp.ui.components.LineChart
import com.callankan.poloapp.ui.components.ListScaffold
import com.callankan.poloapp.ui.components.MetricText
import com.callankan.poloapp.ui.components.PoloCard
import com.callankan.poloapp.ui.components.PoloFab
import com.callankan.poloapp.ui.components.PoloTextField
import com.callankan.poloapp.ui.components.SectionHeader
import com.callankan.poloapp.ui.components.SpanishPlate
import com.callankan.poloapp.ui.components.StatTile
import com.callankan.poloapp.ui.format.Fmt
import com.callankan.poloapp.ui.illustration.CarIllustration
import com.callankan.poloapp.ui.theme.NumberStyles
import com.callankan.poloapp.ui.theme.PoloTheme

@Composable
fun VehiclesScreen(onBack: () -> Unit, onNavigate: (Any) -> Unit, vm: VehiclesViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    ListScaffold("Mis vehículos", onBack, fab = { PoloFab("Vehículo", Icons.Rounded.Add) { onNavigate(OnboardingRoute(additional = true)) } }) {
        val (vehicles, selected) = state ?: return@ListScaffold
        items(vehicles, key = { it.id }) { v ->
            val isSelected = v.id == selected
            PoloCard(
                onClick = {
                    vm.select(v.id)
                    onBack()
                },
                containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SpanishPlate(v.plate)
                    Spacer(Modifier.weight(1f))
                    if (isSelected) Icon(Icons.Rounded.CheckCircle, "Seleccionado", tint = MaterialTheme.colorScheme.primary)
                    IconButton(onClick = { onNavigate(VehicleEditorRoute(v.id)) }) { Icon(Icons.Rounded.Edit, "Editar") }
                }
                CarIllustration(Color(v.colorArgb.toInt()), Modifier.fillMaxWidth().padding(vertical = 4.dp))
                Text(v.alias, style = MaterialTheme.typography.titleLarge)
                Text(listOf(v.displayName, v.version).filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Text(
                "La app está preparada para varios coches: cada uno tiene su propio historial, plan y documentos.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun VehicleEditorScreen(onBack: () -> Unit, onDeleted: () -> Unit, vm: VehicleEditorViewModel = hiltViewModel()) {
    val f = vm.form
    EditorScaffold(
        title = "Datos del vehículo",
        onBack = onBack,
        onSave = { vm.save(onBack) },
        canSave = vm.canSave,
        onDelete = { vm.delete(onDeleted) },
        deleteMessage = "Se borrará el vehículo con TODO su historial, fotos y documentos.",
    ) {
        PoloCard {
            CarIllustration(Color(f.colorArgb.toInt()), Modifier.fillMaxWidth())
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Presets.carColors.forEach { c ->
                val selected = c.argb == f.colorArgb
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(c.argb.toInt()))
                        .border(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape)
                        .clickable { vm.update { it.copy(colorArgb = c.argb, colorName = c.name) } },
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) Icon(Icons.Rounded.Check, null, tint = if (c.argb == 0xFFF1F1EE) Color.Black else Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
        FormSection("Identificación") {
            PoloTextField(f.alias, { v -> vm.update { it.copy(alias = v) } }, "Nombre en la app")
            FieldRow {
                PoloTextField(f.make, { v -> vm.update { it.copy(make = v) } }, "Marca", Modifier.weight(1f), capitalization = KeyboardCapitalization.Words)
                PoloTextField(f.model, { v -> vm.update { it.copy(model = v) } }, "Modelo", Modifier.weight(1f), capitalization = KeyboardCapitalization.Words)
            }
            PoloTextField(f.version, { v -> vm.update { it.copy(version = v) } }, "Versión")
            FieldRow {
                PoloTextField(f.plate, { v -> vm.update { it.copy(plate = v.uppercase()) } }, "Matrícula", Modifier.weight(1f), capitalization = KeyboardCapitalization.Characters)
                PoloTextField(f.engineCode, { v -> vm.update { it.copy(engineCode = v.uppercase()) } }, "Código motor", Modifier.weight(1f), capitalization = KeyboardCapitalization.Characters)
            }
            PoloTextField(f.vin, { v -> vm.update { it.copy(vin = v.uppercase().take(17)) } }, "Bastidor (VIN)", capitalization = KeyboardCapitalization.Characters)
        }
        FormSection("Mecánica") {
            ChoiceChips(FuelType.entries, f.fuelType, { it.label }, { v -> vm.update { it.copy(fuelType = v) } })
            ChoiceChips(TimingDrive.entries, f.timingDrive, { "Distribución: ${it.label.lowercase()}" }, { v -> vm.update { it.copy(timingDrive = v) } })
            FieldRow {
                IntField(f.powerCv, { v -> vm.update { it.copy(powerCv = v) } }, "Potencia", Modifier.weight(1f), suffix = "CV")
                DecimalField(f.tank, { v -> vm.update { it.copy(tank = v) } }, "Depósito", Modifier.weight(1f), suffix = "l")
            }
            FieldRow {
                DecimalField(f.tireFront, { v -> vm.update { it.copy(tireFront = v) } }, "Presión del.", Modifier.weight(1f), suffix = "bar")
                DecimalField(f.tireRear, { v -> vm.update { it.copy(tireRear = v) } }, "Presión tras.", Modifier.weight(1f), suffix = "bar")
            }
        }
        FormSection("Historia") {
            DateField(f.registration, { d -> vm.update { it.copy(registration = d) } }, "Primera matriculación", allowClear = true)
            DateField(f.purchaseDate, { d -> vm.update { it.copy(purchaseDate = d) } }, "Fecha de compra", allowClear = true)
            FieldRow {
                IntField(f.purchaseKm, { v -> vm.update { it.copy(purchaseKm = v) } }, "Km al comprar", Modifier.weight(1f), suffix = "km")
                DecimalField(f.purchasePrice, { v -> vm.update { it.copy(purchasePrice = v) } }, "Precio", Modifier.weight(1f), suffix = "€")
            }
            DateField(f.itvManual, { d -> vm.update { it.copy(itvManual = d) } }, "Próxima ITV (manual)", allowClear = true, supportingText = "Solo se usa si no hay inspecciones registradas")
        }
        FormSection("Propietario") {
            PoloTextField(f.owner, { v -> vm.update { it.copy(owner = v) } }, "Nombre (opcional)", supportingText = "Puedes incluirlo o no en el informe PDF")
            PoloTextField(f.notes, { v -> vm.update { it.copy(notes = v) } }, "Notas", singleLine = false, minLines = 2)
        }
    }
}

@Composable
fun OdometerEditorScreen(onBack: () -> Unit, vm: OdometerEditorViewModel = hiltViewModel()) {
    val f = vm.form
    EditorScaffold(
        title = if (vm.isEdit) "Editar lectura" else "Actualizar kilómetros",
        onBack = onBack,
        onSave = { vm.save(onBack) },
        canSave = vm.canSave,
        onDelete = if (vm.isEdit) ({ vm.delete(onBack) }) else null,
    ) {
        PoloCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Rounded.Speed, MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Última lectura registrada", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    MetricText(Fmt.kmPlain(vm.currentKm), "km", style = NumberStyles.large)
                }
            }
        }
        IntField(f.km, { v -> vm.update { it.copy(km = v) } }, "Km del cuentakilómetros", suffix = "km", supportingText = vm.warning, isError = vm.warning != null)
        DateField(f.date, { d -> d?.let { vm.update { s -> s.copy(date = it) } } }, "Fecha")
        PoloTextField(f.note, { v -> vm.update { it.copy(note = v) } }, "Nota (opcional)")
        Text(
            "Cada lectura queda en el historial de kilometraje del informe PDF, lo que da credibilidad al kilometraje del coche.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun OdometerHistoryScreen(onBack: () -> Unit, onNavigate: (Any) -> Unit, vm: OdometerHistoryViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    ListScaffold("Historial de kilometraje", onBack, fab = { PoloFab("Lectura", Icons.Rounded.Speed) { onNavigate(OdometerEditorRoute()) } }) {
        val s = state ?: return@ListScaffold
        val a = s.analysis
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Actual", Fmt.kmPlain(a.currentKm), Modifier.weight(1f), unit = "km", icon = Icons.Rounded.Speed)
                StatTile("Al año", Fmt.kmPlain(a.kmPerYear), Modifier.weight(1f), unit = "km", caption = a.kmPerDay?.let { "≈ ${Fmt.oneDecimal(it)} km/día" })
            }
        }
        item {
            val ok = a.anomalies.isEmpty()
            PoloCard(containerColor = (if (ok) PoloTheme.colors.success else PoloTheme.colors.danger).copy(alpha = 0.1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber, null, tint = if (ok) PoloTheme.colors.success else PoloTheme.colors.danger)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (ok) "${a.eventCount} lecturas coherentes: el kilometraje siempre sube."
                        else "${a.anomalies.size} lectura(s) con menos km que otra anterior. Revísalas.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        if (s.events.size >= 2) {
            item {
                ChartCard("Evolución") {
                    LineChart(
                        s.events.map { it.km.toDouble() }, MaterialTheme.colorScheme.primary,
                        labels = listOf(Fmt.date(s.events.first().date), Fmt.date(s.events.last().date)),
                        valueLabel = { Fmt.km(it.toInt()) },
                    )
                }
            }
        }
        item { SectionHeader("Lecturas", subtitle = "Cualquier registro con km cuenta como lectura") }
        items(s.events.reversed(), key = { "${it.source}-${it.refId}" }) { e ->
            val anomaly = e in a.anomalies
            PoloCard(
                onClick = if (e.source == OdometerSource.MANUAL) ({ onNavigate(OdometerEditorRoute(e.refId)) }) else null,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(Fmt.date(e.date), style = MaterialTheme.typography.titleSmall)
                        Text(e.source.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (anomaly) Icon(Icons.Rounded.WarningAmber, "Incoherente", tint = PoloTheme.colors.danger, modifier = Modifier.padding(end = 8.dp))
                    Text(Fmt.km(e.km), style = NumberStyles.small, color = if (anomaly) PoloTheme.colors.danger else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
fun SpecsScreen(onBack: () -> Unit, onNavigate: (Any) -> Unit, vm: SpecsViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<SpecEntity?>(null) }
    var adding by remember { mutableStateOf(false) }
    ListScaffold(
        "Ficha rápida", onBack,
        fab = { PoloFab("Dato", Icons.Rounded.Add) { adding = true } },
        actions = { state?.first?.let { v -> IconButton(onClick = { onNavigate(VehicleEditorRoute(v.id)) }) { Icon(Icons.Rounded.Edit, "Editar vehículo") } } },
    ) {
        val (v, specs) = state ?: return@ListScaffold
        item {
            PoloCard {
                Text(v.displayName, style = MaterialTheme.typography.titleLarge)
                if (v.version.isNotBlank()) Text(v.version, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                InfoLine("Matrícula", v.plate.ifBlank { "—" })
                InfoLine("Bastidor", v.vin.ifBlank { "—" })
                InfoLine("Combustible", v.fuelType.label)
                v.powerCv?.let { InfoLine("Potencia", "$it CV") }
                InfoLine("Distribución", v.timingDrive.label)
                v.registrationDate?.let { InfoLine("Matriculado", Fmt.date(it)) }
            }
        }
        specs.groupBy { it.section }.forEach { (section, list) ->
            item(key = "s$section") { SectionHeader(section) }
            item(key = "c$section") {
                PoloCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                    list.forEach { spec ->
                        Row(
                            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable { editing = spec }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(spec.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                            Text(
                                spec.value.ifBlank { "Añadir" },
                                style = MaterialTheme.typography.titleSmall,
                                color = if (spec.value.isBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 2, overflow = TextOverflow.Ellipsis,
                                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
    editing?.let { spec ->
        var value by remember(spec.id) { mutableStateOf(spec.value) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(spec.label) },
            text = { PoloTextField(value, { value = it }, spec.label, placeholder = spec.hint.ifBlank { null }, supportingText = spec.hint.ifBlank { null }) },
            confirmButton = { TextButton(onClick = { vm.save(spec.copy(value = value.trim())); editing = null }) { Text("Guardar") } },
            dismissButton = {
                Row {
                    TextButton(onClick = { vm.delete(spec); editing = null }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = { editing = null }) { Text("Cancelar") }
                }
            },
        )
    }
    if (adding) {
        var section by remember { mutableStateOf("Otros") }
        var label by remember { mutableStateOf("") }
        var value by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text("Nuevo dato") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PoloTextField(section, { section = it }, "Sección")
                    PoloTextField(label, { label = it }, "Dato", placeholder = "p. ej. Código de radio")
                    PoloTextField(value, { value = it }, "Valor")
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.add(section.trim().ifBlank { "Otros" }, label.trim(), value.trim()); adding = false }, enabled = label.isNotBlank()) { Text("Añadir") }
            },
            dismissButton = { TextButton(onClick = { adding = false }) { Text("Cancelar") } },
        )
    }
}
