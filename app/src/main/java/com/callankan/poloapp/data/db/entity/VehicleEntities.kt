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
import com.callankan.poloapp.data.model.FuelType
import com.callankan.poloapp.data.model.TimingDrive

@Serializable
@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val alias: String,
    val make: String,
    val model: String,
    val version: String = "",
    val powerCv: Int? = null,
    val engineCode: String = "",
    val fuelType: FuelType = FuelType.GASOLINE,
    val timingDrive: TimingDrive = TimingDrive.UNKNOWN,
    val tankCapacityLiters: Double? = null,
    val registrationDate: LocalDate? = null,
    val purchaseDate: LocalDate? = null,
    val purchaseKm: Int? = null,
    val purchasePrice: Double? = null,
    val plate: String = "",
    val vin: String = "",
    val colorName: String = "",
    val colorArgb: Long = 0xFFD0121E,
    val tireFrontBar: Double? = null,
    val tireRearBar: Double? = null,
    /** Fecha de la próxima ITV introducida a mano cuando aún no hay inspecciones registradas. */
    val manualItvDueDate: LocalDate? = null,
    val ownerName: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
) {
    val displayName: String get() = listOf(make, model).filter { it.isNotBlank() }.joinToString(" ")
}

/** Lectura manual del cuentakilómetros. */
@Serializable
@Entity(
    tableName = "odometer_entries",
    foreignKeys = [ForeignKey(VehicleEntity::class, ["id"], ["vehicleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("vehicleId")],
)
data class OdometerEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val date: LocalDate,
    val km: Int,
    val note: String = "",
)

/** Ficha técnica rápida: pares etiqueta/valor editables agrupados por sección. */
@Serializable
@Entity(
    tableName = "vehicle_specs",
    foreignKeys = [ForeignKey(VehicleEntity::class, ["id"], ["vehicleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("vehicleId")],
)
data class SpecEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val section: String,
    val label: String,
    val value: String = "",
    val hint: String = "",
    val sortOrder: Int = 0,
)
