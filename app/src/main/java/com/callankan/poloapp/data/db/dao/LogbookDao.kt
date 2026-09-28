package com.callankan.poloapp.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import com.callankan.poloapp.data.db.entity.DamageEntity
import com.callankan.poloapp.data.db.entity.NoteEntity
import com.callankan.poloapp.data.db.entity.TirePressureEntity
import com.callankan.poloapp.data.db.entity.TripEntity

@Dao
interface TripDao {
    @Query("SELECT * FROM trips WHERE vehicleId = :vehicleId ORDER BY date DESC, id DESC")
    fun observe(vehicleId: Long): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun get(id: Long): TripEntity?

    @Insert
    suspend fun insert(trip: TripEntity): Long

    @Update
    suspend fun update(trip: TripEntity)

    @Delete
    suspend fun delete(trip: TripEntity)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE vehicleId = :vehicleId ORDER BY pinned DESC, updatedAt DESC")
    fun observe(vehicleId: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE reminderDate IS NOT NULL AND reminderDone = 0")
    suspend fun pendingReminders(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun get(id: Long): NoteEntity?

    @Insert
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)
}

@Dao
interface TirePressureDao {
    @Query("SELECT * FROM tire_pressure_checks WHERE vehicleId = :vehicleId ORDER BY date DESC, minuteOfDay DESC, id DESC")
    fun observe(vehicleId: Long): Flow<List<TirePressureEntity>>

    @Query("SELECT * FROM tire_pressure_checks WHERE id = :id")
    suspend fun get(id: Long): TirePressureEntity?

    @Insert
    suspend fun insert(check: TirePressureEntity): Long

    @Update
    suspend fun update(check: TirePressureEntity)

    @Delete
    suspend fun delete(check: TirePressureEntity)
}

@Dao
interface DamageDao {
    @Query("SELECT * FROM damages WHERE vehicleId = :vehicleId ORDER BY repaired, date DESC, id DESC")
    fun observe(vehicleId: Long): Flow<List<DamageEntity>>

    @Query("SELECT * FROM damages WHERE vehicleId = :vehicleId ORDER BY date DESC, id DESC")
    suspend fun getAll(vehicleId: Long): List<DamageEntity>

    @Query("SELECT * FROM damages WHERE id = :id")
    suspend fun get(id: Long): DamageEntity?

    @Insert
    suspend fun insert(damage: DamageEntity): Long

    @Update
    suspend fun update(damage: DamageEntity)

    @Delete
    suspend fun delete(damage: DamageEntity)
}
