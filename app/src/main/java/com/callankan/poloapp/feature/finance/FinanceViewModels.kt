package com.callankan.poloapp.feature.finance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.callankan.poloapp.data.db.entity.ExpenseEntity
import com.callankan.poloapp.data.db.entity.LoanEntity
import com.callankan.poloapp.data.db.entity.LoanPaymentEntity
import com.callankan.poloapp.data.model.AttachmentOwner
import com.callankan.poloapp.data.model.ExpenseCategory
import com.callankan.poloapp.data.repository.ActiveVehicle
import com.callankan.poloapp.data.repository.AttachmentRepository
import com.callankan.poloapp.data.repository.FinanceRepository
import com.callankan.poloapp.data.settings.SettingsRepository
import com.callankan.poloapp.domain.DebtCalculator
import com.callankan.poloapp.domain.DebtSimulation
import com.callankan.poloapp.domain.DebtSummary
import com.callankan.poloapp.feature.common.AttachmentDraft
import com.callankan.poloapp.navigation.DebtSimulatorRoute
import com.callankan.poloapp.navigation.ExpenseEditorRoute
import com.callankan.poloapp.navigation.LoanEditorRoute
import com.callankan.poloapp.navigation.PaymentEditorRoute
import com.callankan.poloapp.ui.format.Fmt
import com.callankan.poloapp.ui.format.NumberInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlin.math.ceil

data class LoanWithPayments(val summary: DebtSummary, val payments: List<LoanPaymentEntity>)

data class FinanceUiState(
    val loans: List<LoanWithPayments>,
    val expenses: List<ExpenseEntity>,
    val lockEnabled: Boolean,
)

@HiltViewModel
class FinanceViewModel @Inject constructor(
    active: ActiveVehicle,
    repo: FinanceRepository,
    settings: SettingsRepository,
    val lock: SessionLock,
) : ViewModel() {
    val state: StateFlow<FinanceUiState?> = active.id.flatMapLatest { id ->
        combine(repo.observeLoans(id), repo.observePaymentsForVehicle(id), repo.observeExpenses(id), settings.settings) { loans, payments, expenses, s ->
            val today = LocalDate.now()
            FinanceUiState(
                loans = loans.map { loan ->
                    val p = payments.filter { it.loanId == loan.id }
                    LoanWithPayments(DebtCalculator.summarize(loan, p, today), p)
                },
                expenses = expenses,
                lockEnabled = s.debtLockEnabled,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

// ---------------------------------------------------------------- Préstamo y pagos

data class LoanForm(val lender: String = "", val amount: String = "", val startDate: LocalDate = LocalDate.now(), val notes: String = "")

@HiltViewModel
class LoanEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val active: ActiveVehicle,
    private val repo: FinanceRepository,
) : ViewModel() {
    private val id = savedStateHandle.toRoute<LoanEditorRoute>().id
    val isEdit = id != 0L
    var form by mutableStateOf(LoanForm())
        private set
    private var original: LoanEntity? = null

    init {
        if (isEdit) viewModelScope.launch {
            repo.getLoan(id)?.let { l ->
                original = l
                form = LoanForm(l.lender, Fmt.input(l.initialAmount), l.startDate, l.notes)
            }
        }
    }

    fun update(transform: (LoanForm) -> LoanForm) {
        form = transform(form)
    }

    val canSave get() = form.lender.isNotBlank() && (NumberInput.parseDouble(form.amount) ?: 0.0) > 0

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        repo.saveLoan(
            LoanEntity(
                id = id, vehicleId = original?.vehicleId ?: active.currentId(), lender = form.lender.trim(),
                initialAmount = NumberInput.parseDouble(form.amount)!!, startDate = form.startDate, notes = form.notes.trim(),
                createdAt = original?.createdAt ?: System.currentTimeMillis(),
            ),
        )
        onDone()
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        original?.let { repo.deleteLoan(it) }
        onDone()
    }
}

data class PaymentForm(val date: LocalDate = LocalDate.now(), val amount: String = "", val note: String = "")

@HiltViewModel
class PaymentEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: FinanceRepository,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<PaymentEditorRoute>()
    val isEdit = route.id != 0L
    var form by mutableStateOf(PaymentForm())
        private set
    var remainingBefore by mutableStateOf<Double?>(null)
        private set
    var lender by mutableStateOf("")
        private set
    private var original: LoanPaymentEntity? = null

    init {
        viewModelScope.launch {
            val loan = repo.getLoan(route.loanId) ?: return@launch
            lender = loan.lender
            val payments = repo.observePayments(route.loanId).first()
            original = if (isEdit) repo.getPayment(route.id) else null
            val paidOthers = payments.filter { it.id != route.id }.sumOf { it.amount }
            remainingBefore = loan.initialAmount - paidOthers
            original?.let { p -> form = PaymentForm(p.date, Fmt.input(p.amount), p.note) }
        }
    }

    fun update(transform: (PaymentForm) -> PaymentForm) {
        form = transform(form)
    }

    val canSave get() = (NumberInput.parseDouble(form.amount) ?: 0.0) > 0

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        repo.savePayment(LoanPaymentEntity(route.id, route.loanId, form.date, NumberInput.parseDouble(form.amount)!!, form.note.trim()))
        onDone()
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        original?.let { repo.deletePayment(it) }
        onDone()
    }
}

// ---------------------------------------------------------------- Simulador

@HiltViewModel
class DebtSimulatorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repo: FinanceRepository,
) : ViewModel() {
    private val loanId = savedStateHandle.toRoute<DebtSimulatorRoute>().loanId
    var summary by mutableStateOf<DebtSummary?>(null)
        private set
    var monthly by mutableStateOf("")
        private set
    var targetMonth by mutableStateOf<YearMonth>(YearMonth.now().plusMonths(12))
        private set
    var extras by mutableStateOf<Map<YearMonth, Double>>(emptyMap())
        private set
    val startMonth: YearMonth = YearMonth.now()

    init {
        viewModelScope.launch {
            val loan = repo.getLoan(loanId) ?: return@launch
            val payments = repo.observePayments(loanId).first()
            val s = DebtCalculator.summarize(loan, payments, LocalDate.now())
            summary = s
            val suggested = s.averageMonthly?.let { ceil(it / 10) * 10 } ?: 100.0
            monthly = Fmt.input(suggested.coerceAtMost(s.remaining.coerceAtLeast(1.0)))
        }
    }

    fun onMonthlyChange(value: String) {
        monthly = value
    }

    fun setTarget(month: YearMonth) {
        targetMonth = month
    }

    fun addExtra(month: YearMonth, amount: Double) {
        extras = extras + (month to (extras[month] ?: 0.0) + amount)
    }

    fun removeExtra(month: YearMonth) {
        extras = extras - month
    }

    val simulation: DebtSimulation?
        get() {
            val s = summary ?: return null
            val m = NumberInput.parseDouble(monthly) ?: return null
            return DebtCalculator.simulate(s.remaining, m, startMonth, extras)
        }

    val monthlyForTarget: Double?
        get() {
            val s = summary ?: return null
            val remaining = s.remaining - extras.values.sum()
            return DebtCalculator.monthlyForTarget(remaining, startMonth, targetMonth)
        }
}

// ---------------------------------------------------------------- Gastos

data class ExpenseForm(
    val category: ExpenseCategory = ExpenseCategory.PARKING,
    val amount: String = "",
    val date: LocalDate = LocalDate.now(),
    val description: String = "",
    val km: String = "",
    val notes: String = "",
)

@HiltViewModel
class ExpenseEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val active: ActiveVehicle,
    private val repo: FinanceRepository,
    attachments: AttachmentRepository,
) : ViewModel() {
    private val id = savedStateHandle.toRoute<ExpenseEditorRoute>().id
    val isEdit = id != 0L
    var form by mutableStateOf(ExpenseForm())
        private set
    val photos = AttachmentDraft(attachments, AttachmentOwner.EXPENSE, viewModelScope)
    private var original: ExpenseEntity? = null

    init {
        if (isEdit) viewModelScope.launch {
            repo.getExpense(id)?.let { e ->
                original = e
                form = ExpenseForm(e.category, Fmt.input(e.amount), e.date, e.description, Fmt.input(e.odometerKm), e.notes)
                photos.load(e.id)
            }
        }
    }

    fun update(transform: (ExpenseForm) -> ExpenseForm) {
        form = transform(form)
    }

    val canSave get() = (NumberInput.parseDouble(form.amount) ?: 0.0) > 0

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        val f = form
        val savedId = repo.saveExpense(
            ExpenseEntity(
                id = id, vehicleId = original?.vehicleId ?: active.currentId(), date = f.date, category = f.category,
                amount = NumberInput.parseDouble(f.amount)!!, description = f.description.trim(),
                odometerKm = NumberInput.parseInt(f.km), notes = f.notes.trim(),
            ),
        )
        photos.commit(savedId)
        onDone()
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        original?.let { repo.deleteExpense(it) }
        onDone()
    }

    override fun onCleared() = photos.discardIfNotCommitted()
}
