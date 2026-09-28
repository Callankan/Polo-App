package com.callankan.poloapp.data.repository

import com.callankan.poloapp.data.db.dao.ExpenseDao
import com.callankan.poloapp.data.db.dao.LoanDao
import com.callankan.poloapp.data.db.entity.ExpenseEntity
import com.callankan.poloapp.data.db.entity.LoanEntity
import com.callankan.poloapp.data.db.entity.LoanPaymentEntity
import com.callankan.poloapp.data.model.AttachmentOwner
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinanceRepository @Inject constructor(
    private val loanDao: LoanDao,
    private val expenseDao: ExpenseDao,
    private val attachments: AttachmentRepository,
) {
    fun observeLoans(vehicleId: Long): Flow<List<LoanEntity>> = loanDao.observeLoans(vehicleId)
    suspend fun loans(vehicleId: Long): List<LoanEntity> = loanDao.getLoans(vehicleId)
    fun observeLoan(id: Long): Flow<LoanEntity?> = loanDao.observeLoan(id)
    suspend fun getLoan(id: Long) = loanDao.getLoan(id)
    suspend fun saveLoan(loan: LoanEntity): Long = if (loan.id == 0L) loanDao.insertLoan(loan) else loan.id.also { loanDao.updateLoan(loan) }
    suspend fun deleteLoan(loan: LoanEntity) = loanDao.deleteLoan(loan)

    fun observePayments(loanId: Long): Flow<List<LoanPaymentEntity>> = loanDao.observePayments(loanId)
    fun observePaymentsForVehicle(vehicleId: Long): Flow<List<LoanPaymentEntity>> = loanDao.observePaymentsForVehicle(vehicleId)
    suspend fun getPayment(id: Long) = loanDao.getPayment(id)
    suspend fun savePayment(payment: LoanPaymentEntity): Long =
        if (payment.id == 0L) loanDao.insertPayment(payment) else payment.id.also { loanDao.updatePayment(payment) }
    suspend fun deletePayment(payment: LoanPaymentEntity) = loanDao.deletePayment(payment)

    fun observeExpenses(vehicleId: Long): Flow<List<ExpenseEntity>> = expenseDao.observe(vehicleId)
    suspend fun expenses(vehicleId: Long): List<ExpenseEntity> = expenseDao.getAll(vehicleId)
    suspend fun getExpense(id: Long) = expenseDao.get(id)
    suspend fun saveExpense(expense: ExpenseEntity): Long =
        if (expense.id == 0L) expenseDao.insert(expense) else expense.id.also { expenseDao.update(expense) }
    suspend fun deleteExpense(expense: ExpenseEntity) {
        attachments.deleteAllFor(AttachmentOwner.EXPENSE, expense.id)
        expenseDao.delete(expense)
    }
}
