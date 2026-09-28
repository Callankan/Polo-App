@file:UseSerializers(LocalDateSerializer::class)

package com.callankan.poloapp.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.callankan.poloapp.data.db.LocalDateSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.LocalDate
import com.callankan.poloapp.data.model.InsuranceCoverage
import com.callankan.poloapp.data.model.ItvResult

@Serializable
@Entity(
    tableName = "itv_inspections",
    foreignKeys = [ForeignKey(VehicleEntity::class, ["id"], ["vehicleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("vehicleId")],
)
data class ItvInspectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val date: LocalDate,
    val odometerKm: Int? = null,
    val station: String = "",
    val result: ItvResult,
    val defects: String = "",
    val cost: Double? = null,
    val nextDueDate: LocalDate,
    val notes: String = "",
)

@Serializable
@Entity(
    tableName = "insurance_policies",
    foreignKeys = [ForeignKey(VehicleEntity::class, ["id"], ["vehicleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("vehicleId")],
)
data class InsurancePolicyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val company: String,
    val policyNumber: String = "",
    val coverage: InsuranceCoverage = InsuranceCoverage.THIRD_PARTY_EXTENDED,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val premium: Double? = null,
    val assistancePhone: String = "",
    val notes: String = "",
)
