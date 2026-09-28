package com.callankan.poloapp.data.files

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class StoredFile(val fileName: String, val mimeType: String)

/** Guarda fotos y PDFs adjuntos dentro del almacenamiento privado de la app (sin nube). */
@Singleton
class AttachmentStorage @Inject constructor(@ApplicationContext private val context: Context) {
    val directory: File get() = File(context.filesDir, DIR).apply { mkdirs() }

    fun file(fileName: String): File = File(directory, fileName)

    fun uriFor(fileName: String): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file(fileName))

    /** Crea un archivo temporal para la cámara y devuelve su Uri compartible. */
    fun newCameraTarget(): Pair<File, Uri> {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
        return file to FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    suspend fun importUri(uri: Uri): StoredFile? = withContext(Dispatchers.IO) {
        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
        runCatching {
            if (mime == "application/pdf") {
                val name = "${UUID.randomUUID()}.pdf"
                context.contentResolver.openInputStream(uri)?.use { input ->
                    file(name).outputStream().use { input.copyTo(it) }
                } ?: return@runCatching null
                StoredFile(name, mime)
            } else {
                compressImage { context.contentResolver.openInputStream(uri) }
            }
        }.getOrNull()
    }

    suspend fun importFile(source: File): StoredFile? = withContext(Dispatchers.IO) {
        runCatching { compressImage { source.inputStream() } }.getOrNull().also { source.delete() }
    }

    fun delete(fileName: String) {
        runCatching { file(fileName).delete() }
    }

    /** Reduce las fotos a 2048 px como máximo y corrige la rotación EXIF para ahorrar espacio. */
    private fun compressImage(open: () -> java.io.InputStream?): StoredFile? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open()?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIZE) sample *= 2
        val bitmap = open()?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        val rotation = open()?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        val scale = minOf(1f, MAX_SIZE.toFloat() / maxOf(bitmap.width, bitmap.height))
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotation)
        }
        val output = if (rotation != 0f || scale < 1f) {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else bitmap
        val name = "${UUID.randomUUID()}.jpg"
        file(name).outputStream().use { output.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        if (output !== bitmap) bitmap.recycle()
        output.recycle()
        return StoredFile(name, "image/jpeg")
    }

    companion object {
        const val DIR = "attachments"
        private const val MAX_SIZE = 2048
    }
}
