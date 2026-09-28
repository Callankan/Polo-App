package com.callankan.poloapp.feature.logbook

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.callankan.poloapp.data.db.entity.AttachmentEntity
import com.callankan.poloapp.data.db.entity.DamageEntity
import com.callankan.poloapp.data.db.entity.DocumentEntity
import com.callankan.poloapp.data.db.entity.NoteEntity
import com.callankan.poloapp.data.db.entity.TirePressureEntity
import com.callankan.poloapp.data.db.entity.TripEntity
import com.callankan.poloapp.data.db.entity.VehicleEntity
import com.callankan.poloapp.data.db.entity.WorkshopEntity
import com.callankan.poloapp.data.files.AttachmentStorage
import com.callankan.poloapp.data.model.AttachmentOwner
import com.callankan.poloapp.data.model.CarZone
import com.callankan.poloapp.data.model.DamageSeverity
import com.callankan.poloapp.data.model.DocumentType
import com.callankan.poloapp.data.repository.ActiveVehicle
import com.callankan.poloapp.data.repository.AttachmentRepository
import com.callankan.poloapp.data.repository.FuelRepository
import com.callankan.poloapp.data.repository.LogbookRepository
import com.callankan.poloapp.data.repository.MaintenanceRepository
import com.callankan.poloapp.data.repository.OverviewRepository
import com.callankan.poloapp.domain.FuelCalculator
import com.callankan.poloapp.domain.FuelStats
import com.callankan.poloapp.feature.common.AttachmentDraft
import com.callankan.poloapp.navigation.DamageEditorRoute
import com.callankan.poloapp.navigation.DocumentEditorRoute
import com.callankan.poloapp.navigation.NoteEditorRoute
import com.callankan.poloapp.navigation.TireEditorRoute
import com.callankan.poloapp.navigation.TripEditorRoute
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
import java.time.LocalTime
import javax.inject.Inject

// ---------------------------------------------------------------- Viajes

data class TripsUiState(val trips: List<TripEntity>, val fuel: FuelStats)

@HiltViewModel
class TripsViewModel @Inject constructor(active: ActiveVehicle, repo: LogbookRepository, fuel: FuelRepository) : ViewModel() {
    val state: StateFlow<TripsUiState?> = active.id.flatMapLatest { id ->
        combine(repo.observeTrips(id), fuel.observe(id)) { t, r -> TripsUiState(t, FuelCalculator.compute(r)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

data class TripForm(
    val date: LocalDate = LocalDate.now(),
    val origin: String = "",
    val destination: String = "",
    val startKm: String = "",
    val endKm: String = "",
    val distance: String = "",
    val purpose: String = "",
    val tolls: String = "",
    val notes: String = "",
)

@HiltViewModel
class TripEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val active: ActiveVehicle,
    private val repo: LogbookRepository,
    overview: OverviewRepository,
) : ViewModel() {
    private val id = savedStateHandle.toRoute<TripEditorRoute>().id
    val isEdit = id != 0L
    var form by mutableStateOf(TripForm())
        private set
    private var original: TripEntity? = null

    init {
        viewModelScope.launch {
            if (isEdit) {
                repo.getTrip(id)?.let { t ->
                    original = t
                    form = TripForm(t.date, t.origin, t.destination, Fmt.input(t.startKm), Fmt.input(t.endKm), t.distanceKm.toString(), t.purpose, Fmt.input(t.tolls), t.notes)
                }
            } else {
                form = form.copy(startKm = overview.snapshot(active.currentId())?.currentKm?.toString() ?: "")
            }
        }
    }

    fun update(transform: (TripForm) -> TripForm) {
        form = transform(form)
    }

    /** Distancia: la escrita o, si hay km de salida y llegada, su diferencia. */
    val distance: Int?
        get() = NumberInput.parseInt(form.distance) ?: run {
            val s = NumberInput.parseInt(form.startKm)
            val e = NumberInput.parseInt(form.endKm)
            if (s != null && e != null && e > s) e - s else null
        }

    val canSave get() = form.origin.isNotBlank() && form.destination.isNotBlank() && (distance ?: 0) > 0

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        val f = form
        repo.saveTrip(
            TripEntity(
                id = id, vehicleId = original?.vehicleId ?: active.currentId(), date = f.date,
                origin = f.origin.trim(), destination = f.destination.trim(),
                startKm = NumberInput.parseInt(f.startKm), endKm = NumberInput.parseInt(f.endKm),
                distanceKm = distance!!, purpose = f.purpose.trim(), tolls = NumberInput.parseDouble(f.tolls), notes = f.notes.trim(),
            ),
        )
        onDone()
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        original?.let { repo.deleteTrip(it) }
        onDone()
    }
}

// ---------------------------------------------------------------- Notas

@HiltViewModel
class NotesViewModel @Inject constructor(active: ActiveVehicle, private val repo: LogbookRepository) : ViewModel() {
    val notes: StateFlow<List<NoteEntity>?> = active.id.flatMapLatest { repo.observeNotes(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun togglePin(note: NoteEntity) = viewModelScope.launch { repo.saveNote(note.copy(pinned = !note.pinned)) }
    fun toggleDone(note: NoteEntity) = viewModelScope.launch { repo.saveNote(note.copy(reminderDone = !note.reminderDone)) }
}

data class NoteForm(
    val title: String = "",
    val body: String = "",
    val pinned: Boolean = false,
    val reminder: LocalDate? = null,
    val done: Boolean = false,
)

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val active: ActiveVehicle,
    private val repo: LogbookRepository,
) : ViewModel() {
    private val id = savedStateHandle.toRoute<NoteEditorRoute>().id
    val isEdit = id != 0L
    var form by mutableStateOf(NoteForm())
        private set
    private var original: NoteEntity? = null

    init {
        if (isEdit) viewModelScope.launch {
            repo.getNote(id)?.let { n ->
                original = n
                form = NoteForm(n.title, n.body, n.pinned, n.reminderDate, n.reminderDone)
            }
        }
    }

    fun update(transform: (NoteForm) -> NoteForm) {
        form = transform(form)
    }

    val canSave get() = form.title.isNotBlank() || form.body.isNotBlank()

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        val f = form
        repo.saveNote(
            NoteEntity(
                id = id, vehicleId = original?.vehicleId ?: active.currentId(),
                title = f.title.trim().ifBlank { f.body.trim().lineSequence().first().take(40) },
                body = f.body.trim(), pinned = f.pinned, reminderDate = f.reminder,
                reminderDone = if (f.reminder == null) false else f.done,
                createdAt = original?.createdAt ?: System.currentTimeMillis(),
            ),
        )
        onDone()
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        original?.let { repo.deleteNote(it) }
        onDone()
    }
}

// ---------------------------------------------------------------- Presión de neumáticos

data class TiresUiState(val vehicle: VehicleEntity, val checks: List<TirePressureEntity>)

@HiltViewModel
class TiresViewModel @Inject constructor(active: ActiveVehicle, repo: LogbookRepository) : ViewModel() {
    val state: StateFlow<TiresUiState?> = active.vehicle.flatMapLatest { v ->
        repo.observeTireChecks(v.id).map { TiresUiState(v, it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

data class TireForm(
    val date: LocalDate = LocalDate.now(),
    val minute: Int = LocalTime.now().let { it.hour * 60 + it.minute },
    val sameAxle: Boolean = true,
    val fl: String = "",
    val fr: String = "",
    val rl: String = "",
    val rr: String = "",
    val km: String = "",
    val notes: String = "",
)

@HiltViewModel
class TireEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val active: ActiveVehicle,
    private val repo: LogbookRepository,
) : ViewModel() {
    private val id = savedStateHandle.toRoute<TireEditorRoute>().id
    val isEdit = id != 0L
    var form by mutableStateOf(TireForm())
        private set
    var recommendedFront by mutableStateOf<Double?>(null)
        private set
    var recommendedRear by mutableStateOf<Double?>(null)
        private set
    private var original: TirePressureEntity? = null

    init {
        viewModelScope.launch {
            val v = active.current()
            recommendedFront = v.tireFrontBar
            recommendedRear = v.tireRearBar
            if (isEdit) {
                repo.getTireCheck(id)?.let { t ->
                    original = t
                    form = TireForm(
                        t.date, t.minuteOfDay, t.frontLeft == t.frontRight && t.rearLeft == t.rearRight,
                        Fmt.input(t.frontLeft), Fmt.input(t.frontRight), Fmt.input(t.rearLeft), Fmt.input(t.rearRight),
                        Fmt.input(t.odometerKm), t.notes,
                    )
                }
            }
        }
    }

    fun update(transform: (TireForm) -> TireForm) {
        form = transform(form)
    }

    fun useRecommended() {
        form = form.copy(
            fl = Fmt.input(recommendedFront), fr = Fmt.input(recommendedFront),
            rl = Fmt.input(recommendedRear), rr = Fmt.input(recommendedRear),
        )
    }

    private fun values(): List<Double?> {
        val f = form
        val fl = NumberInput.parseDouble(f.fl)
        val rl = NumberInput.parseDouble(f.rl)
        return if (f.sameAxle) listOf(fl, fl, rl, rl)
        else listOf(fl, NumberInput.parseDouble(f.fr), rl, NumberInput.parseDouble(f.rr))
    }

    val canSave get() = values().all { it != null && it > 0 && it < 6 }

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        val (fl, fr, rl, rr) = values().map { it!! }
        repo.saveTireCheck(
            TirePressureEntity(
                id = id, vehicleId = original?.vehicleId ?: active.currentId(), date = form.date, minuteOfDay = form.minute,
                frontLeft = fl, frontRight = fr, rearLeft = rl, rearRight = rr,
                odometerKm = NumberInput.parseInt(form.km), notes = form.notes.trim(),
            ),
        )
        onDone()
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        original?.let { repo.deleteTireCheck(it) }
        onDone()
    }
}

// ---------------------------------------------------------------- Daños

data class DamagesUiState(
    val vehicle: VehicleEntity,
    val damages: List<DamageEntity>,
    val thumbnails: Map<Long, AttachmentEntity>,
)

@HiltViewModel
class DamagesViewModel @Inject constructor(
    active: ActiveVehicle,
    repo: LogbookRepository,
    attachments: AttachmentRepository,
) : ViewModel() {
    val storage: AttachmentStorage = attachments.storage
    val state: StateFlow<DamagesUiState?> = active.vehicle.flatMapLatest { v ->
        combine(repo.observeDamages(v.id), attachments.observeByType(AttachmentOwner.DAMAGE)) { d, a ->
            DamagesUiState(v, d, a.groupBy { it.ownerId }.mapValues { it.value.first() })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

data class DamageForm(
    val title: String = "",
    val description: String = "",
    val date: LocalDate = LocalDate.now(),
    val km: String = "",
    val zone: CarZone? = null,
    val severity: DamageSeverity = DamageSeverity.MINOR,
    val repaired: Boolean = false,
    val repairedDate: LocalDate? = null,
    val repairCost: String = "",
    val workshopId: Long? = null,
    val insuranceClaim: Boolean = false,
)

@HiltViewModel
class DamageEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val active: ActiveVehicle,
    private val repo: LogbookRepository,
    maintenance: MaintenanceRepository,
    attachments: AttachmentRepository,
) : ViewModel() {
    private val id = savedStateHandle.toRoute<DamageEditorRoute>().id
    val isEdit = id != 0L
    var form by mutableStateOf(DamageForm())
        private set
    var bodyColor by mutableStateOf(0xFFD0121E)
        private set
    val photos = AttachmentDraft(attachments, AttachmentOwner.DAMAGE, viewModelScope)
    val workshops: StateFlow<List<WorkshopEntity>> = maintenance.observeWorkshops()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private var original: DamageEntity? = null

    init {
        viewModelScope.launch {
            bodyColor = active.current().colorArgb
            if (isEdit) repo.getDamage(id)?.let { d ->
                original = d
                form = DamageForm(
                    d.title, d.description, d.date, Fmt.input(d.odometerKm), d.zone, d.severity, d.repaired,
                    d.repairedDate, Fmt.input(d.repairCost), d.workshopId, d.insuranceClaim,
                )
                photos.load(d.id)
            }
        }
    }

    fun update(transform: (DamageForm) -> DamageForm) {
        form = transform(form)
    }

    val canSave get() = form.title.isNotBlank() && form.zone != null

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        val f = form
        val savedId = repo.saveDamage(
            DamageEntity(
                id = id, vehicleId = original?.vehicleId ?: active.currentId(), date = f.date,
                odometerKm = NumberInput.parseInt(f.km), title = f.title.trim(), description = f.description.trim(),
                zone = f.zone!!, severity = f.severity, repaired = f.repaired,
                repairedDate = if (f.repaired) f.repairedDate ?: LocalDate.now() else null,
                repairCost = if (f.repaired) NumberInput.parseDouble(f.repairCost) else null,
                workshopId = if (f.repaired) f.workshopId else null, insuranceClaim = f.insuranceClaim,
            ),
        )
        photos.commit(savedId)
        onDone()
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        original?.let { repo.deleteDamage(it) }
        onDone()
    }

    override fun onCleared() = photos.discardIfNotCommitted()
}

// ---------------------------------------------------------------- Guantera digital

data class DocumentsUiState(val documents: List<DocumentEntity>, val attachments: Map<Long, List<AttachmentEntity>>)

@HiltViewModel
class DocumentsViewModel @Inject constructor(
    active: ActiveVehicle,
    repo: LogbookRepository,
    attachments: AttachmentRepository,
) : ViewModel() {
    val storage: AttachmentStorage = attachments.storage
    val state: StateFlow<DocumentsUiState?> = active.id.flatMapLatest { id ->
        combine(repo.observeDocuments(id), attachments.observeByType(AttachmentOwner.DOCUMENT)) { d, a ->
            DocumentsUiState(d, a.groupBy { it.ownerId })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

data class DocumentForm(
    val type: DocumentType = DocumentType.REGISTRATION,
    val title: String = "",
    val expiry: LocalDate? = null,
    val notes: String = "",
)

@HiltViewModel
class DocumentEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val active: ActiveVehicle,
    private val repo: LogbookRepository,
    attachments: AttachmentRepository,
) : ViewModel() {
    private val id = savedStateHandle.toRoute<DocumentEditorRoute>().id
    val isEdit = id != 0L
    var form by mutableStateOf(DocumentForm())
        private set
    val photos = AttachmentDraft(attachments, AttachmentOwner.DOCUMENT, viewModelScope)
    private var original: DocumentEntity? = null

    init {
        if (isEdit) viewModelScope.launch {
            repo.getDocument(id)?.let { d ->
                original = d
                form = DocumentForm(d.type, d.title, d.expiryDate, d.notes)
                photos.load(d.id)
            }
        }
    }

    fun update(transform: (DocumentForm) -> DocumentForm) {
        form = transform(form)
    }

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        val f = form
        val savedId = repo.saveDocument(
            DocumentEntity(
                id = id, vehicleId = original?.vehicleId ?: active.currentId(), type = f.type,
                title = f.title.trim().ifBlank { f.type.label }, expiryDate = f.expiry, notes = f.notes.trim(),
                createdAt = original?.createdAt ?: System.currentTimeMillis(),
            ),
        )
        photos.commit(savedId)
        onDone()
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        original?.let { repo.deleteDocument(it) }
        onDone()
    }

    override fun onCleared() = photos.discardIfNotCommitted()
}
