package com.callankan.poloapp.feature.compliance

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
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callankan.poloapp.data.model.InsuranceCoverage
import com.callankan.poloapp.data.model.ItvResult
import com.callankan.poloapp.domain.Deadline
import com.callankan.poloapp.domain.DeadlineState
import com.callankan.poloapp.domain.ItvRules
import com.callankan.poloapp.feature.common.PhotoStrip
import com.callankan.poloapp.feature.common.rememberAttachmentOpener
import com.callankan.poloapp.feature.maintenance.dial
import com.callankan.poloapp.navigation.InsuranceEditorRoute
import com.callankan.poloapp.navigation.ItvEditorRoute
import com.callankan.poloapp.navigation.PhotoViewerRoute
import com.callankan.poloapp.ui.components.ChoiceChips
import com.callankan.poloapp.ui.components.DateField
import com.callankan.poloapp.ui.components.DecimalField
import com.callankan.poloapp.ui.components.EditorScaffold
import com.callankan.poloapp.ui.components.FieldRow
import com.callankan.poloapp.ui.components.FormSection
import com.callankan.poloapp.ui.components.IconBadge
import com.callankan.poloapp.ui.components.InfoLine
import com.callankan.poloapp.ui.components.IntField
import com.callankan.poloapp.ui.components.ListScaffold
import com.callankan.poloapp.ui.components.PoloCard
import com.callankan.poloapp.ui.components.PoloFab
import com.callankan.poloapp.ui.components.PoloTextField
import com.callankan.poloapp.ui.components.ProgressRing
import com.callankan.poloapp.ui.components.SectionHeader
import com.callankan.poloapp.ui.components.StatusPill
import com.callankan.poloapp.ui.components.color
import com.callankan.poloapp.ui.format.Fmt
import com.callankan.poloapp.ui.theme.NumberStyles
import com.callankan.poloapp.ui.theme.PoloTheme
import java.time.LocalDate
import kotlin.math.abs

/** Gran cuenta atrás con anillo: ITV y seguro. */
@Composable
fun CountdownHero(title: String, icon: ImageVector, deadline: Deadline, caption: String?, periodDays: Int = 365) {
    val color = deadline.state.color()
    PoloCard {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(16.dp))
            val days = deadline.daysLeft
            ProgressRing(
                progress = if (days == null) 0f else if (days < 0) 1f else 1f - (days / periodDays.toFloat()).coerceIn(0f, 1f),
                color = color, size = 168.dp, strokeWidth = 14.dp,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(days?.let { Fmt.number(abs(it)) } ?: "—", style = NumberStyles.hero)
                    Text(
                        when {
                            days == null -> "sin fecha"
                            days < 0 -> if (abs(days) == 1L) "día caducada" else "días caducada"
                            else -> if (days == 1L) "día" else "días"
                        },
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            deadline.date?.let { Text(Fmt.longDate(it), style = MaterialTheme.typography.titleMedium) }
            Spacer(Modifier.height(6.dp))
            StatusPill(
                when (deadline.state) {
                    DeadlineState.OK -> "En vigor"
                    DeadlineState.SOON -> "Vence este mes"
                    DeadlineState.URGENT -> "¡Vence en días!"
                    DeadlineState.EXPIRED -> "Caducada"
                    DeadlineState.UNKNOWN -> "Sin registrar"
                },
                color,
            )
            if (caption != null) {
                Spacer(Modifier.height(10.dp))
                Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun ItvResult.color() = when (this) {
    ItvResult.FAVORABLE -> PoloTheme.colors.success
    ItvResult.FAVORABLE_WITH_DEFECTS -> PoloTheme.colors.teal
    ItvResult.UNFAVORABLE -> PoloTheme.colors.warning
    ItvResult.NEGATIVE -> PoloTheme.colors.danger
}

@Composable
fun ItvScreen(onBack: () -> Unit, onNavigate: (Any) -> Unit, vm: ItvViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    ListScaffold("ITV", onBack, fab = { PoloFab("Inspección", Icons.Rounded.Add) { onNavigate(ItvEditorRoute()) } }) {
        val s = state ?: return@ListScaffold
        item {
            CountdownHero(
                "Próxima ITV", Icons.Rounded.TaskAlt, s.deadline,
                ItvRules.describeFrequency(s.vehicle.registrationDate, LocalDate.now()),
            )
        }
        if (s.inspections.isEmpty()) {
            item {
                PoloCard {
                    Text("¿Sabes la fecha de tu próxima ITV?", style = MaterialTheme.typography.titleSmall)
                    Text("Está en la pegatina del parabrisas. Cuando registres una inspección se calculará sola.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    DateField(s.vehicle.manualItvDueDate, { vm.setManualDate(s.vehicle, it) }, "Próxima ITV", allowClear = true)
                }
            }
            return@ListScaffold
        }
        item { SectionHeader("Historial de inspecciones") }
        items(s.inspections, key = { it.id }) { i ->
            PoloCard(onClick = { onNavigate(ItvEditorRoute(i.id)) }, contentPadding = PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Rounded.TaskAlt, i.result.color())
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(Fmt.longDate(i.date), style = MaterialTheme.typography.titleSmall)
                        Text(
                            listOfNotNull(i.station.ifBlank { null }, i.odometerKm?.let { Fmt.km(it) }).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    i.cost?.let { Text(Fmt.money(it), style = NumberStyles.small) }
                }
                Spacer(Modifier.height(8.dp))
                StatusPill(i.result.label, i.result.color())
                if (i.defects.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text("Defectos: ${i.defects}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun ItvEditorScreen(onBack: () -> Unit, onNavigate: (Any) -> Unit, vm: ItvEditorViewModel = hiltViewModel()) {
    val f = vm.form
    val open = rememberAttachmentOpener(vm.photos.storage) { onNavigate(PhotoViewerRoute(it)) }
    EditorScaffold(
        title = if (vm.isEdit) "Editar inspección" else "Nueva inspección ITV",
        onBack = onBack,
        onSave = { vm.save(onBack) },
        canSave = vm.canSave,
        onDelete = if (vm.isEdit) ({ vm.delete(onBack) }) else null,
    ) {
        DateField(f.date, { d -> d?.let { vm.update { s -> s.copy(date = it) } } }, "Fecha de la inspección")
        FormSection("Resultado") {
            ChoiceChips(ItvResult.entries, f.result, { it.label }, { v -> vm.update { it.copy(result = v) } })
        }
        PoloTextField(f.station, { v -> vm.update { it.copy(station = v) } }, "Estación ITV", placeholder = "p. ej. ITV Badalona")
        FieldRow {
            IntField(f.km, { v -> vm.update { it.copy(km = v) } }, "Km", Modifier.weight(1f), suffix = "km", leadingIcon = Icons.Rounded.Speed)
            DecimalField(f.cost, { v -> vm.update { it.copy(cost = v) } }, "Precio", Modifier.weight(1f), suffix = "€")
        }
        if (f.result != ItvResult.FAVORABLE) {
            PoloTextField(f.defects, { v -> vm.update { it.copy(defects = v) } }, "Defectos detectados", singleLine = false, minLines = 2)
        }
        FormSection("Próxima ITV") {
            DateField(
                f.nextDue, { d -> vm.update { it.copy(nextDue = d, nextDueEdited = true) } }, "Fecha límite",
                supportingText = if (f.nextDueEdited) "Fecha introducida a mano" else "Calculada: ${vm.frequencyHint}",
            )
        }
        PhotoStrip(vm.photos, open, title = "Informe o tarjeta ITV", allowPdf = true)
        PoloTextField(f.notes, { v -> vm.update { it.copy(notes = v) } }, "Notas", singleLine = false, minLines = 2)
    }
}

@Composable
fun InsuranceScreen(onBack: () -> Unit, onNavigate: (Any) -> Unit, vm: InsuranceViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val s = state
    ListScaffold(
        "Seguro", onBack,
        fab = { PoloFab(if (s?.policies.isNullOrEmpty()) "Póliza" else "Renovar", if (s?.policies.isNullOrEmpty()) Icons.Rounded.Add else Icons.Rounded.Autorenew) { onNavigate(InsuranceEditorRoute()) } },
    ) {
        if (s == null) return@ListScaffold
        val current = s.policies.maxByOrNull { it.endDate }
        item { CountdownHero("Vencimiento del seguro", Icons.Rounded.Shield, s.deadline, current?.company) }
        if (current == null) {
            item {
                Text(
                    "Registra tu póliza para tener a mano el número, la asistencia en carretera y un aviso antes del vencimiento.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@ListScaffold
        }
        if (current.assistancePhone.isNotBlank()) {
            item {
                Button(
                    onClick = { dial(context, current.assistancePhone) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Icon(Icons.Rounded.Call, null)
                    Spacer(Modifier.width(10.dp))
                    Text("Llamar a asistencia · ${current.assistancePhone}", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
        item {
            PoloCard(onClick = { onNavigate(InsuranceEditorRoute(current.id)) }) {
                Text("Póliza en vigor", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                InfoLine("Compañía", current.company)
                if (current.policyNumber.isNotBlank()) InfoLine("Nº de póliza", current.policyNumber)
                InfoLine("Modalidad", current.coverage.label)
                InfoLine("Vigencia", "${Fmt.date(current.startDate)} – ${Fmt.date(current.endDate)}")
                current.premium?.let { InfoLine("Prima", Fmt.money(it)) }
            }
        }
        val history = s.policies.filter { it.id != current.id }
        if (history.isNotEmpty()) {
            item { SectionHeader("Pólizas anteriores") }
            items(history, key = { it.id }) { p ->
                PoloCard(onClick = { onNavigate(InsuranceEditorRoute(p.id)) }, contentPadding = PaddingValues(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(p.company, style = MaterialTheme.typography.titleSmall)
                            Text("${Fmt.date(p.startDate)} – ${Fmt.date(p.endDate)} · ${p.coverage.label}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        p.premium?.let { Text(Fmt.money(it), style = NumberStyles.small) }
                    }
                }
            }
        }
    }
}

@Composable
fun InsuranceEditorScreen(onBack: () -> Unit, onNavigate: (Any) -> Unit, vm: InsuranceEditorViewModel = hiltViewModel()) {
    val f = vm.form
    val open = rememberAttachmentOpener(vm.photos.storage) { onNavigate(PhotoViewerRoute(it)) }
    EditorScaffold(
        title = when {
            vm.isEdit -> "Editar póliza"
            vm.isRenewal -> "Renovar seguro"
            else -> "Nueva póliza"
        },
        onBack = onBack,
        onSave = { vm.save(onBack) },
        canSave = vm.canSave,
        onDelete = if (vm.isEdit) ({ vm.delete(onBack) }) else null,
    ) {
        PoloTextField(f.company, { v -> vm.update { it.copy(company = v) } }, "Compañía")
        PoloTextField(f.policyNumber, { v -> vm.update { it.copy(policyNumber = v) } }, "Nº de póliza")
        FormSection("Modalidad") {
            ChoiceChips(InsuranceCoverage.entries, f.coverage, { it.label }, { v -> vm.update { it.copy(coverage = v) } })
        }
        FieldRow {
            DateField(f.start, { d -> d?.let { vm.update { s -> s.copy(start = it) } } }, "Inicio", Modifier.weight(1f))
            DateField(f.end, { d -> d?.let { vm.update { s -> s.copy(end = it) } } }, "Vencimiento", Modifier.weight(1f))
        }
        DecimalField(f.premium, { v -> vm.update { it.copy(premium = v) } }, "Prima anual", suffix = "€")
        PoloTextField(f.phone, { v -> vm.update { it.copy(phone = v) } }, "Teléfono de asistencia en carretera", keyboardType = KeyboardType.Phone, leadingIcon = Icons.Rounded.Call)
        PhotoStrip(vm.photos, open, title = "Póliza y recibos", allowPdf = true)
        PoloTextField(f.notes, { v -> vm.update { it.copy(notes = v) } }, "Notas", singleLine = false, minLines = 2)
    }
}
