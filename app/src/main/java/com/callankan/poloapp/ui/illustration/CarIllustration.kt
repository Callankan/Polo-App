package com.callankan.poloapp.ui.illustration

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb

/** Ilustración del coche en Compose (reutiliza [CarPainter]). */
@Composable
fun CarIllustration(
    bodyColor: Color,
    modifier: Modifier = Modifier,
    shadow: Boolean = true,
) {
    Canvas(modifier.aspectRatio(CarPainter.DESIGN_WIDTH / CarPainter.DESIGN_HEIGHT)) {
        drawIntoCanvas { canvas ->
            CarPainter.draw(canvas.nativeCanvas, size.width, size.height, bodyColor.toArgb(), shadow)
        }
    }
}
