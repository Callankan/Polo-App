package com.callankan.poloapp.data.repository

import com.callankan.poloapp.data.db.dao.DamageDao
import com.callankan.poloapp.data.db.dao.ExpenseDao
import com.callankan.poloapp.data.db.dao.InsuranceDao
import com.callankan.poloapp.data.db.dao.ItvDao
import com.callankan.poloapp.data.db.dao.LoanDao
import com.callankan.poloapp.data.db.dao.MaintenanceDao
import com.callankan.poloapp.data.db.dao.MaintenanceWithDetails
import com.callankan.poloapp.data.db.dao.NoteDao
import com.callankan.poloapp.data.db.dao.OdometerDao
import com.callankan.poloapp.data.db.dao.RefuelDao
import com.callankan.poloapp.data.db.dao.TirePressureDao
import com.callankan.poloapp.data.db.dao.VehicleDao
import com.callankan.poloapp.data.db.entity.ComponentEntity
import com.callankan.poloapp.data.db.entity.DamageEntity
import com.callankan.poloapp.data.db.entity.ExpenseEntity
import com.callankan.poloapp.data.db.entity.InsurancePolicyEntity
import com.callankan.poloapp.data.db.entity.ItvInspectionEntity
import com.callankan.poloapp.data.db.entity.LoanEntity
import com.callankan.poloapp.data.db.entity.LoanPaymentEntity
import com.callankan.poloapp.data.db.entity.NoteEntity
import com.callankan.poloapp.data.db.entity.OdometerEvent
import com.callankan.poloapp.data.db.entity.PartInstallationEntity
import com.callankan.poloapp.data.db.entity.RefuelEntity
import com.callankan.poloapp.data.db.entity.TirePressureEntity
import com.callankan.poloapp.data.db.entity.VehicleEntity
import com.callankan.poloapp.data.settings.AppSettings
import com.callankan.poloapp.data.settings.SettingsRepository
import com.callankan.poloapp.domain.ComponentStatus
import com.callankan.poloapp.domain.CostAnalyzer
import com.callankan.poloapp.domain.DebtCalculator
import com.callankan.poloapp.domain.DebtSummary
import com.callankan.poloapp.domain.Deadline
import com.callankan.poloapp.domain.DueState
import com.callankan.poloapp.domain.FuelCalculator
import com.callankan.poloapp.domain.FuelStats
import com.callankan.poloapp.domain.HealthScore
import com.callankan.poloapp.domain.HealthScoreCalculator
import com.callankan.poloapp.domain.ItvRules
import com.callankan.poloapp.domain.MaintenanceCalculator
import com.callankan.poloapp.domain.OdometerAnalysis
import com.callankan.poloapp.domain.OdometerAnalyzer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/** Todo lo que se sabe del coche en un momento dado: alimenta dashboard, widget, avisos y PDF. */
data class VehicleOverview(
    val vehicle: VehicleEntity,
    val odometer: OdometerAnalysis,
    val currentKm: Int,
    val fuel: FuelStats,
    val components: List<ComponentStatus>,
    val itv: Deadline,
    val lastItv: ItvInspectionEntity?,
    val insurance: Deadline,
    val policy: InsurancePolicyEntity?,
    val debts: List<DebtSummary>,
    val health: HealthScore,
    val lastTireCheck: TirePressureEntity?,
    val openDamages: List<DamageEntity>,
    val pinnedNotes: List<NoteEntity>,
    val dueReminders: List<NoteEntity>,
    val monthSpend: Double,
    val lastMonthSpend: Double,
    val settings: AppSettings,
    val today: LocalDate,
) {
    val nextMaintenance: ComponentStatus?
        get() = components.firstOrNull { it.state == DueState.OVERDUE || it.state == DueState.SOON || it.state == DueState.OK }
    val alerts: List<ComponentStatus>
        get() = components.filter { it.state == DueState.OVERDUE || it.state == DueState.SOON }
    val primaryDebt: DebtSummary? get() = debts.firstOrNull { !it.isPaidOff } ?: debts.firstOrNull()
}

/** Datos crudos necesarios para calcular un [VehicleOverview]. */
data class OverviewInputs(
    val vehicle: VehicleEntity,
    val events: List<OdometerEvent>,
    val refuels: List<RefuelEntity>,
    val components: List<ComponentEntity>,
    val installations: List<PartInstallationEntity>,
    val itv: List<ItvInspectionEntity>,
    val insurance: List<InsurancePolicyEntity>,
    val loans: List<LoanEntity>,
    val payments: List<LoanPaymentEntity>,
    val tireChecks: List<TirePressureEntity>,
    val damages: List<DamageEntity>,
    val notes: List<NoteEntity>,
    val expenses: List<ExpenseEntity>,
    val maintenance: List<MaintenanceWithDetails>,
    val settings: AppSettings,
)

object OverviewBuilder {
    fun build(i: OverviewInputs, today: LocalDate = LocalDate.now()): VehicleOverview {
        val odometer = OdometerAnalyzer.analyze(i.events, i.vehicle.purchaseKm)
        val currentKm = odometer.currentKm ?: i.vehicle.purchaseKm ?: 0
        val statuses = MaintenanceCalculator.statuses(
            i.components, i.installations, currentKm, today, odometer.kmPerDay,
            i.settings.maintenanceLeadKm, i.settings.maintenanceLeadDays,
        )
        val itvDue = ItvRules.currentDueDate(i.itv, i.vehicle.manualItvDueDate, i.vehicle.registrationDate, today)
        val itv = Deadline.of(itvDue, today)
        val policy = i.insurance.maxByOrNull { it.endDate }
        val insurance = Deadline.of(policy?.endDate, today)
        val debts = i.loans.map { loan -> DebtCalculator.summarize(loan, i.payments.filter { it.loanId == loan.id }, today) }
        val lastTire = i.tireChecks.maxWithOrNull(compareBy({ it.date }, { it.minuteOfDay }))
        val openDamages = i.damages.filter { !it.repaired }
        val health = HealthScoreCalculator.compute(
            itv, insurance, statuses, lastTire?.date, i.settings.pressureReminderDays,
            i.damages, odometer.anomalies.size, today,
        )
        val costs = CostAnalyzer.entries(i.refuels, i.maintenance.map { it.record }, i.expenses, i.itv, i.insurance, i.damages, emptyList())
        val thisMonth = YearMonth.from(today)
        val monthSpend = costs.filter { YearMonth.from(it.date) == thisMonth }.sumOf { it.amount }
        val lastMonthSpend = costs.filter { YearMonth.from(it.date) == thisMonth.minusMonths(1) }.sumOf { it.amount }
        return VehicleOverview(
            vehicle = i.vehicle,
            odometer = odometer,
            currentKm = currentKm,
            fuel = FuelCalculator.compute(i.refuels),
            components = statuses,
            itv = itv,
            lastItv = i.itv.maxWithOrNull(compareBy({ it.date }, { it.id })),
            insurance = insurance,
            policy = policy,
            debts = debts,
            health = health,
            lastTireCheck = lastTire,
            openDamages = openDamages,
            pinnedNotes = i.notes.filter { it.pinned }.take(3),
            dueReminders = i.notes.filter { n -> !n.reminderDone && n.reminderDate?.let { !it.isAfter(today) } == true },
            monthSpend = monthSpend,
            lastMonthSpend = lastMonthSpend,
            settings = i.settings,
            today = today,
        )
    }
}

@Singleton
class OverviewRepository @Inject constructor(
    private val vehicleDao: VehicleDao,
    private val odometerDao: OdometerDao,
    private val refuelDao: RefuelDao,
    private val maintenanceDao: MaintenanceDao,
    private val itvDao: ItvDao,
    private val insuranceDao: InsuranceDao,
    private val loanDao: LoanDao,
    private val tireDao: TirePressureDao,
    private val damageDao: DamageDao,
    private val noteDao: NoteDao,
    private val expenseDao: ExpenseDao,
    private val settings: SettingsRepository,
) {
    @Suppress("UNCHECKED_CAST")
    fun observe(vehicleId: Long): Flow<VehicleOverview?> = combine(
        listOf(
            vehicleDao.observe(vehicleId),
            odometerDao.observeEvents(vehicleId),
            refuelDao.observe(vehicleId),
            maintenanceDao.observeComponents(vehicleId),
            maintenanceDao.observeInstallations(vehicleId),
            itvDao.observe(vehicleId),
            insuranceDao.observe(vehicleId),
            loanDao.observeLoans(vehicleId),
            loanDao.observePaymentsForVehicle(vehicleId),
            tireDao.observe(vehicleId),
            damageDao.observe(vehicleId),
            noteDao.observe(vehicleId),
            expenseDao.observe(vehicleId),
            maintenanceDao.observeRecords(vehicleId),
            settings.settings,
        ),
    ) { a ->
        val vehicle = a[0] as VehicleEntity? ?: return@combine null
        OverviewBuilder.build(
            OverviewInputs(
                vehicle = vehicle,
                events = a[1] as List<OdometerEvent>,
                refuels = a[2] as List<RefuelEntity>,
                components = a[3] as List<ComponentEntity>,
                installations = a[4] as List<PartInstallationEntity>,
                itv = a[5] as List<ItvInspectionEntity>,
                insurance = a[6] as List<InsurancePolicyEntity>,
                loans = a[7] as List<LoanEntity>,
                payments = a[8] as List<LoanPaymentEntity>,
                tireChecks = a[9] as List<TirePressureEntity>,
                damages = a[10] as List<DamageEntity>,
                notes = a[11] as List<NoteEntity>,
                expenses = a[12] as List<ExpenseEntity>,
                maintenance = a[13] as List<MaintenanceWithDetails>,
                settings = a[14] as AppSettings,
            ),
        )
    }

    suspend fun snapshot(vehicleId: Long): VehicleOverview? = observe(vehicleId).first()
}
