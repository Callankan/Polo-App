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
import com.callankan.poloapp.data.model.MaintenanceCategory
import com.callankan.poloapp.data.model.PerformedBy

@Serializable
@Entity(tableName = "workshops")
data class WorkshopEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val notes: String = "",
    /** Valoración personal de 0 a 5. */
    val rating: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Intervención: mantenimiento, reparación o avería. */
@Serializable
@Entity(
    tableName = "maintenance_records",
    foreignKeys = [
        ForeignKey(VehicleEntity::class, ["id"], ["vehicleId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(WorkshopEntity::class, ["id"], ["workshopId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("vehicleId"), Index("workshopId")],
)
data class MaintenanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val date: LocalDate,
    val odometerKm: Int,
    val title: String,
    val category: MaintenanceCategory,
    val performedBy: PerformedBy,
    val workshopId: Long? = null,
    val partsCost: Double? = null,
    val laborCost: Double? = null,
    val totalCost: Double = 0.0,
    val invoiceNumber: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * Pieza o tarea con seguimiento de vida útil (p. ej. "Aceite de motor", "Pastillas delanteras").
 * Los intervalos alimentan las alertas por km y/o tiempo.
 */
@Serializable
@Entity(
    tableName = "components",
    foreignKeys = [ForeignKey(VehicleEntity::class, ["id"], ["vehicleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("vehicleId")],
)
data class ComponentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val name: String,
    val category: MaintenanceCategory,
    val intervalKm: Int? = null,
    val intervalMonths: Int? = null,
    val alertsEnabled: Boolean = true,
    val hint: String = "",
    val sortOrder: Int = 0,
    val archived: Boolean = false,
)

/** Instalación de una pieza: km y fecha exactos. La siguiente instalación marca su retirada. */
@Serializable
@Entity(
    tableName = "part_installations",
    foreignKeys = [
        ForeignKey(VehicleEntity::class, ["id"], ["vehicleId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(ComponentEntity::class, ["id"], ["componentId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(MaintenanceEntity::class, ["id"], ["maintenanceId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("vehicleId"), Index("componentId"), Index("maintenanceId")],
)
data class PartInstallationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val componentId: Long,
    val maintenanceId: Long? = null,
    val date: LocalDate,
    val odometerKm: Int,
    val brand: String = "",
    val reference: String = "",
    val cost: Double? = null,
    val notes: String = "",
)
