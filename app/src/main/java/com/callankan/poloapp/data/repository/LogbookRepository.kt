package com.callankan.poloapp.data.repository

import com.callankan.poloapp.data.db.dao.DamageDao
import com.callankan.poloapp.data.db.dao.DocumentDao
import com.callankan.poloapp.data.db.dao.NoteDao
import com.callankan.poloapp.data.db.dao.TirePressureDao
import com.callankan.poloapp.data.db.dao.TripDao
import com.callankan.poloapp.data.db.entity.DamageEntity
import com.callankan.poloapp.data.db.entity.DocumentEntity
import com.callankan.poloapp.data.db.entity.NoteEntity
import com.callankan.poloapp.data.db.entity.TirePressureEntity
import com.callankan.poloapp.data.db.entity.TripEntity
import com.callankan.poloapp.data.model.AttachmentOwner
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Cuaderno de bitácora: viajes, notas, presiones, daños y guantera digital. */
@Singleton
class LogbookRepository @Inject constructor(
    private val tripDao: TripDao,
    private val noteDao: NoteDao,
    private val tireDao: TirePressureDao,
    private val damageDao: DamageDao,
    private val documentDao: DocumentDao,
    private val attachments: AttachmentRepository,
) {
    fun observeTrips(vehicleId: Long): Flow<List<TripEntity>> = tripDao.observe(vehicleId)
    suspend fun getTrip(id: Long) = tripDao.get(id)
    suspend fun saveTrip(trip: TripEntity): Long = if (trip.id == 0L) tripDao.insert(trip) else trip.id.also { tripDao.update(trip) }
    suspend fun deleteTrip(trip: TripEntity) = tripDao.delete(trip)

    fun observeNotes(vehicleId: Long): Flow<List<NoteEntity>> = noteDao.observe(vehicleId)
    suspend fun pendingReminders(): List<NoteEntity> = noteDao.pendingReminders()
    suspend fun getNote(id: Long) = noteDao.get(id)
    suspend fun saveNote(note: NoteEntity): Long = if (note.id == 0L) noteDao.insert(note) else note.id.also { noteDao.update(note) }
    suspend fun deleteNote(note: NoteEntity) = noteDao.delete(note)

    fun observeTireChecks(vehicleId: Long): Flow<List<TirePressureEntity>> = tireDao.observe(vehicleId)
    suspend fun getTireCheck(id: Long) = tireDao.get(id)
    suspend fun saveTireCheck(check: TirePressureEntity): Long =
        if (check.id == 0L) tireDao.insert(check) else check.id.also { tireDao.update(check) }
    suspend fun deleteTireCheck(check: TirePressureEntity) = tireDao.delete(check)

    fun observeDamages(vehicleId: Long): Flow<List<DamageEntity>> = damageDao.observe(vehicleId)
    suspend fun damages(vehicleId: Long): List<DamageEntity> = damageDao.getAll(vehicleId)
    suspend fun getDamage(id: Long) = damageDao.get(id)
    suspend fun saveDamage(damage: DamageEntity): Long =
        if (damage.id == 0L) damageDao.insert(damage) else damage.id.also { damageDao.update(damage) }
    suspend fun deleteDamage(damage: DamageEntity) {
        attachments.deleteAllFor(AttachmentOwner.DAMAGE, damage.id)
        damageDao.delete(damage)
    }

    fun observeDocuments(vehicleId: Long): Flow<List<DocumentEntity>> = documentDao.observe(vehicleId)
    suspend fun getDocument(id: Long) = documentDao.get(id)
    suspend fun saveDocument(document: DocumentEntity): Long =
        if (document.id == 0L) documentDao.insert(document) else document.id.also { documentDao.update(document) }
    suspend fun deleteDocument(document: DocumentEntity) {
        attachments.deleteAllFor(AttachmentOwner.DOCUMENT, document.id)
        documentDao.delete(document)
    }
}
