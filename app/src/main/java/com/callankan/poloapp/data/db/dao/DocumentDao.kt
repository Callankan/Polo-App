package com.callankan.poloapp.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import com.callankan.poloapp.data.db.entity.AttachmentEntity
import com.callankan.poloapp.data.db.entity.DocumentEntity
import com.callankan.poloapp.data.model.AttachmentOwner

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents WHERE vehicleId = :vehicleId ORDER BY type, createdAt DESC")
    fun observe(vehicleId: Long): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun get(id: Long): DocumentEntity?

    @Insert
    suspend fun insert(document: DocumentEntity): Long

    @Update
    suspend fun update(document: DocumentEntity)

    @Delete
    suspend fun delete(document: DocumentEntity)
}

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE ownerType = :ownerType AND ownerId = :ownerId ORDER BY createdAt, id")
    fun observe(ownerType: AttachmentOwner, ownerId: Long): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE ownerType = :ownerType AND ownerId = :ownerId ORDER BY createdAt, id")
    suspend fun get(ownerType: AttachmentOwner, ownerId: Long): List<AttachmentEntity>

    @Query("SELECT * FROM attachments WHERE ownerType = :ownerType")
    fun observeByType(ownerType: AttachmentOwner): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE ownerType = :ownerType")
    suspend fun getByType(ownerType: AttachmentOwner): List<AttachmentEntity>

    @Query("SELECT fileName FROM attachments")
    suspend fun allFileNames(): List<String>

    @Insert
    suspend fun insert(attachment: AttachmentEntity): Long

    @Insert
    suspend fun insertAll(attachments: List<AttachmentEntity>)

    @Delete
    suspend fun delete(attachment: AttachmentEntity)

    @Query("DELETE FROM attachments WHERE ownerType = :ownerType AND ownerId = :ownerId")
    suspend fun deleteFor(ownerType: AttachmentOwner, ownerId: Long)

    /** Borra adjuntos cuyo registro propietario ya no existe (p. ej. tras borrar un vehículo). */
    @Query(
        """
        DELETE FROM attachments WHERE
            (ownerType = 'MAINTENANCE' AND ownerId NOT IN (SELECT id FROM maintenance_records)) OR
            (ownerType = 'DAMAGE' AND ownerId NOT IN (SELECT id FROM damages)) OR
            (ownerType = 'DOCUMENT' AND ownerId NOT IN (SELECT id FROM documents)) OR
            (ownerType = 'ITV' AND ownerId NOT IN (SELECT id FROM itv_inspections)) OR
            (ownerType = 'EXPENSE' AND ownerId NOT IN (SELECT id FROM expenses)) OR
            (ownerType = 'INSURANCE' AND ownerId NOT IN (SELECT id FROM insurance_policies))
        """,
    )
    suspend fun deleteOrphans()
}
