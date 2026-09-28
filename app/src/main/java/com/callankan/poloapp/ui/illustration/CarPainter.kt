package com.callankan.poloapp.ui.illustration

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.PathParser
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Ilustración vectorial de perfil de un Volkswagen Polo Mk5 (6R) de 5 puertas.
 *
 * Se dibuja sobre un [Canvas] de Android para poder reutilizarla tanto en Compose
 * (vía `nativeCanvas`) como en el informe PDF. El color de carrocería es configurable.
 * Coordenadas de diseño: 1000 x 420, morro a la izquierda.
 */
object CarPainter {
    const val DESIGN_WIDTH = 1000f
    const val DESIGN_HEIGHT = 420f

    private const val BODY =
        "M72 333 C58 329 50 316 48 298 C46 280 47 262 52 246 C56 230 66 208 90 197 C170 177 262 157 336 144 " +
            "C408 98 474 60 548 38 C630 26 760 28 850 40 C864 42 874 47 876 54 C877 58 874 60 868 60 " +
            "C888 86 912 112 926 130 C938 152 944 196 946 238 C948 262 952 290 950 310 C948 324 940 332 926 334 " +
            "L861 334 A82 82 0 1 0 717 334 L301 334 A82 82 0 1 0 157 334 Z"
    private const val GLASS =
        "M366 148 C418 110 482 72 552 52 C640 42 752 44 828 54 C842 56 848 62 850 72 L858 118 " +
            "C860 124 856 128 848 128 C700 132 520 140 380 152 C368 153 362 151 366 148 Z"
    private const val PILLAR_B = "M594 48 L612 47 L606 141 L590 142 Z"
    private const val PILLAR_C = "M790 48 L800 49 L806 128 L796 128 Z"
    private const val REFLECTION_1 = "M470 90 L520 60 L470 146 L430 148 Z"
    private const val REFLECTION_2 = "M660 48 L700 47 L650 140 L615 141 Z"
    private const val SHOULDER = "M112 200 C300 186 600 174 944 162"
    private const val LOWER_SHADE = "M104 206 C300 192 600 180 945 168 L947 238 C700 246 400 262 90 268 Z"
    private const val LOWER_CREASE = "M316 300 C450 296 600 294 712 292"
    private const val DOOR_FRONT = "M374 152 C362 192 352 250 350 326"
    private const val DOOR_MIDDLE = "M606 141 C604 200 603 260 604 330"
    private const val DOOR_REAR = "M806 128 C810 160 810 190 800 208 C780 212 748 222 730 240 C718 254 714 280 714 300"
    private const val MIRROR = "M372 146 C374 130 384 122 402 120 C416 119 424 124 424 134 C424 144 418 150 406 152 L380 154 C374 154 371 151 372 146 Z"
    private const val MIRROR_SHADE = "M376 149 L420 144 C418 150 412 153 404 153 L380 155 Z"
    private const val HEADLIGHT = "M58 236 C62 222 72 210 90 202 C124 194 162 188 186 186 C180 200 164 216 142 228 C114 238 86 242 66 242 C60 242 57 240 58 236 Z"
    private const val HEADLIGHT_LENS = "M76 220 C98 210 132 202 162 197 C152 207 138 216 120 223 C104 228 90 230 78 230 Z"
    private const val FOG = "M50 286 C54 280 70 278 92 280 L100 300 C80 302 62 302 52 300 Z"
    private const val FOG_LENS = "M60 288 L84 287 L88 294 L62 295 Z"
    private const val TAIL = "M904 150 C920 150 934 150 942 152 C944 170 946 188 946 204 C930 200 916 196 906 190 C902 176 902 162 904 150 Z"
    private const val TAIL_LENS = "M910 156 C924 156 934 157 939 159 C940 172 941 184 941 194 C928 190 918 186 911 182 Z"
    private const val REFLECTOR = "M930 288 L948 286 L948 294 L930 296 Z"
    private const val SILL = "M300 322 L718 322 L717 334 L301 334 Z"
    private const val ARCHES = "M861 334 A82 82 0 1 0 717 334 M301 334 A82 82 0 1 0 157 334"

    private val paths = HashMap<String, Path>()
    private fun path(d: String): Path = paths.getOrPut(d) { PathParser.createPathFromPathData(d) }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    /**
     * Dibuja el coche centrado en el rectángulo [0, width] x [0, height].
     * @param shadow dibuja la sombra en el suelo.
     */
    fun draw(canvas: Canvas, width: Float, height: Float, bodyColor: Int, shadow: Boolean = true) {
        val scale = min(width / DESIGN_WIDTH, height / DESIGN_HEIGHT)
        val dx = (width - DESIGN_WIDTH * scale) / 2f
        val dy = (height - DESIGN_HEIGHT * scale) / 2f
        canvas.save()
        canvas.translate(dx, dy)
        canvas.scale(scale, scale)
        synchronized(this) { drawDesign(canvas, bodyColor, shadow) }
        canvas.restore()
    }

    private fun drawDesign(c: Canvas, base: Int, shadow: Boolean) {
        val luminance = ColorUtils.calculateLuminance(base)
        val top = ColorUtils.blendARGB(base, Color.WHITE, if (luminance > 0.7) 0.4f else 0.28f)
        val mid = base
        val low = ColorUtils.blendARGB(base, Color.BLACK, if (luminance < 0.05) 0.2f else 0.24f)
        val bottom = ColorUtils.blendARGB(base, Color.BLACK, if (luminance < 0.05) 0.45f else 0.55f)
        val seam = ColorUtils.setAlphaComponent(ColorUtils.blendARGB(base, Color.BLACK, 0.72f), 210)
        val handle = ColorUtils.blendARGB(base, Color.WHITE, 0.22f)

        if (shadow) {
            paint.shader = RadialGradient(
                500f, 362f, 480f,
                intArrayOf(Color.argb(190, 0, 0, 0), Color.argb(0, 0, 0, 0)),
                floatArrayOf(0f, 1f), Shader.TileMode.CLAMP,
            ).also { g -> g.setLocalMatrix(Matrix().apply { setScale(1f, 22f / 480f, 500f, 362f) }) }
            c.drawOval(20f, 340f, 980f, 384f, paint)
            paint.shader = null
        }

        // Carrocería con degradado vertical y volumen lateral
        paint.shader = LinearGradient(
            0f, 28f, 0f, 334f,
            intArrayOf(top, mid, low, bottom), floatArrayOf(0f, 0.38f, 0.62f, 1f), Shader.TileMode.CLAMP,
        )
        c.drawPath(path(BODY), paint)
        paint.shader = LinearGradient(
            48f, 0f, 952f, 0f,
            intArrayOf(Color.argb(90, 0, 0, 0), 0, 0, Color.argb(90, 0, 0, 0)),
            floatArrayOf(0f, 0.12f, 0.86f, 1f), Shader.TileMode.CLAMP,
        )
        c.drawPath(path(BODY), paint)
        paint.shader = null

        // Cristales
        paint.shader = LinearGradient(366f, 40f, 860f, 152f, Color.rgb(42, 49, 60), Color.rgb(11, 13, 17), Shader.TileMode.CLAMP)
        c.drawPath(path(GLASS), paint)
        paint.shader = null
        fill(c, PILLAR_B, Color.rgb(10, 12, 16))
        fill(c, PILLAR_C, Color.rgb(10, 12, 16))
        fill(c, REFLECTION_1, Color.argb(18, 255, 255, 255))
        fill(c, REFLECTION_2, Color.argb(13, 255, 255, 255))

        // Línea de hombro (tornado line) y volúmenes
        line(c, SHOULDER, Color.argb(if (luminance > 0.7) 150 else 90, 255, 255, 255), 2.5f)
        fill(c, LOWER_SHADE, Color.argb(26, 0, 0, 0))
        line(c, LOWER_CREASE, Color.argb(46, 0, 0, 0), 2f)

        // Juntas de puertas
        line(c, DOOR_FRONT, seam, 2f)
        line(c, DOOR_MIDDLE, seam, 2f)
        line(c, DOOR_REAR, seam, 2f)

        // Tiradores
        paint.color = Color.argb(64, 0, 0, 0)
        c.drawRoundRect(508f, 178f, 552f, 187f, 4.5f, 4.5f, paint)
        c.drawRoundRect(716f, 170f, 758f, 179f, 4.5f, 4.5f, paint)
        paint.color = handle
        c.drawRoundRect(508f, 175f, 552f, 183f, 4f, 4f, paint)
        c.drawRoundRect(716f, 167f, 758f, 175f, 4f, 4f, paint)

        // Tapa del depósito
        stroke.color = ColorUtils.setAlphaComponent(seam, 150)
        stroke.strokeWidth = 2f
        c.drawCircle(850f, 178f, 15f, stroke)

        // Retrovisor
        paint.shader = LinearGradient(0f, 118f, 0f, 154f, top, low, Shader.TileMode.CLAMP)
        c.drawPath(path(MIRROR), paint)
        paint.shader = null
        fill(c, MIRROR_SHADE, Color.argb(90, 0, 0, 0))

        // Faros y pilotos
        fill(c, HEADLIGHT, Color.rgb(27, 31, 38))
        fill(c, HEADLIGHT_LENS, Color.argb(230, 233, 238, 245))
        paint.color = Color.WHITE
        c.drawCircle(104f, 219f, 7f, paint)
        fill(c, FOG, Color.rgb(22, 25, 30))
        fill(c, FOG_LENS, Color.argb(204, 223, 230, 238))
        fill(c, TAIL, Color.rgb(122, 10, 16))
        fill(c, TAIL_LENS, Color.argb(230, 255, 59, 59))
        fill(c, REFLECTOR, Color.rgb(192, 17, 26))

        // Taloneras y pasos de rueda
        fill(c, SILL, Color.argb(140, 12, 14, 18))
        line(c, ARCHES, Color.argb(102, 0, 0, 0), 4f)

        wheel(c, 229f, 292f)
        wheel(c, 789f, 292f)
    }

    private fun wheel(c: Canvas, cx: Float, cy: Float) {
        paint.shader = null
        paint.color = Color.rgb(7, 8, 10)
        c.drawCircle(cx, cy, 80f, paint)
        paint.color = Color.rgb(21, 23, 27)
        c.drawCircle(cx, cy, 68f, paint)
        stroke.color = Color.rgb(43, 47, 54)
        stroke.strokeWidth = 3f
        c.drawCircle(cx, cy, 66f, stroke)
        paint.color = Color.rgb(59, 64, 72)
        c.drawCircle(cx, cy, 48f, paint)
        paint.color = Color.rgb(85, 91, 99)
        c.drawCircle(cx, cy, 30f, paint)

        paint.shader = RadialGradient(
            cx - 10f, cy - 16f, 60f,
            intArrayOf(Color.rgb(244, 246, 248), Color.rgb(185, 191, 199), Color.rgb(124, 131, 140)),
            floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP,
        )
        val spoke = Path()
        for (i in 0 until 10) {
            val a = i * PI / 5
            spoke.reset()
            spoke.moveTo(cx + (cos(a - 0.09) * 9).toFloat(), cy + (sin(a - 0.09) * 9).toFloat())
            spoke.lineTo(cx + (cos(a - 0.16) * 46).toFloat(), cy + (sin(a - 0.16) * 46).toFloat())
            spoke.lineTo(cx + (cos(a + 0.16) * 46).toFloat(), cy + (sin(a + 0.16) * 46).toFloat())
            spoke.lineTo(cx + (cos(a + 0.09) * 9).toFloat(), cy + (sin(a + 0.09) * 9).toFloat())
            spoke.close()
            c.drawPath(spoke, paint)
        }
        paint.shader = null
        stroke.color = Color.rgb(207, 212, 218)
        stroke.strokeWidth = 3f
        c.drawCircle(cx, cy, 48f, stroke)
        paint.color = Color.rgb(201, 206, 212)
        c.drawCircle(cx, cy, 10f, paint)
        paint.color = Color.rgb(29, 33, 39)
        c.drawCircle(cx, cy, 6f, paint)
    }

    private fun fill(c: Canvas, d: String, color: Int) {
        paint.shader = null
        paint.color = color
        c.drawPath(path(d), paint)
    }

    private fun line(c: Canvas, d: String, color: Int, width: Float) {
        stroke.color = color
        stroke.strokeWidth = width
        c.drawPath(path(d), stroke)
    }
}
