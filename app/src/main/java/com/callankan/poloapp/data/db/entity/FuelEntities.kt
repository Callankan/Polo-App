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

@Serializable
@Entity(
    tableName = "refuels",
    foreignKeys = [ForeignKey(VehicleEntity::class, ["id"], ["vehicleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("vehicleId")],
)
data class RefuelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val date: LocalDate,
    val odometerKm: Int,
    val liters: Double,
    val totalCost: Double,
    val pricePerLiter: Double,
    /** Depósito lleno: necesario para calcular el consumo real (método lleno a lleno). */
    val fullTank: Boolean = true,
    /** El usuario olvidó registrar el repostaje anterior: invalida el tramo actual. */
    val missedPrevious: Boolean = false,
    val fuelGrade: String = "",
    val station: String = "",
    val notes: String = "",
)
