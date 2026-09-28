package com.callankan.poloapp.feature.garage

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.callankan.poloapp.data.db.entity.OdometerEntryEntity
import com.callankan.poloapp.data.db.entity.OdometerEvent
import com.callankan.poloapp.data.db.entity.SpecEntity
import com.callankan.poloapp.data.db.entity.VehicleEntity
import com.callankan.poloapp.data.model.FuelType
import com.callankan.poloapp.data.model.TimingDrive
import com.callankan.poloapp.data.repository.ActiveVehicle
import com.callankan.poloapp.data.repository.OverviewRepository
import com.callankan.poloapp.data.repository.VehicleRepository
import com.callankan.poloapp.data.settings.SettingsRepository
import com.callankan.poloapp.domain.OdometerAnalysis
import com.callankan.poloapp.domain.OdometerAnalyzer
import com.callankan.poloapp.navigation.OdometerEditorRoute
import com.callankan.poloapp.navigation.VehicleEditorRoute
import com.callankan.poloapp.ui.format.Fmt
import com.callankan.poloapp.ui.format.NumberInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class VehiclesViewModel @Inject constructor(
    private val repo: VehicleRepository,
    settings: SettingsRepository,
) : ViewModel() {
    val state: StateFlow<Pair<List<VehicleEntity>, Long?>?> = combine(repo.observeAll(), settings.selectedVehicleId) { v, s -> v to s }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun select(id: Long) = viewModelScope.launch { repo.select(id) }
}

data class VehicleForm(
    val alias: String = "",
    val make: String = "",
    val model: String = "",
    val version: String = "",
    val powerCv: String = "",
    val engineCode: String = "",
    val fuelType: FuelType = FuelType.GASOLINE,
    val timingDrive: TimingDrive = TimingDrive.UNKNOWN,
    val tank: String = "",
    val registration: LocalDate? = null,
    val purchaseDate: LocalDate? = null,
    val purchaseKm: String = "",
    val purchasePrice: String = "",
    val plate: String = "",
    val vin: String = "",
    val colorName: String = "",
    val colorArgb: Long = 0xFFD0121E,
    val tireFront: String = "",
    val tireRear: String = "",
    val itvManual: LocalDate? = null,
    val owner: String = "",
    val notes: String = "",
)

@HiltViewModel
class VehicleEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: VehicleRepository,
) : ViewModel() {
    private val id = savedStateHandle.toRoute<VehicleEditorRoute>().id
    var form by mutableStateOf(VehicleForm())
        private set
    private var original: VehicleEntity? = null

    init {
        viewModelScope.launch {
            repo.get(id)?.let { v ->
                original = v
                form = VehicleForm(
                    v.alias, v.make, v.model, v.version, Fmt.input(v.powerCv), v.engineCode, v.fuelType, v.timingDrive,
                    Fmt.input(v.tankCapacityLiters), v.registrationDate, v.purchaseDate, Fmt.input(v.purchaseKm),
                    Fmt.input(v.purchasePrice), v.plate, v.vin, v.colorName, v.colorArgb, Fmt.input(v.tireFrontBar),
                    Fmt.input(v.tireRearBar), v.manualItvDueDate, v.ownerName, v.notes,
                )
            }
        }
    }

    fun update(transform: (VehicleForm) -> VehicleForm) {
        form = transform(form)
    }

    val canSave get() = form.make.isNotBlank() && form.model.isNotBlank() && form.alias.isNotBlank()

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        val v = original ?: return@launch
        val f = form
        repo.update(
            v.copy(
                alias = f.alias.trim(), make = f.make.trim(), model = f.model.trim(), version = f.version.trim(),
                powerCv = NumberInput.parseInt(f.powerCv), engineCode = f.engineCode.trim().uppercase(), fuelType = f.fuelType,
                timingDrive = f.timingDrive, tankCapacityLiters = NumberInput.parseDouble(f.tank), registrationDate = f.registration,
                purchaseDate = f.purchaseDate, purchaseKm = NumberInput.parseInt(f.purchaseKm), purchasePrice = NumberInput.parseDouble(f.purchasePrice),
                plate = f.plate.trim().uppercase(), vin = f.vin.trim().uppercase(), colorName = f.colorName, colorArgb = f.colorArgb,
                tireFrontBar = NumberInput.parseDouble(f.tireFront), tireRearBar = NumberInput.parseDouble(f.tireRear),
                manualItvDueDate = f.itvManual, ownerName = f.owner.trim(), notes = f.notes.trim(),
            ),
        )
        onDone()
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        original?.let { repo.delete(it) }
        onDone()
    }
}

data class OdometerForm(val date: LocalDate = LocalDate.now(), val km: String = "", val note: String = "")

@HiltViewModel
class OdometerEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val active: ActiveVehicle,
    private val repo: VehicleRepository,
    overview: OverviewRepository,
) : ViewModel() {
    private val id = savedStateHandle.toRoute<OdometerEditorRoute>().id
    val isEdit = id != 0L
    var form by mutableStateOf(OdometerForm())
        private set
    var currentKm by mutableStateOf<Int?>(null)
        private set
    private var original: OdometerEntryEntity? = null

    init {
        viewModelScope.launch {
            currentKm = overview.snapshot(active.currentId())?.currentKm
            if (isEdit) repo.getOdometerEntry(id)?.let { e ->
                original = e
                form = OdometerForm(e.date, e.km.toString(), e.note)
            }
        }
    }

    fun update(transform: (OdometerForm) -> OdometerForm) {
        form = transform(form)
    }

    val warning: String?
        get() {
            val km = NumberInput.parseInt(form.km) ?: return null
            val current = currentKm ?: return null
            return if (!isEdit && km < current) "Menos que la lectura más alta (${Fmt.km(current)})" else null
        }

    val canSave get() = NumberInput.parseInt(form.km) != null

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        repo.saveOdometerEntry(
            OdometerEntryEntity(id, original?.vehicleId ?: active.currentId(), form.date, NumberInput.parseInt(form.km)!!, form.note.trim()),
        )
        onDone()
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        original?.let { repo.deleteOdometerEntry(it) }
        onDone()
    }
}

data class OdometerHistoryState(val events: List<OdometerEvent>, val analysis: OdometerAnalysis)

@HiltViewModel
class OdometerHistoryViewModel @Inject constructor(active: ActiveVehicle, repo: VehicleRepository) : ViewModel() {
    val state: StateFlow<OdometerHistoryState?> = active.vehicle.flatMapLatest { v ->
        repo.observeOdometerEvents(v.id).map { OdometerHistoryState(it, OdometerAnalyzer.analyze(it, v.purchaseKm)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@HiltViewModel
class SpecsViewModel @Inject constructor(
    private val active: ActiveVehicle,
    private val repo: VehicleRepository,
) : ViewModel() {
    val state: StateFlow<Pair<VehicleEntity, List<SpecEntity>>?> = active.vehicle.flatMapLatest { v ->
        repo.observeSpecs(v.id).map { v to it }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun save(spec: SpecEntity) = viewModelScope.launch { repo.saveSpec(spec) }
    fun delete(spec: SpecEntity) = viewModelScope.launch { repo.deleteSpec(spec) }
    fun add(section: String, label: String, value: String) = viewModelScope.launch {
        repo.saveSpec(SpecEntity(vehicleId = active.currentId(), section = section, label = label, value = value, sortOrder = 999))
    }
}
