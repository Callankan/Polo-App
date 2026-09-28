package com.callankan.poloapp.feature.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.callankan.poloapp.data.db.entity.AttachmentEntity
import com.callankan.poloapp.data.files.AttachmentStorage
import com.callankan.poloapp.data.files.StoredFile
import com.callankan.poloapp.data.model.AttachmentOwner
import com.callankan.poloapp.data.repository.AttachmentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

data class PhotoItem(val fileName: String, val mimeType: String, val saved: AttachmentEntity?) {
    val isPdf: Boolean get() = mimeType == "application/pdf"
}

/**
 * Borrador de adjuntos de un formulario: las fotos nuevas se copian al instante, pero solo se
 * vinculan al registro al guardar. Si se descarta el formulario se eliminan.
 */
class AttachmentDraft(
    private val repo: AttachmentRepository,
    private val owner: AttachmentOwner,
    private val scope: CoroutineScope,
) {
    val storage: AttachmentStorage get() = repo.storage
    private var existing by mutableStateOf<List<AttachmentEntity>>(emptyList())
    private var pending by mutableStateOf<List<StoredFile>>(emptyList())
    private val removed = mutableListOf<AttachmentEntity>()
    var busy by mutableStateOf(false)
        private set
    private var committed = false

    val items: List<PhotoItem>
        get() = existing.map { PhotoItem(it.fileName, it.mimeType, it) } + pending.map { PhotoItem(it.fileName, it.mimeType, null) }

    fun load(ownerId: Long) = scope.launch { existing = repo.get(owner, ownerId) }

    fun addUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        scope.launch {
            busy = true
            pending = pending + uris.mapNotNull { storage.importUri(it) }
            busy = false
        }
    }

    fun addCameraFile(file: File) = scope.launch {
        busy = true
        storage.importFile(file)?.let { pending = pending + it }
        busy = false
    }

    fun remove(item: PhotoItem) {
        if (item.saved != null) {
            removed += item.saved
            existing = existing - item.saved
        } else {
            pending = pending.filterNot { it.fileName == item.fileName }
            storage.delete(item.fileName)
        }
    }

    suspend fun commit(ownerId: Long) {
        repo.attach(owner, ownerId, pending)
        removed.forEach { repo.delete(it) }
        removed.clear()
        pending = emptyList()
        committed = true
    }

    /** Llamar desde onCleared(): borra fotos copiadas que no llegaron a guardarse. */
    fun discardIfNotCommitted() {
        if (!committed) pending.forEach { storage.delete(it.fileName) }
    }
}

/** Tira horizontal de fotos con botones de cámara y galería. */
@Composable
fun PhotoStrip(
    draft: AttachmentDraft,
    onOpen: (PhotoItem) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Fotos",
    allowPdf: Boolean = false,
) {
    var cameraFile by remember { mutableStateOf<File?>(null) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(8)) { draft.addUris(it) }
    val documents = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { draft.addUris(it) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val f = cameraFile
        if (ok && f != null) draft.addCameraFile(f)
        cameraFile = null
    }
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            if (draft.busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                AddTile(Icons.Rounded.PhotoCamera, "Cámara") {
                    val (file, uri) = draft.storage.newCameraTarget()
                    cameraFile = file
                    camera.launch(uri)
                }
            }
            item {
                AddTile(if (allowPdf) Icons.Rounded.PictureAsPdf else Icons.Rounded.AddPhotoAlternate, if (allowPdf) "Archivo" else "Galería") {
                    if (allowPdf) documents.launch(arrayOf("image/*", "application/pdf"))
                    else gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            }
            items(draft.items, key = { it.fileName }) { item ->
                Thumbnail(item, draft.storage, onClick = { onOpen(item) }, onRemove = { draft.remove(item) })
            }
        }
    }
}

@Composable
private fun AddTile(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        Modifier
            .size(92.dp)
            .clip(MaterialTheme.shapes.medium)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Thumbnail(item: PhotoItem, storage: AttachmentStorage, onClick: () -> Unit, onRemove: (() -> Unit)?) {
    Box(Modifier.size(92.dp)) {
        AttachmentThumb(item, storage, Modifier.fillMaxSize().clip(MaterialTheme.shapes.medium).clickable(onClick = onClick))
        if (onRemove != null) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Close, "Quitar", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun AttachmentThumb(item: PhotoItem, storage: AttachmentStorage, modifier: Modifier = Modifier) {
    if (item.isPdf) {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.PictureAsPdf, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
        }
    } else {
        AsyncImage(
            model = storage.file(item.fileName),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
        )
    }
}

/** Fila de miniaturas de solo lectura para pantallas de detalle. */
@Composable
fun AttachmentGallery(items: List<AttachmentEntity>, storage: AttachmentStorage, onOpen: (PhotoItem) -> Unit, modifier: Modifier = Modifier) {
    if (items.isEmpty()) return
    LazyRow(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(items, key = { it.id }) { a ->
            val item = PhotoItem(a.fileName, a.mimeType, a)
            AttachmentThumb(item, storage, Modifier.size(110.dp).clip(MaterialTheme.shapes.medium).clickable { onOpen(item) })
        }
    }
}

/** Abre un PDF adjunto con la app del sistema. */
fun openPdf(context: Context, storage: AttachmentStorage, fileName: String) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(storage.uriFor(fileName), "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        android.widget.Toast.makeText(context, "No hay ninguna app para abrir PDF", android.widget.Toast.LENGTH_SHORT).show()
    }
}

/** Abre la foto en el visor o el PDF en una app externa. */
@Composable
fun rememberAttachmentOpener(storage: AttachmentStorage, openPhoto: (String) -> Unit): (PhotoItem) -> Unit {
    val context = LocalContext.current
    return remember(storage, openPhoto) {
        { item: PhotoItem -> if (item.isPdf) openPdf(context, storage, item.fileName) else openPhoto(item.fileName) }
    }
}
