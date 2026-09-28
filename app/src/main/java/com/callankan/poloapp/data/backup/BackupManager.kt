package com.callankan.poloapp.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.callankan.poloapp.data.db.PoloDatabase
import com.callankan.poloapp.data.db.dao.BackupDao
import com.callankan.poloapp.data.db.entity.AttachmentEntity
import com.callankan.poloapp.data.db.entity.ComponentEntity
import com.callankan.poloapp.data.db.entity.DamageEntity
import com.callankan.poloapp.data.db.entity.DocumentEntity
import com.callankan.poloapp.data.db.entity.ExpenseEntity
import com.callankan.poloapp.data.db.entity.InsurancePolicyEntity
import com.callankan.poloapp.data.db.entity.ItvInspectionEntity
import com.callankan.poloapp.data.db.entity.LoanEntity
import com.callankan.poloapp.data.db.entity.LoanPaymentEntity
import com.callankan.poloapp.data.db.entity.MaintenanceEntity
import com.callankan.poloapp.data.db.entity.NoteEntity
import com.callankan.poloapp.data.db.entity.OdometerEntryEntity
import com.callankan.poloapp.data.db.entity.PartInstallationEntity
import com.callankan.poloapp.data.db.entity.RefuelEntity
import com.callankan.poloapp.data.db.entity.SpecEntity
import com.callankan.poloapp.data.db.entity.TirePressureEntity
import com.callankan.poloapp.data.db.entity.TripEntity
import com.callankan.poloapp.data.db.entity.VehicleEntity
import com.callankan.poloapp.data.db.entity.WorkshopEntity
import com.callankan.poloapp.data.files.AttachmentStorage
import com.callankan.poloapp.data.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/** Contenido completo de una copia de seguridad (formato JSON dentro de un .zip con las fotos). */
@Serializable
data class BackupData(
    val format: Int = FORMAT,
    val app: String = "Polo App",
    val exportedAt: Long = System.currentTimeMillis(),
    val vehicles: List<VehicleEntity> = emptyList(),
    val odometerEntries: List<OdometerEntryEntity> = emptyList(),
    val specs: List<SpecEntity> = emptyList(),
    val refuels: List<RefuelEntity> = emptyList(),
    val workshops: List<WorkshopEntity> = emptyList(),
    val maintenance: List<MaintenanceEntity> = emptyList(),
    val components: List<ComponentEntity> = emptyList(),
    val installations: List<PartInstallationEntity> = emptyList(),
    val loans: List<LoanEntity> = emptyList(),
    val payments: List<LoanPaymentEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val itv: List<ItvInspectionEntity> = emptyList(),
    val insurance: List<InsurancePolicyEntity> = emptyList(),
    val trips: List<TripEntity> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
    val tireChecks: List<TirePressureEntity> = emptyList(),
    val damages: List<DamageEntity> = emptyList(),
    val documents: List<DocumentEntity> = emptyList(),
    val attachments: List<AttachmentEntity> = emptyList(),
) {
    companion object {
        const val FORMAT = 1
    }
}

data class BackupSummary(val vehicles: Int, val records: Int, val files: Int)

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: PoloDatabase,
    private val dao: BackupDao,
    private val storage: AttachmentStorage,
    private val settings: SettingsRepository,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun snapshot(): BackupData = BackupData(
        vehicles = dao.allVehicle(), odometerEntries = dao.allOdometerEntry(), specs = dao.allSpec(),
        refuels = dao.allRefuel(), workshops = dao.allWorkshop(), maintenance = dao.allMaintenance(),
        components = dao.allComponent(), installations = dao.allPartInstallation(), loans = dao.allLoan(),
        payments = dao.allLoanPayment(), expenses = dao.allExpense(), itv = dao.allItvInspection(),
        insurance = dao.allInsurancePolicy(), trips = dao.allTrip(), notes = dao.allNote(),
        tireChecks = dao.allTirePressure(), damages = dao.allDamage(), documents = dao.allDocument(),
        attachments = dao.allAttachment(),
    )

    fun encode(data: BackupData): String = json.encodeToString(BackupData.serializer(), data)
    fun decode(text: String): BackupData = json.decodeFromString(BackupData.serializer(), text)

    suspend fun export(target: Uri): BackupSummary = withContext(Dispatchers.IO) {
        val data = snapshot()
        var files = 0
        val out = requireNotNull(context.contentResolver.openOutputStream(target)) { "No se pudo abrir el destino" }
        ZipOutputStream(BufferedOutputStream(out)).use { zip ->
            zip.putNextEntry(ZipEntry(JSON_ENTRY))
            zip.write(encode(data).toByteArray())
            zip.closeEntry()
            data.attachments.forEach { a ->
                val f = storage.file(a.fileName)
                if (f.exists()) {
                    zip.putNextEntry(ZipEntry("attachments/${a.fileName}"))
                    f.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                    files++
                }
            }
        }
        settings.setLastBackup(System.currentTimeMillis())
        BackupSummary(data.vehicles.size, data.recordCount(), files)
    }

    /** Sustituye todos los datos actuales por los de la copia. */
    suspend fun import(source: Uri): BackupSummary = withContext(Dispatchers.IO) {
        val tmp = File(context.cacheDir, "restore").apply {
            deleteRecursively()
            mkdirs()
        }
        var data: BackupData? = null
        val input = requireNotNull(context.contentResolver.openInputStream(source)) { "No se pudo abrir el archivo" }
        ZipInputStream(BufferedInputStream(input)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                when {
                    entry.name == JSON_ENTRY -> data = decode(zip.readBytes().decodeToString())
                    entry.name.startsWith("attachments/") && !entry.isDirectory -> {
                        // Solo el nombre del archivo: evita rutas maliciosas dentro del zip.
                        val name = File(entry.name).name
                        File(tmp, name).outputStream().use { zip.copyTo(it) }
                    }
                }
                zip.closeEntry()
            }
        }
        val d = data ?: error("El archivo no es una copia de seguridad de Polo App")
        require(d.format <= BackupData.FORMAT) { "La copia es de una versión más nueva de la app" }

        db.withTransaction {
            dao.clearAll()
            dao.insertVehicle(d.vehicles)
            dao.insertWorkshop(d.workshops)
            dao.insertOdometerEntry(d.odometerEntries)
            dao.insertSpec(d.specs)
            dao.insertRefuel(d.refuels)
            dao.insertMaintenance(d.maintenance)
            dao.insertComponent(d.components)
            dao.insertPartInstallation(d.installations)
            dao.insertLoan(d.loans)
            dao.insertLoanPayment(d.payments)
            dao.insertExpense(d.expenses)
            dao.insertItvInspection(d.itv)
            dao.insertInsurancePolicy(d.insurance)
            dao.insertTrip(d.trips)
            dao.insertNote(d.notes)
            dao.insertTirePressure(d.tireChecks)
            dao.insertDamage(d.damages)
            dao.insertDocument(d.documents)
            dao.insertAttachment(d.attachments)
        }
        storage.directory.listFiles()?.forEach { it.delete() }
        var files = 0
        tmp.listFiles()?.forEach {
            it.copyTo(storage.file(it.name), overwrite = true)
            files++
        }
        tmp.deleteRecursively()
        settings.selectVehicle(d.vehicles.firstOrNull()?.id)
        BackupSummary(d.vehicles.size, d.recordCount(), files)
    }

    private fun BackupData.recordCount() = refuels.size + maintenance.size + expenses.size + itv.size + insurance.size +
        trips.size + notes.size + tireChecks.size + damages.size + documents.size + payments.size + odometerEntries.size

    companion object {
        const val JSON_ENTRY = "backup.json"
    }
}
