package com.callankan.poloapp.feature.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.callankan.poloapp.data.model.ExpenseCategory
import com.callankan.poloapp.feature.common.PhotoStrip
import com.callankan.poloapp.feature.common.rememberAttachmentOpener
import com.callankan.poloapp.navigation.PhotoViewerRoute
import com.callankan.poloapp.ui.components.ChartCard
import com.callankan.poloapp.ui.components.ChartColors
import com.callankan.poloapp.ui.components.ChoiceChips
import com.callankan.poloapp.ui.components.DateField
import com.callankan.poloapp.ui.components.DecimalField
import com.callankan.poloapp.ui.components.DropdownField
import com.callankan.poloapp.ui.components.EditorScaffold
import com.callankan.poloapp.ui.components.IconBadge
import com.callankan.poloapp.ui.components.InfoLine
import com.callankan.poloapp.ui.components.IntField
import com.callankan.poloapp.ui.components.LineChart
import com.callankan.poloapp.ui.components.ListScaffold
import com.callankan.poloapp.ui.components.MetricText
import com.callankan.poloapp.ui.components.PoloCard
import com.callankan.poloapp.ui.components.PoloTextField
import com.callankan.poloapp.ui.components.SectionHeader
import com.callankan.poloapp.ui.components.SegmentedTabs
import com.callankan.poloapp.ui.components.icon
import com.callankan.poloapp.ui.format.Fmt
import com.callankan.poloapp.ui.format.NumberInput
import com.callankan.poloapp.ui.theme.NumberStyles
import com.callankan.poloapp.ui.theme.PoloTheme

@Composable
fun LoanEditorScreen(onBack: () -> Unit, vm: LoanEditorViewModel = hiltViewModel()) {
    val f = vm.form
    EditorScaffold(
        title = if (vm.isEdit) "Editar deuda" else "Nueva deuda",
        onBack = onBack,
        onSave = { vm.save(onBack) },
        canSave = vm.canSave,
        onDelete = if (vm.isEdit) ({ vm.delete(onBack) }) else null,
        deleteMessage = "Se borrará la deuda y todo su historial de pagos.",
    ) {
        Text(
            "Préstamo sin intereses: solo cuenta lo que debes y lo que vas pagando.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PoloTextField(f.lender, { v -> vm.update { it.copy(lender = v) } }, "¿A quién se lo debes?", placeholder = "p. ej. Mamá")
        DecimalField(f.amount, { v -> vm.update { it.copy(amount = v) } }, "Cantidad inicial", suffix = "€")
        DateField(f.startDate, { d -> d?.let { vm.update { s -> s.copy(startDate = it) } } }, "Fecha del préstamo")
        PoloTextField(f.notes, { v -> vm.update { it.copy(notes = v) } }, "Notas / acuerdo", singleLine = false, minLines = 3)
    }
}

@Composable
fun PaymentEditorScreen(onBack: () -> Unit, vm: PaymentEditorViewModel = hiltViewModel()) {
    val f = vm.form
    val amount = NumberInput.parseDouble(f.amount) ?: 0.0
    val after = vm.remainingBefore?.let { (it - amount).coerceAtLeast(0.0) }
    EditorScaffold(
        title = if (vm.isEdit) "Editar pago" else "Nuevo pago",
        onBack = onBack,
        onSave = { vm.save(onBack) },
        canSave = vm.canSave,
        onDelete = if (vm.isEdit) ({ vm.delete(onBack) }) else null,
    ) {
        PoloCard {
            Text("Pago a ${vm.lender}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            MetricText(Fmt.moneyPlain(amount), "€", style = NumberStyles.large)
            Spacer(Modifier.height(8.dp))
            InfoLine("Pendiente antes", Fmt.money(vm.remainingBefore))
            InfoLine("Pendiente después", Fmt.money(after), valueColor = PoloTheme.colors.success)
            if (after != null && after <= 0.005 && amount > 0) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.EmojiEvents, null, tint = PoloTheme.colors.warning)
                    Spacer(Modifier.width(8.dp))
                    Text("¡Con este pago saldas la deuda!", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
        DecimalField(f.amount, { v -> vm.update { it.copy(amount = v) } }, "Cantidad", suffix = "€")
        vm.remainingBefore?.takeIf { it > 0 }?.let { rem ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(50.0, 100.0, 200.0, 500.0).filter { it < rem }.forEach { v ->
                    InputChip(selected = false, onClick = { vm.update { it.copy(amount = Fmt.input(v)) } }, label = { Text(Fmt.moneyRound(v)) })
                }
                InputChip(selected = false, onClick = { vm.update { it.copy(amount = Fmt.input(rem)) } }, label = { Text("Todo") })
            }
        }
        DateField(f.date, { d -> d?.let { vm.update { s -> s.copy(date = it) } } }, "Fecha")
        PoloTextField(f.note, { v -> vm.update { it.copy(note = v) } }, "Nota", placeholder = "p. ej. Transferencia")
    }
}

@Composable
fun DebtSimulatorScreen(onBack: () -> Unit, vm: DebtSimulatorViewModel = hiltViewModel()) {
    var mode by rememberSaveable { mutableIntStateOf(0) }
    var extraDialog by remember { mutableStateOf(false) }
    val summary = vm.summary
    val violet = ChartColors.other
    ListScaffold(title = "Simulador de pagos", onBack = onBack) {
        if (summary == null) return@ListScaffold
        item {
            PoloCard {
                Text("Pendiente hoy", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MetricText(Fmt.moneyPlain(summary.remaining), "€", style = NumberStyles.large)
                summary.averageMonthly?.let {
                    Text("Tu media real: ${Fmt.money(it)} al mes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { SegmentedTabs(listOf("Por cuota", "Por fecha final"), mode, { mode = it }) }
        if (mode == 0) {
            item {
                PoloCard {
                    DecimalField(vm.monthly, vm::onMonthlyChange, "Cuota mensual", suffix = "€")
                    val value = (NumberInput.parseDouble(vm.monthly) ?: 0.0).toFloat()
                    val max = summary.remaining.toFloat().coerceAtLeast(50f).coerceAtMost(3000f)
                    Slider(
                        value = value.coerceIn(10f, max),
                        onValueChange = { vm.onMonthlyChange(Fmt.input((Math.round(it / 10f) * 10).toDouble())) },
                        valueRange = 10f..max,
                        colors = SliderDefaults.colors(thumbColor = violet, activeTrackColor = violet, inactiveTrackColor = violet.copy(alpha = 0.18f)),
                    )
                }
            }
            val sim = vm.simulation
            item {
                PoloCard(containerColor = violet.copy(alpha = 0.12f)) {
                    if (sim == null) {
                        Text("Introduce una cuota mayor que cero.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text("Terminarás de pagar en", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(Fmt.monthYear(sim.payoffMonth), style = MaterialTheme.typography.headlineMedium, color = violet)
                        Text("${sim.months} pagos · ${Fmt.duration(sim.months)}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        InfoLine("Último pago", Fmt.money(sim.lastPayment))
                        InfoLine("Total", Fmt.money(sim.totalPaid))
                    }
                }
            }
        } else {
            item {
                PoloCard {
                    val months = (1..72).map { vm.startMonth.plusMonths(it.toLong() - 1) }
                    DropdownField(vm.targetMonth, months, "Quiero terminar en", { Fmt.monthYear(it) }, vm::setTarget)
                    Spacer(Modifier.height(14.dp))
                    Text("Necesitas pagar", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    MetricText(Fmt.moneyPlain(vm.monthlyForTarget), "€ / mes", style = NumberStyles.large, color = violet)
                    Text(
                        "Durante ${Fmt.duration(java.time.temporal.ChronoUnit.MONTHS.between(vm.startMonth, vm.targetMonth).toInt() + 1)}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = {
                        vm.monthlyForTarget?.let { vm.onMonthlyChange(Fmt.input(it)) }
                        mode = 0
                    }) { Text("Ver calendario con esta cuota") }
                }
            }
        }
        item {
            SectionHeader("Pagos extra", subtitle = "Pagas extra, devoluciones de Hacienda...", actionLabel = "Añadir", onAction = { extraDialog = true })
        }
        if (vm.extras.isNotEmpty()) {
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    vm.extras.toSortedMap().forEach { (m, amount) ->
                        InputChip(
                            selected = true,
                            onClick = { vm.removeExtra(m) },
                            label = { Text("${Fmt.shortMonthYear(m)} · ${Fmt.moneyRound(amount)}") },
                            trailingIcon = { Icon(Icons.Rounded.Close, "Quitar") },
                        )
                    }
                }
            }
        }
        val sim = vm.simulation
        if (mode == 0 && sim != null && sim.rows.size > 1) {
            item {
                ChartCard("Cómo baja la deuda") {
                    LineChart(
                        listOf(summary.remaining) + sim.rows.map { it.remainingAfter },
                        violet,
                        labels = listOf(Fmt.shortMonthYear(vm.startMonth), Fmt.shortMonthYear(sim.payoffMonth)),
                        valueLabel = { Fmt.moneyRound(it) },
                    )
                }
            }
            item { SectionHeader("Calendario", subtitle = "Desglose mes a mes") }
            item {
                PoloCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(Modifier.padding(vertical = 6.dp)) {
                        Text("Mes", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.3f))
                        Text("Pago", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Text("Queda", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    }
                    sim.rows.take(120).forEach { row ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(Modifier.padding(vertical = 8.dp)) {
                            Text(Fmt.monthYear(row.month), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1.3f))
                            Text(
                                Fmt.money(row.payment + row.extra),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (row.extra > 0) FontWeight.Bold else FontWeight.Medium),
                                color = if (row.extra > 0) violet else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            Text(Fmt.money(row.remainingAfter), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
    if (extraDialog) {
        var amount by remember { mutableStateOf("") }
        var month by remember { mutableStateOf(vm.startMonth.plusMonths(1)) }
        AlertDialog(
            onDismissRequest = { extraDialog = false },
            title = { Text("Pago extra") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DropdownField(month, (0..48).map { vm.startMonth.plusMonths(it.toLong()) }, "Mes", { Fmt.monthYear(it) }, { month = it })
                    DecimalField(amount, { amount = it }, "Cantidad", suffix = "€")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    NumberInput.parseDouble(amount)?.takeIf { it > 0 }?.let { vm.addExtra(month, it) }
                    extraDialog = false
                }) { Text("Añadir") }
            },
            dismissButton = { TextButton(onClick = { extraDialog = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
fun ExpenseEditorScreen(onBack: () -> Unit, onNavigate: (Any) -> Unit, vm: ExpenseEditorViewModel = hiltViewModel()) {
    val f = vm.form
    val open = rememberAttachmentOpener(vm.photos.storage) { onNavigate(PhotoViewerRoute(it)) }
    EditorScaffold(
        title = if (vm.isEdit) "Editar gasto" else "Nuevo gasto",
        onBack = onBack,
        onSave = { vm.save(onBack) },
        canSave = vm.canSave,
        onDelete = if (vm.isEdit) ({ vm.delete(onBack) }) else null,
    ) {
        PoloCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(f.category.icon, ChartColors.palette[(f.category.ordinal + 4) % ChartColors.palette.size])
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(f.category.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    MetricText(Fmt.moneyPlain(NumberInput.parseDouble(f.amount) ?: 0.0), "€", style = NumberStyles.large)
                }
            }
        }
        ChoiceChips(ExpenseCategory.entries, f.category, { it.label }, { v -> vm.update { it.copy(category = v) } }, optionIcon = { it.icon })
        DecimalField(f.amount, { v -> vm.update { it.copy(amount = v) } }, "Importe", suffix = "€")
        PoloTextField(f.description, { v -> vm.update { it.copy(description = v) } }, "Concepto", placeholder = "p. ej. Parking aeropuerto")
        DateField(f.date, { d -> d?.let { vm.update { s -> s.copy(date = it) } } }, "Fecha")
        IntField(f.km, { v -> vm.update { it.copy(km = v) } }, "Km (opcional)", suffix = "km", leadingIcon = Icons.Rounded.Speed)
        PhotoStrip(vm.photos, open, title = "Ticket o justificante", allowPdf = true)
        PoloTextField(f.notes, { v -> vm.update { it.copy(notes = v) } }, "Notas", singleLine = false, minLines = 2)
        Spacer(Modifier.fillMaxWidth())
    }
}
