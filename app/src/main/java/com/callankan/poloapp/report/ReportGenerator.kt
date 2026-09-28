package com.callankan.poloapp.report

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.pdf.PdfDocument
import androidx.core.content.res.ResourcesCompat
import com.callankan.poloapp.R
import com.callankan.poloapp.data.db.entity.AttachmentEntity
import com.callankan.poloapp.data.db.entity.VehicleEntity
import com.callankan.poloapp.data.model.AttachmentOwner
import com.callankan.poloapp.data.model.ItvResult
import com.callankan.poloapp.data.model.PerformedBy
import com.callankan.poloapp.data.repository.AttachmentRepository
import com.callankan.poloapp.data.repository.ComplianceRepository
import com.callankan.poloapp.data.repository.LogbookRepository
import com.callankan.poloapp.data.repository.MaintenanceRepository
import com.callankan.poloapp.data.repository.OverviewRepository
import com.callankan.poloapp.data.repository.VehicleOverview
import com.callankan.poloapp.data.repository.VehicleRepository
import com.callankan.poloapp.domain.DueState
import com.callankan.poloapp.ui.format.Fmt
import com.callankan.poloapp.ui.illustration.CarPainter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

data class ReportOptions(
    val includePrices: Boolean = true,
    val includePlate: Boolean = true,
    val includeVin: Boolean = true,
    val includeOwner: Boolean = false,
    val includeDamages: Boolean = true,
    val includeFuel: Boolean = true,
    val includePhotos: Boolean = true,
)

/**
 * Genera el informe "Historial del vehículo" pensado para un comprador: portada, kilometraje,
 * mantenimiento con talleres, estado de las piezas, ITV, daños y anexo de facturas.
 * La deuda y las notas personales nunca se incluyen.
 */
@Singleton
class ReportGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vehicles: VehicleRepository,
    private val overviews: OverviewRepository,
    private val maintenance: MaintenanceRepository,
    private val compliance: ComplianceRepository,
    private val logbook: LogbookRepository,
    private val attachments: AttachmentRepository,
) {
    suspend fun generate(vehicleId: Long, options: ReportOptions): File = withContext(Dispatchers.IO) {
        val vehicle = requireNotNull(vehicles.get(vehicleId))
        val overview = requireNotNull(overviews.snapshot(vehicleId))
        val records = maintenance.records(vehicleId)
        val itv = compliance.itv(vehicleId)
        val damages = if (options.includeDamages) logbook.damages(vehicleId) else emptyList()
        val events = vehicles.odometerEvents(vehicleId)
        val today = LocalDate.now()

        val style = PdfStyle(
            ResourcesCompat.getFont(context, R.font.manrope_regular)!!,
            ResourcesCompat.getFont(context, R.font.manrope_bold)!!,
            ResourcesCompat.getFont(context, R.font.spacegrotesk_bold)!!,
        )
        val body = style.text(9.5f)
        val small = style.text(8f, color = style.muted)
        val workshopCount = records.count { it.record.performedBy == PerformedBy.WORKSHOP }
        val reference = "PA-" + MessageDigest.getInstance("SHA-256")
            .digest("${vehicle.id}${vehicle.vin}${records.size}${events.size}$today".toByteArray())
            .take(4).joinToString("") { "%02X".format(it) }

        val blocks = mutableListOf<Block>()

        // Resumen
        blocks += SectionTitle(style, "Resumen", "Datos registrados por el propietario en Polo App")
        val first = events.minByOrNull { it.date }?.date
        val consistency = if (overview.odometer.anomalies.isEmpty()) "El kilometraje registrado es coherente: nunca disminuye entre lecturas."
        else "Atención: hay ${overview.odometer.anomalies.size} lectura(s) de kilometraje inferiores a otras anteriores."
        blocks += Paragraph(
            "Este informe recoge ${records.size} intervenciones de mantenimiento y reparación ($workshopCount en taller), " +
                "${itv.size} inspecciones ITV y ${events.size} lecturas del cuentakilómetros" +
                (first?.let { " registradas desde ${Fmt.longDate(it)}" } ?: "") + ". $consistency",
            body,
        )
        blocks += StatCards(
            style,
            listOfNotNull(
                "Kilometraje" to Fmt.km(overview.currentKm),
                overview.odometer.kmPerYear?.let { "Media anual" to Fmt.km(it) },
                "Intervenciones" to "${records.size}",
                if (options.includeFuel) overview.fuel.averageLitersPer100?.let { "Consumo medio" to "${Fmt.twoDecimals(it)} l" } else null,
            ).take(4),
        )
        blocks += SpacerBlock(8f)

        // Kilometraje
        if (events.isNotEmpty()) {
            blocks += SectionTitle(style, "Historial de kilometraje", "Última lectura de cada mes. Cualquier registro con km (repostajes, taller, ITV...) cuenta como lectura.")
            val sorted = events.sortedWith(compareBy({ it.date }, { it.km }))
            if (sorted.size >= 2) {
                blocks += LineChartBlock(style, sorted.map { it.km.toDouble() }, Fmt.date(sorted.first().date), Fmt.date(sorted.last().date)) { Fmt.km(it.toInt()) }
            }
            val spec = TableSpec(listOf("Mes", "Kilómetros", "Origen", "Lecturas"), listOf(1.3f, 1.2f, 1.4f, 0.8f), alignEnd = setOf(1, 3))
            val header = TableHeader(style, spec)
            blocks += header
            sorted.groupBy { YearMonth.from(it.date) }.toSortedMap().forEach { (month, list) ->
                val last = list.maxBy { it.km }
                blocks += TableRow(style, spec, listOf(Fmt.monthYear(month), Fmt.km(last.km), last.source.label, "${list.size}"), header, boldColumns = setOf(1))
            }
            blocks += SpacerBlock(10f)
        }

        // Mantenimiento
        blocks += SectionTitle(style, "Mantenimiento y reparaciones", "$workshopCount en taller · ${records.size - workshopCount} realizadas por el propietario")
        if (records.isEmpty()) {
            blocks += Paragraph("No hay intervenciones registradas.", small)
        } else {
            val titles = mutableListOf("Fecha", "Km", "Intervención", "Realizado por")
            val weights = mutableListOf(0.9f, 0.9f, 2.6f, 1.3f)
            if (options.includePrices) {
                titles += "Coste"
                weights += 0.8f
            }
            val spec = TableSpec(titles, weights, alignEnd = setOf(1) + if (options.includePrices) setOf(4) else emptySet())
            val header = TableHeader(style, spec)
            blocks += header
            records.sortedWith(compareBy({ it.record.date }, { it.record.odometerKm })).forEach { d ->
                val r = d.record
                val parts = d.installations.joinToString(", ") { i ->
                    i.component.name + listOf(i.installation.brand, i.installation.reference).filter { it.isNotBlank() }.joinToString(" ").let { if (it.isBlank()) "" else " ($it)" }
                }
                val what = buildString {
                    append(r.title)
                    if (parts.isNotBlank()) append("\nPiezas: ").append(parts)
                    if (r.invoiceNumber.isNotBlank()) append("\nFactura ").append(r.invoiceNumber)
                }
                val who = if (r.performedBy == PerformedBy.WORKSHOP) d.workshop?.name ?: "Taller" else "Propietario"
                val cells = mutableListOf<CharSequence>(Fmt.numericDate(r.date), Fmt.km(r.odometerKm), what, who)
                if (options.includePrices) cells += if (r.totalCost > 0) Fmt.money(r.totalCost) else "—"
                blocks += TableRow(style, spec, cells, header)
            }
            if (options.includePrices) {
                blocks += Paragraph("Total invertido en mantenimiento y reparaciones: ${Fmt.money(records.sumOf { it.record.totalCost })}", style.text(9f, bold = true), after = 10f)
            }
        }

        // Estado de las piezas
        val tracked = overview.components.filter { it.current != null }
        if (tracked.isNotEmpty()) {
            blocks += SectionTitle(style, "Estado de las piezas", "Kilómetros recorridos desde el último cambio de cada elemento")
            val spec = TableSpec(listOf("Pieza", "Último cambio", "Km desde", "Próximo", "Estado"), listOf(1.8f, 1.4f, 0.9f, 1.3f, 0.9f), alignEnd = setOf(2))
            val header = TableHeader(style, spec)
            blocks += header
            tracked.sortedBy { it.component.sortOrder }.forEach { s ->
                val next = listOfNotNull(s.dueKm?.let { Fmt.km(it) }, s.dueDate?.let { Fmt.numericDate(it) }).joinToString("\n").ifEmpty { "—" }
                val (label, color) = when (s.state) {
                    DueState.OVERDUE -> "Vencido" to style.red
                    DueState.SOON -> "Próximo" to style.amber
                    else -> "Al día" to style.green
                }
                blocks += TableRow(
                    style, spec,
                    listOf(s.component.name, "${Fmt.numericDate(s.current!!.date)}\n${Fmt.km(s.current.odometerKm)}", Fmt.km(s.kmSinceInstall), next, label),
                    header, colors = mapOf(4 to color), boldColumns = setOf(4),
                )
            }
            blocks += SpacerBlock(10f)
        }

        // ITV
        blocks += SectionTitle(style, "Inspecciones técnicas (ITV)", overview.itv.date?.let { "ITV en vigor hasta el ${Fmt.longDate(it)}" })
        if (itv.isEmpty()) {
            blocks += Paragraph("No hay inspecciones registradas en la app.", small)
        } else {
            val spec = TableSpec(listOf("Fecha", "Km", "Estación", "Resultado", "Válida hasta"), listOf(0.9f, 0.9f, 1.6f, 1.5f, 1f), alignEnd = setOf(1))
            val header = TableHeader(style, spec)
            blocks += header
            itv.sortedBy { it.date }.forEach { i ->
                val color = if (i.result == ItvResult.FAVORABLE || i.result == ItvResult.FAVORABLE_WITH_DEFECTS) style.green else style.red
                val result = i.result.label + if (i.defects.isNotBlank()) "\n${i.defects}" else ""
                blocks += TableRow(
                    style, spec,
                    listOf(Fmt.numericDate(i.date), i.odometerKm?.let { Fmt.km(it) } ?: "—", i.station.ifBlank { "—" }, result, Fmt.numericDate(i.nextDueDate)),
                    header, colors = mapOf(3 to color),
                )
            }
            blocks += SpacerBlock(10f)
        }

        // Daños
        if (options.includeDamages && damages.isNotEmpty()) {
            blocks += SectionTitle(style, "Daños y reparaciones de carrocería", "Registro transparente de golpes y su estado")
            val spec = TableSpec(listOf("Fecha", "Zona", "Descripción", "Gravedad", "Estado"), listOf(0.9f, 1.3f, 2f, 0.8f, 1.2f))
            val header = TableHeader(style, spec)
            blocks += header
            damages.sortedBy { it.date }.forEach { d ->
                val status = if (d.repaired) "Reparado ${Fmt.numericDate(d.repairedDate)}" + if (options.includePrices && d.repairCost != null) "\n${Fmt.money(d.repairCost)}" else "" else "Sin reparar"
                blocks += TableRow(
                    style, spec,
                    listOf(Fmt.numericDate(d.date), d.zone.label, listOf(d.title, d.description).filter { it.isNotBlank() }.joinToString(". "), d.severity.label, status),
                    header, colors = mapOf(4 to if (d.repaired) style.green else style.amber),
                )
            }
            blocks += SpacerBlock(10f)
        }

        // Consumo
        if (options.includeFuel && overview.fuel.refuelCount > 0) {
            blocks += SectionTitle(style, "Consumo registrado", "Calculado con el método de depósito lleno a lleno")
            blocks += StatCards(
                style,
                listOfNotNull(
                    "Consumo medio" to "${Fmt.twoDecimals(overview.fuel.averageLitersPer100)} l/100",
                    "Repostajes" to "${overview.fuel.refuelCount}",
                    "Litros" to Fmt.number(Math.round(overview.fuel.totalLiters)),
                    if (options.includePrices) "€/100 km" to Fmt.twoDecimals(overview.fuel.costPer100Km) else null,
                ),
            )
            blocks += SpacerBlock(8f)
        }

        // Anexo de fotos
        if (options.includePhotos) {
            val recordIds = records.associateBy { it.record.id }
            val damageIds = damages.associateBy { it.id }
            val photos: List<Pair<AttachmentEntity, String>> =
                attachments.byType(AttachmentOwner.MAINTENANCE).filter { it.ownerId in recordIds && it.mimeType.startsWith("image") }
                    .sortedBy { recordIds[it.ownerId]!!.record.date }
                    .map { it to "${recordIds[it.ownerId]!!.record.title} · ${Fmt.date(recordIds[it.ownerId]!!.record.date)}" } +
                    attachments.byType(AttachmentOwner.ITV).filter { a -> itv.any { it.id == a.ownerId } && a.mimeType.startsWith("image") }
                        .map { a -> a to "ITV · ${Fmt.date(itv.first { it.id == a.ownerId }.date)}" } +
                    attachments.byType(AttachmentOwner.DAMAGE).filter { it.ownerId in damageIds && it.mimeType.startsWith("image") }
                        .map { it to "Daño: ${damageIds[it.ownerId]!!.title} · ${Fmt.date(damageIds[it.ownerId]!!.date)}" }
            if (photos.isNotEmpty()) {
                blocks += PageBreak()
                blocks += SectionTitle(style, "Anexo: facturas y fotografías", "${photos.size} documentos adjuntos")
                photos.forEach { (a, caption) ->
                    blocks += ImageBlock(style, caption, { loadScaled(attachments.storage.file(a.fileName)) })
                }
            }
        }

        blocks += SpacerBlock(14f)
        blocks += Paragraph(
            "Informe generado el ${Fmt.longDate(today)} con Polo App. Los datos han sido introducidos por el propietario " +
                "a lo largo del tiempo; las facturas adjuntas permiten contrastarlos. Referencia: $reference.",
            small,
        )

        val header = listOfNotNull(vehicle.displayName, vehicle.version.ifBlank { null }, if (options.includePlate) vehicle.plate.ifBlank { null } else null).joinToString(" · ")
        val composer = PdfComposer(style, "Historial del vehículo · $header", "Polo App · Ref. $reference")
        val doc = PdfDocument()
        try {
            composer.render(doc, { c -> drawCover(c, style, options, reference, vehicle, overview, records.size, itv.size, today) }, blocks)
            val dir = File(context.cacheDir, "reports").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val name = listOf("Historial", vehicle.make, vehicle.model, if (options.includePlate) vehicle.plate else "", today.format(DateTimeFormatter.BASIC_ISO_DATE))
                .filter { it.isNotBlank() }.joinToString("_") { it.replace(Regex("[^A-Za-z0-9ÁÉÍÓÚáéíóúÑñ-]"), "") }
            val file = File(dir, "$name.pdf")
            file.outputStream().use { doc.writeTo(it) }
            file
        } finally {
            doc.close()
        }
    }

    private fun drawCover(
        c: Canvas,
        style: PdfStyle,
        options: ReportOptions,
        reference: String,
        vehicle: VehicleEntity,
        overview: VehicleOverview,
        interventions: Int,
        inspections: Int,
        today: LocalDate,
    ) {
        val w = PdfComposer.PAGE_W
        val h = PdfComposer.PAGE_H
        val band = 400f
        c.drawColor(Color.WHITE)
        c.drawRect(0f, 0f, w, band, Paint().apply { shader = LinearGradient(0f, 0f, 0f, band, Color.rgb(26, 29, 35), Color.rgb(12, 13, 16), Shader.TileMode.CLAMP) })
        c.drawRect(
            0f, 0f, w, band,
            Paint().apply { shader = RadialGradient(w / 2, 300f, 320f, intArrayOf(Color.argb(120, 232, 55, 62), Color.TRANSPARENT), null, Shader.TileMode.CLAMP) },
        )
        val kicker = style.text(8f, bold = true, color = Color.argb(170, 255, 255, 255)).apply { letterSpacing = 0.25f }
        c.drawText("HISTORIAL DEL VEHÍCULO", 42f, 62f, kicker)
        c.drawText(vehicle.displayName, 42f, 102f, style.text(30f, bold = true, color = Color.WHITE))
        if (vehicle.version.isNotBlank()) c.drawText(vehicle.version, 42f, 124f, style.text(12f, color = Color.argb(190, 255, 255, 255)))
        c.save()
        c.translate(40f, 148f)
        CarPainter.draw(c, w - 80f, 236f, vehicle.colorArgb.toInt(), shadow = true)
        c.restore()
        c.drawRect(0f, band, w, band + 4f, style.fill(style.accent))

        var y = band + 44f
        if (options.includePlate && vehicle.plate.isNotBlank()) drawPlate(c, style, 42f, y - 20f, vehicle.plate)
        val dateText = "Informe del ${Fmt.longDate(today)}"
        val datePaint = style.text(9f, color = style.muted)
        c.drawText(dateText, w - 42f - datePaint.measureText(dateText), y - 4f, datePaint)
        y += 22f

        val facts = buildList {
            if (options.includePlate && vehicle.plate.isNotBlank()) add("Matrícula" to vehicle.plate)
            if (options.includeVin && vehicle.vin.isNotBlank()) add("Bastidor (VIN)" to vehicle.vin)
            vehicle.registrationDate?.let { add("Primera matriculación" to Fmt.longDate(it)) }
            add("Kilometraje actual" to Fmt.km(overview.currentKm))
            add("Combustible" to listOfNotNull(vehicle.fuelType.label, vehicle.powerCv?.let { "$it CV" }).joinToString(" · "))
            if (vehicle.colorName.isNotBlank()) add("Color" to vehicle.colorName)
            add("Intervenciones registradas" to "$interventions")
            add("Inspecciones ITV" to "$inspections")
            if (options.includeOwner && vehicle.ownerName.isNotBlank()) add("Propietario" to vehicle.ownerName)
        }
        FactGrid(style, facts, 2).draw(c, 42f, y, w - 84f)

        val boxTop = h - 190f
        c.drawRoundRect(RectF(42f, boxTop, w - 42f, boxTop + 110f), 12f, 12f, style.fill(style.soft))
        c.drawText("Mantenimiento documentado", 60f, boxTop + 28f, style.text(12f, bold = true))
        val lastService = overview.components.mapNotNull { it.current }.maxByOrNull { it.date }
        val lines = listOfNotNull(
            lastService?.let { "Último mantenimiento registrado: ${Fmt.longDate(it.date)} a ${Fmt.km(it.odometerKm)}." },
            overview.itv.date?.let { if ((overview.itv.daysLeft ?: -1) >= 0) "ITV en vigor hasta el ${Fmt.longDate(it)}." else "ITV caducada el ${Fmt.longDate(it)}." },
            if (overview.odometer.anomalies.isEmpty()) "Kilometraje coherente en ${overview.odometer.eventCount} lecturas." else null,
        )
        c.drawLayout(textLayout(lines.joinToString("\n"), style.text(9.5f), w - 120f), 60f, boxTop + 40f)
        c.drawText("Generado con Polo App · Ref. $reference", 42f, h - 40f, style.text(7.5f, color = style.muted))
    }

    private fun drawPlate(c: Canvas, style: PdfStyle, x: Float, y: Float, plate: String) {
        val text = style.text(15f, numbers = true, color = Color.rgb(17, 17, 17)).apply { letterSpacing = 0.08f }
        val tw = text.measureText(plate.uppercase())
        val rect = RectF(x, y, x + 22f + tw + 16f, y + 26f)
        c.drawRoundRect(rect, 4f, 4f, style.fill(Color.WHITE))
        c.drawRoundRect(rect, 4f, 4f, style.stroke(Color.rgb(27, 27, 27), 1.2f))
        c.drawRect(x + 0.6f, y + 0.6f, x + 16f, y + 25.4f, style.fill(Color.rgb(31, 63, 158)))
        c.drawText("E", x + 5f, y + 22f, style.text(8f, bold = true, color = Color.WHITE))
        c.drawText(plate.uppercase(), x + 24f, y + 19f, text)
    }

    private fun loadScaled(file: File, maxSize: Int = 1000): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSize) sample *= 2
        BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }.getOrNull()
}
