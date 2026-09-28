package com.callankan.poloapp.data.model

enum class FuelType(val label: String) {
    GASOLINE("Gasolina"),
    DIESEL("Diésel"),
    HYBRID("Híbrido"),
    PLUG_IN_HYBRID("Híbrido enchufable"),
    LPG("GLP / Autogas"),
    ELECTRIC("Eléctrico"),
}

enum class TimingDrive(val label: String) {
    CHAIN("Cadena"),
    BELT("Correa"),
    UNKNOWN("No lo sé"),
}

enum class PerformedBy(val label: String) {
    WORKSHOP("Taller"),
    DIY("Por mi cuenta"),
}

enum class MaintenanceCategory(val label: String) {
    OIL_SERVICE("Aceite y revisión"),
    FILTERS("Filtros"),
    BRAKES("Frenos"),
    TIRES("Neumáticos"),
    TIMING("Distribución y correas"),
    IGNITION("Encendido"),
    FLUIDS("Líquidos"),
    BATTERY("Batería y eléctrico"),
    SUSPENSION("Suspensión y dirección"),
    CLIMATE("Climatización"),
    BODY("Carrocería y cristales"),
    REPAIR("Avería / reparación"),
    INSPECTION("Diagnóstico / inspección"),
    OTHER("Otros"),
}

enum class ExpenseCategory(val label: String) {
    TAX("Impuesto de circulación"),
    PARKING("Parking"),
    TOLL("Peajes"),
    WASH("Lavado y limpieza"),
    FINE("Multas"),
    ACCESSORIES("Accesorios"),
    PAPERWORK("Gestiones y trámites"),
    OTHER("Otros"),
}

enum class ItvResult(val label: String, val passed: Boolean) {
    FAVORABLE("Favorable", true),
    FAVORABLE_WITH_DEFECTS("Favorable con defectos leves", true),
    UNFAVORABLE("Desfavorable", false),
    NEGATIVE("Negativa", false),
}

enum class InsuranceCoverage(val label: String) {
    THIRD_PARTY("Terceros básico"),
    THIRD_PARTY_EXTENDED("Terceros ampliado"),
    COMPREHENSIVE_EXCESS("Todo riesgo con franquicia"),
    COMPREHENSIVE("Todo riesgo"),
}

enum class DamageSeverity(val label: String) {
    MINOR("Leve"),
    MODERATE("Moderado"),
    SEVERE("Grave"),
}

enum class CarZone(val label: String) {
    FRONT_BUMPER("Paragolpes delantero"),
    HOOD("Capó"),
    WINDSHIELD("Parabrisas"),
    ROOF("Techo"),
    TAILGATE("Portón trasero"),
    REAR_BUMPER("Paragolpes trasero"),
    FRONT_LEFT_WING("Aleta delantera izquierda"),
    FRONT_LEFT_DOOR("Puerta delantera izquierda"),
    REAR_LEFT_DOOR("Puerta trasera izquierda"),
    REAR_LEFT_WING("Aleta trasera izquierda"),
    FRONT_RIGHT_WING("Aleta delantera derecha"),
    FRONT_RIGHT_DOOR("Puerta delantera derecha"),
    REAR_RIGHT_DOOR("Puerta trasera derecha"),
    REAR_RIGHT_WING("Aleta trasera derecha"),
    LEFT_MIRROR("Retrovisor izquierdo"),
    RIGHT_MIRROR("Retrovisor derecho"),
    WHEELS("Llantas / neumáticos"),
    INTERIOR("Interior"),
    OTHER("Otra zona"),
}

enum class DocumentType(val label: String) {
    REGISTRATION("Permiso de circulación"),
    TECH_SHEET("Ficha técnica"),
    INSURANCE("Póliza del seguro"),
    ITV_REPORT("Informe de ITV"),
    PURCHASE("Contrato / factura de compra"),
    MANUAL("Manual / libro de mantenimiento"),
    OTHER("Otro documento"),
}

enum class AttachmentOwner {
    MAINTENANCE,
    DAMAGE,
    DOCUMENT,
    ITV,
    EXPENSE,
    INSURANCE,
}

enum class OdometerSource(val label: String) {
    PURCHASE("Compra"),
    MANUAL("Lectura manual"),
    REFUEL("Repostaje"),
    MAINTENANCE("Mantenimiento"),
    TRIP("Viaje"),
    ITV("ITV"),
    DAMAGE("Daño"),
    TIRES("Neumáticos"),
    EXPENSE("Gasto"),
}
