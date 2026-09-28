package com.callankan.poloapp.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.callankan.poloapp.data.model.FuelType
import com.callankan.poloapp.data.model.TimingDrive
import com.callankan.poloapp.data.preset.Presets
import com.callankan.poloapp.ui.components.ChoiceChips
import com.callankan.poloapp.ui.components.DateField
import com.callankan.poloapp.ui.components.DecimalField
import com.callankan.poloapp.ui.components.FieldRow
import com.callankan.poloapp.ui.components.IconBadge
import com.callankan.poloapp.ui.components.IntField
import com.callankan.poloapp.ui.components.LinearMeter
import com.callankan.poloapp.ui.components.PoloCard
import com.callankan.poloapp.ui.components.PoloTextField
import com.callankan.poloapp.ui.components.SpanishPlate
import com.callankan.poloapp.ui.components.icon
import com.callankan.poloapp.ui.format.Fmt
import com.callankan.poloapp.ui.illustration.CarIllustration
import com.callankan.poloapp.ui.theme.PoloDimens
import com.callankan.poloapp.ui.theme.PoloTheme

private const val LAST_STEP = 4

@Composable
fun OnboardingScreen(
    additional: Boolean,
    onFinished: () -> Unit,
    onCancel: (() -> Unit)?,
    vm: OnboardingViewModel = hiltViewModel(),
) {
    var step by rememberSaveable { mutableIntStateOf(if (additional) 1 else 0) }
    val firstStep = if (additional) 1 else 0
    BackHandler(enabled = step > firstStep) { step-- }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        if (step > 0) {
            Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (step > firstStep) step-- else onCancel?.invoke() }) {
                    Icon(if (step > firstStep) Icons.AutoMirrored.Rounded.ArrowBack else Icons.Rounded.Close, "Atrás")
                }
                LinearMeter(step / LAST_STEP.toFloat(), MaterialTheme.colorScheme.primary, Modifier.weight(1f).padding(end = 16.dp), height = 6.dp)
                Text("$step/$LAST_STEP", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 16.dp))
            }
        }
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally(tween(380)) { if (forward) it / 3 else -it / 3 } + fadeIn(tween(380)))
                    .togetherWith(slideOutHorizontally(tween(300)) { if (forward) -it / 3 else it / 3 } + fadeOut(tween(250)))
            },
            modifier = Modifier.weight(1f),
            label = "onboarding",
        ) { s ->
            when (s) {
                0 -> WelcomeStep()
                1 -> StepScaffold("¿Qué coche tienes?", "Empieza por lo básico. Podrás cambiarlo cuando quieras.") { VehicleStep(vm) }
                2 -> StepScaffold("Hazlo tuyo", "Matrícula, bastidor y color: tu coche tal y como es.") { IdentityStep(vm) }
                3 -> StepScaffold("¿Cómo está ahora?", "Con estos datos calculamos alertas, ITV y kilometraje.") { StatusStep(vm) }
                else -> StepScaffold("Plan de mantenimiento", "Intervalos orientativos que podrás ajustar en cualquier momento.") { PlanStep(vm) }
            }
        }
        Box(Modifier.padding(horizontal = PoloDimens.screenPadding, vertical = 12.dp)) {
            Button(
                onClick = { if (step < LAST_STEP) step++ else vm.finish(onFinished) },
                enabled = vm.canContinue(step) && !vm.saving,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.large,
            ) {
                if (vm.saving) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Text(
                        when (step) {
                            0 -> "Empezar"
                            LAST_STEP -> if (additional) "Añadir vehículo" else "Crear mi garaje"
                            else -> "Continuar"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(if (step == LAST_STEP) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.ArrowForward, null)
                }
            }
        }
    }
}

@Composable
private fun StepScaffold(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = PoloDimens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        Text(title, style = MaterialTheme.typography.headlineLarge)
        Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        content()
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun WelcomeStep() {
    val colors = PoloTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = PoloDimens.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(36.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            listOf(colors.heroGlow.copy(alpha = 0.5f), Color.Transparent),
                            center = Offset(size.width / 2, size.height * 0.6f),
                            radius = size.minDimension * 0.58f,
                        ),
                    )
                }
                .padding(vertical = 24.dp),
        ) {
            CarIllustration(Color(0xFFD0121E), Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(18.dp))
        Text("Polo App", style = MaterialTheme.typography.displayMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "Todo el historial de tu coche en un solo sitio. Sin cuentas, sin nube: tus datos se quedan en tu móvil.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Feature(Icons.Rounded.Build, colors.info, "Mantenimiento con cabeza", "Cada pieza cuenta sus km y te avisa a tiempo.")
            Feature(Icons.Rounded.LocalGasStation, Color(0xFFFF8A3D), "Consumo real", "Método lleno a lleno, precio medio y gasto por km.")
            Feature(Icons.Rounded.Speed, colors.success, "ITV, seguro y deuda", "Cuentas atrás y avisos antes de que se te pase.")
            Feature(Icons.Rounded.PictureAsPdf, MaterialTheme.colorScheme.primary, "Informe para vender", "Un PDF profesional que transmite confianza al comprador.")
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Feature(icon: ImageVector, tint: Color, title: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon, tint, size = 44.dp)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun VehicleStep(vm: OnboardingViewModel) {
    val f = vm.form
    Presets.vehiclePresets.forEach { preset ->
        val selected = vm.selectedPreset == preset
        val border by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else PoloTheme.colors.cardBorder, label = "preset")
        Row(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .border(if (selected) 2.dp else 1.dp, border, MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable { vm.applyPreset(preset) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(Icons.Rounded.Star, MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(preset.title, style = MaterialTheme.typography.titleSmall)
                Text(preset.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Rellena los datos y el plan de mantenimiento", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            if (selected) Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
    FieldRow {
        PoloTextField(f.make, { v -> vm.update { it.copy(make = v) } }, "Marca", Modifier.weight(1f), capitalization = KeyboardCapitalization.Words)
        PoloTextField(f.model, { v -> vm.update { it.copy(model = v) } }, "Modelo", Modifier.weight(1f), capitalization = KeyboardCapitalization.Words)
    }
    PoloTextField(f.version, { v -> vm.update { it.copy(version = v) } }, "Versión / motor", placeholder = "p. ej. 1.2 TSI 90 CV")
    Text("Combustible", style = MaterialTheme.typography.titleSmall)
    ChoiceChips(FuelType.entries, f.fuelType, { it.label }, { v -> vm.update { it.copy(fuelType = v) } })
    Text("Distribución", style = MaterialTheme.typography.titleSmall)
    ChoiceChips(TimingDrive.entries, f.timingDrive, { it.label }, { v -> vm.update { it.copy(timingDrive = v) } })
    FieldRow {
        IntField(f.powerCv, { v -> vm.update { it.copy(powerCv = v) } }, "Potencia", Modifier.weight(1f), suffix = "CV")
        DecimalField(f.tank, { v -> vm.update { it.copy(tank = v) } }, "Depósito", Modifier.weight(1f), suffix = "l")
    }
}

@Composable
private fun IdentityStep(vm: OnboardingViewModel) {
    val f = vm.form
    PoloCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SpanishPlate(f.plate)
            Spacer(Modifier.weight(1f))
            Text(f.colorName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        CarIllustration(Color(f.colorArgb.toInt()), Modifier.fillMaxWidth().padding(top = 8.dp))
    }
    Text("Color", style = MaterialTheme.typography.titleSmall)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Presets.carColors.forEach { c ->
            val selected = c.argb == f.colorArgb
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(c.argb.toInt()))
                    .border(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable { vm.update { it.copy(colorName = c.name, colorArgb = c.argb) } },
                contentAlignment = Alignment.Center,
            ) {
                if (selected) Icon(Icons.Rounded.Check, null, tint = if (c.argb == 0xFFF1F1EE) Color.Black else Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
    PoloTextField(f.alias, { v -> vm.update { it.copy(alias = v) } }, "Nombre en la app", placeholder = "Mi ${f.model.ifBlank { "coche" }}")
    PoloTextField(
        f.plate, { v -> vm.update { it.copy(plate = v.uppercase()) } }, "Matrícula",
        placeholder = "0000 ABC", capitalization = KeyboardCapitalization.Characters,
    )
    PoloTextField(
        f.vin, { v -> vm.update { it.copy(vin = v.uppercase().take(17)) } }, "Número de bastidor (VIN)",
        supportingText = "17 caracteres. Está en la ficha técnica y en el parabrisas.",
        capitalization = KeyboardCapitalization.Characters,
    )
}

@Composable
private fun StatusStep(vm: OnboardingViewModel) {
    val f = vm.form
    DateField(f.registrationDate, { d -> vm.update { it.copy(registrationDate = d) } }, "Primera matriculación", supportingText = vm.itvHint)
    DateField(f.purchaseDate, { d -> vm.update { it.copy(purchaseDate = d) } }, "Fecha de compra", allowClear = true)
    FieldRow {
        IntField(f.purchaseKm, { v -> vm.update { it.copy(purchaseKm = v) } }, "Km al comprarlo", Modifier.weight(1f), suffix = "km")
        IntField(f.currentKm, { v -> vm.update { it.copy(currentKm = v) } }, "Km actuales", Modifier.weight(1f), suffix = "km")
    }
    DateField(
        f.itvDueDate, { d -> vm.update { it.copy(itvDueDate = d) } }, "Próxima ITV", allowClear = true,
        supportingText = "Mira la pegatina del parabrisas o la tarjeta ITV.",
    )
    Text("Presión recomendada (opcional)", style = MaterialTheme.typography.titleSmall)
    FieldRow {
        DecimalField(f.tireFront, { v -> vm.update { it.copy(tireFront = v) } }, "Delantera", Modifier.weight(1f), suffix = "bar")
        DecimalField(f.tireRear, { v -> vm.update { it.copy(tireRear = v) } }, "Trasera", Modifier.weight(1f), suffix = "bar")
    }
    Text(
        "La encontrarás en la etiqueta del interior de la tapa del depósito.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PlanStep(vm: OnboardingViewModel) {
    vm.plan.forEachIndexed { i, (item, enabled) ->
        Row(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable { vm.togglePlanItem(i) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(item.category.icon, if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, size = 36.dp, iconSize = 18.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    listOfNotNull(
                        item.intervalKm?.let { "cada ${Fmt.km(it)}" },
                        item.intervalMonths?.let { if (it % 12 == 0) "${it / 12} año${if (it > 12) "s" else ""}" else "$it meses" },
                    ).joinToString(" o "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Checkbox(checked = enabled, onCheckedChange = { vm.togglePlanItem(i) })
        }
    }
}
