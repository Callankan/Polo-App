package com.callankan.poloapp.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.callankan.poloapp.data.db.dao.BackupDao
import com.callankan.poloapp.data.db.dao.DamageDao
import com.callankan.poloapp.data.db.dao.DocumentDao
import com.callankan.poloapp.data.db.dao.AttachmentDao
import com.callankan.poloapp.data.db.dao.ExpenseDao
import com.callankan.poloapp.data.db.dao.InsuranceDao
import com.callankan.poloapp.data.db.dao.ItvDao
import com.callankan.poloapp.data.db.dao.LoanDao
import com.callankan.poloapp.data.db.dao.MaintenanceDao
import com.callankan.poloapp.data.db.dao.NoteDao
import com.callankan.poloapp.data.db.dao.OdometerDao
import com.callankan.poloapp.data.db.dao.RefuelDao
import com.callankan.poloapp.data.db.dao.SpecDao
import com.callankan.poloapp.data.db.dao.TirePressureDao
import com.callankan.poloapp.data.db.dao.TripDao
import com.callankan.poloapp.data.db.dao.VehicleDao
import com.callankan.poloapp.data.db.entity.OdometerEvent
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

@Database(
    entities = [
        VehicleEntity::class,
        OdometerEntryEntity::class,
        SpecEntity::class,
        RefuelEntity::class,
        WorkshopEntity::class,
        MaintenanceEntity::class,
        ComponentEntity::class,
        PartInstallationEntity::class,
        LoanEntity::class,
        LoanPaymentEntity::class,
        ExpenseEntity::class,
        ItvInspectionEntity::class,
        InsurancePolicyEntity::class,
        TripEntity::class,
        NoteEntity::class,
        TirePressureEntity::class,
        DamageEntity::class,
        DocumentEntity::class,
        AttachmentEntity::class,
    ],
    views = [OdometerEvent::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class PoloDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun odometerDao(): OdometerDao
    abstract fun specDao(): SpecDao
    abstract fun refuelDao(): RefuelDao
    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun loanDao(): LoanDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun itvDao(): ItvDao
    abstract fun insuranceDao(): InsuranceDao
    abstract fun tripDao(): TripDao
    abstract fun noteDao(): NoteDao
    abstract fun tirePressureDao(): TirePressureDao
    abstract fun damageDao(): DamageDao
    abstract fun documentDao(): DocumentDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun backupDao(): BackupDao

    companion object {
        const val NAME = "polo.db"
    }
}
