package com.callankan.poloapp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callankan.poloapp.ui.format.Fmt
import com.callankan.poloapp.ui.theme.NumberStyles
import kotlin.math.roundToLong

/** Anillo de progreso animado con contenido centrado. */
@Composable
fun ProgressRing(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    strokeWidth: Dp = 7.dp,
    trackColor: Color = color.copy(alpha = 0.16f),
    content: @Composable BoxScope.() -> Unit = {},
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "ring",
    )
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(trackColor, -90f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            if (animated > 0f) {
                drawArc(
                    Brush.sweepGradient(listOf(color.copy(alpha = 0.7f), color, color)),
                    -90f, 360f * animated, false, Offset(inset, inset), arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}

/** Barra de progreso lineal redondeada con degradado. */
@Composable
fun LinearMeter(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "meter",
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(trackColor),
    ) {
        if (animated > 0.001f) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animated)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.75f), color))),
            )
        }
    }
}

/** Número que cuenta hacia su valor al aparecer (km, euros). */
@Composable
fun AnimatedCounter(
    value: Double,
    format: (Double) -> String,
    modifier: Modifier = Modifier,
    style: TextStyle = NumberStyles.hero,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(value) {
        anim.animateTo(value.toFloat(), tween(1100, easing = FastOutSlowInEasing))
    }
    Text(format(anim.value.toDouble()), style = style, color = color, modifier = modifier, maxLines = 1)
}

@Composable
fun AnimatedKm(km: Int, modifier: Modifier = Modifier, style: TextStyle = NumberStyles.hero, color: Color = MaterialTheme.colorScheme.onSurface) {
    AnimatedCounter(km.toDouble(), { Fmt.kmPlain(it.roundToLong().toInt()) }, modifier, style, color)
}
