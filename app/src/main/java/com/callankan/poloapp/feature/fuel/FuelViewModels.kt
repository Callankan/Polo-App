package com.callankan.poloapp.feature.fuel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.callankan.poloapp.data.db.entity.RefuelEntity
import com.callankan.poloapp.data.model.FuelType
import com.callankan.poloapp.data.repository.ActiveVehicle
import com.callankan.poloapp.data.repository.FuelRepository
import com.callankan.poloapp.data.repository.VehicleRepository
import com.callankan.poloapp.domain.FuelCalculator
import com.callankan.poloapp.domain.FuelStats
import com.callankan.poloapp.domain.OdometerAnalyzer
import com.callankan.poloapp.navigation.RefuelEditorRoute
import com.callankan.poloapp.ui.format.Fmt
import com.callankan.poloapp.ui.format.NumberInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class FuelUiState(val refuels: List<RefuelEntity>, val stats: FuelStats)

@HiltViewModel
class FuelViewModel @Inject constructor(
    active: ActiveVehicle,
    repo: FuelRepository,
) : ViewModel() {
    val state: StateFlow<FuelUiState?> = active.id
        .flatMapLatest { repo.observe(it) }
        .map { FuelUiState(it, FuelCalculator.compute(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

data class RefuelForm(
    val date: LocalDate = LocalDate.now(),
    val km: String = "",
    val liters: String = "",
    val price: String = "",
    val total: String = "",
    val fullTank: Boolean = true,
    val missedPrevious: Boolean = false,
    val grade: String = "",
    val station: String = "",
    val notes: String = "",
)

@HiltViewModel
class RefuelEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val active: ActiveVehicle,
    private val repo: FuelRepository,
    private val vehicles: VehicleRepository,
) : ViewModel() {
    private val id = savedStateHandle.toRoute<RefuelEditorRoute>().id
    val isEdit = id != 0L
    var form by mutableStateOf(RefuelForm())
        private set
    var lastKm by mutableStateOf<Int?>(null)
        private set
    var grades by mutableStateOf(listOf("Gasolina 95", "Gasolina 98"))
        private set
    var stations by mutableStateOf<List<String>>(emptyList())
        private set
    private var original: RefuelEntity? = null

    init {
        viewModelScope.launch {
            val vehicle = active.current()
            grades = when (vehicle.fuelType) {
                FuelType.DIESEL -> listOf("Diésel", "Diésel premium")
                FuelType.LPG -> listOf("GLP", "Gasolina 95", "Gasolina 98")
                FuelType.ELECTRIC -> listOf("Carga eléctrica")
                else -> listOf("Gasolina 95", "Gasolina 98")
            }
            stations = repo.observeRecentStations(vehicle.id).first()
            val events = vehicles.odometerEvents(vehicle.id)
            lastKm = OdometerAnalyzer.analyze(events, vehicle.purchaseKm).currentKm
            if (isEdit) {
                repo.get(id)?.let { r ->
                    original = r
                    form = RefuelForm(
                        date = r.date, km = r.odometerKm.toString(), liters = Fmt.input(r.liters),
                        price = Fmt.input(r.pricePerLiter), total = Fmt.input(r.totalCost),
                        fullTank = r.fullTank, missedPrevious = r.missedPrevious,
                        grade = r.fuelGrade, station = r.station, notes = r.notes,
                    )
                }
            } else {
                val last = repo.last(vehicle.id)
                form = form.copy(grade = last?.fuelGrade?.ifBlank { null } ?: grades.first(), station = last?.station ?: "")
            }
        }
    }

    fun update(transform: (RefuelForm) -> RefuelForm) {
        form = transform(form)
    }

    /** Valores con el tercero calculado a partir de los otros dos. */
    val computed: Triple<Double?, Double?, Double?>
        get() {
            val l = NumberInput.parseDouble(form.liters)
            val p = NumberInput.parseDouble(form.price)
            val t = NumberInput.parseDouble(form.total)
            return if (l != null && p != null && t != null) Triple(l, p, t) else FuelCalculator.completeTriple(l, p, t)
        }

    val kmWarning: String?
        get() {
            val km = NumberInput.parseInt(form.km) ?: return null
            val last = lastKm ?: return null
            return if (!isEdit && km < last) "Es menor que la última lectura (${Fmt.km(last)})" else null
        }

    val canSave: Boolean
        get() {
            val (l, _, t) = computed
            return NumberInput.parseInt(form.km) != null && (l ?: 0.0) > 0 && (t ?: 0.0) > 0
        }

    fun save(onDone: () -> Unit) {
        if (!canSave) return
        viewModelScope.launch {
            val (l, p, t) = computed
            val liters = l!!
            val total = t!!
            val entity = RefuelEntity(
                id = id,
                vehicleId = original?.vehicleId ?: active.currentId(),
                date = form.date,
                odometerKm = NumberInput.parseInt(form.km)!!,
                liters = liters,
                totalCost = total,
                pricePerLiter = p ?: (total / liters),
                fullTank = form.fullTank,
                missedPrevious = form.missedPrevious,
                fuelGrade = form.grade,
                station = form.station.trim(),
                notes = form.notes.trim(),
            )
            repo.save(entity)
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        val r = original ?: return
        viewModelScope.launch {
            repo.delete(r)
            onDone()
        }
    }
}
