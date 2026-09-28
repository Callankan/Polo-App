package com.callankan.poloapp.screenshots

import com.callankan.poloapp.data.db.entity.ComponentEntity
import com.callankan.poloapp.data.db.entity.DamageEntity
import com.callankan.poloapp.data.db.entity.ExpenseEntity
import com.callankan.poloapp.data.db.entity.InsurancePolicyEntity
import com.callankan.poloapp.data.db.entity.ItvInspectionEntity
import com.callankan.poloapp.data.db.entity.LoanEntity
import com.callankan.poloapp.data.db.entity.LoanPaymentEntity
import com.callankan.poloapp.data.db.entity.NoteEntity
import com.callankan.poloapp.data.db.entity.OdometerEvent
import com.callankan.poloapp.data.db.entity.PartInstallationEntity
import com.callankan.poloapp.data.db.entity.RefuelEntity
import com.callankan.poloapp.data.db.entity.TirePressureEntity
import com.callankan.poloapp.data.db.entity.VehicleEntity
import com.callankan.poloapp.data.model.CarZone
import com.callankan.poloapp.data.model.DamageSeverity
import com.callankan.poloapp.data.model.ExpenseCategory
import com.callankan.poloapp.data.model.FuelType
import com.callankan.poloapp.data.model.InsuranceCoverage
import com.callankan.poloapp.data.model.ItvResult
import com.callankan.poloapp.data.model.OdometerSource
import com.callankan.poloapp.data.model.TimingDrive
import com.callankan.poloapp.data.preset.Presets
import com.callankan.poloapp.data.repository.OverviewBuilder
import com.callankan.poloapp.data.repository.OverviewInputs
import com.callankan.poloapp.data.repository.VehicleOverview
import com.callankan.poloapp.data.settings.AppSettings
import java.time.LocalDate

/** Datos ficticios de un Polo 1.2 TSI para las capturas. */
object SampleData {
    val today: LocalDate = LocalDate.of(2026, 9, 28)

    val vehicle = VehicleEntity(
        id = 1, alias = "Mi Polo", make = "Volkswagen", model = "Polo", version = "1.2 TSI 90 CV",
        powerCv = 90, fuelType = FuelType.GASOLINE, timingDrive = TimingDrive.CHAIN, tankCapacityLiters = 45.0,
        registrationDate = LocalDate.of(2013, 6, 14), purchaseDate = LocalDate.of(2024, 3, 2), purchaseKm = 128_400,
        plate = "8841 HVZ", vin = "WVWZZZ6RZDY000000", colorName = "Rojo", colorArgb = 0xFFD0121E,
        tireFrontBar = 2.3, tireRearBar = 2.1,
    )

    val components: List<ComponentEntity> = Presets.maintenancePlan(FuelType.GASOLINE, TimingDrive.CHAIN, Presets.polo.extraHints)
        .mapIndexed { i, p ->
            ComponentEntity(
                id = i + 1L, vehicleId = 1, name = p.name, category = p.category,
                intervalKm = p.intervalKm, intervalMonths = p.intervalMonths, hint = p.hint, sortOrder = i,
            )
        }

    private fun component(name: String) = components.first { it.name == name }.id

    val installations = listOf(
        PartInstallationEntity(1, 1, component("Aceite de motor"), null, LocalDate.of(2026, 2, 10), 140_900, "Castrol Edge", "5W-30 VW 502.00"),
        PartInstallationEntity(2, 1, component("Filtro de aceite"), null, LocalDate.of(2026, 2, 10), 140_900, "Mann", "W 712/93"),
        PartInstallationEntity(3, 1, component("Filtro de aire"), null, LocalDate.of(2024, 4, 20), 129_300, "Bosch"),
        PartInstallationEntity(4, 1, component("Neumáticos delanteros"), null, LocalDate.of(2025, 5, 3), 134_100, "Michelin", "Primacy 4 185/60 R15"),
        PartInstallationEntity(5, 1, component("Pastillas de freno delanteras"), null, LocalDate.of(2024, 9, 12), 131_200, "Brembo"),
        PartInstallationEntity(6, 1, component("Líquido de frenos"), null, LocalDate.of(2024, 10, 1), 131_600),
        PartInstallationEntity(7, 1, component("Filtro de habitáculo"), null, LocalDate.of(2025, 9, 1), 136_800),
    )

    val refuels: List<RefuelEntity> = run {
        var km = 141_200
        var date = LocalDate.of(2026, 3, 1)
        val consumptions = listOf(6.1, 5.8, 6.4, 6.0, 5.6, 6.7, 5.9, 6.2, 5.7, 6.0, 5.5, 5.9)
        val prices = listOf(1.549, 1.569, 1.579, 1.559, 1.539, 1.529, 1.519, 1.549, 1.569, 1.589, 1.579, 1.559)
        consumptions.mapIndexed { i, c ->
            val dist = 520 + (i % 3) * 45
            km += dist
            date = date.plusDays(15)
            val liters = dist * c / 100
            RefuelEntity(i + 1L, 1, date, km, liters, liters * prices[i], prices[i], fuelGrade = "Gasolina 95", station = if (i % 2 == 0) "Repsol Badalona" else "BP Montgat")
        }
    }

    val events: List<OdometerEvent> = refuels.map { OdometerEvent(1, it.date, it.odometerKm, OdometerSource.REFUEL, it.id) } +
        OdometerEvent(1, vehicle.purchaseDate!!, vehicle.purchaseKm!!, OdometerSource.PURCHASE, 1) +
        OdometerEvent(1, LocalDate.of(2026, 2, 10), 140_900, OdometerSource.MAINTENANCE, 1)

    val itv = listOf(
        ItvInspectionEntity(1, 1, LocalDate.of(2025, 11, 20), 138_200, "ITV Badalona", ItvResult.FAVORABLE, cost = 42.5, nextDueDate = LocalDate.of(2026, 11, 20)),
    )
    val insurance = listOf(
        InsurancePolicyEntity(1, 1, "Mutua Madrileña", "P-448812", InsuranceCoverage.THIRD_PARTY_EXTENDED, LocalDate.of(2026, 3, 2), LocalDate.of(2027, 3, 1), 389.0, "900 555 555"),
    )
    val loan = LoanEntity(1, 1, "Mamá", 4500.0, LocalDate.of(2024, 3, 1))
    val payments = listOf(
        LoanPaymentEntity(1, 1, LocalDate.of(2024, 5, 1), 300.0),
        LoanPaymentEntity(2, 1, LocalDate.of(2024, 9, 1), 450.0),
        LoanPaymentEntity(3, 1, LocalDate.of(2025, 1, 1), 400.0),
        LoanPaymentEntity(4, 1, LocalDate.of(2025, 6, 1), 600.0),
        LoanPaymentEntity(5, 1, LocalDate.of(2026, 1, 1), 500.0),
        LoanPaymentEntity(6, 1, LocalDate.of(2026, 7, 1), 450.0),
    )
    val expenses = listOf(
        ExpenseEntity(1, 1, LocalDate.of(2026, 9, 5), ExpenseCategory.WASH, 12.0, "Lavado completo"),
        ExpenseEntity(2, 1, LocalDate.of(2026, 9, 12), ExpenseCategory.PARKING, 8.5, "Parking centro"),
        ExpenseEntity(3, 1, LocalDate.of(2026, 4, 2), ExpenseCategory.TAX, 68.0, "IVTM 2026"),
    )
    val damages = listOf(
        DamageEntity(1, 1, LocalDate.of(2026, 8, 22), 146_300, "Rayón en parking", "Rozadura con una columna", CarZone.REAR_RIGHT_DOOR, DamageSeverity.MINOR),
    )
    val tires = listOf(
        TirePressureEntity(1, 1, LocalDate.of(2026, 9, 20), 18 * 60 + 12, 2.3, 2.3, 2.1, 2.1),
    )
    val notes = listOf(
        NoteEntity(1, 1, "Ruido al arrancar en frío", "Vigilar la cadena de distribución y comentarlo en la próxima revisión.", pinned = true),
    )

    fun overview(): VehicleOverview = OverviewBuilder.build(
        OverviewInputs(
            vehicle = vehicle, events = events, refuels = refuels, components = components, installations = installations,
            itv = itv, insurance = insurance, loans = listOf(loan), payments = payments, tireChecks = tires,
            damages = damages, notes = notes, expenses = expenses, maintenance = emptyList(), settings = AppSettings(selectedVehicleId = 1),
        ),
        today,
    )
}
