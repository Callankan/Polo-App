package com.callankan.poloapp.data.repository

import androidx.room.withTransaction
import com.callankan.poloapp.data.db.PoloDatabase
import com.callankan.poloapp.data.db.dao.MaintenanceDao
import com.callankan.poloapp.data.db.dao.OdometerDao
import com.callankan.poloapp.data.db.dao.SpecDao
import com.callankan.poloapp.data.db.dao.VehicleDao
import com.callankan.poloapp.data.db.entity.ComponentEntity
import com.callankan.poloapp.data.db.entity.OdometerEntryEntity
import com.callankan.poloapp.data.db.entity.OdometerEvent
import com.callankan.poloapp.data.db.entity.SpecEntity
import com.callankan.poloapp.data.db.entity.VehicleEntity
import com.callankan.poloapp.data.preset.ComponentPreset
import com.callankan.poloapp.data.preset.Presets
import com.callankan.poloapp.data.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VehicleRepository @Inject constructor(
    private val db: PoloDatabase,
    private val vehicleDao: VehicleDao,
    private val odometerDao: OdometerDao,
    private val specDao: SpecDao,
    private val maintenanceDao: MaintenanceDao,
    private val settings: SettingsRepository,
    private val attachments: AttachmentRepository,
) {
    fun observeAll(): Flow<List<VehicleEntity>> = vehicleDao.observeAll()
    fun observe(id: Long): Flow<VehicleEntity?> = vehicleDao.observe(id)
    suspend fun get(id: Long): VehicleEntity? = vehicleDao.get(id)
    suspend fun getAll(): List<VehicleEntity> = vehicleDao.getAll()

    /** Alta de un vehículo con su plan de mantenimiento, ficha rápida y lectura inicial de km. */
    suspend fun create(
        vehicle: VehicleEntity,
        currentKm: Int?,
        plan: List<ComponentPreset>,
    ): Long {
        val id = db.withTransaction {
            val id = vehicleDao.insert(vehicle)
            maintenanceDao.insertComponents(
                plan.mapIndexed { i, p ->
                    ComponentEntity(
                        vehicleId = id, name = p.name, category = p.category,
                        intervalKm = p.intervalKm, intervalMonths = p.intervalMonths,
                        hint = p.hint, sortOrder = i,
                    )
                },
            )
            specDao.insertAll(
                Presets.specTemplate.mapIndexed { i, (section, label, hint) ->
                    SpecEntity(vehicleId = id, section = section, label = label, hint = hint, value = prefill(vehicle, label), sortOrder = i)
                },
            )
            if (currentKm != null && currentKm != vehicle.purchaseKm) {
                odometerDao.insert(OdometerEntryEntity(vehicleId = id, date = LocalDate.now(), km = currentKm, note = "Lectura inicial"))
            }
            id
        }
        settings.selectVehicle(id)
        return id
    }

    private fun prefill(v: VehicleEntity, label: String): String = when (label) {
        "Código de motor" -> v.engineCode
        "Combustible" -> v.fuelType.label
        "Capacidad del depósito" -> v.tankCapacityLiters?.let { "${it.toInt()} l" } ?: ""
        "Presión delantera (bar)" -> v.tireFrontBar?.toString()?.replace('.', ',') ?: ""
        "Presión trasera (bar)" -> v.tireRearBar?.toString()?.replace('.', ',') ?: ""
        else -> ""
    }

    suspend fun update(vehicle: VehicleEntity) = vehicleDao.update(vehicle)

    suspend fun delete(vehicle: VehicleEntity) {
        vehicleDao.delete(vehicle)
        attachments.purgeOrphans()
        val next = vehicleDao.getAll().firstOrNull()
        settings.selectVehicle(next?.id)
    }

    suspend fun select(id: Long) = settings.selectVehicle(id)

    // Cuentakilómetros
    fun observeOdometerEvents(vehicleId: Long): Flow<List<OdometerEvent>> = odometerDao.observeEvents(vehicleId)
    suspend fun odometerEvents(vehicleId: Long): List<OdometerEvent> = odometerDao.getEvents(vehicleId)
    fun observeOdometerEntries(vehicleId: Long): Flow<List<OdometerEntryEntity>> = odometerDao.observeEntries(vehicleId)
    suspend fun getOdometerEntry(id: Long) = odometerDao.getEntry(id)
    suspend fun saveOdometerEntry(entry: OdometerEntryEntity): Long =
        if (entry.id == 0L) odometerDao.insert(entry) else entry.id.also { odometerDao.update(entry) }
    suspend fun deleteOdometerEntry(entry: OdometerEntryEntity) = odometerDao.delete(entry)

    // Ficha técnica
    fun observeSpecs(vehicleId: Long): Flow<List<SpecEntity>> = specDao.observe(vehicleId)
    suspend fun specs(vehicleId: Long): List<SpecEntity> = specDao.get(vehicleId)
    suspend fun saveSpec(spec: SpecEntity): Long = if (spec.id == 0L) specDao.insert(spec) else spec.id.also { specDao.update(spec) }
    suspend fun deleteSpec(spec: SpecEntity) = specDao.delete(spec)
}
