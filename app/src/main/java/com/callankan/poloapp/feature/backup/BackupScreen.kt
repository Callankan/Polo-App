package com.callankan.poloapp.feature.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.callankan.poloapp.data.backup.BackupManager
import com.callankan.poloapp.data.settings.AppSettings
import com.callankan.poloapp.data.settings.SettingsRepository
import com.callankan.poloapp.ui.components.ConfirmDialog
import com.callankan.poloapp.ui.components.IconBadge
import com.callankan.poloapp.ui.components.ListScaffold
import com.callankan.poloapp.ui.components.PoloCard
import com.callankan.poloapp.ui.format.Fmt
import com.callankan.poloapp.ui.theme.PoloTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val manager: BackupManager,
    settings: SettingsRepository,
) : ViewModel() {
    val settings: StateFlow<AppSettings?> = settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    var busy by mutableStateOf(false)
        private set
    var message by mutableStateOf<Pair<Boolean, String>?>(null)
        private set

    fun export(uri: Uri) = run("Copia guardada") { manager.export(uri).let { "${it.vehicles} vehículo(s), ${it.records} registros y ${it.files} fotos" } }
    fun import(uri: Uri) = run("Copia restaurada") { manager.import(uri).let { "${it.vehicles} vehículo(s), ${it.records} registros y ${it.files} fotos" } }

    private fun run(title: String, block: suspend () -> String) {
        busy = true
        message = null
        viewModelScope.launch {
            message = runCatching { block() }.fold({ true to "$title: $it" }, { false to (it.message ?: "Algo ha fallado") })
            busy = false
        }
    }
}

@Composable
fun BackupScreen(onBack: () -> Unit, vm: BackupViewModel = hiltViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    var pendingImport by remember { mutableStateOf<Uri?>(null) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { it?.let(vm::export) }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { pendingImport = it }
    ListScaffold("Copia de seguridad", onBack) {
        item {
            PoloCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Rounded.Save, PoloTheme.colors.success, size = 48.dp, iconSize = 24.dp)
                    Spacer(Modifier.width(14.dp))
                    Text(
                        "Todo vive en tu móvil. Guarda de vez en cuando una copia (un único archivo .zip con datos y fotos) en Drive, en tu correo o en el ordenador.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.height(12.dp))
                val last = settings?.lastBackupAt
                Text(
                    last?.let { "Última copia: ${Fmt.longDate(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate())}" } ?: "Aún no has hecho ninguna copia",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (last == null) PoloTheme.colors.warning else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Button(
                onClick = { exporter.launch("PoloApp_copia_${LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)}.zip") },
                enabled = !vm.busy,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Icon(Icons.Rounded.CloudUpload, null)
                Spacer(Modifier.width(8.dp))
                Text("Crear copia de seguridad")
            }
        }
        item {
            OutlinedButton(
                onClick = { importer.launch(arrayOf("application/zip", "application/octet-stream", "application/x-zip-compressed")) },
                enabled = !vm.busy,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Icon(Icons.Rounded.Restore, null)
                Spacer(Modifier.width(8.dp))
                Text("Restaurar una copia")
            }
        }
        if (vm.busy) item { CircularProgressIndicator() }
        vm.message?.let { (ok, text) ->
            item {
                PoloCard(containerColor = (if (ok) PoloTheme.colors.success else PoloTheme.colors.danger).copy(alpha = 0.1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber, null, tint = if (ok) PoloTheme.colors.success else PoloTheme.colors.danger, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
    pendingImport?.let { uri ->
        ConfirmDialog(
            title = "¿Restaurar copia?",
            message = "Se sustituirán TODOS los datos actuales de la app por los de la copia. Esta acción no se puede deshacer.",
            confirmLabel = "Restaurar",
            destructive = true,
            onConfirm = {
                pendingImport = null
                vm.import(uri)
            },
            onDismiss = { pendingImport = null },
        )
    }
}
