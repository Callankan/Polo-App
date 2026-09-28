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
import com.callankan.poloapp.data.model.CarZone
import com.callankan.poloapp.data.model.DamageSeverity

@Serializable
@Entity(
    tableName = "trips",
    foreignKeys = [ForeignKey(VehicleEntity::class, ["id"], ["vehicleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("vehicleId")],
)
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val date: LocalDate,
    val origin: String,
    val destination: String,
    val startKm: Int? = null,
    val endKm: Int? = null,
    val distanceKm: Int,
    val purpose: String = "",
    val tolls: Double? = null,
    val notes: String = "",
)

@Serializable
@Entity(
    tableName = "notes",
    foreignKeys = [ForeignKey(VehicleEntity::class, ["id"], ["vehicleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("vehicleId")],
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val title: String,
    val body: String = "",
    val pinned: Boolean = false,
    val reminderDate: LocalDate? = null,
    val reminderDone: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

/** Comprobación de presiones: se guarda la fecha y hora en que se hizo. */
@Serializable
@Entity(
    tableName = "tire_pressure_checks",
    foreignKeys = [ForeignKey(VehicleEntity::class, ["id"], ["vehicleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("vehicleId")],
)
data class TirePressureEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val date: LocalDate,
    /** Minuto del día (0-1439) en que se hizo la comprobación. */
    val minuteOfDay: Int = 0,
    val frontLeft: Double,
    val frontRight: Double,
    val rearLeft: Double,
    val rearRight: Double,
    val odometerKm: Int? = null,
    val notes: String = "",
)

@Serializable
@Entity(
    tableName = "damages",
    foreignKeys = [
        ForeignKey(VehicleEntity::class, ["id"], ["vehicleId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(WorkshopEntity::class, ["id"], ["workshopId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("vehicleId"), Index("workshopId")],
)
data class DamageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val date: LocalDate,
    val odometerKm: Int? = null,
    val title: String,
    val description: String = "",
    val zone: CarZone,
    val severity: DamageSeverity = DamageSeverity.MINOR,
    val repaired: Boolean = false,
    val repairedDate: LocalDate? = null,
    val repairCost: Double? = null,
    val workshopId: Long? = null,
    val insuranceClaim: Boolean = false,
)
