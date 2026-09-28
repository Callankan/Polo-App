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
import com.callankan.poloapp.data.model.ExpenseCategory

/** Préstamo sin intereses (p. ej. familiar) asociado al coche. */
@Serializable
@Entity(
    tableName = "loans",
    foreignKeys = [ForeignKey(VehicleEntity::class, ["id"], ["vehicleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("vehicleId")],
)
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val lender: String,
    val initialAmount: Double,
    val startDate: LocalDate,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "loan_payments",
    foreignKeys = [ForeignKey(LoanEntity::class, ["id"], ["loanId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("loanId")],
)
data class LoanPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,
    val date: LocalDate,
    val amount: Double,
    val note: String = "",
)

@Serializable
@Entity(
    tableName = "expenses",
    foreignKeys = [ForeignKey(VehicleEntity::class, ["id"], ["vehicleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("vehicleId")],
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val date: LocalDate,
    val category: ExpenseCategory,
    val amount: Double,
    val description: String = "",
    val odometerKm: Int? = null,
    val notes: String = "",
)
