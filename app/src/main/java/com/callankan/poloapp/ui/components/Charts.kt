package com.callankan.poloapp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callankan.poloapp.ui.theme.PoloTheme

@Composable
private fun rememberReveal(key: Any?): Animatable<Float, *> {
    val anim = remember(key) { Animatable(0f) }
    LaunchedEffect(key) { anim.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    return anim
}

/** Línea suavizada con relleno degradado. Se revela de izquierda a derecha. */
@Composable
fun LineChart(
    values: List<Double>,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
    labels: List<String> = emptyList(),
    valueLabel: (Double) -> String = { "%.1f".format(it) },
    showReference: Double? = null,
) {
    if (values.isEmpty()) return
    val reveal = rememberReveal(values)
    val grid = PoloTheme.colors.chartGrid
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val max = values.max()
    val min = values.min()
    Column(modifier) {
        Row(Modifier.fillMaxWidth()) {
            Text(valueLabel(max), style = MaterialTheme.typography.labelSmall, color = labelColor)
            Spacer(Modifier.weight(1f))
            Text("mín. ${valueLabel(min)}", style = MaterialTheme.typography.labelSmall, color = labelColor)
        }
        Spacer(Modifier.height(6.dp))
        Canvas(Modifier.fillMaxWidth().height(height)) {
            val range = (max - min).takeIf { it > 1e-6 } ?: 1.0
            val padTop = 8.dp.toPx()
            val padBottom = 8.dp.toPx()
            val h = size.height - padTop - padBottom
            fun y(v: Double) = padTop + (h * (1 - (v - min) / range)).toFloat()
            val step = if (values.size > 1) size.width / (values.size - 1) else 0f
            val points = values.mapIndexed { i, v -> Offset(if (values.size == 1) size.width / 2 else i * step, y(v)) }

            for (i in 0..3) {
                val gy = padTop + h * i / 3f
                drawLine(grid, Offset(0f, gy), Offset(size.width, gy), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)))
            }
            if (showReference != null && showReference in min..max) {
                val ry = y(showReference)
                drawLine(color.copy(alpha = 0.45f), Offset(0f, ry), Offset(size.width, ry), 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
            }

            val line = Path()
            points.forEachIndexed { i, p ->
                if (i == 0) line.moveTo(p.x, p.y) else {
                    val prev = points[i - 1]
                    val cx = (prev.x + p.x) / 2
                    line.cubicTo(cx, prev.y, cx, p.y, p.x, p.y)
                }
            }
            val fill = Path().apply {
                addPath(line)
                lineTo(points.last().x, size.height)
                lineTo(points.first().x, size.height)
                close()
            }
            clipRect(right = size.width * reveal.value) {
                drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.32f), color.copy(alpha = 0f))))
                drawPath(line, color, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                points.forEachIndexed { i, p ->
                    if (i == points.lastIndex || values.size <= 12) {
                        drawCircle(color, if (i == points.lastIndex) 5.dp.toPx() else 3.dp.toPx(), p)
                        if (i == points.lastIndex) drawCircle(Color.White, 2.dp.toPx(), p)
                    }
                }
            }
        }
        if (labels.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth()) {
                labels.forEachIndexed { i, l ->
                    Text(
                        l,
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor,
                        modifier = Modifier.weight(1f),
                        textAlign = when (i) {
                            0 -> TextAlign.Start
                            labels.lastIndex -> TextAlign.End
                            else -> TextAlign.Center
                        },
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

data class BarSeries(val label: String, val color: Color)

/** Barras apiladas por mes. [values] = una lista por barra con un valor por serie. */
@Composable
fun StackedBarChart(
    values: List<List<Double>>,
    series: List<BarSeries>,
    barLabels: List<String>,
    modifier: Modifier = Modifier,
    height: Dp = 170.dp,
    highlightLast: Boolean = true,
) {
    if (values.isEmpty()) return
    val reveal = rememberReveal(values)
    val grid = PoloTheme.colors.chartGrid
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val max = values.maxOf { it.sum() }.takeIf { it > 0 } ?: 1.0
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            for (i in 0..3) {
                val gy = size.height * i / 3f
                drawLine(grid, Offset(0f, gy), Offset(size.width, gy), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)))
            }
            val slot = size.width / values.size
            val barW = (slot * 0.56f).coerceAtMost(28.dp.toPx())
            values.forEachIndexed { i, stack ->
                var top = size.height
                val x = i * slot + (slot - barW) / 2
                val dim = highlightLast && i != values.lastIndex
                stack.forEachIndexed { s, v ->
                    if (v <= 0) return@forEachIndexed
                    val h = (size.height * (v / max) * reveal.value).toFloat()
                    top -= h
                    val c = series[s].color.let { if (dim) it.copy(alpha = 0.65f) else it }
                    drawRoundRect(c, Offset(x, top), Size(barW, h), CornerRadius(6.dp.toPx(), 6.dp.toPx()))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            barLabels.forEach {
                Text(it, style = MaterialTheme.typography.labelSmall, color = labelColor, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, maxLines = 1)
            }
        }
        if (series.size > 1) {
            Spacer(Modifier.height(10.dp))
            Legend(series.map { it.label to it.color })
        }
    }
}

@Composable
fun Legend(items: List<Pair<String, Color>>, modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Donut con huecos entre segmentos. */
@Composable
fun DonutChart(
    slices: List<Pair<Double, Color>>,
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,
    thickness: Dp = 18.dp,
    center: @Composable () -> Unit = {},
) {
    val total = slices.sumOf { it.first }.takeIf { it > 0 } ?: return
    val reveal = rememberReveal(slices)
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = thickness.toPx()
            val inset = stroke / 2
            val arc = Size(this.size.width - stroke, this.size.height - stroke)
            var start = -90f
            val gap = if (slices.size > 1) 3f else 0f
            slices.forEach { (v, c) ->
                val sweep = (360f * (v / total).toFloat()) * reveal.value
                if (sweep > gap) {
                    drawArc(c, start + gap / 2, sweep - gap, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Butt))
                }
                start += sweep
            }
        }
        center()
    }
}

/** Mini gráfica sin ejes para tarjetas. */
@Composable
fun Sparkline(values: List<Double>, color: Color, modifier: Modifier = Modifier) {
    if (values.size < 2) return
    val reveal = rememberReveal(values)
    Canvas(modifier) {
        val max = values.max()
        val min = values.min()
        val range = (max - min).takeIf { it > 1e-6 } ?: 1.0
        val step = size.width / (values.size - 1)
        val pad = 3.dp.toPx()
        val pts = values.mapIndexed { i, v -> Offset(i * step, pad + ((size.height - 2 * pad) * (1 - (v - min) / range)).toFloat()) }
        val path = Path().apply {
            pts.forEachIndexed { i, p ->
                if (i == 0) moveTo(p.x, p.y) else {
                    val prev = pts[i - 1]
                    val cx = (prev.x + p.x) / 2
                    cubicTo(cx, prev.y, cx, p.y, p.x, p.y)
                }
            }
        }
        clipRect(right = size.width * reveal.value) {
            drawPath(path, color, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawCircle(color, 3.5.dp.toPx(), pts.last())
        }
    }
}

/** Barra horizontal segmentada (reparto de costes compacto). */
@Composable
fun SegmentedBar(parts: List<Pair<Double, Color>>, modifier: Modifier = Modifier, height: Dp = 10.dp) {
    val total = parts.sumOf { it.first }.takeIf { it > 0 } ?: return
    Row(modifier.fillMaxWidth().height(height).clip(CircleShape), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        parts.filter { it.first > 0 }.forEach { (v, c) ->
            Box(Modifier.weight((v / total).toFloat().coerceAtLeast(0.01f)).height(height).background(c))
        }
    }
}

@Composable
fun ChartCard(title: String, subtitle: String? = null, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    PoloCard(modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.padding(top = 14.dp))
        content()
    }
}
