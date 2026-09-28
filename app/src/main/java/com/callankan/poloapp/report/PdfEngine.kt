package com.callankan.poloapp.report

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import kotlin.math.max

/** Estilos del informe: sobrio, legible y con el rojo de la app como acento. */
class PdfStyle(val regular: Typeface, val bold: Typeface, val numbers: Typeface) {
    val ink = Color.rgb(22, 24, 29)
    val muted = Color.rgb(107, 114, 128)
    val accent = Color.rgb(209, 30, 42)
    val line = Color.rgb(229, 231, 235)
    val soft = Color.rgb(245, 246, 248)
    val green = Color.rgb(18, 165, 106)
    val amber = Color.rgb(224, 138, 0)
    val red = Color.rgb(217, 45, 58)

    // Texto con avance fraccional: sin esto el PDF pierde espacios en los tamaños pequeños.
    fun text(size: Float, bold: Boolean = false, color: Int = ink, numbers: Boolean = false) = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG or Paint.LINEAR_TEXT_FLAG).apply {
        typeface = if (numbers) this@PdfStyle.numbers else if (bold) this@PdfStyle.bold else regular
        textSize = size
        this.color = color
    }

    fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL }
    fun stroke(color: Int, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.STROKE; strokeWidth = width }
}

fun textLayout(text: CharSequence, paint: TextPaint, width: Float, align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL): StaticLayout =
    StaticLayout.Builder.obtain(text, 0, text.length, paint, max(1f, width).toInt())
        .setAlignment(align)
        .setLineSpacing(0f, 1.15f)
        .setIncludePad(false)
        .build()

fun Canvas.drawLayout(layout: StaticLayout, x: Float, y: Float) {
    save()
    translate(x, y)
    layout.draw(this)
    restore()
}

/** Pieza maquetable del informe. */
abstract class Block {
    abstract fun height(width: Float): Float
    abstract fun draw(canvas: Canvas, x: Float, y: Float, width: Float)
    open val keepWithNext: Boolean = false
    open val pageBreakBefore: Boolean = false
    /** Cabecera de tabla que se repite si la fila cae en una página nueva. */
    open val repeatHeader: Block? = null
}

class SpacerBlock(private val h: Float) : Block() {
    override fun height(width: Float) = h
    override fun draw(canvas: Canvas, x: Float, y: Float, width: Float) = Unit
}

class PageBreak : Block() {
    override val pageBreakBefore = true
    override fun height(width: Float) = 0f
    override fun draw(canvas: Canvas, x: Float, y: Float, width: Float) = Unit
}

class SectionTitle(private val style: PdfStyle, private val title: String, private val subtitle: String?) : Block() {
    override val keepWithNext = true
    private val titlePaint = style.text(15f, bold = true)
    private val subPaint = style.text(8.5f, color = style.muted)
    override fun height(width: Float) = 14f + 20f + (subtitle?.let { textLayout(it, subPaint, width).height + 4f } ?: 0f) + 10f
    override fun draw(canvas: Canvas, x: Float, y: Float, width: Float) {
        canvas.drawRect(x, y + 14f, x + 4f, y + 32f, style.fill(style.accent))
        canvas.drawText(title, x + 12f, y + 29f, titlePaint)
        subtitle?.let { canvas.drawLayout(textLayout(it, subPaint, width), x, y + 38f) }
    }
}

class Paragraph(private val text: CharSequence, private val paint: TextPaint, private val after: Float = 6f) : Block() {
    override fun height(width: Float) = textLayout(text, paint, width).height + after
    override fun draw(canvas: Canvas, x: Float, y: Float, width: Float) = canvas.drawLayout(textLayout(text, paint, width), x, y)
}

/** Rejilla de datos clave (etiqueta arriba, valor debajo). */
class FactGrid(private val style: PdfStyle, private val facts: List<Pair<String, String>>, private val columns: Int = 2) : Block() {
    private val label = style.text(7.5f, color = style.muted)
    private val value = style.text(10.5f, bold = true)
    private val rowH = 34f
    override fun height(width: Float) = ((facts.size + columns - 1) / columns) * rowH + 6f
    override fun draw(canvas: Canvas, x: Float, y: Float, width: Float) {
        val cw = width / columns
        facts.forEachIndexed { i, (l, v) ->
            val cx = x + (i % columns) * cw
            val cy = y + (i / columns) * rowH
            canvas.drawText(l.uppercase(), cx, cy + 10f, label)
            canvas.drawLayout(textLayout(v, value, cw - 10f), cx, cy + 14f)
            canvas.drawLine(cx, cy + rowH - 3f, cx + cw - 12f, cy + rowH - 3f, style.stroke(style.line, 0.6f))
        }
    }
}

/** Fila de tarjetas con cifras destacadas. */
class StatCards(private val style: PdfStyle, private val stats: List<Pair<String, String>>) : Block() {
    private val label = style.text(7.5f, color = style.muted)
    private val value = style.text(15f, numbers = true)
    override fun height(width: Float) = 58f
    override fun draw(canvas: Canvas, x: Float, y: Float, width: Float) {
        val gap = 8f
        val w = (width - gap * (stats.size - 1)) / stats.size
        stats.forEachIndexed { i, (l, v) ->
            val left = x + i * (w + gap)
            canvas.drawRoundRect(RectF(left, y, left + w, y + 50f), 8f, 8f, style.fill(style.soft))
            canvas.drawText(l.uppercase(), left + 10f, y + 17f, label)
            canvas.drawText(v, left + 10f, y + 38f, value)
        }
    }
}

class TableSpec(val titles: List<String>, val weights: List<Float>, val alignEnd: Set<Int> = emptySet())

class TableHeader(private val style: PdfStyle, private val spec: TableSpec) : Block() {
    override val keepWithNext = true
    private val paint = style.text(7.5f, bold = true, color = style.muted)
    override fun height(width: Float) = 22f
    override fun draw(canvas: Canvas, x: Float, y: Float, width: Float) {
        canvas.drawRoundRect(RectF(x, y, x + width, y + 20f), 5f, 5f, style.fill(style.soft))
        var cx = x
        val total = spec.weights.sum()
        spec.titles.forEachIndexed { i, t ->
            val cw = width * spec.weights[i] / total
            val tw = paint.measureText(t.uppercase())
            val tx = if (i in spec.alignEnd) cx + cw - 6f - tw else cx + 6f
            canvas.drawText(t.uppercase(), tx, y + 13.5f, paint)
            cx += cw
        }
    }
}

/** Fila de tabla con ajuste de línea por celda. [colors] permite resaltar una celda. */
class TableRow(
    private val style: PdfStyle,
    private val spec: TableSpec,
    private val cells: List<CharSequence>,
    private val header: TableHeader,
    private val colors: Map<Int, Int> = emptyMap(),
    private val boldColumns: Set<Int> = emptySet(),
) : Block() {
    override val repeatHeader: Block get() = header
    private fun paintFor(i: Int) = style.text(8.5f, bold = i in boldColumns, color = colors[i] ?: style.ink)
    private fun layouts(width: Float): List<StaticLayout> {
        val total = spec.weights.sum()
        return cells.mapIndexed { i, c ->
            val cw = width * spec.weights[i] / total - 12f
            textLayout(c, paintFor(i), cw, if (i in spec.alignEnd) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL)
        }
    }
    override fun height(width: Float) = (layouts(width).maxOfOrNull { it.height } ?: 10) + 14f
    override fun draw(canvas: Canvas, x: Float, y: Float, width: Float) {
        val total = spec.weights.sum()
        var cx = x
        layouts(width).forEachIndexed { i, l ->
            val cw = width * spec.weights[i] / total
            canvas.drawLayout(l, cx + 6f, y + 7f)
            cx += cw
        }
        val h = height(width)
        canvas.drawLine(x, y + h, x + width, y + h, style.stroke(style.line, 0.6f))
    }
}

/** Gráfica de líneas sencilla (kilometraje a lo largo del tiempo). */
class LineChartBlock(
    private val style: PdfStyle,
    private val values: List<Double>,
    private val startLabel: String,
    private val endLabel: String,
    private val format: (Double) -> String,
) : Block() {
    override fun height(width: Float) = 150f
    override fun draw(canvas: Canvas, x: Float, y: Float, width: Float) {
        if (values.size < 2) return
        val label = style.text(7.5f, color = style.muted)
        val top = y + 10f
        val bottom = y + 120f
        val left = x + 50f
        val right = x + width
        val min = values.min()
        val max = values.max()
        val range = (max - min).takeIf { it > 0 } ?: 1.0
        for (i in 0..3) {
            val gy = top + (bottom - top) * i / 3f
            canvas.drawLine(left, gy, right, gy, style.stroke(style.line, 0.6f))
            canvas.drawText(format(max - range * i / 3), x, gy + 3f, label)
        }
        val step = (right - left) / (values.size - 1)
        val path = Path()
        val fill = Path()
        values.forEachIndexed { i, v ->
            val px = left + i * step
            val py = (bottom - (bottom - top) * ((v - min) / range)).toFloat()
            if (i == 0) {
                path.moveTo(px, py)
                fill.moveTo(px, bottom)
                fill.lineTo(px, py)
            } else {
                path.lineTo(px, py)
                fill.lineTo(px, py)
            }
        }
        fill.lineTo(right, bottom)
        fill.close()
        canvas.drawPath(fill, style.fill(Color.argb(28, 209, 30, 42)))
        canvas.drawPath(path, style.stroke(style.accent, 1.8f).apply { strokeJoin = Paint.Join.ROUND })
        canvas.drawText(startLabel, left, bottom + 16f, label)
        canvas.drawText(endLabel, right - label.measureText(endLabel), bottom + 16f, label)
    }
}

/** Foto con pie. Se decodifica solo al dibujar (la maquetación no carga imágenes). */
class ImageBlock(
    private val style: PdfStyle,
    private val caption: String,
    private val loader: () -> Bitmap?,
    private val boxHeight: Float = 300f,
) : Block() {
    override fun height(width: Float) = boxHeight + 30f
    override fun draw(canvas: Canvas, x: Float, y: Float, width: Float) {
        val box = RectF(x, y, x + width, y + boxHeight)
        canvas.drawRoundRect(box, 8f, 8f, style.fill(style.soft))
        loader()?.let { bmp ->
            val scale = minOf(box.width() / bmp.width, box.height() / bmp.height)
            val w = bmp.width * scale
            val h = bmp.height * scale
            val dst = RectF(box.centerX() - w / 2, box.centerY() - h / 2, box.centerX() + w / 2, box.centerY() + h / 2)
            canvas.drawBitmap(bmp, null, dst, Paint(Paint.FILTER_BITMAP_FLAG))
            bmp.recycle()
        }
        canvas.drawLayout(textLayout(caption, style.text(8f, color = style.muted), width), x, y + boxHeight + 6f)
    }
}

/** Bloque a medida, p. ej. la portada. */
class CustomBlock(private val h: Float, private val drawer: (Canvas, Float, Float, Float) -> Unit) : Block() {
    override fun height(width: Float) = h
    override fun draw(canvas: Canvas, x: Float, y: Float, width: Float) = drawer(canvas, x, y, width)
}

/**
 * Pagina los bloques en A4 y los dibuja. Repite cabeceras de tabla y respeta "keepWithNext".
 */
class PdfComposer(
    private val style: PdfStyle,
    private val headerText: String,
    private val footerText: String,
) {
    private val pageW = 595
    private val pageH = 842
    private val margin = 42f
    private val contentTop = 64f
    private val contentBottom = pageH - 48f
    private val width = pageW - margin * 2

    private data class Placed(val block: Block, val y: Float)

    private fun paginate(blocks: List<Block>): List<List<Placed>> {
        val pages = mutableListOf<MutableList<Placed>>()
        var current = mutableListOf<Placed>()
        var y = contentTop
        fun newPage() {
            pages += current
            current = mutableListOf()
            y = contentTop
        }
        var i = 0
        while (i < blocks.size) {
            val b = blocks[i]
            if (b.pageBreakBefore && current.isNotEmpty()) newPage()
            // Mantiene juntos los bloques encadenados (título → cabecera de tabla → primera fila)
            var h = b.height(width)
            var j = i
            while (blocks[j].keepWithNext && j + 1 < blocks.size) {
                j++
                h += blocks[j].height(width)
            }
            if (y + h > contentBottom && current.isNotEmpty()) {
                newPage()
                b.repeatHeader?.let { header ->
                    current += Placed(header, y)
                    y += header.height(width)
                }
            }
            current += Placed(b, y)
            y += b.height(width)
            i++
        }
        if (current.isNotEmpty()) pages += current
        return pages
    }

    /** @param cover dibuja la portada a página completa (sin cabecera ni pie). */
    fun render(doc: PdfDocument, cover: ((Canvas) -> Unit)?, blocks: List<Block>) {
        val pages = paginate(blocks)
        val total = pages.size + if (cover != null) 1 else 0
        var number = 1
        if (cover != null) {
            val page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, number).create())
            cover(page.canvas)
            doc.finishPage(page)
            number++
        }
        pages.forEach { placed ->
            val page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, number).create())
            val c = page.canvas
            c.drawColor(Color.WHITE)
            val small = style.text(7.5f, color = style.muted)
            c.drawText(headerText, margin, 34f, small)
            c.drawLine(margin, 42f, pageW - margin, 42f, style.stroke(style.line, 0.8f))
            placed.forEach { it.block.draw(c, margin, it.y, width) }
            c.drawLine(margin, pageH - 34f, pageW - margin, pageH - 34f, style.stroke(style.line, 0.8f))
            c.drawText(footerText, margin, pageH - 20f, small)
            val pn = "Página $number de $total"
            c.drawText(pn, pageW - margin - small.measureText(pn), pageH - 20f, small)
            doc.finishPage(page)
            number++
        }
    }

    companion object {
        const val PAGE_W = 595f
        const val PAGE_H = 842f
    }
}
