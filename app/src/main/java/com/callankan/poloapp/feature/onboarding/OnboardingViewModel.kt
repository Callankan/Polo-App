package com.callankan.poloapp.feature.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callankan.poloapp.data.db.entity.VehicleEntity
import com.callankan.poloapp.data.model.FuelType
import com.callankan.poloapp.data.model.TimingDrive
import com.callankan.poloapp.data.preset.ComponentPreset
import com.callankan.poloapp.data.preset.Presets
import com.callankan.poloapp.data.preset.VehiclePreset
import com.callankan.poloapp.data.repository.VehicleRepository
import com.callankan.poloapp.domain.ItvRules
import com.callankan.poloapp.ui.format.NumberInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class OnboardingForm(
    val make: String = "",
    val model: String = "",
    val version: String = "",
    val fuelType: FuelType = FuelType.GASOLINE,
    val timingDrive: TimingDrive = TimingDrive.UNKNOWN,
    val powerCv: String = "",
    val tank: String = "",
    val alias: String = "",
    val plate: String = "",
    val vin: String = "",
    val colorName: String = "Rojo",
    val colorArgb: Long = 0xFFD0121E,
    val registrationDate: LocalDate? = null,
    val purchaseDate: LocalDate? = null,
    val purchaseKm: String = "",
    val currentKm: String = "",
    val itvDueDate: LocalDate? = null,
    val tireFront: String = "",
    val tireRear: String = "",
    val presetHints: Map<String, String> = emptyMap(),
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val vehicles: VehicleRepository,
) : ViewModel() {
    var form by mutableStateOf(OnboardingForm())
        private set
    var plan by mutableStateOf<List<Pair<ComponentPreset, Boolean>>>(emptyList())
        private set
    var saving by mutableStateOf(false)
        private set
    var selectedPreset by mutableStateOf<VehiclePreset?>(null)
        private set

    init {
        rebuildPlan()
    }

    fun update(transform: (OnboardingForm) -> OnboardingForm) {
        val before = form
        form = transform(form)
        if (before.fuelType != form.fuelType || before.timingDrive != form.timingDrive) rebuildPlan()
    }

    fun applyPreset(preset: VehiclePreset) {
        selectedPreset = preset
        form = form.copy(
            make = preset.make,
            model = preset.model,
            version = preset.version,
            fuelType = preset.fuelType,
            timingDrive = preset.timingDrive,
            powerCv = preset.powerCv?.toString() ?: "",
            tank = preset.tankLiters?.let { it.toInt().toString() } ?: "",
            alias = form.alias.ifBlank { "Mi ${preset.model}" },
            presetHints = preset.extraHints,
        )
        rebuildPlan()
    }

    fun togglePlanItem(index: Int) {
        plan = plan.mapIndexed { i, p -> if (i == index) p.first to !p.second else p }
    }

    private fun rebuildPlan() {
        plan = Presets.maintenancePlan(form.fuelType, form.timingDrive, form.presetHints).map { it to true }
    }

    /** ITV sugerida a partir de la matriculación cuando el usuario no la conoce. */
    val itvHint: String?
        get() = form.registrationDate?.let { ItvRules.describeFrequency(it, LocalDate.now()) }

    fun canContinue(step: Int): Boolean = when (step) {
        1 -> form.make.isNotBlank() && form.model.isNotBlank()
        3 -> NumberInput.parseInt(form.currentKm) != null
        else -> true
    }

    fun finish(onDone: () -> Unit) {
        if (saving) return
        saving = true
        viewModelScope.launch {
            val f = form
            val current = NumberInput.parseInt(f.currentKm)
            val vehicle = VehicleEntity(
                alias = f.alias.ifBlank { "Mi ${f.model}" }.trim(),
                make = f.make.trim(),
                model = f.model.trim(),
                version = f.version.trim(),
                powerCv = NumberInput.parseInt(f.powerCv),
                fuelType = f.fuelType,
                timingDrive = f.timingDrive,
                tankCapacityLiters = NumberInput.parseDouble(f.tank),
                registrationDate = f.registrationDate,
                purchaseDate = f.purchaseDate,
                purchaseKm = NumberInput.parseInt(f.purchaseKm),
                plate = f.plate.trim().uppercase(),
                vin = f.vin.trim().uppercase(),
                colorName = f.colorName,
                colorArgb = f.colorArgb,
                tireFrontBar = NumberInput.parseDouble(f.tireFront),
                tireRearBar = NumberInput.parseDouble(f.tireRear),
                manualItvDueDate = f.itvDueDate,
            )
            vehicles.create(vehicle, current, plan.filter { it.second }.map { it.first })
            saving = false
            onDone()
        }
    }
}
