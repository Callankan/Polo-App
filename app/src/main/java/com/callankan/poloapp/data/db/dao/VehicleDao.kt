package com.callankan.poloapp.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import com.callankan.poloapp.data.db.entity.OdometerEntryEntity
import com.callankan.poloapp.data.db.entity.OdometerEvent
import com.callankan.poloapp.data.db.entity.SpecEntity
import com.callankan.poloapp.data.db.entity.VehicleEntity

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY createdAt")
    fun observeAll(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    fun observe(id: Long): Flow<VehicleEntity?>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun get(id: Long): VehicleEntity?

    @Query("SELECT * FROM vehicles ORDER BY createdAt")
    suspend fun getAll(): List<VehicleEntity>

    @Insert
    suspend fun insert(vehicle: VehicleEntity): Long

    @Update
    suspend fun update(vehicle: VehicleEntity)

    @Delete
    suspend fun delete(vehicle: VehicleEntity)
}

@Dao
interface OdometerDao {
    @Query("SELECT * FROM odometer_events WHERE vehicleId = :vehicleId ORDER BY date, km")
    fun observeEvents(vehicleId: Long): Flow<List<OdometerEvent>>

    @Query("SELECT * FROM odometer_events WHERE vehicleId = :vehicleId ORDER BY date, km")
    suspend fun getEvents(vehicleId: Long): List<OdometerEvent>

    @Query("SELECT * FROM odometer_entries WHERE vehicleId = :vehicleId ORDER BY date DESC, km DESC")
    fun observeEntries(vehicleId: Long): Flow<List<OdometerEntryEntity>>

    @Query("SELECT * FROM odometer_entries WHERE id = :id")
    suspend fun getEntry(id: Long): OdometerEntryEntity?

    @Insert
    suspend fun insert(entry: OdometerEntryEntity): Long

    @Update
    suspend fun update(entry: OdometerEntryEntity)

    @Delete
    suspend fun delete(entry: OdometerEntryEntity)
}

@Dao
interface SpecDao {
    @Query("SELECT * FROM vehicle_specs WHERE vehicleId = :vehicleId ORDER BY sortOrder, id")
    fun observe(vehicleId: Long): Flow<List<SpecEntity>>

    @Query("SELECT * FROM vehicle_specs WHERE vehicleId = :vehicleId ORDER BY sortOrder, id")
    suspend fun get(vehicleId: Long): List<SpecEntity>

    @Insert
    suspend fun insertAll(specs: List<SpecEntity>)

    @Insert
    suspend fun insert(spec: SpecEntity): Long

    @Update
    suspend fun update(spec: SpecEntity)

    @Delete
    suspend fun delete(spec: SpecEntity)
}
