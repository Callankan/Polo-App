package com.callankan.poloapp.data.db.entity

import androidx.room.DatabaseView
import com.callankan.poloapp.data.model.OdometerSource
import java.time.LocalDate

/**
 * Línea temporal unificada del cuentakilómetros: cualquier registro con km cuenta como lectura.
 * Al ser una vista, nunca se desincroniza al editar o borrar los registros originales.
 */
@DatabaseView(
    viewName = "odometer_events",
    value = """
        SELECT vehicleId, date, odometerKm AS km, 'REFUEL' AS source, id AS refId FROM refuels
        UNION ALL SELECT vehicleId, date, odometerKm, 'MAINTENANCE', id FROM maintenance_records
        UNION ALL SELECT vehicleId, date, km, 'MANUAL', id FROM odometer_entries
        UNION ALL SELECT vehicleId, date, endKm, 'TRIP', id FROM trips WHERE endKm IS NOT NULL
        UNION ALL SELECT vehicleId, date, odometerKm, 'ITV', id FROM itv_inspections WHERE odometerKm IS NOT NULL
        UNION ALL SELECT vehicleId, date, odometerKm, 'DAMAGE', id FROM damages WHERE odometerKm IS NOT NULL
        UNION ALL SELECT vehicleId, date, odometerKm, 'TIRES', id FROM tire_pressure_checks WHERE odometerKm IS NOT NULL
        UNION ALL SELECT vehicleId, date, odometerKm, 'EXPENSE', id FROM expenses WHERE odometerKm IS NOT NULL
        UNION ALL SELECT id, purchaseDate, purchaseKm, 'PURCHASE', id FROM vehicles
            WHERE purchaseKm IS NOT NULL AND purchaseDate IS NOT NULL
    """,
)
data class OdometerEvent(
    val vehicleId: Long,
    val date: LocalDate,
    val km: Int,
    val source: OdometerSource,
    val refId: Long,
)
