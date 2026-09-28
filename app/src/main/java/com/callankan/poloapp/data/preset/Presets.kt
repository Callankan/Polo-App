package com.callankan.poloapp.data.preset

import com.callankan.poloapp.data.model.FuelType
import com.callankan.poloapp.data.model.MaintenanceCategory
import com.callankan.poloapp.data.model.TimingDrive

data class ComponentPreset(
    val name: String,
    val category: MaintenanceCategory,
    val intervalKm: Int?,
    val intervalMonths: Int?,
    val hint: String = "",
)

data class VehiclePreset(
    val title: String,
    val subtitle: String,
    val make: String,
    val model: String,
    val version: String,
    val fuelType: FuelType,
    val timingDrive: TimingDrive,
    val powerCv: Int?,
    val tankLiters: Double?,
    val extraHints: Map<String, String> = emptyMap(),
)

data class CarColor(val name: String, val argb: Long)

object Presets {
    val polo = VehiclePreset(
        title = "Volkswagen Polo Mk5",
        subtitle = "6R · 1.2 TSI 90 CV · 2009-2014",
        make = "Volkswagen",
        model = "Polo",
        version = "Mk5 (6R) 1.2 TSI 90 CV",
        fuelType = FuelType.GASOLINE,
        timingDrive = TimingDrive.CHAIN,
        powerCv = 90,
        tankLiters = 45.0,
        extraHints = mapOf(
            "Cadena de distribución (revisión)" to
                "El 1.2 TSI EA111 (2009-2014) es conocido por el desgaste de la cadena y su tensor. " +
                "Atento a un traqueteo metálico durante los primeros segundos al arrancar en frío.",
            "Aceite de motor" to
                "Usa aceite con la norma que indique tu libro de mantenimiento (en los TSI suele ser VW 502.00).",
        ),
    )

    val vehiclePresets = listOf(polo)

    val carColors = listOf(
        CarColor("Rojo", 0xFFD0121E),
        CarColor("Blanco", 0xFFF1F1EE),
        CarColor("Negro", 0xFF1A1B1E),
        CarColor("Gris plata", 0xFFB9BDC3),
        CarColor("Gris grafito", 0xFF4B4F57),
        CarColor("Azul", 0xFF1F4FA0),
        CarColor("Azul noche", 0xFF1B2847),
        CarColor("Verde", 0xFF2E5E3B),
        CarColor("Amarillo", 0xFFF2C21A),
        CarColor("Naranja", 0xFFE3661E),
        CarColor("Beige", 0xFFCDBB9A),
        CarColor("Marrón", 0xFF5B3B2A),
    )

    /**
     * Plan de mantenimiento orientativo. Los intervalos son editables: cada coche tiene su plan
     * oficial y conviene revisarlo en el libro de mantenimiento.
     */
    fun maintenancePlan(fuel: FuelType, timing: TimingDrive, hints: Map<String, String> = emptyMap()): List<ComponentPreset> {
        val combustion = fuel != FuelType.ELECTRIC
        val list = buildList {
            if (combustion) {
                add(ComponentPreset("Aceite de motor", MaintenanceCategory.OIL_SERVICE, 15_000, 12, "Cambio de aceite con la viscosidad y norma del fabricante."))
                add(ComponentPreset("Filtro de aceite", MaintenanceCategory.FILTERS, 15_000, 12, "Se cambia siempre junto al aceite."))
                add(ComponentPreset("Filtro de aire", MaintenanceCategory.FILTERS, 30_000, 24))
            }
            add(ComponentPreset("Filtro de habitáculo", MaintenanceCategory.FILTERS, 15_000, 12, "También llamado filtro de polen."))
            if (fuel == FuelType.GASOLINE || fuel == FuelType.HYBRID || fuel == FuelType.PLUG_IN_HYBRID || fuel == FuelType.LPG) {
                add(ComponentPreset("Bujías", MaintenanceCategory.IGNITION, 60_000, 48))
            }
            if (fuel == FuelType.DIESEL) {
                add(ComponentPreset("Filtro de combustible", MaintenanceCategory.FILTERS, 30_000, 24))
            }
            when (timing) {
                TimingDrive.CHAIN -> add(ComponentPreset("Cadena de distribución (revisión)", MaintenanceCategory.TIMING, 60_000, null, "No tiene cambio periódico: se revisa tensión, tensor y ruidos."))
                TimingDrive.BELT -> add(ComponentPreset("Kit de distribución", MaintenanceCategory.TIMING, 120_000, 72, "Correa, tensores y bomba de agua. Consulta el intervalo exacto de tu motor."))
                TimingDrive.UNKNOWN -> Unit
            }
            if (combustion) add(ComponentPreset("Correa de accesorios", MaintenanceCategory.TIMING, 90_000, null))
            add(ComponentPreset("Líquido de frenos", MaintenanceCategory.FLUIDS, null, 24, "Absorbe humedad: se cambia por tiempo."))
            add(ComponentPreset("Pastillas de freno delanteras", MaintenanceCategory.BRAKES, 30_000, null, "Revisa el grosor en cada revisión."))
            add(ComponentPreset("Discos de freno delanteros", MaintenanceCategory.BRAKES, 60_000, null))
            add(ComponentPreset("Frenos traseros", MaintenanceCategory.BRAKES, 60_000, null, "Pastillas/zapatas y discos/tambores traseros."))
            add(ComponentPreset("Neumáticos delanteros", MaintenanceCategory.TIRES, 40_000, 60, "Mínimo legal 1,6 mm de dibujo; recomendable cambiar antes de 3 mm."))
            add(ComponentPreset("Neumáticos traseros", MaintenanceCategory.TIRES, 50_000, 60))
            if (combustion) add(ComponentPreset("Líquido refrigerante", MaintenanceCategory.FLUIDS, null, 60, "Comprueba nivel y protección anticongelante."))
            add(ComponentPreset("Batería", MaintenanceCategory.BATTERY, null, 60))
            add(ComponentPreset("Escobillas limpiaparabrisas", MaintenanceCategory.BODY, null, 12))
            add(ComponentPreset("Aire acondicionado (carga y limpieza)", MaintenanceCategory.CLIMATE, null, 24))
        }
        return list.map { p -> hints[p.name]?.let { p.copy(hint = it) } ?: p }
    }

    /** Campos de la ficha rápida con ejemplos orientativos como ayuda. */
    val specTemplate: List<Triple<String, String, String>> = listOf(
        Triple("Motor", "Código de motor", "Aparece en la ficha técnica"),
        Triple("Motor", "Aceite recomendado", "p. ej. 5W-30 VW 502.00"),
        Triple("Motor", "Capacidad de aceite (con filtro)", "p. ej. 3,6 l"),
        Triple("Motor", "Refrigerante", "p. ej. G13"),
        Triple("Neumáticos", "Medida delantera", "p. ej. 185/60 R15 84H"),
        Triple("Neumáticos", "Medida trasera", "p. ej. 185/60 R15 84H"),
        Triple("Neumáticos", "Presión delantera (bar)", "Etiqueta en la tapa del depósito"),
        Triple("Neumáticos", "Presión trasera (bar)", "Etiqueta en la tapa del depósito"),
        Triple("Neumáticos", "Par de apriete de ruedas", "p. ej. 120 Nm"),
        Triple("Eléctrico", "Batería", "p. ej. 12 V 61 Ah 540 A"),
        Triple("Eléctrico", "Bombilla faro (cruce/carretera)", "p. ej. H4 / H7"),
        Triple("Eléctrico", "Escobillas", "p. ej. 650 + 400 mm"),
        Triple("Otros", "Combustible", "p. ej. Gasolina 95 E10"),
        Triple("Otros", "Capacidad del depósito", "p. ej. 45 l"),
    )
}
