package com.callankan.poloapp.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import com.callankan.poloapp.data.db.entity.ExpenseEntity
import com.callankan.poloapp.data.db.entity.LoanEntity
import com.callankan.poloapp.data.db.entity.LoanPaymentEntity

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans WHERE vehicleId = :vehicleId ORDER BY startDate, id")
    fun observeLoans(vehicleId: Long): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE vehicleId = :vehicleId ORDER BY startDate, id")
    suspend fun getLoans(vehicleId: Long): List<LoanEntity>

    @Query("SELECT * FROM loans WHERE id = :id")
    suspend fun getLoan(id: Long): LoanEntity?

    @Query("SELECT * FROM loans WHERE id = :id")
    fun observeLoan(id: Long): Flow<LoanEntity?>

    @Insert
    suspend fun insertLoan(loan: LoanEntity): Long

    @Update
    suspend fun updateLoan(loan: LoanEntity)

    @Delete
    suspend fun deleteLoan(loan: LoanEntity)

    @Query("SELECT p.* FROM loan_payments p INNER JOIN loans l ON l.id = p.loanId WHERE l.vehicleId = :vehicleId ORDER BY p.date DESC, p.id DESC")
    fun observePaymentsForVehicle(vehicleId: Long): Flow<List<LoanPaymentEntity>>

    @Query("SELECT * FROM loan_payments WHERE loanId = :loanId ORDER BY date DESC, id DESC")
    fun observePayments(loanId: Long): Flow<List<LoanPaymentEntity>>

    @Query("SELECT * FROM loan_payments WHERE id = :id")
    suspend fun getPayment(id: Long): LoanPaymentEntity?

    @Insert
    suspend fun insertPayment(payment: LoanPaymentEntity): Long

    @Update
    suspend fun updatePayment(payment: LoanPaymentEntity)

    @Delete
    suspend fun deletePayment(payment: LoanPaymentEntity)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE vehicleId = :vehicleId ORDER BY date DESC, id DESC")
    fun observe(vehicleId: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE vehicleId = :vehicleId ORDER BY date DESC, id DESC")
    suspend fun getAll(vehicleId: Long): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun get(id: Long): ExpenseEntity?

    @Insert
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)
}
