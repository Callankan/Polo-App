package com.callankan.poloapp.feature.maintenance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.callankan.poloapp.data.db.dao.MaintenanceWithDetails
import com.callankan.poloapp.data.db.entity.AttachmentEntity
import com.callankan.poloapp.data.db.entity.ComponentEntity
import com.callankan.poloapp.data.db.entity.MaintenanceEntity
import com.callankan.poloapp.data.db.entity.PartInstallationEntity
import com.callankan.poloapp.data.db.entity.WorkshopEntity
import com.callankan.poloapp.data.files.AttachmentStorage
import com.callankan.poloapp.data.model.AttachmentOwner
import com.callankan.poloapp.data.model.MaintenanceCategory
import com.callankan.poloapp.data.model.PerformedBy
import com.callankan.poloapp.data.repository.ActiveVehicle
import com.callankan.poloapp.data.repository.AttachmentRepository
import com.callankan.poloapp.data.repository.InstallDraft
import com.callankan.poloapp.data.repository.MaintenanceRepository
import com.callankan.poloapp.data.repository.OverviewRepository
import com.callankan.poloapp.data.repository.VehicleOverview
import com.callankan.poloapp.domain.ComponentStatus
import com.callankan.poloapp.feature.common.AttachmentDraft
import com.callankan.poloapp.navigation.ComponentDetailRoute
import com.callankan.poloapp.navigation.ComponentEditorRoute
import com.callankan.poloapp.navigation.InstallationEditorRoute
import com.callankan.poloapp.navigation.MaintenanceDetailRoute
import com.callankan.poloapp.navigation.MaintenanceEditorRoute
import com.callankan.poloapp.navigation.WorkshopEditorRoute
import com.callankan.poloapp.ui.format.Fmt
import com.callankan.poloapp.ui.format.NumberInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class MaintenanceUiState(
    val overview: VehicleOverview,
    val records: List<MaintenanceWithDetails>,
    val workshops: List<WorkshopEntity>,
)

@HiltViewModel
class MaintenanceViewModel @Inject constructor(
    active: ActiveVehicle,
    overviewRepository: OverviewRepository,
    repo: MaintenanceRepository,
) : ViewModel() {
    val state: StateFlow<MaintenanceUiState?> = active.id.flatMapLatest { id ->
        combine(overviewRepository.observe(id).filterNotNull(), repo.observeRecords(id), repo.observeWorkshops()) { o, r, w ->
            MaintenanceUiState(o, r, w)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

// ---------------------------------------------------------------- Intervención

data class PartSelection(val componentId: Long, val brand: String = "", val reference: String = "")

data class MaintenanceForm(
    val title: String = "",
    val category: MaintenanceCategory = MaintenanceCategory.OIL_SERVICE,
    val date: LocalDate = LocalDate.now(),
    val km: String = "",
    val performedBy: PerformedBy = PerformedBy.WORKSHOP,
    val workshopId: Long? = null,
    val partsCost: String = "",
    val laborCost: String = "",
    val totalCost: String = "",
    val invoice: String = "",
    val notes: String = "",
    val parts: List<PartSelection> = emptyList(),
)

@HiltViewModel
class MaintenanceEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val active: ActiveVehicle,
    private val repo: MaintenanceRepository,
    overviewRepository: OverviewRepository,
    attachments: AttachmentRepository,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<MaintenanceEditorRoute>()
    val isEdit = route.id != 0L
    var form by mutableStateOf(MaintenanceForm())
        private set
    var components by mutableStateOf<List<ComponentEntity>>(emptyList())
        private set
    var currentKm by mutableStateOf<Int?>(null)
        private set
    val photos = AttachmentDraft(attachments, AttachmentOwner.MAINTENANCE, viewModelScope)
    val workshops: StateFlow<List<WorkshopEntity>> = repo.observeWorkshops()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private var original: MaintenanceEntity? = null

    init {
        viewModelScope.launch {
            val vehicleId = active.currentId()
            components = repo.components(vehicleId).filter { !it.archived }
            currentKm = overviewRepository.snapshot(vehicleId)?.currentKm
            if (isEdit) {
                repo.getRecord(route.id)?.let { d ->
                    val r = d.record
                    original = r
                    form = MaintenanceForm(
                        title = r.title, category = r.category, date = r.date, km = r.odometerKm.toString(),
                        performedBy = r.performedBy, workshopId = r.workshopId,
                        partsCost = Fmt.input(r.partsCost), laborCost = Fmt.input(r.laborCost),
                        totalCost = if (r.totalCost > 0) Fmt.input(r.totalCost) else "",
                        invoice = r.invoiceNumber, notes = r.notes,
                        parts = d.installations.map { PartSelection(it.installation.componentId, it.installation.brand, it.installation.reference) },
                    )
                    photos.load(r.id)
                }
            } else {
                val preselected = components.firstOrNull { it.id == route.componentId }
                form = form.copy(
                    km = currentKm?.toString() ?: "",
                    parts = listOfNotNull(preselected?.let { PartSelection(it.id) }),
                    category = preselected?.category ?: form.category,
                    title = preselected?.let { "Cambio de ${it.name.lowercase()}" } ?: "",
                    workshopId = workshops.value.firstOrNull()?.id,
                )
            }
        }
    }

    fun update(transform: (MaintenanceForm) -> MaintenanceForm) {
        form = transform(form)
    }

    fun togglePart(componentId: Long) {
        val parts = form.parts
        form = form.copy(
            parts = if (parts.any { it.componentId == componentId }) parts.filterNot { it.componentId == componentId }
            else parts + PartSelection(componentId),
        )
    }

    fun updatePart(componentId: Long, transform: (PartSelection) -> PartSelection) {
        form = form.copy(parts = form.parts.map { if (it.componentId == componentId) transform(it) else it })
    }

    val computedTotal: Double?
        get() = NumberInput.parseDouble(form.totalCost) ?: run {
            val p = NumberInput.parseDouble(form.partsCost)
            val l = NumberInput.parseDouble(form.laborCost)
            if (p == null && l == null) null else (p ?: 0.0) + (l ?: 0.0)
        }

    val canSave: Boolean get() = form.title.isNotBlank() && NumberInput.parseInt(form.km) != null

    fun quickAddWorkshop(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = repo.saveWorkshop(WorkshopEntity(name = name.trim()))
            form = form.copy(workshopId = id, performedBy = PerformedBy.WORKSHOP)
        }
    }

    fun save(onDone: () -> Unit) {
        if (!canSave) return
        viewModelScope.launch {
            val f = form
            val record = MaintenanceEntity(
                id = route.id,
                vehicleId = original?.vehicleId ?: active.currentId(),
                date = f.date,
                odometerKm = NumberInput.parseInt(f.km)!!,
                title = f.title.trim(),
                category = f.category,
                performedBy = f.performedBy,
                workshopId = if (f.performedBy == PerformedBy.WORKSHOP) f.workshopId else null,
                partsCost = NumberInput.parseDouble(f.partsCost),
                laborCost = NumberInput.parseDouble(f.laborCost),
                totalCost = computedTotal ?: 0.0,
                invoiceNumber = f.invoice.trim(),
                notes = f.notes.trim(),
                createdAt = original?.createdAt ?: System.currentTimeMillis(),
            )
            val id = repo.saveRecord(record, f.parts.map { InstallDraft(it.componentId, it.brand.trim(), it.reference.trim()) })
            photos.commit(id)
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.deleteRecord(route.id)
            onDone()
        }
    }

    override fun onCleared() = photos.discardIfNotCommitted()
}

@HiltViewModel
class MaintenanceDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repo: MaintenanceRepository,
    attachments: AttachmentRepository,
) : ViewModel() {
    private val id = savedStateHandle.toRoute<MaintenanceDetailRoute>().id
    val storage: AttachmentStorage = attachments.storage
    val record: StateFlow<MaintenanceWithDetails?> = repo.observeRecord(id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val photos: StateFlow<List<AttachmentEntity>> = attachments.observe(AttachmentOwner.MAINTENANCE, id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

// ---------------------------------------------------------------- Pieza

@HiltViewModel
class ComponentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    active: ActiveVehicle,
    overviewRepository: OverviewRepository,
) : ViewModel() {
    val componentId = savedStateHandle.toRoute<ComponentDetailRoute>().id
    val state: StateFlow<Pair<ComponentStatus, Int>?> = active.id
        .flatMapLatest { overviewRepository.observe(it) }
        .map { o -> o?.components?.firstOrNull { it.component.id == componentId }?.let { it to o.currentKm } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

data class ComponentForm(
    val name: String = "",
    val category: MaintenanceCategory = MaintenanceCategory.OTHER,
    val intervalKm: String = "",
    val intervalMonths: String = "",
    val alerts: Boolean = true,
    val hint: String = "",
    val archived: Boolean = false,
)

@HiltViewModel
class ComponentEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val active: ActiveVehicle,
    private val repo: MaintenanceRepository,
) : ViewModel() {
    private val id = savedStateHandle.toRoute<ComponentEditorRoute>().id
    val isEdit = id != 0L
    var form by mutableStateOf(ComponentForm())
        private set
    private var original: ComponentEntity? = null

    init {
        if (isEdit) viewModelScope.launch {
            repo.getComponent(id)?.let { c ->
                original = c
                form = ComponentForm(c.name, c.category, Fmt.input(c.intervalKm), Fmt.input(c.intervalMonths), c.alertsEnabled, c.hint, c.archived)
            }
        }
    }

    fun update(transform: (ComponentForm) -> ComponentForm) {
        form = transform(form)
    }

    val canSave get() = form.name.isNotBlank()

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        val f = form
        repo.saveComponent(
            ComponentEntity(
                id = id,
                vehicleId = original?.vehicleId ?: active.currentId(),
                name = f.name.trim(),
                category = f.category,
                intervalKm = NumberInput.parseInt(f.intervalKm)?.takeIf { it > 0 },
                intervalMonths = NumberInput.parseInt(f.intervalMonths)?.takeIf { it > 0 },
                alertsEnabled = f.alerts,
                hint = f.hint.trim(),
                sortOrder = original?.sortOrder ?: 0,
                archived = f.archived,
            ),
        )
        onDone()
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        original?.let { repo.deleteComponent(it) }
        onDone()
    }
}

data class InstallationForm(
    val date: LocalDate = LocalDate.now(),
    val km: String = "",
    val brand: String = "",
    val reference: String = "",
    val cost: String = "",
    val notes: String = "",
)

/** Registro rápido de un cambio de pieza sin crear una intervención completa (p. ej. datos antiguos). */
@HiltViewModel
class InstallationEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val active: ActiveVehicle,
    private val repo: MaintenanceRepository,
    overviewRepository: OverviewRepository,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<InstallationEditorRoute>()
    val isEdit = route.id != 0L
    var form by mutableStateOf(InstallationForm())
        private set
    var componentName by mutableStateOf("")
        private set
    var linkedToMaintenance by mutableStateOf(false)
        private set
    private var original: PartInstallationEntity? = null

    init {
        viewModelScope.launch {
            componentName = repo.getComponent(route.componentId)?.name ?: ""
            if (isEdit) {
                repo.getInstallation(route.id)?.let { i ->
                    original = i
                    linkedToMaintenance = i.maintenanceId != null
                    form = InstallationForm(i.date, i.odometerKm.toString(), i.brand, i.reference, Fmt.input(i.cost), i.notes)
                }
            } else {
                form = form.copy(km = overviewRepository.snapshot(active.currentId())?.currentKm?.toString() ?: "")
            }
        }
    }

    fun update(transform: (InstallationForm) -> InstallationForm) {
        form = transform(form)
    }

    val canSave get() = NumberInput.parseInt(form.km) != null

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        val f = form
        repo.saveInstallation(
            PartInstallationEntity(
                id = route.id,
                vehicleId = original?.vehicleId ?: active.currentId(),
                componentId = route.componentId,
                maintenanceId = original?.maintenanceId,
                date = f.date,
                odometerKm = NumberInput.parseInt(f.km)!!,
                brand = f.brand.trim(),
                reference = f.reference.trim(),
                cost = NumberInput.parseDouble(f.cost),
                notes = f.notes.trim(),
            ),
        )
        onDone()
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        original?.let { repo.deleteInstallation(it) }
        onDone()
    }
}

// ---------------------------------------------------------------- Taller

data class WorkshopForm(
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val rating: Int = 0,
    val notes: String = "",
)

@HiltViewModel
class WorkshopEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: MaintenanceRepository,
) : ViewModel() {
    private val id = savedStateHandle.toRoute<WorkshopEditorRoute>().id
    val isEdit = id != 0L
    var form by mutableStateOf(WorkshopForm())
        private set
    private var original: WorkshopEntity? = null
    val usage: Flow<Int> = if (isEdit) repo.observeWorkshopUsage(id) else flowOf(0)

    init {
        if (isEdit) viewModelScope.launch {
            repo.getWorkshop(id)?.let { w ->
                original = w
                form = WorkshopForm(w.name, w.phone, w.address, w.rating, w.notes)
            }
        }
    }

    fun update(transform: (WorkshopForm) -> WorkshopForm) {
        form = transform(form)
    }

    val canSave get() = form.name.isNotBlank()

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        val f = form
        repo.saveWorkshop(
            WorkshopEntity(
                id = id, name = f.name.trim(), phone = f.phone.trim(), address = f.address.trim(),
                notes = f.notes.trim(), rating = f.rating, createdAt = original?.createdAt ?: System.currentTimeMillis(),
            ),
        )
        onDone()
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        original?.let { repo.deleteWorkshop(it) }
        onDone()
    }
}
