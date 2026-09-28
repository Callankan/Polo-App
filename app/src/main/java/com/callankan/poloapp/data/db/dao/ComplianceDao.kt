package com.callankan.poloapp.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import com.callankan.poloapp.data.db.entity.InsurancePolicyEntity
import com.callankan.poloapp.data.db.entity.ItvInspectionEntity

@Dao
interface ItvDao {
    @Query("SELECT * FROM itv_inspections WHERE vehicleId = :vehicleId ORDER BY date DESC, id DESC")
    fun observe(vehicleId: Long): Flow<List<ItvInspectionEntity>>

    @Query("SELECT * FROM itv_inspections WHERE vehicleId = :vehicleId ORDER BY date DESC, id DESC")
    suspend fun getAll(vehicleId: Long): List<ItvInspectionEntity>

    @Query("SELECT * FROM itv_inspections WHERE id = :id")
    suspend fun get(id: Long): ItvInspectionEntity?

    @Insert
    suspend fun insert(itv: ItvInspectionEntity): Long

    @Update
    suspend fun update(itv: ItvInspectionEntity)

    @Delete
    suspend fun delete(itv: ItvInspectionEntity)
}

@Dao
interface InsuranceDao {
    @Query("SELECT * FROM insurance_policies WHERE vehicleId = :vehicleId ORDER BY endDate DESC, id DESC")
    fun observe(vehicleId: Long): Flow<List<InsurancePolicyEntity>>

    @Query("SELECT * FROM insurance_policies WHERE vehicleId = :vehicleId ORDER BY endDate DESC, id DESC")
    suspend fun getAll(vehicleId: Long): List<InsurancePolicyEntity>

    @Query("SELECT * FROM insurance_policies WHERE id = :id")
    suspend fun get(id: Long): InsurancePolicyEntity?

    @Insert
    suspend fun insert(policy: InsurancePolicyEntity): Long

    @Update
    suspend fun update(policy: InsurancePolicyEntity)

    @Delete
    suspend fun delete(policy: InsurancePolicyEntity)
}
