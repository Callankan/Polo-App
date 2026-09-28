package com.callankan.poloapp.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.callankan.poloapp.data.db.entity.AttachmentEntity
import com.callankan.poloapp.data.db.entity.ComponentEntity
import com.callankan.poloapp.data.db.entity.DamageEntity
import com.callankan.poloapp.data.db.entity.DocumentEntity
import com.callankan.poloapp.data.db.entity.ExpenseEntity
import com.callankan.poloapp.data.db.entity.InsurancePolicyEntity
import com.callankan.poloapp.data.db.entity.ItvInspectionEntity
import com.callankan.poloapp.data.db.entity.LoanEntity
import com.callankan.poloapp.data.db.entity.LoanPaymentEntity
import com.callankan.poloapp.data.db.entity.MaintenanceEntity
import com.callankan.poloapp.data.db.entity.NoteEntity
import com.callankan.poloapp.data.db.entity.OdometerEntryEntity
import com.callankan.poloapp.data.db.entity.PartInstallationEntity
import com.callankan.poloapp.data.db.entity.RefuelEntity
import com.callankan.poloapp.data.db.entity.SpecEntity
import com.callankan.poloapp.data.db.entity.TirePressureEntity
import com.callankan.poloapp.data.db.entity.TripEntity
import com.callankan.poloapp.data.db.entity.VehicleEntity
import com.callankan.poloapp.data.db.entity.WorkshopEntity

/** Acceso en bloque a todas las tablas para la copia de seguridad. */
@Suppress("FunctionName")
@Dao
interface BackupDao {
    @Query("SELECT * FROM vehicles")
    suspend fun allVehicle(): List<VehicleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(items: List<VehicleEntity>)

    @Query("SELECT * FROM odometer_entries")
    suspend fun allOdometerEntry(): List<OdometerEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOdometerEntry(items: List<OdometerEntryEntity>)

    @Query("SELECT * FROM vehicle_specs")
    suspend fun allSpec(): List<SpecEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpec(items: List<SpecEntity>)

    @Query("SELECT * FROM refuels")
    suspend fun allRefuel(): List<RefuelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRefuel(items: List<RefuelEntity>)

    @Query("SELECT * FROM workshops")
    suspend fun allWorkshop(): List<WorkshopEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkshop(items: List<WorkshopEntity>)

    @Query("SELECT * FROM maintenance_records")
    suspend fun allMaintenance(): List<MaintenanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaintenance(items: List<MaintenanceEntity>)

    @Query("SELECT * FROM components")
    suspend fun allComponent(): List<ComponentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComponent(items: List<ComponentEntity>)

    @Query("SELECT * FROM part_installations")
    suspend fun allPartInstallation(): List<PartInstallationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPartInstallation(items: List<PartInstallationEntity>)

    @Query("SELECT * FROM loans")
    suspend fun allLoan(): List<LoanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(items: List<LoanEntity>)

    @Query("SELECT * FROM loan_payments")
    suspend fun allLoanPayment(): List<LoanPaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoanPayment(items: List<LoanPaymentEntity>)

    @Query("SELECT * FROM expenses")
    suspend fun allExpense(): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(items: List<ExpenseEntity>)

    @Query("SELECT * FROM itv_inspections")
    suspend fun allItvInspection(): List<ItvInspectionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItvInspection(items: List<ItvInspectionEntity>)

    @Query("SELECT * FROM insurance_policies")
    suspend fun allInsurancePolicy(): List<InsurancePolicyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsurancePolicy(items: List<InsurancePolicyEntity>)

    @Query("SELECT * FROM trips")
    suspend fun allTrip(): List<TripEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(items: List<TripEntity>)

    @Query("SELECT * FROM notes")
    suspend fun allNote(): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(items: List<NoteEntity>)

    @Query("SELECT * FROM tire_pressure_checks")
    suspend fun allTirePressure(): List<TirePressureEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTirePressure(items: List<TirePressureEntity>)

    @Query("SELECT * FROM damages")
    suspend fun allDamage(): List<DamageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDamage(items: List<DamageEntity>)

    @Query("SELECT * FROM documents")
    suspend fun allDocument(): List<DocumentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(items: List<DocumentEntity>)

    @Query("SELECT * FROM attachments")
    suspend fun allAttachment(): List<AttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(items: List<AttachmentEntity>)

    @Query("DELETE FROM attachments")
    suspend fun clear_attachments()

    @Query("DELETE FROM part_installations")
    suspend fun clear_part_installations()

    @Query("DELETE FROM maintenance_records")
    suspend fun clear_maintenance_records()

    @Query("DELETE FROM components")
    suspend fun clear_components()

    @Query("DELETE FROM workshops")
    suspend fun clear_workshops()

    @Query("DELETE FROM loan_payments")
    suspend fun clear_loan_payments()

    @Query("DELETE FROM loans")
    suspend fun clear_loans()

    @Query("DELETE FROM expenses")
    suspend fun clear_expenses()

    @Query("DELETE FROM refuels")
    suspend fun clear_refuels()

    @Query("DELETE FROM odometer_entries")
    suspend fun clear_odometer_entries()

    @Query("DELETE FROM vehicle_specs")
    suspend fun clear_vehicle_specs()

    @Query("DELETE FROM itv_inspections")
    suspend fun clear_itv_inspections()

    @Query("DELETE FROM insurance_policies")
    suspend fun clear_insurance_policies()

    @Query("DELETE FROM trips")
    suspend fun clear_trips()

    @Query("DELETE FROM notes")
    suspend fun clear_notes()

    @Query("DELETE FROM tire_pressure_checks")
    suspend fun clear_tire_pressure_checks()

    @Query("DELETE FROM damages")
    suspend fun clear_damages()

    @Query("DELETE FROM documents")
    suspend fun clear_documents()

    @Query("DELETE FROM vehicles")
    suspend fun clear_vehicles()

    suspend fun clearAll() {
        clear_attachments()
        clear_part_installations()
        clear_maintenance_records()
        clear_components()
        clear_workshops()
        clear_loan_payments()
        clear_loans()
        clear_expenses()
        clear_refuels()
        clear_odometer_entries()
        clear_vehicle_specs()
        clear_itv_inspections()
        clear_insurance_policies()
        clear_trips()
        clear_notes()
        clear_tire_pressure_checks()
        clear_damages()
        clear_documents()
        clear_vehicles()
    }
}
