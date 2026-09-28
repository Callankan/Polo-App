package com.callankan.poloapp.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import com.callankan.poloapp.data.db.entity.RefuelEntity

@Dao
interface RefuelDao {
    @Query("SELECT * FROM refuels WHERE vehicleId = :vehicleId ORDER BY odometerKm DESC, date DESC, id DESC")
    fun observe(vehicleId: Long): Flow<List<RefuelEntity>>

    @Query("SELECT * FROM refuels WHERE vehicleId = :vehicleId ORDER BY odometerKm DESC, date DESC, id DESC")
    suspend fun getAll(vehicleId: Long): List<RefuelEntity>

    @Query("SELECT * FROM refuels WHERE id = :id")
    suspend fun get(id: Long): RefuelEntity?

    @Query("SELECT * FROM refuels WHERE vehicleId = :vehicleId ORDER BY odometerKm DESC, id DESC LIMIT 1")
    suspend fun last(vehicleId: Long): RefuelEntity?

    @Query("SELECT DISTINCT station FROM refuels WHERE vehicleId = :vehicleId AND station != '' ORDER BY date DESC LIMIT 12")
    fun observeRecentStations(vehicleId: Long): Flow<List<String>>

    @Insert
    suspend fun insert(refuel: RefuelEntity): Long

    @Update
    suspend fun update(refuel: RefuelEntity)

    @Delete
    suspend fun delete(refuel: RefuelEntity)
}
