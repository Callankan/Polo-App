package com.callankan.poloapp.data.repository

import com.callankan.poloapp.data.db.dao.InsuranceDao
import com.callankan.poloapp.data.db.dao.ItvDao
import com.callankan.poloapp.data.db.entity.InsurancePolicyEntity
import com.callankan.poloapp.data.db.entity.ItvInspectionEntity
import com.callankan.poloapp.data.model.AttachmentOwner
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** ITV y seguro: los dos vencimientos legales del coche. */
@Singleton
class ComplianceRepository @Inject constructor(
    private val itvDao: ItvDao,
    private val insuranceDao: InsuranceDao,
    private val attachments: AttachmentRepository,
) {
    fun observeItv(vehicleId: Long): Flow<List<ItvInspectionEntity>> = itvDao.observe(vehicleId)
    suspend fun itv(vehicleId: Long): List<ItvInspectionEntity> = itvDao.getAll(vehicleId)
    suspend fun getItv(id: Long) = itvDao.get(id)
    suspend fun saveItv(itv: ItvInspectionEntity): Long = if (itv.id == 0L) itvDao.insert(itv) else itv.id.also { itvDao.update(itv) }
    suspend fun deleteItv(itv: ItvInspectionEntity) {
        attachments.deleteAllFor(AttachmentOwner.ITV, itv.id)
        itvDao.delete(itv)
    }

    fun observeInsurance(vehicleId: Long): Flow<List<InsurancePolicyEntity>> = insuranceDao.observe(vehicleId)
    suspend fun insurance(vehicleId: Long): List<InsurancePolicyEntity> = insuranceDao.getAll(vehicleId)
    suspend fun getPolicy(id: Long) = insuranceDao.get(id)
    suspend fun savePolicy(policy: InsurancePolicyEntity): Long =
        if (policy.id == 0L) insuranceDao.insert(policy) else policy.id.also { insuranceDao.update(policy) }
    suspend fun deletePolicy(policy: InsurancePolicyEntity) {
        attachments.deleteAllFor(AttachmentOwner.INSURANCE, policy.id)
        insuranceDao.delete(policy)
    }
}
