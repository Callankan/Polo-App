package com.callankan.poloapp.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import androidx.room.Embedded
import androidx.room.Relation
import androidx.room.Transaction
import com.callankan.poloapp.data.db.entity.ComponentEntity
import com.callankan.poloapp.data.db.entity.MaintenanceEntity
import com.callankan.poloapp.data.db.entity.PartInstallationEntity
import com.callankan.poloapp.data.db.entity.WorkshopEntity

data class InstallationWithComponent(
    @Embedded val installation: PartInstallationEntity,
    @Relation(parentColumn = "componentId", entityColumn = "id")
    val component: ComponentEntity,
)

data class MaintenanceWithDetails(
    @Embedded val record: MaintenanceEntity,
    @Relation(parentColumn = "workshopId", entityColumn = "id")
    val workshop: WorkshopEntity?,
    @Relation(entity = PartInstallationEntity::class, parentColumn = "id", entityColumn = "maintenanceId")
    val installations: List<InstallationWithComponent>,
)

@Dao
interface MaintenanceDao {
    @Transaction
    @Query("SELECT * FROM maintenance_records WHERE vehicleId = :vehicleId ORDER BY date DESC, odometerKm DESC, id DESC")
    fun observeRecords(vehicleId: Long): Flow<List<MaintenanceWithDetails>>

    @Transaction
    @Query("SELECT * FROM maintenance_records WHERE vehicleId = :vehicleId ORDER BY date DESC, odometerKm DESC, id DESC")
    suspend fun getRecords(vehicleId: Long): List<MaintenanceWithDetails>

    @Transaction
    @Query("SELECT * FROM maintenance_records WHERE id = :id")
    fun observeRecord(id: Long): Flow<MaintenanceWithDetails?>

    @Transaction
    @Query("SELECT * FROM maintenance_records WHERE id = :id")
    suspend fun getRecord(id: Long): MaintenanceWithDetails?

    @Insert
    suspend fun insertRecord(record: MaintenanceEntity): Long

    @Update
    suspend fun updateRecord(record: MaintenanceEntity)

    @Query("DELETE FROM maintenance_records WHERE id = :id")
    suspend fun deleteRecord(id: Long)

    // Piezas / tareas del plan
    @Query("SELECT * FROM components WHERE vehicleId = :vehicleId ORDER BY archived, sortOrder, name")
    fun observeComponents(vehicleId: Long): Flow<List<ComponentEntity>>

    @Query("SELECT * FROM components WHERE vehicleId = :vehicleId ORDER BY archived, sortOrder, name")
    suspend fun getComponents(vehicleId: Long): List<ComponentEntity>

    @Query("SELECT * FROM components WHERE id = :id")
    suspend fun getComponent(id: Long): ComponentEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM components WHERE vehicleId = :vehicleId")
    suspend fun maxSortOrder(vehicleId: Long): Int

    @Insert
    suspend fun insertComponent(component: ComponentEntity): Long

    @Insert
    suspend fun insertComponents(components: List<ComponentEntity>)

    @Update
    suspend fun updateComponent(component: ComponentEntity)

    @Delete
    suspend fun deleteComponent(component: ComponentEntity)

    // Instalaciones
    @Query("SELECT * FROM part_installations WHERE vehicleId = :vehicleId ORDER BY odometerKm, date, id")
    fun observeInstallations(vehicleId: Long): Flow<List<PartInstallationEntity>>

    @Query("SELECT * FROM part_installations WHERE vehicleId = :vehicleId ORDER BY odometerKm, date, id")
    suspend fun getInstallations(vehicleId: Long): List<PartInstallationEntity>

    @Query("SELECT * FROM part_installations WHERE maintenanceId = :maintenanceId")
    suspend fun installationsFor(maintenanceId: Long): List<PartInstallationEntity>

    @Query("SELECT * FROM part_installations WHERE id = :id")
    suspend fun getInstallation(id: Long): PartInstallationEntity?

    @Insert
    suspend fun insertInstallation(installation: PartInstallationEntity): Long

    @Update
    suspend fun updateInstallation(installation: PartInstallationEntity)

    @Delete
    suspend fun deleteInstallation(installation: PartInstallationEntity)

    @Query("DELETE FROM part_installations WHERE maintenanceId = :maintenanceId")
    suspend fun deleteInstallationsFor(maintenanceId: Long)

    // Talleres
    @Query("SELECT * FROM workshops ORDER BY name COLLATE NOCASE")
    fun observeWorkshops(): Flow<List<WorkshopEntity>>

    @Query("SELECT * FROM workshops WHERE id = :id")
    suspend fun getWorkshop(id: Long): WorkshopEntity?

    @Query("SELECT COUNT(*) FROM maintenance_records WHERE workshopId = :workshopId")
    fun observeWorkshopUsage(workshopId: Long): Flow<Int>

    @Insert
    suspend fun insertWorkshop(workshop: WorkshopEntity): Long

    @Update
    suspend fun updateWorkshop(workshop: WorkshopEntity)

    @Delete
    suspend fun deleteWorkshop(workshop: WorkshopEntity)
}
