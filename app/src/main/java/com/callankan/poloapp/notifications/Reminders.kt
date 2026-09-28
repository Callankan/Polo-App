package com.callankan.poloapp.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.callankan.poloapp.MainActivity
import com.callankan.poloapp.R
import com.callankan.poloapp.data.repository.LogbookRepository
import com.callankan.poloapp.data.repository.OverviewRepository
import com.callankan.poloapp.data.repository.VehicleOverview
import com.callankan.poloapp.data.repository.VehicleRepository
import com.callankan.poloapp.data.settings.SettingsRepository
import com.callankan.poloapp.domain.Deadline
import com.callankan.poloapp.domain.DueState
import com.callankan.poloapp.ui.format.Fmt
import com.callankan.poloapp.widget.PoloWidget
import androidx.glance.appwidget.updateAll
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class Reminder(val key: String, val title: String, val text: String)

/** Decide qué avisos tocan hoy. Es puro para poder probarlo sin Android. */
object ReminderPlanner {
    private val thresholds = listOf(0L, 1L, 7L, 30L)

    fun deadlineBucket(deadline: Deadline): Long? {
        val days = deadline.daysLeft ?: return null
        if (days < 0) return -1
        return thresholds.firstOrNull { days <= it }
    }

    fun plan(o: VehicleOverview): List<Reminder> = buildList {
        val name = o.vehicle.alias
        deadlineBucket(o.itv)?.let { b ->
            add(Reminder("itv:${o.vehicle.id}:${o.itv.date}:$b", "ITV · $name", deadlineText("La ITV", o.itv)))
        }
        deadlineBucket(o.insurance)?.let { b ->
            add(Reminder("ins:${o.vehicle.id}:${o.insurance.date}:$b", "Seguro · $name", deadlineText("El seguro", o.insurance)))
        }
        o.components
            .filter { it.component.alertsEnabled && (it.state == DueState.OVERDUE || it.state == DueState.SOON) }
            .forEach { s ->
                val text = when {
                    s.state == DueState.OVERDUE -> "${s.component.name}: toca cambiarlo ya."
                    s.kmRemaining != null && s.kmRemaining <= o.settings.maintenanceLeadKm -> "${s.component.name}: quedan ${Fmt.km(s.kmRemaining)}."
                    else -> "${s.component.name}: vence ${Fmt.relativeDays(s.daysRemaining)}."
                }
                add(Reminder("comp:${s.component.id}:${s.current?.id}:${s.state}", "Mantenimiento · $name", text))
            }
        val lastTire = o.lastTireCheck?.date
        val tireDays = lastTire?.let { ChronoUnit.DAYS.between(it, o.today) }
        if (tireDays != null && tireDays > o.settings.pressureReminderDays) {
            add(Reminder("tire:${o.vehicle.id}:$lastTire", "Presión de neumáticos", "Hace $tireDays días que no revisas la presión del $name."))
        }
    }

    private fun deadlineText(what: String, d: Deadline): String {
        val days = d.daysLeft ?: 0
        return when {
            days < 0 -> "$what está caducada desde el ${Fmt.date(d.date)}."
            days == 0L -> "$what vence hoy."
            else -> "$what vence ${Fmt.relativeDays(days)} (${Fmt.date(d.date)})."
        }
    }
}

@Singleton
class NotificationHelper @Inject constructor(@ApplicationContext private val context: Context) {
    fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Vencimientos y mantenimiento", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "ITV, seguro, piezas y recordatorios del coche"
            },
        )
    }

    fun show(reminder: Reminder) {
        val compat = NotificationManagerCompat.from(context)
        if (!compat.areNotificationsEnabled()) return
        val intent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFFE8373E.toInt())
            .setContentTitle(reminder.title)
            .setContentText(reminder.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reminder.text))
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
        try {
            compat.notify(reminder.key.hashCode(), notification)
        } catch (_: SecurityException) {
            // Permiso de notificaciones revocado: no hay nada que hacer.
        }
    }

    companion object {
        const val CHANNEL = "reminders"
    }
}

/** Revisa cada día los vencimientos y actualiza el widget. */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val vehicles: VehicleRepository,
    private val overview: OverviewRepository,
    private val logbook: LogbookRepository,
    private val settings: SettingsRepository,
    private val notifications: NotificationHelper,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val prefs = settings.current()
        if (prefs.notificationsEnabled) {
            notifications.ensureChannel()
            val already = settings.notifiedKeys()
            val reminders = buildList {
                vehicles.getAll().forEach { v -> overview.snapshot(v.id)?.let { addAll(ReminderPlanner.plan(it)) } }
                val today = LocalDate.now()
                logbook.pendingReminders().filter { !it.reminderDate!!.isAfter(today) }.forEach { n ->
                    add(Reminder("note:${n.id}:${n.reminderDate}", "Recordatorio", n.title))
                }
            }.filter { it.key !in already }
            reminders.forEach(notifications::show)
            settings.markNotified(reminders.map { it.key })
        }
        runCatching { PoloWidget().updateAll(applicationContext) }
        return Result.success()
    }
}

@Singleton
class ReminderScheduler @Inject constructor(@ApplicationContext private val context: Context) {
    fun schedule() {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "reminders",
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<ReminderWorker>(12, TimeUnit.HOURS).build(),
        )
    }

    fun runNow() {
        WorkManager.getInstance(context).enqueueUniqueWork(
            "reminders-now",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<ReminderWorker>().build(),
        )
    }
}
