package com.callankan.poloapp.feature.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.callankan.poloapp.BuildConfig
import com.callankan.poloapp.data.settings.AppSettings
import com.callankan.poloapp.data.settings.SettingsRepository
import com.callankan.poloapp.data.settings.ThemeMode
import com.callankan.poloapp.navigation.BackupRoute
import com.callankan.poloapp.navigation.ReportRoute
import com.callankan.poloapp.navigation.VehiclesRoute
import com.callankan.poloapp.notifications.ReminderScheduler
import com.callankan.poloapp.ui.components.ChoiceChips
import com.callankan.poloapp.ui.components.ListRow
import com.callankan.poloapp.ui.components.ListScaffold
import com.callankan.poloapp.ui.components.PoloCard
import com.callankan.poloapp.ui.components.SectionHeader
import com.callankan.poloapp.ui.components.SegmentedTabs
import com.callankan.poloapp.ui.components.SwitchRow
import com.callankan.poloapp.ui.theme.PoloTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository,
    private val scheduler: ReminderScheduler,
) : ViewModel() {
    val settings: StateFlow<AppSettings?> = repo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { repo.setThemeMode(mode) }
    fun setDynamic(enabled: Boolean) = viewModelScope.launch { repo.setDynamicColor(enabled) }
    fun setNotifications(enabled: Boolean) = viewModelScope.launch {
        repo.setNotifications(enabled)
        if (enabled) scheduler.runNow()
    }
    fun setLead(km: Int, days: Int) = viewModelScope.launch { repo.setMaintenanceLead(km, days) }
    fun setPressureDays(days: Int) = viewModelScope.launch { repo.setPressureReminderDays(days) }
    fun setDebtLock(enabled: Boolean) = viewModelScope.launch { repo.setDebtLock(enabled) }
}

@Composable
fun SettingsScreen(onBack: () -> Unit, onNavigate: (Any) -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val state by vm.settings.collectAsStateWithLifecycle()
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> vm.setNotifications(granted) }
    ListScaffold("Ajustes", onBack) {
        val s = state ?: return@ListScaffold
        item { SectionHeader("Apariencia") }
        item {
            PoloCard {
                SegmentedTabs(ThemeMode.entries.map { it.label }, ThemeMode.entries.indexOf(s.themeMode), { vm.setTheme(ThemeMode.entries[it]) })
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Spacer(Modifier.height(12.dp))
                    SwitchRow("Colores del fondo de pantalla", s.dynamicColor, vm::setDynamic, subtitle = "Material You en lugar del rojo Polo", icon = Icons.Rounded.Palette)
                }
            }
        }
        item { SectionHeader("Avisos") }
        item {
            SwitchRow(
                "Notificaciones", s.notificationsEnabled,
                { enabled ->
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else vm.setNotifications(enabled)
                },
                subtitle = "ITV y seguro (30, 7 y 1 día antes), mantenimiento y recordatorios",
                icon = Icons.Rounded.Notifications,
            )
        }
        item {
            PoloCard {
                Text("Avisar del mantenimiento con antelación de", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                ChoiceChips(listOf(500, 1000, 1500, 2000), s.maintenanceLeadKm, { "$it km" }, { vm.setLead(it, s.maintenanceLeadDays) })
                ChoiceChips(listOf(15, 30, 45, 60), s.maintenanceLeadDays, { "$it días" }, { vm.setLead(s.maintenanceLeadKm, it) })
                Spacer(Modifier.height(8.dp))
                Text("Recordar revisar la presión cada", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                ChoiceChips(listOf(15, 30, 45, 60), s.pressureReminderDays, { "$it días" }, vm::setPressureDays)
            }
        }
        item { SectionHeader("Privacidad") }
        item {
            SwitchRow(
                "Proteger Finanzas", s.debtLockEnabled, vm::setDebtLock,
                subtitle = "Pedir huella o PIN para ver la deuda", icon = Icons.Rounded.Fingerprint,
            )
        }
        item {
            PoloCard(containerColor = PoloTheme.colors.info.copy(alpha = 0.08f)) {
                ListRow(
                    "Tus datos se quedan en tu móvil",
                    subtitle = "La app funciona sin conexión y no envía nada a ningún servidor. Haz copias de seguridad de vez en cuando.",
                    icon = Icons.Rounded.CloudOff, tint = PoloTheme.colors.info,
                )
            }
        }
        item { SectionHeader("Herramientas") }
        item {
            PoloCard(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                ListRow("Mis vehículos", icon = Icons.Rounded.DirectionsCar, onClick = { onNavigate(VehiclesRoute) })
                ListRow("Copia de seguridad", icon = Icons.Rounded.Save, onClick = { onNavigate(BackupRoute) })
                ListRow("Informe PDF para la venta", icon = Icons.Rounded.PictureAsPdf, onClick = { onNavigate(ReportRoute) })
            }
        }
        item {
            Column {
                Text("Polo App ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Hecha con cariño para un Polo 1.2 TSI · Tipografías Manrope y Space Grotesk (OFL)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
