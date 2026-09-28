package com.callankan.poloapp

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.callankan.poloapp.data.db.entity.DamageEntity
import com.callankan.poloapp.data.db.entity.InsurancePolicyEntity
import com.callankan.poloapp.data.db.entity.ItvInspectionEntity
import com.callankan.poloapp.data.db.entity.MaintenanceEntity
import com.callankan.poloapp.data.db.entity.RefuelEntity
import com.callankan.poloapp.data.db.entity.VehicleEntity
import com.callankan.poloapp.data.db.entity.WorkshopEntity
import com.callankan.poloapp.data.model.CarZone
import com.callankan.poloapp.data.model.DamageSeverity
import com.callankan.poloapp.data.model.FuelType
import com.callankan.poloapp.data.model.InsuranceCoverage
import com.callankan.poloapp.data.model.ItvResult
import com.callankan.poloapp.data.model.MaintenanceCategory
import com.callankan.poloapp.data.model.PerformedBy
import com.callankan.poloapp.data.model.TimingDrive
import com.callankan.poloapp.data.preset.Presets
import com.callankan.poloapp.data.repository.InstallDraft
import com.callankan.poloapp.di.AppEntryPoint
import com.callankan.poloapp.report.ReportOptions
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

/**
 * Genera el informe PDF en un dispositivo real/emulador y guarda cada página como PNG
 * en files/report para poder revisarlas (el CI las publica en la rama ci-report-preview).
 */
@RunWith(AndroidJUnit4::class)
class ReportOnDeviceTest {
    @Test
    fun generatesReadablePdf() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val e = EntryPointAccessors.fromApplication(context.applicationContext, AppEntryPoint::class.java)

        val id = e.vehicles().create(
            VehicleEntity(
                alias = "Mi Polo", make = "Volkswagen", model = "Polo", version = "Mk5 (6R) 1.2 TSI 90 CV", powerCv = 90,
                fuelType = FuelType.GASOLINE, timingDrive = TimingDrive.CHAIN, tankCapacityLiters = 45.0,
                registrationDate = LocalDate.of(2013, 6, 14), purchaseDate = LocalDate.of(2024, 3, 2), purchaseKm = 128_400,
                plate = "8841 HVZ", vin = "WVWZZZ6RZDY123456", colorName = "Rojo", colorArgb = 0xFFD0121E, ownerName = "Joel",
            ),
            147_980,
            Presets.maintenancePlan(FuelType.GASOLINE, TimingDrive.CHAIN, Presets.polo.extraHints),
        )
        var km = 141_200
        var date = LocalDate.of(2026, 3, 1)
        listOf(6.1, 5.8, 6.4, 6.0, 5.6, 6.7, 5.9, 6.2, 5.7, 6.0).forEachIndexed { i, c ->
            val dist = 520 + (i % 3) * 45
            km += dist
            date = date.plusDays(15)
            val liters = dist * c / 100
            e.fuel().save(RefuelEntity(vehicleId = id, date = date, odometerKm = km, liters = liters, totalCost = liters * 1.559, pricePerLiter = 1.559))
        }
        val components = e.maintenance().components(id).associateBy { it.name }
        fun c(name: String) = components.getValue(name).id
        val taller = e.maintenance().saveWorkshop(WorkshopEntity(name = "Talleres Montgat", phone = "934 000 000"))
        e.maintenance().saveRecord(
            MaintenanceEntity(vehicleId = id, date = LocalDate.of(2024, 4, 20), odometerKm = 129_300, title = "Revisión tras la compra", category = MaintenanceCategory.OIL_SERVICE, performedBy = PerformedBy.WORKSHOP, workshopId = taller, totalCost = 156.0, invoiceNumber = "F-2024-0412"),
            listOf(InstallDraft(c("Aceite de motor"), "Castrol", "Edge 5W-30"), InstallDraft(c("Filtro de aceite"), "Mann"), InstallDraft(c("Filtro de aire"), "Bosch")),
        )
        e.maintenance().saveRecord(
            MaintenanceEntity(vehicleId = id, date = LocalDate.of(2026, 2, 10), odometerKm = 140_900, title = "Cambio de aceite y filtro", category = MaintenanceCategory.OIL_SERVICE, performedBy = PerformedBy.WORKSHOP, workshopId = taller, totalCost = 118.0),
            listOf(InstallDraft(c("Aceite de motor"), "Castrol", "Edge 5W-30 VW 502.00"), InstallDraft(c("Filtro de aceite"), "Mann", "W 712/93")),
        )
        e.maintenance().saveRecord(
            MaintenanceEntity(vehicleId = id, date = LocalDate.of(2025, 9, 1), odometerKm = 136_800, title = "Filtro de habitáculo", category = MaintenanceCategory.FILTERS, performedBy = PerformedBy.DIY, totalCost = 14.5),
            listOf(InstallDraft(c("Filtro de habitáculo"), "Mann", "CUK 2939")),
        )
        e.compliance().saveItv(ItvInspectionEntity(vehicleId = id, date = LocalDate.of(2025, 11, 10), odometerKm = 138_200, station = "ITV Badalona", result = ItvResult.FAVORABLE, cost = 42.5, nextDueDate = LocalDate.of(2026, 11, 18)))
        e.compliance().savePolicy(InsurancePolicyEntity(vehicleId = id, company = "Mutua Madrileña", coverage = InsuranceCoverage.THIRD_PARTY_EXTENDED, startDate = LocalDate.of(2026, 3, 2), endDate = LocalDate.of(2027, 3, 1), premium = 389.0))
        e.logbook().saveDamage(DamageEntity(vehicleId = id, date = LocalDate.of(2025, 1, 12), title = "Retrovisor golpeado", zone = CarZone.LEFT_MIRROR, severity = DamageSeverity.MODERATE, repaired = true, repairedDate = LocalDate.of(2025, 1, 20), repairCost = 85.0))

        val pdf = e.report().generate(id, ReportOptions(includeOwner = true))
        assertTrue("PDF demasiado pequeño", pdf.length() > 10_000)

        val out = File(context.getExternalFilesDir(null), "report").apply {
            deleteRecursively()
            mkdirs()
        }
        pdf.copyTo(File(out, "informe_ejemplo.pdf"), overwrite = true)
        ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                assertTrue(renderer.pageCount >= 3)
                for (i in 0 until renderer.pageCount) {
                    renderer.openPage(i).use { page ->
                        val bitmap = Bitmap.createBitmap(1190, 1684, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        File(out, "pagina_${i + 1}.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    }
                }
            }
        }
    }
}
