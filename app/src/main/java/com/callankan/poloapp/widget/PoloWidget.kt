package com.callankan.poloapp.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.callankan.poloapp.MainActivity
import com.callankan.poloapp.data.repository.OverviewRepository
import com.callankan.poloapp.data.repository.VehicleOverview
import com.callankan.poloapp.data.settings.SettingsRepository
import com.callankan.poloapp.domain.DeadlineState
import com.callankan.poloapp.domain.DueState
import com.callankan.poloapp.ui.format.Fmt
import com.callankan.poloapp.ui.illustration.CarPainter
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun settings(): SettingsRepository
    fun overview(): OverviewRepository
}

/** Widget de escritorio: km, ITV, seguro y próximo mantenimiento. */
class PoloWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entry = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        val vehicleId = entry.settings().current().selectedVehicleId
        val overview = vehicleId?.let { entry.overview().snapshot(it) }
        val car = overview?.let { carBitmap(it.vehicle.colorArgb.toInt()) }
        provideContent { WidgetContent(overview, car) }
    }

    private fun carBitmap(color: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(500, 210, Bitmap.Config.ARGB_8888)
        CarPainter.draw(Canvas(bitmap), 500f, 210f, color, shadow = false)
        return bitmap
    }
}

class PoloWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PoloWidget()
}

private val bg = Color(0xFF121418)
private val white = Color(0xFFF2F3F5)
private val muted = Color(0xFF9AA1AC)
private val green = Color(0xFF2FD08A)
private val amber = Color(0xFFFFB020)
private val red = Color(0xFFFF5A5F)

@Composable
private fun WidgetContent(o: VehicleOverview?, car: Bitmap?) {
    Column(
        GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(bg))
            .cornerRadius(24.dp)
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        if (o == null) {
            Text("Polo App", style = TextStyle(color = ColorProvider(white), fontSize = 16.sp, fontWeight = FontWeight.Bold))
            Text("Abre la app para configurar tu coche", style = TextStyle(color = ColorProvider(muted), fontSize = 12.sp))
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(GlanceModifier.defaultWeight()) {
                Text(o.vehicle.alias, style = TextStyle(color = ColorProvider(muted), fontSize = 12.sp, fontWeight = FontWeight.Medium))
                Text(
                    Fmt.km(o.currentKm),
                    style = TextStyle(color = ColorProvider(white), fontSize = 24.sp, fontWeight = FontWeight.Bold),
                )
            }
            if (car != null) Image(ImageProvider(car), contentDescription = null, modifier = GlanceModifier.width(110.dp).height(46.dp))
        }
        Spacer(GlanceModifier.height(10.dp))
        Row {
            Chip("ITV", o.itv.daysLeft?.let { if (it < 0) "caducada" else "$it d" } ?: "—", stateColor(o.itv.state))
            Spacer(GlanceModifier.width(8.dp))
            Chip("Seguro", o.insurance.daysLeft?.let { if (it < 0) "vencido" else "$it d" } ?: "—", stateColor(o.insurance.state))
        }
        Spacer(GlanceModifier.height(6.dp))
        val next = o.nextMaintenance
        if (next != null) {
            val color = when (next.state) {
                DueState.OVERDUE -> red
                DueState.SOON -> amber
                else -> green
            }
            val detail = next.kmRemaining?.let { if (it < 0) "pasado" else Fmt.km(it) } ?: Fmt.date(next.estimatedDueDate)
            Chip(next.component.name, detail, color)
        }
    }
}

private fun stateColor(state: DeadlineState) = when (state) {
    DeadlineState.OK -> green
    DeadlineState.SOON -> amber
    DeadlineState.URGENT, DeadlineState.EXPIRED -> red
    DeadlineState.UNKNOWN -> muted
}

@Composable
private fun Chip(label: String, value: String, color: Color) {
    Row(
        GlanceModifier
            .background(ColorProvider(Color(0xFF1C2026)))
            .cornerRadius(12.dp)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$label ", style = TextStyle(color = ColorProvider(muted), fontSize = 12.sp))
        Text(value, style = TextStyle(color = ColorProvider(color), fontSize = 12.sp, fontWeight = FontWeight.Bold))
    }
}
