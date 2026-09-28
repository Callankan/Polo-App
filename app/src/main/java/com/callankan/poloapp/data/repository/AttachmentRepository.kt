package com.callankan.poloapp.data.repository

import com.callankan.poloapp.data.db.dao.AttachmentDao
import com.callankan.poloapp.data.db.entity.AttachmentEntity
import com.callankan.poloapp.data.files.AttachmentStorage
import com.callankan.poloapp.data.files.StoredFile
import com.callankan.poloapp.data.model.AttachmentOwner
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttachmentRepository @Inject constructor(
    private val dao: AttachmentDao,
    val storage: AttachmentStorage,
) {
    fun observe(owner: AttachmentOwner, ownerId: Long): Flow<List<AttachmentEntity>> = dao.observe(owner, ownerId)
    suspend fun get(owner: AttachmentOwner, ownerId: Long): List<AttachmentEntity> = dao.get(owner, ownerId)
    suspend fun byType(owner: AttachmentOwner): List<AttachmentEntity> = dao.getByType(owner)
    fun observeByType(owner: AttachmentOwner): Flow<List<AttachmentEntity>> = dao.observeByType(owner)

    suspend fun attach(owner: AttachmentOwner, ownerId: Long, files: List<StoredFile>) {
        if (files.isEmpty()) return
        dao.insertAll(files.map { AttachmentEntity(ownerType = owner, ownerId = ownerId, fileName = it.fileName, mimeType = it.mimeType) })
    }

    suspend fun delete(attachment: AttachmentEntity) {
        dao.delete(attachment)
        storage.delete(attachment.fileName)
    }

    /** Limpia filas huérfanas y archivos que ya no referencia ninguna fila. */
    suspend fun purgeOrphans() {
        dao.deleteOrphans()
        val referenced = dao.allFileNames().toSet()
        storage.directory.listFiles()?.filter { it.name !in referenced }?.forEach { it.delete() }
    }

    suspend fun deleteAllFor(owner: AttachmentOwner, ownerId: Long) {
        dao.get(owner, ownerId).forEach { storage.delete(it.fileName) }
        dao.deleteFor(owner, ownerId)
    }
}
