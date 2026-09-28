package com.callankan.poloapp.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode(val label: String) { DARK("Oscuro"), LIGHT("Claro"), SYSTEM("Según el sistema") }

data class AppSettings(
    val selectedVehicleId: Long? = null,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val dynamicColor: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val maintenanceLeadKm: Int = 1000,
    val maintenanceLeadDays: Int = 30,
    val pressureReminderDays: Int = 30,
    val debtLockEnabled: Boolean = false,
    val lastBackupAt: Long? = null,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private object Keys {
        val vehicle = longPreferencesKey("selected_vehicle")
        val theme = stringPreferencesKey("theme_mode")
        val dynamic = booleanPreferencesKey("dynamic_color")
        val notifications = booleanPreferencesKey("notifications")
        val leadKm = intPreferencesKey("lead_km")
        val leadDays = intPreferencesKey("lead_days")
        val pressureDays = intPreferencesKey("pressure_days")
        val debtLock = booleanPreferencesKey("debt_lock")
        val lastBackup = longPreferencesKey("last_backup")
        val notified = stringSetPreferencesKey("notified_keys")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            selectedVehicleId = p[Keys.vehicle],
            themeMode = p[Keys.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.DARK,
            dynamicColor = p[Keys.dynamic] ?: false,
            notificationsEnabled = p[Keys.notifications] ?: true,
            maintenanceLeadKm = p[Keys.leadKm] ?: 1000,
            maintenanceLeadDays = p[Keys.leadDays] ?: 30,
            pressureReminderDays = p[Keys.pressureDays] ?: 30,
            debtLockEnabled = p[Keys.debtLock] ?: false,
            lastBackupAt = p[Keys.lastBackup],
        )
    }

    val selectedVehicleId: Flow<Long?> = settings.map { it.selectedVehicleId }.distinctUntilChanged()

    suspend fun current(): AppSettings = settings.first()

    suspend fun selectVehicle(id: Long?) = context.dataStore.edit {
        if (id == null) it.remove(Keys.vehicle) else it[Keys.vehicle] = id
    }

    suspend fun setThemeMode(mode: ThemeMode) = context.dataStore.edit { it[Keys.theme] = mode.name }
    suspend fun setDynamicColor(enabled: Boolean) = context.dataStore.edit { it[Keys.dynamic] = enabled }
    suspend fun setNotifications(enabled: Boolean) = context.dataStore.edit { it[Keys.notifications] = enabled }
    suspend fun setMaintenanceLead(km: Int, days: Int) = context.dataStore.edit {
        it[Keys.leadKm] = km
        it[Keys.leadDays] = days
    }
    suspend fun setPressureReminderDays(days: Int) = context.dataStore.edit { it[Keys.pressureDays] = days }
    suspend fun setDebtLock(enabled: Boolean) = context.dataStore.edit { it[Keys.debtLock] = enabled }
    suspend fun setLastBackup(timestamp: Long) = context.dataStore.edit { it[Keys.lastBackup] = timestamp }

    /** Claves de avisos ya notificados, para no repetir la misma notificación cada día. */
    suspend fun notifiedKeys(): Set<String> = context.dataStore.data.first()[Keys.notified] ?: emptySet()

    suspend fun markNotified(keys: Collection<String>) = context.dataStore.edit {
        val current = it[Keys.notified] ?: emptySet()
        // Se limita el tamaño para que el conjunto no crezca indefinidamente.
        it[Keys.notified] = (current + keys).toList().takeLast(400).toSet()
    }
}
