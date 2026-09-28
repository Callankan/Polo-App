package com.callankan.poloapp.data.repository

import com.callankan.poloapp.data.db.dao.RefuelDao
import com.callankan.poloapp.data.db.entity.RefuelEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FuelRepository @Inject constructor(private val dao: RefuelDao) {
    fun observe(vehicleId: Long): Flow<List<RefuelEntity>> = dao.observe(vehicleId)
    fun observeRecentStations(vehicleId: Long): Flow<List<String>> = dao.observeRecentStations(vehicleId)
    suspend fun all(vehicleId: Long): List<RefuelEntity> = dao.getAll(vehicleId)
    suspend fun get(id: Long): RefuelEntity? = dao.get(id)
    suspend fun last(vehicleId: Long): RefuelEntity? = dao.last(vehicleId)
    suspend fun save(refuel: RefuelEntity): Long = if (refuel.id == 0L) dao.insert(refuel) else refuel.id.also { dao.update(refuel) }
    suspend fun delete(refuel: RefuelEntity) = dao.delete(refuel)
}
