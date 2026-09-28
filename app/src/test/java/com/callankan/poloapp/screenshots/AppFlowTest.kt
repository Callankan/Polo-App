package com.callankan.poloapp.screenshots

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callankan.poloapp.MainActivity
import com.callankan.poloapp.data.db.entity.DamageEntity
import com.callankan.poloapp.data.db.entity.ExpenseEntity
import com.callankan.poloapp.data.db.entity.InsurancePolicyEntity
import com.callankan.poloapp.data.db.entity.ItvInspectionEntity
import com.callankan.poloapp.data.db.entity.LoanEntity
import com.callankan.poloapp.data.db.entity.LoanPaymentEntity
import com.callankan.poloapp.data.db.entity.MaintenanceEntity
import com.callankan.poloapp.data.db.entity.NoteEntity
import com.callankan.poloapp.data.db.entity.TirePressureEntity
import com.callankan.poloapp.data.db.entity.TripEntity
import com.callankan.poloapp.data.db.entity.WorkshopEntity
import com.callankan.poloapp.data.model.CarZone
import com.callankan.poloapp.data.model.DamageSeverity
import com.callankan.poloapp.data.model.ExpenseCategory
import com.callankan.poloapp.data.model.InsuranceCoverage
import com.callankan.poloapp.data.model.ItvResult
import com.callankan.poloapp.data.model.MaintenanceCategory
import com.callankan.poloapp.data.model.PerformedBy
import com.callankan.poloapp.data.repository.InstallDraft
import com.callankan.poloapp.di.AppEntryPoint
import com.callankan.poloapp.report.ReportOptions
import com.github.takahirom.roborazzi.captureRoboImage
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/**
 * Prueba de extremo a extremo con la app real (Hilt + Room + DataStore): alta del coche,
 * datos de ejemplo y recorrido por todas las pantallas, guardando capturas en docs/screenshots.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class AppFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private val entry by lazy { EntryPointAccessors.fromApplication(compose.activity.applicationContext, AppEntryPoint::class.java) }

    private fun waitText(text: String, timeout: Long = 15_000) = compose.waitUntil(timeout) {
        compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
    }

    private fun scrollTo(text: String) {
        val scrollables = compose.onAllNodes(hasScrollToNodeAction()).fetchSemanticsNodes().size
        for (i in 0 until scrollables) {
            val done = runCatching { compose.onAllNodes(hasScrollToNodeAction())[i].performScrollToNode(hasText(text, substring = true)) }.isSuccess
            if (done) return
        }
    }

    private fun click(text: String) {
        scrollTo(text)
        waitText(text)
        compose.onAllNodes(hasText(text) and hasClickAction()).onFirst().performClick()
        compose.waitForIdle()
    }

    private fun type(label: String, value: String) {
        scrollTo(label)
        compose.onAllNodes(hasText(label) and hasSetTextAction()).onFirst().performTextInput(value)
    }

    private fun shot(name: String) {
        compose.mainClock.advanceTimeBy(1_500)
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("../docs/screenshots/$name.png")
    }

    private fun back() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    @Test
    fun fullFlow() {
        // 1. Alta del coche
        waitText("Empezar")
        shot("00_bienvenida")
        click("Empezar")
        click("Volkswagen Polo Mk5")
        shot("00b_alta_coche")
        click("Continuar")
        type("Matrícula", "8841HVZ")
        type("Número de bastidor (VIN)", "WVWZZZ6RZDY123456")
        shot("00c_alta_identidad")
        click("Continuar")
        type("Km al comprarlo", "128400")
        type("Km actuales", "147980")
        type("Delantera", "2,3")
        type("Trasera", "2,1")
        click("Continuar")
        shot("00d_alta_plan")
        click("Crear mi garaje")
        waitText("Mi Polo")

        // 2. Datos de ejemplo
        seed()
        compose.waitForIdle()
        waitText("Próximo mantenimiento")
        shot("01_dashboard")

        // 3. Recorrido por las pestañas
        click("Combustible")
        waitText("Consumo medio real")
        shot("02_combustible")
        click("Taller")
        waitText("Vencidos")
        shot("03_taller_plan")
        click("Historial")
        shot("03b_taller_historial")
        click("Talleres")
        shot("03c_talleres")
        click("Plan")
        click("Aceite de motor")
        waitText("Ciclo de vida")
        shot("03d_pieza_aceite")
        back()
        click("Finanzas")
        waitText("Te queda por pagar")
        shot("04_finanzas_deuda")
        click("Simulador")
        waitText("Terminarás de pagar en")
        shot("04b_simulador")
        back()
        click("Gastos")
        shot("04c_gastos")
        click("Más")
        shot("05_mas")

        // 4. Secciones de "Más"
        listOf(
            Triple("Viajes", "Madrid", "06_viajes"), Triple("Notas", "Ruido al arrancar", "07_notas"),
            Triple("Presiones", "Última comprobación", "08_presiones"), Triple("Daños", "Rayón con una columna", "09_danos"),
            Triple("ITV", "Próxima ITV", "10_itv"), Triple("Seguro", "Vencimiento del seguro", "11_seguro"),
            Triple("Ficha rápida", "Bastidor", "12_ficha"), Triple("Estadísticas", "Coste total del coche", "13_estadisticas"),
            Triple("Kilometraje", "lecturas coherentes", "14_kilometraje"), Triple("Informe PDF", "Qué incluir", "15_informe"),
        ).forEach { (entryName, expected, file) ->
            click(entryName)
            waitText(expected)
            shot(file)
            back()
        }

        // 5. Formularios principales
        click("Inicio")
        click("Repostaje")
        waitText("Nuevo repostaje")
        type("Litros", "38,5")
        type("Precio", "1,559")
        shot("16_nuevo_repostaje")
        back()
        click("Más")
        click("Daños")
        waitText("Rayón con una columna")
        compose.onAllNodes(hasText("Rayón con una columna", substring = true) and hasClickAction()).onFirst().performClick()
        waitText("¿Qué ha pasado?")
        shot("17_nuevo_dano")
        back()

        // 6. Informe PDF real y copia de seguridad
        runBlocking {
            val id = entry.active().currentId()
            // Robolectric no implementa el motor nativo de PdfDocument: el PDF se prueba en el emulador (androidTest).
            val pdf = runCatching { entry.report().generate(id, ReportOptions()) }
            pdf.exceptionOrNull()?.let { if (it !is IllegalStateException) throw it }
            pdf.getOrNull()?.let { assertTrue(it.length() > 10_000) }
            val backup = entry.backup()
            val json = backup.encode(backup.snapshot())
            val restored = backup.decode(json)
            assertTrue(restored.refuels.size >= 10 && restored.vehicles.size == 1)
        }
    }

    private fun seed() = runBlocking {
        val e = entry
        val vehicleId = e.active().currentId()
        val vehicle = e.vehicles().get(vehicleId)!!
        e.vehicles().update(vehicle.copy(registrationDate = LocalDate.of(2013, 6, 14), purchaseDate = LocalDate.of(2024, 3, 2), ownerName = "Joel"))

        SampleData.refuels.forEach { e.fuel().save(it.copy(id = 0, vehicleId = vehicleId)) }

        val components = e.maintenance().components(vehicleId).associateBy { it.name }
        fun c(name: String) = components.getValue(name).id
        val taller = e.maintenance().saveWorkshop(WorkshopEntity(name = "Talleres Montgat", phone = "934 000 000", address = "Av. Maresme 12, Montgat", rating = 5))
        val norauto = e.maintenance().saveWorkshop(WorkshopEntity(name = "Norauto Badalona", phone = "933 000 000", rating = 4))
        e.maintenance().saveRecord(
            MaintenanceEntity(vehicleId = vehicleId, date = LocalDate.of(2024, 4, 20), odometerKm = 129_300, title = "Revisión tras la compra", category = MaintenanceCategory.OIL_SERVICE, performedBy = PerformedBy.WORKSHOP, workshopId = taller, partsCost = 96.0, laborCost = 60.0, totalCost = 156.0, invoiceNumber = "F-2024-0412"),
            listOf(InstallDraft(c("Aceite de motor"), "Castrol", "Edge 5W-30"), InstallDraft(c("Filtro de aceite"), "Mann"), InstallDraft(c("Filtro de aire"), "Bosch"), InstallDraft(c("Bujías"), "NGK")),
        )
        e.maintenance().saveRecord(
            MaintenanceEntity(vehicleId = vehicleId, date = LocalDate.of(2024, 9, 12), odometerKm = 131_200, title = "Cambio de pastillas delanteras", category = MaintenanceCategory.BRAKES, performedBy = PerformedBy.WORKSHOP, workshopId = norauto, totalCost = 89.9),
            listOf(InstallDraft(c("Pastillas de freno delanteras"), "Brembo")),
        )
        e.maintenance().saveRecord(
            MaintenanceEntity(vehicleId = vehicleId, date = LocalDate.of(2025, 5, 3), odometerKm = 134_100, title = "Neumáticos delanteros nuevos", category = MaintenanceCategory.TIRES, performedBy = PerformedBy.WORKSHOP, workshopId = norauto, totalCost = 172.0),
            listOf(InstallDraft(c("Neumáticos delanteros"), "Michelin", "Primacy 4 185/60 R15")),
        )
        e.maintenance().saveRecord(
            MaintenanceEntity(vehicleId = vehicleId, date = LocalDate.of(2025, 9, 1), odometerKm = 136_800, title = "Filtro de habitáculo", category = MaintenanceCategory.FILTERS, performedBy = PerformedBy.DIY, totalCost = 14.5),
            listOf(InstallDraft(c("Filtro de habitáculo"), "Mann", "CUK 2939")),
        )
        e.maintenance().saveRecord(
            MaintenanceEntity(vehicleId = vehicleId, date = LocalDate.of(2026, 2, 10), odometerKm = 140_900, title = "Cambio de aceite y filtro", category = MaintenanceCategory.OIL_SERVICE, performedBy = PerformedBy.WORKSHOP, workshopId = taller, totalCost = 118.0),
            listOf(InstallDraft(c("Aceite de motor"), "Castrol", "Edge 5W-30 VW 502.00"), InstallDraft(c("Filtro de aceite"), "Mann", "W 712/93")),
        )

        e.compliance().saveItv(ItvInspectionEntity(vehicleId = vehicleId, date = LocalDate.of(2024, 11, 18), odometerKm = 131_900, station = "ITV Badalona", result = ItvResult.FAVORABLE_WITH_DEFECTS, defects = "Holgura leve en rótula", cost = 41.2, nextDueDate = LocalDate.of(2025, 11, 18)))
        e.compliance().saveItv(ItvInspectionEntity(vehicleId = vehicleId, date = LocalDate.of(2025, 11, 10), odometerKm = 138_200, station = "ITV Badalona", result = ItvResult.FAVORABLE, cost = 42.5, nextDueDate = LocalDate.of(2026, 11, 18)))
        e.compliance().savePolicy(InsurancePolicyEntity(vehicleId = vehicleId, company = "Mutua Madrileña", policyNumber = "P-448812", coverage = InsuranceCoverage.THIRD_PARTY_EXTENDED, startDate = LocalDate.of(2025, 3, 2), endDate = LocalDate.of(2026, 3, 1), premium = 372.0, assistancePhone = "900 555 555"))
        e.compliance().savePolicy(InsurancePolicyEntity(vehicleId = vehicleId, company = "Mutua Madrileña", policyNumber = "P-448812", coverage = InsuranceCoverage.THIRD_PARTY_EXTENDED, startDate = LocalDate.of(2026, 3, 2), endDate = LocalDate.of(2027, 3, 1), premium = 389.0, assistancePhone = "900 555 555"))

        val loan = e.finance().saveLoan(LoanEntity(vehicleId = vehicleId, lender = "Mamá", initialAmount = 4500.0, startDate = LocalDate.of(2024, 3, 1)))
        SampleData.payments.forEach { e.finance().savePayment(it.copy(id = 0, loanId = loan)) }
        e.finance().saveExpense(ExpenseEntity(vehicleId = vehicleId, date = LocalDate.of(2026, 9, 5), category = ExpenseCategory.WASH, amount = 12.0, description = "Lavado completo"))
        e.finance().saveExpense(ExpenseEntity(vehicleId = vehicleId, date = LocalDate.of(2026, 9, 12), category = ExpenseCategory.PARKING, amount = 8.5, description = "Parking centro"))
        e.finance().saveExpense(ExpenseEntity(vehicleId = vehicleId, date = LocalDate.of(2026, 4, 2), category = ExpenseCategory.TAX, amount = 68.0, description = "IVTM 2026"))
        e.finance().saveExpense(ExpenseEntity(vehicleId = vehicleId, date = LocalDate.of(2026, 7, 20), category = ExpenseCategory.TOLL, amount = 23.4, description = "AP-2 Zaragoza"))

        e.logbook().saveTrip(TripEntity(vehicleId = vehicleId, date = LocalDate.of(2026, 7, 20), origin = "Badalona", destination = "Madrid", distanceKm = 624, purpose = "Vacaciones", tolls = 23.4, notes = "Sin incidencias. Consumo de 5,4 l/100 a 120 km/h."))
        e.logbook().saveTrip(TripEntity(vehicleId = vehicleId, date = LocalDate.of(2026, 4, 3), origin = "Badalona", destination = "Andorra", distanceKm = 410, purpose = "Escapada"))
        e.logbook().saveNote(NoteEntity(vehicleId = vehicleId, title = "Ruido al arrancar en frío", body = "Vigilar la cadena de distribución y comentarlo en la próxima revisión.", pinned = true))
        e.logbook().saveNote(NoteEntity(vehicleId = vehicleId, title = "Pedir cita ITV", body = "", reminderDate = LocalDate.of(2026, 10, 20)))
        e.logbook().saveTireCheck(TirePressureEntity(vehicleId = vehicleId, date = LocalDate.of(2026, 9, 20), minuteOfDay = 18 * 60 + 12, frontLeft = 2.3, frontRight = 2.3, rearLeft = 2.1, rearRight = 2.0))
        e.logbook().saveDamage(DamageEntity(vehicleId = vehicleId, date = LocalDate.of(2026, 8, 22), odometerKm = 147_600, title = "Rayón con una columna", description = "Parking del súper", zone = CarZone.REAR_RIGHT_DOOR, severity = DamageSeverity.MINOR))
        e.logbook().saveDamage(DamageEntity(vehicleId = vehicleId, date = LocalDate.of(2025, 1, 12), title = "Retrovisor golpeado", zone = CarZone.LEFT_MIRROR, severity = DamageSeverity.MODERATE, repaired = true, repairedDate = LocalDate.of(2025, 1, 20), repairCost = 85.0))
    }
}
