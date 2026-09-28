package com.callankan.poloapp.data.repository

import com.callankan.poloapp.data.db.dao.VehicleDao
import com.callankan.poloapp.data.db.entity.VehicleEntity
import com.callankan.poloapp.data.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject
import javax.inject.Singleton

/** El vehículo seleccionado: todas las pantallas trabajan sobre él. */
@Singleton
class ActiveVehicle @Inject constructor(
    settings: SettingsRepository,
    private val vehicleDao: VehicleDao,
) {
    val id: Flow<Long> = settings.selectedVehicleId.filterNotNull()
    val vehicle: Flow<VehicleEntity> = id.flatMapLatest { vehicleDao.observe(it) }.filterNotNull()

    suspend fun currentId(): Long = id.first()
    suspend fun current(): VehicleEntity = vehicle.first()
}
