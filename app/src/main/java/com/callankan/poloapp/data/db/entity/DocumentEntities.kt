@file:UseSerializers(LocalDateSerializer::class)

package com.callankan.poloapp.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.callankan.poloapp.data.db.LocalDateSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.LocalDate
import com.callankan.poloapp.data.model.AttachmentOwner
import com.callankan.poloapp.data.model.DocumentType

/** Guantera digital: permiso de circulación, ficha técnica, póliza... */
@Serializable
@Entity(
    tableName = "documents",
    foreignKeys = [ForeignKey(VehicleEntity::class, ["id"], ["vehicleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("vehicleId")],
)
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val type: DocumentType,
    val title: String,
    val expiryDate: LocalDate? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

/** Foto o PDF adjunto a cualquier registro. El archivo vive en filesDir/attachments. */
@Serializable
@Entity(tableName = "attachments", indices = [Index("ownerType", "ownerId")])
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerType: AttachmentOwner,
    val ownerId: Long,
    val fileName: String,
    val mimeType: String = "image/jpeg",
    @ColumnInfo(defaultValue = "0") val createdAt: Long = System.currentTimeMillis(),
)
