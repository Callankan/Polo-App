package com.callankan.poloapp.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.DiscFull
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.LocalCarWash
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalParking
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.OilBarrel
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.TireRepair
import androidx.compose.material.icons.rounded.Toll
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.callankan.poloapp.data.model.DamageSeverity
import com.callankan.poloapp.data.model.DocumentType
import com.callankan.poloapp.data.model.ExpenseCategory
import com.callankan.poloapp.data.model.MaintenanceCategory
import com.callankan.poloapp.domain.CostGroup
import com.callankan.poloapp.domain.CostKind
import com.callankan.poloapp.domain.DeadlineState
import com.callankan.poloapp.domain.DueState
import com.callankan.poloapp.domain.HealthLevel
import com.callankan.poloapp.ui.theme.PoloTheme

val MaintenanceCategory.icon: ImageVector
    get() = when (this) {
        MaintenanceCategory.OIL_SERVICE -> Icons.Rounded.OilBarrel
        MaintenanceCategory.FILTERS -> Icons.Rounded.FilterAlt
        MaintenanceCategory.BRAKES -> Icons.Rounded.DiscFull
        MaintenanceCategory.TIRES -> Icons.Rounded.TireRepair
        MaintenanceCategory.TIMING -> Icons.Rounded.Settings
        MaintenanceCategory.IGNITION -> Icons.Rounded.ElectricBolt
        MaintenanceCategory.FLUIDS -> Icons.Rounded.WaterDrop
        MaintenanceCategory.BATTERY -> Icons.Rounded.BatteryChargingFull
        MaintenanceCategory.SUSPENSION -> Icons.Rounded.Tune
        MaintenanceCategory.CLIMATE -> Icons.Rounded.AcUnit
        MaintenanceCategory.BODY -> Icons.Rounded.DirectionsCar
        MaintenanceCategory.REPAIR -> Icons.Rounded.Build
        MaintenanceCategory.INSPECTION -> Icons.Rounded.Search
        MaintenanceCategory.OTHER -> Icons.Rounded.MoreHoriz
    }

val ExpenseCategory.icon: ImageVector
    get() = when (this) {
        ExpenseCategory.TAX -> Icons.Rounded.AccountBalance
        ExpenseCategory.PARKING -> Icons.Rounded.LocalParking
        ExpenseCategory.TOLL -> Icons.Rounded.Toll
        ExpenseCategory.WASH -> Icons.Rounded.LocalCarWash
        ExpenseCategory.FINE -> Icons.Rounded.Gavel
        ExpenseCategory.ACCESSORIES -> Icons.Rounded.ShoppingBag
        ExpenseCategory.PAPERWORK -> Icons.Rounded.Description
        ExpenseCategory.OTHER -> Icons.Rounded.Receipt
    }

val DocumentType.icon: ImageVector
    get() = when (this) {
        DocumentType.REGISTRATION -> Icons.Rounded.Badge
        DocumentType.TECH_SHEET -> Icons.Rounded.Description
        DocumentType.INSURANCE -> Icons.Rounded.Shield
        DocumentType.ITV_REPORT -> Icons.Rounded.TaskAlt
        DocumentType.PURCHASE -> Icons.Rounded.Handshake
        DocumentType.MANUAL -> Icons.Rounded.AutoStories
        DocumentType.OTHER -> Icons.Rounded.Folder
    }

val CostKind.icon: ImageVector
    get() = when (this) {
        CostKind.FUEL -> Icons.Rounded.LocalGasStation
        CostKind.MAINTENANCE -> Icons.Rounded.Build
        CostKind.DAMAGE -> Icons.Rounded.DirectionsCar
        CostKind.INSURANCE -> Icons.Rounded.Shield
        CostKind.ITV -> Icons.Rounded.TaskAlt
        CostKind.TAX -> Icons.Rounded.AccountBalance
        CostKind.PARKING -> Icons.Rounded.LocalParking
        CostKind.TOLL -> Icons.Rounded.Toll
        CostKind.WASH -> Icons.Rounded.LocalCarWash
        CostKind.FINE -> Icons.Rounded.Gavel
        CostKind.ACCESSORIES -> Icons.Rounded.ShoppingBag
        CostKind.PAPERWORK -> Icons.Rounded.Description
        CostKind.OTHER -> Icons.Rounded.Receipt
    }

object ChartColors {
    val fuel = Color(0xFFFF8A3D)
    val maintenance = Color(0xFF4DA3FF)
    val other = Color(0xFF9B87F5)
    val palette = listOf(
        Color(0xFFFF8A3D), Color(0xFF4DA3FF), Color(0xFF9B87F5), Color(0xFF2FD08A),
        Color(0xFFFFC53D), Color(0xFFFF5A8A), Color(0xFF2DD4BF), Color(0xFFB0B8C4),
        Color(0xFFE8373E), Color(0xFF7DD3FC), Color(0xFFA3E635), Color(0xFFF472B6), Color(0xFF94A3B8),
    )
}

val CostGroup.color: Color
    get() = when (this) {
        CostGroup.FUEL -> ChartColors.fuel
        CostGroup.MAINTENANCE -> ChartColors.maintenance
        CostGroup.OTHER -> ChartColors.other
    }

val CostKind.color: Color get() = ChartColors.palette[ordinal % ChartColors.palette.size]

@Composable
@ReadOnlyComposable
fun DueState.color(): Color = when (this) {
    DueState.OVERDUE -> PoloTheme.colors.danger
    DueState.SOON -> PoloTheme.colors.warning
    DueState.OK -> PoloTheme.colors.success
    DueState.NEVER_DONE -> PoloTheme.colors.info
    DueState.NO_INTERVAL -> PoloTheme.colors.info
}

val DueState.label: String
    get() = when (this) {
        DueState.OVERDUE -> "Vencido"
        DueState.SOON -> "Pronto"
        DueState.OK -> "Al día"
        DueState.NEVER_DONE -> "Sin registrar"
        DueState.NO_INTERVAL -> "Seguimiento"
    }

@Composable
@ReadOnlyComposable
fun DeadlineState.color(): Color = when (this) {
    DeadlineState.OK -> PoloTheme.colors.success
    DeadlineState.SOON -> PoloTheme.colors.warning
    DeadlineState.URGENT -> PoloTheme.colors.danger
    DeadlineState.EXPIRED -> PoloTheme.colors.danger
    DeadlineState.UNKNOWN -> PoloTheme.colors.info
}

@Composable
@ReadOnlyComposable
fun HealthLevel.color(): Color = when (this) {
    HealthLevel.EXCELLENT -> PoloTheme.colors.success
    HealthLevel.GOOD -> PoloTheme.colors.teal
    HealthLevel.FAIR -> PoloTheme.colors.warning
    HealthLevel.ATTENTION -> PoloTheme.colors.danger
}

@Composable
@ReadOnlyComposable
fun DamageSeverity.color(): Color = when (this) {
    DamageSeverity.MINOR -> PoloTheme.colors.warning
    DamageSeverity.MODERATE -> Color(0xFFFF8A3D)
    DamageSeverity.SEVERE -> PoloTheme.colors.danger
}
