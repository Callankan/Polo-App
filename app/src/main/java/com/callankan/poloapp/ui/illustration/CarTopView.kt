package com.callankan.poloapp.ui.illustration

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.callankan.poloapp.data.model.CarZone

/** Zonas en coordenadas relativas (0..1) de una vista cenital con el morro arriba. */
private val zoneRects: List<Pair<CarZone, Rect>> = listOf(
    CarZone.LEFT_MIRROR to Rect(0.03f, 0.29f, 0.15f, 0.34f),
    CarZone.RIGHT_MIRROR to Rect(0.85f, 0.29f, 0.97f, 0.34f),
    CarZone.FRONT_BUMPER to Rect(0.18f, 0.02f, 0.82f, 0.075f),
    CarZone.HOOD to Rect(0.28f, 0.075f, 0.72f, 0.27f),
    CarZone.WINDSHIELD to Rect(0.28f, 0.27f, 0.72f, 0.36f),
    CarZone.ROOF to Rect(0.30f, 0.36f, 0.70f, 0.74f),
    CarZone.TAILGATE to Rect(0.28f, 0.74f, 0.72f, 0.925f),
    CarZone.REAR_BUMPER to Rect(0.18f, 0.925f, 0.82f, 0.98f),
    CarZone.FRONT_LEFT_WING to Rect(0.15f, 0.075f, 0.28f, 0.30f),
    CarZone.FRONT_LEFT_DOOR to Rect(0.15f, 0.30f, 0.30f, 0.52f),
    CarZone.REAR_LEFT_DOOR to Rect(0.15f, 0.52f, 0.30f, 0.72f),
    CarZone.REAR_LEFT_WING to Rect(0.15f, 0.72f, 0.28f, 0.925f),
    CarZone.FRONT_RIGHT_WING to Rect(0.72f, 0.075f, 0.85f, 0.30f),
    CarZone.FRONT_RIGHT_DOOR to Rect(0.70f, 0.30f, 0.85f, 0.52f),
    CarZone.REAR_RIGHT_DOOR to Rect(0.70f, 0.52f, 0.85f, 0.72f),
    CarZone.REAR_RIGHT_WING to Rect(0.72f, 0.72f, 0.85f, 0.925f),
)

/** Ruedas: delantera izq., delantera der., trasera izq., trasera der. */
private val wheelRects = listOf(
    Rect(0.08f, 0.15f, 0.17f, 0.27f),
    Rect(0.83f, 0.15f, 0.92f, 0.27f),
    Rect(0.08f, 0.73f, 0.17f, 0.85f),
    Rect(0.83f, 0.73f, 0.92f, 0.85f),
)

fun zoneCenter(zone: CarZone): Offset? = zoneRects.firstOrNull { it.first == zone }?.second?.center
    ?: if (zone == CarZone.WHEELS) wheelRects[0].center else null

/**
 * Vista cenital del coche con zonas pulsables. Sirve para ubicar daños y para mostrar la
 * presión de cada rueda.
 */
@Composable
fun CarTopView(
    bodyColor: Color,
    modifier: Modifier = Modifier,
    selected: CarZone? = null,
    selectionColor: Color = Color(0xFFE8373E),
    markers: Map<CarZone, Color> = emptyMap(),
    wheelLabels: List<String>? = null,
    wheelColors: List<Color>? = null,
    onZoneClick: ((CarZone) -> Unit)? = null,
) {
    val measurer = rememberTextMeasurer()
    val tapModifier = if (onZoneClick != null) {
        Modifier.pointerInput(Unit) {
            detectTapGestures { p ->
                val rx = p.x / size.width
                val ry = p.y / size.height
                val hit = wheelRects.firstOrNull { it.contains(Offset(rx, ry)) }?.let { CarZone.WHEELS }
                    ?: zoneRects.firstOrNull { it.second.contains(Offset(rx, ry)) }?.first
                hit?.let(onZoneClick)
            }
        }
    } else Modifier
    Canvas(modifier.aspectRatio(0.52f).then(tapModifier)) {
        val w = size.width
        val h = size.height
        fun r(rect: Rect) = Rect(rect.left * w, rect.top * h, rect.right * w, rect.bottom * h)

        // Ruedas
        wheelRects.forEachIndexed { i, wr ->
            val rr = r(wr)
            drawRoundRect(wheelColors?.getOrNull(i) ?: Color(0xFF2C3139), rr.topLeft, rr.size, CornerRadius(w * 0.03f))
        }

        // Carrocería
        val body = Path().apply {
            moveTo(0.30f * w, 0.02f * h)
            cubicTo(0.18f * w, 0.02f * h, 0.15f * w, 0.05f * h, 0.15f * w, 0.12f * h)
            lineTo(0.15f * w, 0.90f * h)
            cubicTo(0.15f * w, 0.96f * h, 0.18f * w, 0.98f * h, 0.30f * w, 0.98f * h)
            lineTo(0.70f * w, 0.98f * h)
            cubicTo(0.82f * w, 0.98f * h, 0.85f * w, 0.96f * h, 0.85f * w, 0.90f * h)
            lineTo(0.85f * w, 0.12f * h)
            cubicTo(0.85f * w, 0.05f * h, 0.82f * w, 0.02f * h, 0.70f * w, 0.02f * h)
            close()
        }
        drawPath(
            body,
            Brush.horizontalGradient(
                0f to lerp(bodyColor, Color.Black, 0.35f),
                0.5f to lerp(bodyColor, Color.White, 0.12f),
                1f to lerp(bodyColor, Color.Black, 0.35f),
                startX = 0.15f * w, endX = 0.85f * w,
            ),
        )

        // Retrovisores
        listOf(CarZone.LEFT_MIRROR, CarZone.RIGHT_MIRROR).forEach { z ->
            val rr = r(zoneRects.first { it.first == z }.second)
            drawRoundRect(lerp(bodyColor, Color.Black, 0.2f), rr.topLeft, rr.size, CornerRadius(w * 0.03f))
        }

        // Cristales
        val glass = Color(0xFF1B2029)
        val windshield = Path().apply {
            moveTo(0.25f * w, 0.36f * h); lineTo(0.30f * w, 0.28f * h); lineTo(0.70f * w, 0.28f * h); lineTo(0.75f * w, 0.36f * h); close()
        }
        drawPath(windshield, glass)
        val rear = Path().apply {
            moveTo(0.26f * w, 0.76f * h); lineTo(0.74f * w, 0.76f * h); lineTo(0.70f * w, 0.84f * h); lineTo(0.30f * w, 0.84f * h); close()
        }
        drawPath(rear, glass)
        drawRoundRect(lerp(bodyColor, Color.Black, 0.12f), Offset(0.30f * w, 0.37f * h), Size(0.40f * w, 0.38f * h), CornerRadius(w * 0.05f))
        // Ventanillas laterales
        listOf(0.155f, 0.80f).forEach { x ->
            drawRoundRect(glass.copy(alpha = 0.9f), Offset(x * w + 0.015f * w, 0.37f * h), Size(0.03f * w, 0.36f * h), CornerRadius(w * 0.01f))
        }
        // Juntas de puertas y capó
        val seam = Color.Black.copy(alpha = 0.35f)
        drawLine(seam, Offset(0.15f * w, 0.52f * h), Offset(0.30f * w, 0.52f * h), w * 0.006f)
        drawLine(seam, Offset(0.70f * w, 0.52f * h), Offset(0.85f * w, 0.52f * h), w * 0.006f)
        drawLine(seam, Offset(0.26f * w, 0.08f * h), Offset(0.74f * w, 0.08f * h), w * 0.006f)
        // Faros y pilotos
        drawRoundRect(Color(0xFFEAF0F7), Offset(0.19f * w, 0.035f * h), Size(0.14f * w, 0.02f * h), CornerRadius(w * 0.02f))
        drawRoundRect(Color(0xFFEAF0F7), Offset(0.67f * w, 0.035f * h), Size(0.14f * w, 0.02f * h), CornerRadius(w * 0.02f))
        drawRoundRect(Color(0xFFE0262B), Offset(0.18f * w, 0.945f * h), Size(0.14f * w, 0.02f * h), CornerRadius(w * 0.02f))
        drawRoundRect(Color(0xFFE0262B), Offset(0.68f * w, 0.945f * h), Size(0.14f * w, 0.02f * h), CornerRadius(w * 0.02f))

        // Selección
        if (selected != null) {
            val rects = if (selected == CarZone.WHEELS) wheelRects else zoneRects.filter { it.first == selected }.map { it.second }
            rects.forEach { rect ->
                val rr = r(rect)
                drawRoundRect(selectionColor.copy(alpha = 0.45f), rr.topLeft, rr.size, CornerRadius(w * 0.03f))
                drawRoundRect(selectionColor, rr.topLeft, rr.size, CornerRadius(w * 0.03f), style = Stroke(w * 0.012f))
            }
        }

        // Marcadores de daños
        markers.forEach { (zone, color) -> zoneCenter(zone)?.let { c -> marker(Offset(c.x * w, c.y * h), color, w) } }

        // Etiquetas de presión
        wheelLabels?.forEachIndexed { i, label ->
            val wr = r(wheelRects[i])
            val layout = measurer.measure(label, TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White))
            val left = i % 2 == 0
            val x = if (left) wr.right + w * 0.04f else wr.left - layout.size.width - w * 0.04f
            val y = wr.center.y - layout.size.height / 2f
            drawRoundRect(
                Color.Black.copy(alpha = 0.55f),
                Offset(x - w * 0.015f, y - h * 0.004f),
                Size(layout.size.width + w * 0.03f, layout.size.height + h * 0.008f),
                CornerRadius(w * 0.03f),
            )
            drawText(layout, topLeft = Offset(x, y))
        }
    }
}

private fun DrawScope.marker(center: Offset, color: Color, w: Float) {
    drawCircle(color.copy(alpha = 0.3f), w * 0.07f, center)
    drawCircle(color, w * 0.04f, center)
    drawCircle(Color.White, w * 0.015f, center)
}
