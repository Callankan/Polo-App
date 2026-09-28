package com.callankan.poloapp.data.repository

import androidx.room.withTransaction
import com.callankan.poloapp.data.db.PoloDatabase
import com.callankan.poloapp.data.db.dao.MaintenanceDao
import com.callankan.poloapp.data.db.dao.MaintenanceWithDetails
import com.callankan.poloapp.data.db.entity.ComponentEntity
import com.callankan.poloapp.data.db.entity.MaintenanceEntity
import com.callankan.poloapp.data.db.entity.PartInstallationEntity
import com.callankan.poloapp.data.db.entity.WorkshopEntity
import com.callankan.poloapp.data.model.AttachmentOwner
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Pieza cambiada en una intervención (marca/referencia y coste opcionales). */
data class InstallDraft(
    val componentId: Long,
    val brand: String = "",
    val reference: String = "",
    val cost: Double? = null,
)

@Singleton
class MaintenanceRepository @Inject constructor(
    private val db: PoloDatabase,
    private val dao: MaintenanceDao,
    private val attachments: AttachmentRepository,
) {
    fun observeRecords(vehicleId: Long): Flow<List<MaintenanceWithDetails>> = dao.observeRecords(vehicleId)
    suspend fun records(vehicleId: Long): List<MaintenanceWithDetails> = dao.getRecords(vehicleId)
    fun observeRecord(id: Long): Flow<MaintenanceWithDetails?> = dao.observeRecord(id)
    suspend fun getRecord(id: Long): MaintenanceWithDetails? = dao.getRecord(id)

    /** Guarda la intervención y regenera sus instalaciones de piezas en una única transacción. */
    suspend fun saveRecord(record: MaintenanceEntity, parts: List<InstallDraft>): Long = db.withTransaction {
        val id = if (record.id == 0L) dao.insertRecord(record) else record.id.also { dao.updateRecord(record) }
        dao.deleteInstallationsFor(id)
        parts.forEach { p ->
            dao.insertInstallation(
                PartInstallationEntity(
                    vehicleId = record.vehicleId, componentId = p.componentId, maintenanceId = id,
                    date = record.date, odometerKm = record.odometerKm,
                    brand = p.brand, reference = p.reference, cost = p.cost,
                ),
            )
        }
        id
    }

    suspend fun deleteRecord(id: Long) {
        attachments.deleteAllFor(AttachmentOwner.MAINTENANCE, id)
        dao.deleteRecord(id)
    }

    fun observeComponents(vehicleId: Long): Flow<List<ComponentEntity>> = dao.observeComponents(vehicleId)
    suspend fun components(vehicleId: Long): List<ComponentEntity> = dao.getComponents(vehicleId)
    suspend fun getComponent(id: Long): ComponentEntity? = dao.getComponent(id)
    suspend fun saveComponent(component: ComponentEntity): Long =
        if (component.id == 0L) {
            dao.insertComponent(component.copy(sortOrder = dao.maxSortOrder(component.vehicleId) + 1))
        } else component.id.also { dao.updateComponent(component) }
    suspend fun deleteComponent(component: ComponentEntity) = dao.deleteComponent(component)

    fun observeInstallations(vehicleId: Long): Flow<List<PartInstallationEntity>> = dao.observeInstallations(vehicleId)
    suspend fun installations(vehicleId: Long): List<PartInstallationEntity> = dao.getInstallations(vehicleId)
    suspend fun getInstallation(id: Long) = dao.getInstallation(id)
    suspend fun saveInstallation(installation: PartInstallationEntity): Long =
        if (installation.id == 0L) dao.insertInstallation(installation) else installation.id.also { dao.updateInstallation(installation) }
    suspend fun deleteInstallation(installation: PartInstallationEntity) = dao.deleteInstallation(installation)

    fun observeWorkshops(): Flow<List<WorkshopEntity>> = dao.observeWorkshops()
    fun observeWorkshopUsage(id: Long): Flow<Int> = dao.observeWorkshopUsage(id)
    suspend fun getWorkshop(id: Long) = dao.getWorkshop(id)
    suspend fun saveWorkshop(workshop: WorkshopEntity): Long =
        if (workshop.id == 0L) dao.insertWorkshop(workshop) else workshop.id.also { dao.updateWorkshop(workshop) }
    suspend fun deleteWorkshop(workshop: WorkshopEntity) = dao.deleteWorkshop(workshop)
}
