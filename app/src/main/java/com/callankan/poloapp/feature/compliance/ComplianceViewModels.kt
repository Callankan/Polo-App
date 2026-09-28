package com.callankan.poloapp.feature.compliance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.callankan.poloapp.data.db.entity.InsurancePolicyEntity
import com.callankan.poloapp.data.db.entity.ItvInspectionEntity
import com.callankan.poloapp.data.db.entity.VehicleEntity
import com.callankan.poloapp.data.model.AttachmentOwner
import com.callankan.poloapp.data.model.InsuranceCoverage
import com.callankan.poloapp.data.model.ItvResult
import com.callankan.poloapp.data.repository.ActiveVehicle
import com.callankan.poloapp.data.repository.AttachmentRepository
import com.callankan.poloapp.data.repository.ComplianceRepository
import com.callankan.poloapp.data.repository.OverviewRepository
import com.callankan.poloapp.data.repository.VehicleRepository
import com.callankan.poloapp.domain.Deadline
import com.callankan.poloapp.domain.ItvRules
import com.callankan.poloapp.feature.common.AttachmentDraft
import com.callankan.poloapp.navigation.InsuranceEditorRoute
import com.callankan.poloapp.navigation.ItvEditorRoute
import com.callankan.poloapp.ui.format.Fmt
import com.callankan.poloapp.ui.format.NumberInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ItvUiState(val vehicle: VehicleEntity, val inspections: List<ItvInspectionEntity>, val deadline: Deadline)

@HiltViewModel
class ItvViewModel @Inject constructor(
    active: ActiveVehicle,
    repo: ComplianceRepository,
    private val vehicles: VehicleRepository,
) : ViewModel() {
    val state: StateFlow<ItvUiState?> = active.vehicle.flatMapLatest { v ->
        repo.observeItv(v.id).let { flow ->
            combine(flow, active.vehicle) { list, vehicle ->
                val today = LocalDate.now()
                ItvUiState(vehicle, list, Deadline.of(ItvRules.currentDueDate(list, vehicle.manualItvDueDate, vehicle.registrationDate, today), today))
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setManualDate(vehicle: VehicleEntity, date: LocalDate?) = viewModelScope.launch {
        vehicles.update(vehicle.copy(manualItvDueDate = date))
    }
}

data class ItvForm(
    val date: LocalDate = LocalDate.now(),
    val result: ItvResult = ItvResult.FAVORABLE,
    val station: String = "",
    val km: String = "",
    val cost: String = "",
    val defects: String = "",
    val nextDue: LocalDate? = null,
    val nextDueEdited: Boolean = false,
    val notes: String = "",
)

@HiltViewModel
class ItvEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val active: ActiveVehicle,
    private val repo: ComplianceRepository,
    overview: OverviewRepository,
    attachments: AttachmentRepository,
) : ViewModel() {
    private val id = savedStateHandle.toRoute<ItvEditorRoute>().id
    val isEdit = id != 0L
    var form by mutableStateOf(ItvForm())
        private set
    val photos = AttachmentDraft(attachments, AttachmentOwner.ITV, viewModelScope)
    private var original: ItvInspectionEntity? = null
    private var registration: LocalDate? = null
    private var previousDue: LocalDate? = null

    init {
        viewModelScope.launch {
            val vehicle = active.current()
            registration = vehicle.registrationDate
            val all = repo.itv(vehicle.id)
            if (isEdit) {
                repo.getItv(id)?.let { i ->
                    original = i
                    previousDue = all.filter { it.date < i.date }.maxByOrNull { it.date }?.nextDueDate
                    form = ItvForm(i.date, i.result, i.station, Fmt.input(i.odometerKm), Fmt.input(i.cost), i.defects, i.nextDueDate, true, i.notes)
                    photos.load(i.id)
                }
            } else {
                previousDue = ItvRules.currentDueDate(all, vehicle.manualItvDueDate, vehicle.registrationDate, LocalDate.now())
                form = form.copy(
                    km = overview.snapshot(vehicle.id)?.currentKm?.toString() ?: "",
                    station = all.firstOrNull()?.station ?: "",
                )
                recompute()
            }
        }
    }

    fun update(transform: (ItvForm) -> ItvForm) {
        form = transform(form)
        if (!form.nextDueEdited) recompute()
    }

    private fun recompute() {
        form = form.copy(nextDue = ItvRules.nextDueDate(registration, form.date, form.result, previousDue))
    }

    val frequencyHint: String get() = ItvRules.describeFrequency(registration, form.date)

    val canSave get() = form.nextDue != null

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        val f = form
        val savedId = repo.saveItv(
            ItvInspectionEntity(
                id = id, vehicleId = original?.vehicleId ?: active.currentId(), date = f.date,
                odometerKm = NumberInput.parseInt(f.km), station = f.station.trim(), result = f.result,
                defects = f.defects.trim(), cost = NumberInput.parseDouble(f.cost), nextDueDate = f.nextDue!!, notes = f.notes.trim(),
            ),
        )
        photos.commit(savedId)
        onDone()
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        original?.let { repo.deleteItv(it) }
        onDone()
    }

    override fun onCleared() = photos.discardIfNotCommitted()
}

data class InsuranceUiState(val policies: List<InsurancePolicyEntity>, val deadline: Deadline)

@HiltViewModel
class InsuranceViewModel @Inject constructor(active: ActiveVehicle, repo: ComplianceRepository) : ViewModel() {
    val state: StateFlow<InsuranceUiState?> = active.id.flatMapLatest { id ->
        repo.observeInsurance(id).let { flow ->
            combine(flow, active.vehicle) { list, _ ->
                InsuranceUiState(list, Deadline.of(list.maxByOrNull { it.endDate }?.endDate, LocalDate.now()))
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

data class InsuranceForm(
    val company: String = "",
    val policyNumber: String = "",
    val coverage: InsuranceCoverage = InsuranceCoverage.THIRD_PARTY_EXTENDED,
    val start: LocalDate = LocalDate.now(),
    val end: LocalDate = LocalDate.now().plusYears(1).minusDays(1),
    val premium: String = "",
    val phone: String = "",
    val notes: String = "",
)

@HiltViewModel
class InsuranceEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val active: ActiveVehicle,
    private val repo: ComplianceRepository,
    attachments: AttachmentRepository,
) : ViewModel() {
    private val id = savedStateHandle.toRoute<InsuranceEditorRoute>().id
    val isEdit = id != 0L
    var form by mutableStateOf(InsuranceForm())
        private set
    var isRenewal by mutableStateOf(false)
        private set
    val photos = AttachmentDraft(attachments, AttachmentOwner.INSURANCE, viewModelScope)
    private var original: InsurancePolicyEntity? = null

    init {
        viewModelScope.launch {
            if (isEdit) {
                repo.getPolicy(id)?.let { p ->
                    original = p
                    form = InsuranceForm(p.company, p.policyNumber, p.coverage, p.startDate, p.endDate, Fmt.input(p.premium), p.assistancePhone, p.notes)
                    photos.load(p.id)
                }
            } else {
                // Renovación: se parte de la póliza vigente.
                repo.insurance(active.currentId()).maxByOrNull { it.endDate }?.let { p ->
                    isRenewal = true
                    val start = p.endDate.plusDays(1)
                    form = InsuranceForm(p.company, p.policyNumber, p.coverage, start, start.plusYears(1).minusDays(1), "", p.assistancePhone)
                }
            }
        }
    }

    fun update(transform: (InsuranceForm) -> InsuranceForm) {
        form = transform(form)
    }

    val canSave get() = form.company.isNotBlank() && form.end.isAfter(form.start)

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        val f = form
        val savedId = repo.savePolicy(
            InsurancePolicyEntity(
                id = id, vehicleId = original?.vehicleId ?: active.currentId(), company = f.company.trim(),
                policyNumber = f.policyNumber.trim(), coverage = f.coverage, startDate = f.start, endDate = f.end,
                premium = NumberInput.parseDouble(f.premium), assistancePhone = f.phone.trim(), notes = f.notes.trim(),
            ),
        )
        photos.commit(savedId)
        onDone()
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        original?.let { repo.deletePolicy(it) }
        onDone()
    }

    override fun onCleared() = photos.discardIfNotCommitted()
}
