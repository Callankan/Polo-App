package com.callankan.poloapp.feature.fuel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.callankan.poloapp.ui.components.ChartColors
import com.callankan.poloapp.ui.components.ChoiceChips
import com.callankan.poloapp.ui.components.DateField
import com.callankan.poloapp.ui.components.DecimalField
import com.callankan.poloapp.ui.components.EditorScaffold
import com.callankan.poloapp.ui.components.FieldRow
import com.callankan.poloapp.ui.components.FormSection
import com.callankan.poloapp.ui.components.IconBadge
import com.callankan.poloapp.ui.components.IntField
import com.callankan.poloapp.ui.components.MetricText
import com.callankan.poloapp.ui.components.PoloCard
import com.callankan.poloapp.ui.components.PoloTextField
import com.callankan.poloapp.ui.components.SwitchRow
import com.callankan.poloapp.ui.format.Fmt
import com.callankan.poloapp.ui.theme.NumberStyles

@Composable
fun RefuelEditorScreen(onBack: () -> Unit, vm: RefuelEditorViewModel = hiltViewModel()) {
    val f = vm.form
    val (liters, price, total) = vm.computed
    EditorScaffold(
        title = if (vm.isEdit) "Editar repostaje" else "Nuevo repostaje",
        onBack = onBack,
        onSave = { vm.save(onBack) },
        canSave = vm.canSave,
        onDelete = if (vm.isEdit) ({ vm.delete(onBack) }) else null,
    ) {
        PoloCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Rounded.LocalGasStation, ChartColors.fuel)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    MetricText(Fmt.moneyPlain(total), "€", style = NumberStyles.large)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(Fmt.liters(liters), style = NumberStyles.small)
                    Text(Fmt.pricePerLiter(price), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        DateField(f.date, { d -> d?.let { vm.update { s -> s.copy(date = it) } } }, "Fecha")
        IntField(
            f.km, { v -> vm.update { it.copy(km = v) } }, "Cuentakilómetros", suffix = "km",
            leadingIcon = Icons.Rounded.Speed,
            supportingText = vm.kmWarning ?: vm.lastKm?.let { "Última lectura: ${Fmt.km(it)}" },
            isError = vm.kmWarning != null,
        )
        FormSection("Importe · rellena dos y calculo el tercero") {
            FieldRow {
                DecimalField(
                    f.liters, { v -> vm.update { it.copy(liters = v) } }, "Litros", Modifier.weight(1f), suffix = "l",
                    placeholder = if (f.liters.isBlank()) liters?.let { Fmt.twoDecimals(it) } else null,
                )
                DecimalField(
                    f.price, { v -> vm.update { it.copy(price = v) } }, "Precio", Modifier.weight(1f), suffix = "€/l",
                    placeholder = if (f.price.isBlank()) price?.let { Fmt.threeDecimals(it) } else null,
                )
            }
            DecimalField(
                f.total, { v -> vm.update { it.copy(total = v) } }, "Total pagado", suffix = "€",
                placeholder = if (f.total.isBlank()) total?.let { Fmt.twoDecimals(it) } else null,
            )
        }
        FormSection("Depósito") {
            SwitchRow(
                "Depósito lleno", f.fullTank, { v -> vm.update { it.copy(fullTank = v) } },
                subtitle = "Necesario para calcular el consumo real",
            )
            SwitchRow(
                "Olvidé anotar el anterior", f.missedPrevious, { v -> vm.update { it.copy(missedPrevious = v) } },
                subtitle = "Evita un consumo falso en este tramo", icon = Icons.Rounded.History,
            )
        }
        FormSection("Combustible y gasolinera") {
            ChoiceChips(vm.grades, f.grade, { it }, { v -> vm.update { it.copy(grade = v) } })
            PoloTextField(f.station, { v -> vm.update { it.copy(station = v) } }, "Gasolinera", placeholder = "p. ej. Repsol Badalona")
            if (vm.stations.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(vm.stations) { st ->
                        AssistChip(onClick = { vm.update { it.copy(station = st) } }, label = { Text(st) })
                    }
                }
            }
        }
        PoloTextField(f.notes, { v -> vm.update { it.copy(notes = v) } }, "Notas", singleLine = false, minLines = 2)
    }
}
