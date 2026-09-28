package com.callankan.poloapp.feature.report

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callankan.poloapp.data.repository.ActiveVehicle
import com.callankan.poloapp.report.ReportGenerator
import com.callankan.poloapp.report.ReportOptions
import com.callankan.poloapp.ui.components.IconBadge
import com.callankan.poloapp.ui.components.ListScaffold
import com.callankan.poloapp.ui.components.PoloCard
import com.callankan.poloapp.ui.components.SectionHeader
import com.callankan.poloapp.ui.components.SwitchRow
import com.callankan.poloapp.ui.theme.PoloTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/** Renderiza páginas del PDF para la vista previa (PdfRenderer no admite accesos concurrentes). */
class PdfPreview(file: File) {
    private val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer = PdfRenderer(descriptor)
    private val mutex = Mutex()
    val pageCount: Int get() = renderer.pageCount

    suspend fun render(index: Int, width: Int): Bitmap = mutex.withLock {
        withContext(Dispatchers.IO) {
            renderer.openPage(index).use { page ->
                val height = (width * page.height.toFloat() / page.width).toInt()
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(AndroidColor.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            }
        }
    }

    fun close() {
        runCatching { renderer.close() }
        runCatching { descriptor.close() }
    }
}

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val generator: ReportGenerator,
    private val active: ActiveVehicle,
) : ViewModel() {
    var options by mutableStateOf(ReportOptions())
        private set
    var generating by mutableStateOf(false)
        private set
    var file by mutableStateOf<File?>(null)
        private set
    var preview by mutableStateOf<PdfPreview?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun update(transform: (ReportOptions) -> ReportOptions) {
        options = transform(options)
    }

    fun generate() {
        if (generating) return
        generating = true
        error = null
        viewModelScope.launch {
            runCatching { generator.generate(active.currentId(), options) }
                .onSuccess { f ->
                    preview?.close()
                    file = f
                    preview = PdfPreview(f)
                }
                .onFailure { error = it.message ?: "No se pudo generar el informe" }
            generating = false
        }
    }

    override fun onCleared() {
        preview?.close()
    }
}

private fun shareFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension.replace('_', ' '))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Compartir informe"))
}

private fun copyTo(context: Context, file: File, target: Uri) {
    context.contentResolver.openOutputStream(target)?.use { out -> file.inputStream().use { it.copyTo(out) } }
}

@Composable
fun ReportScreen(onBack: () -> Unit, vm: ReportViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val o = vm.options
    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val f = vm.file
        if (uri != null && f != null) {
            runCatching { copyTo(context, f, uri) }
                .onSuccess { Toast.makeText(context, "Informe guardado", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(context, "No se pudo guardar", Toast.LENGTH_SHORT).show() }
        }
    }
    LaunchedEffect(Unit) { if (vm.file == null) vm.generate() }

    ListScaffold("Informe para la venta", onBack) {
        item {
            PoloCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Rounded.VerifiedUser, MaterialTheme.colorScheme.primary, size = 48.dp, iconSize = 24.dp)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Transmite confianza", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Un PDF limpio con todo el historial: kilometraje, mantenimientos con taller, ITV y facturas. La deuda y tus notas nunca se incluyen.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item {
            val preview = vm.preview
            when {
                vm.generating || (preview == null && vm.error == null) -> Box(Modifier.fillMaxWidth().height(420.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("Preparando el informe…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                vm.error != null -> Text(vm.error!!, color = MaterialTheme.colorScheme.error)
                preview != null -> PreviewPager(preview)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { vm.file?.let { shareFile(context, it) } }, enabled = vm.file != null && !vm.generating, modifier = Modifier.weight(1f).height(52.dp)) {
                    Icon(Icons.Rounded.Share, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Compartir")
                }
                FilledTonalButton(onClick = { vm.file?.let { saver.launch(it.name) } }, enabled = vm.file != null && !vm.generating, modifier = Modifier.weight(1f).height(52.dp)) {
                    Icon(Icons.Rounded.Download, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Guardar")
                }
            }
        }
        item { SectionHeader("Qué incluir", subtitle = "Cambia una opción y vuelve a generar") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SwitchRow("Precios y costes", o.includePrices, { v -> vm.update { it.copy(includePrices = v) } }, subtitle = "Algunos vendedores prefieren ocultarlos")
                SwitchRow("Matrícula", o.includePlate, { v -> vm.update { it.copy(includePlate = v) } })
                SwitchRow("Número de bastidor", o.includeVin, { v -> vm.update { it.copy(includeVin = v) } }, subtitle = "Permite al comprador verificar el historial")
                SwitchRow("Nombre del propietario", o.includeOwner, { v -> vm.update { it.copy(includeOwner = v) } })
                SwitchRow("Daños y reparaciones de carrocería", o.includeDamages, { v -> vm.update { it.copy(includeDamages = v) } }, subtitle = "La transparencia genera confianza")
                SwitchRow("Consumo registrado", o.includeFuel, { v -> vm.update { it.copy(includeFuel = v) } })
                SwitchRow("Anexo con fotos de facturas", o.includePhotos, { v -> vm.update { it.copy(includePhotos = v) } })
            }
        }
        item {
            Button(onClick = vm::generate, enabled = !vm.generating, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Rounded.PictureAsPdf, null)
                Spacer(Modifier.width(8.dp))
                Text("Volver a generar")
            }
        }
    }
}

@Composable
private fun PreviewPager(preview: PdfPreview) {
    val pager = rememberPagerState { preview.pageCount }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(pager, contentPadding = PaddingValues(horizontal = 28.dp), pageSpacing = 14.dp) { index ->
            val bitmap by produceState<Bitmap?>(null, preview, index) { value = preview.render(index, 900) }
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(595f / 842f)
                    .shadow(10.dp, MaterialTheme.shapes.medium)
                    .clip(MaterialTheme.shapes.medium)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                bitmap?.let { Image(it.asImageBitmap(), contentDescription = "Página ${index + 1}", modifier = Modifier.fillMaxWidth()) }
                    ?: CircularProgressIndicator(Modifier.size(28.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("Página ${pager.currentPage + 1} de ${preview.pageCount}", style = MaterialTheme.typography.labelMedium, color = PoloTheme.colors.info)
    }
}
