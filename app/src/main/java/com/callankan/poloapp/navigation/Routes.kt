package com.callankan.poloapp.navigation

import kotlinx.serialization.Serializable

// Pestañas principales
@Serializable data object DashboardRoute
@Serializable data object FuelRoute
@Serializable data class MaintenanceRoute(val tab: Int = 0)
@Serializable data class FinanceRoute(val tab: Int = 0)
@Serializable data object MoreRoute

// Alta y gestión de vehículos
@Serializable data class OnboardingRoute(val additional: Boolean = false)
@Serializable data object VehiclesRoute
@Serializable data class VehicleEditorRoute(val id: Long)
@Serializable data class OdometerEditorRoute(val id: Long = 0)
@Serializable data object OdometerHistoryRoute

// Combustible
@Serializable data class RefuelEditorRoute(val id: Long = 0)

// Taller
@Serializable data class MaintenanceEditorRoute(val id: Long = 0, val componentId: Long = 0)
@Serializable data class MaintenanceDetailRoute(val id: Long)
@Serializable data class ComponentDetailRoute(val id: Long)
@Serializable data class ComponentEditorRoute(val id: Long = 0)
@Serializable data class InstallationEditorRoute(val componentId: Long, val id: Long = 0)
@Serializable data class WorkshopEditorRoute(val id: Long = 0)

// Finanzas
@Serializable data class ExpenseEditorRoute(val id: Long = 0)
@Serializable data class LoanEditorRoute(val id: Long = 0)
@Serializable data class PaymentEditorRoute(val loanId: Long, val id: Long = 0)
@Serializable data class DebtSimulatorRoute(val loanId: Long)

// Más
@Serializable data object TripsRoute
@Serializable data class TripEditorRoute(val id: Long = 0)
@Serializable data object NotesRoute
@Serializable data class NoteEditorRoute(val id: Long = 0)
@Serializable data object ItvRoute
@Serializable data class ItvEditorRoute(val id: Long = 0)
@Serializable data object InsuranceRoute
@Serializable data class InsuranceEditorRoute(val id: Long = 0)
@Serializable data object TiresRoute
@Serializable data class TireEditorRoute(val id: Long = 0)
@Serializable data object DamagesRoute
@Serializable data class DamageEditorRoute(val id: Long = 0)
@Serializable data object DocumentsRoute
@Serializable data class DocumentEditorRoute(val id: Long = 0)
@Serializable data object SpecsRoute
@Serializable data object StatsRoute
@Serializable data object ReportRoute
@Serializable data object BackupRoute
@Serializable data object SettingsRoute
@Serializable data class PhotoViewerRoute(val fileName: String)
