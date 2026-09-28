package com.callankan.poloapp.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.callankan.poloapp.feature.backup.BackupScreen
import com.callankan.poloapp.feature.common.PhotoViewerScreen
import com.callankan.poloapp.feature.compliance.InsuranceEditorScreen
import com.callankan.poloapp.feature.compliance.InsuranceScreen
import com.callankan.poloapp.feature.compliance.ItvEditorScreen
import com.callankan.poloapp.feature.compliance.ItvScreen
import com.callankan.poloapp.feature.dashboard.DashboardScreen
import com.callankan.poloapp.feature.finance.DebtSimulatorScreen
import com.callankan.poloapp.feature.finance.ExpenseEditorScreen
import com.callankan.poloapp.feature.finance.FinanceScreen
import com.callankan.poloapp.feature.finance.LoanEditorScreen
import com.callankan.poloapp.feature.finance.PaymentEditorScreen
import com.callankan.poloapp.feature.fuel.FuelScreen
import com.callankan.poloapp.feature.fuel.RefuelEditorScreen
import com.callankan.poloapp.feature.garage.OdometerEditorScreen
import com.callankan.poloapp.feature.garage.OdometerHistoryScreen
import com.callankan.poloapp.feature.garage.SpecsScreen
import com.callankan.poloapp.feature.garage.VehicleEditorScreen
import com.callankan.poloapp.feature.garage.VehiclesScreen
import com.callankan.poloapp.feature.logbook.DamageEditorScreen
import com.callankan.poloapp.feature.logbook.DamagesScreen
import com.callankan.poloapp.feature.logbook.DocumentEditorScreen
import com.callankan.poloapp.feature.logbook.DocumentsScreen
import com.callankan.poloapp.feature.logbook.NoteEditorScreen
import com.callankan.poloapp.feature.logbook.NotesScreen
import com.callankan.poloapp.feature.logbook.TireEditorScreen
import com.callankan.poloapp.feature.logbook.TiresScreen
import com.callankan.poloapp.feature.logbook.TripEditorScreen
import com.callankan.poloapp.feature.logbook.TripsScreen
import com.callankan.poloapp.feature.maintenance.ComponentDetailScreen
import com.callankan.poloapp.feature.maintenance.ComponentEditorScreen
import com.callankan.poloapp.feature.maintenance.InstallationEditorScreen
import com.callankan.poloapp.feature.maintenance.MaintenanceDetailScreen
import com.callankan.poloapp.feature.maintenance.MaintenanceEditorScreen
import com.callankan.poloapp.feature.maintenance.MaintenanceScreen
import com.callankan.poloapp.feature.maintenance.WorkshopEditorScreen
import com.callankan.poloapp.feature.more.MoreScreen
import com.callankan.poloapp.feature.onboarding.OnboardingScreen
import com.callankan.poloapp.feature.report.ReportScreen
import com.callankan.poloapp.feature.settings.SettingsScreen
import com.callankan.poloapp.feature.stats.StatsScreen
import kotlin.reflect.KClass

private data class Tab(val label: String, val icon: ImageVector, val route: Any, val type: KClass<*>)

private val tabs = listOf(
    Tab("Inicio", Icons.Rounded.Dashboard, DashboardRoute, DashboardRoute::class),
    Tab("Combustible", Icons.Rounded.LocalGasStation, FuelRoute, FuelRoute::class),
    Tab("Taller", Icons.Rounded.Build, MaintenanceRoute(), MaintenanceRoute::class),
    Tab("Finanzas", Icons.Rounded.Savings, FinanceRoute(), FinanceRoute::class),
    Tab("Más", Icons.Rounded.Apps, MoreRoute, MoreRoute::class),
)

private fun NavDestination?.isTab(): Boolean = this != null && tabs.any { t -> hierarchy.any { it.hasRoute(t.type) } }

@Composable
fun PoloNavHost(hasVehicle: Boolean, navController: NavHostController = rememberNavController()) {
    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val navigate: (Any) -> Unit = { route ->
        val tab = tabs.firstOrNull { it.type.isInstance(route) }
        if (tab != null) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = route !is MaintenanceRoute && route !is FinanceRoute
            }
        } else {
            navController.navigate(route) { launchSingleTop = true }
        }
    }
    val back: () -> Unit = { navController.popBackStack() }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AnimatedVisibility(
                visible = destination.isTab(),
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow, tonalElevation = 0.dp) {
                    tabs.forEach { tab ->
                        val selected = destination?.hierarchy?.any { it.hasRoute(tab.type) } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigate(tab.route) },
                            icon = { Icon(tab.icon, null) },
                            label = { Text(tab.label, style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (hasVehicle) DashboardRoute else OnboardingRoute(),
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
            enterTransition = {
                if (targetState.destination.isTab() && initialState.destination.isTab()) fadeIn(tween(220))
                else slideInHorizontally(tween(320)) { it / 5 } + fadeIn(tween(320))
            },
            exitTransition = {
                if (targetState.destination.isTab() && initialState.destination.isTab()) fadeOut(tween(160))
                else fadeOut(tween(220)) + scaleOut(tween(320), targetScale = 0.97f)
            },
            popEnterTransition = { fadeIn(tween(260)) + scaleIn(tween(320), initialScale = 0.97f) },
            popExitTransition = { slideOutHorizontally(tween(280)) { it / 5 } + fadeOut(tween(240)) },
        ) {
            composable<OnboardingRoute> { entry ->
                val route = entry.toRoute<OnboardingRoute>()
                OnboardingScreen(
                    additional = route.additional,
                    onFinished = {
                        navController.navigate(DashboardRoute) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    },
                    onCancel = if (route.additional) back else null,
                )
            }
            composable<DashboardRoute> { DashboardScreen(navigate) }
            composable<FuelRoute> { FuelScreen(navigate) }
            composable<MaintenanceRoute> { MaintenanceScreen(it.toRoute<MaintenanceRoute>().tab, navigate) }
            composable<FinanceRoute> { FinanceScreen(it.toRoute<FinanceRoute>().tab, navigate) }
            composable<MoreRoute> { MoreScreen(navigate) }

            composable<VehiclesRoute> { VehiclesScreen(back, navigate) }
            composable<VehicleEditorRoute> {
                VehicleEditorScreen(back, onDeleted = {
                    navController.navigate(DashboardRoute) { popUpTo(navController.graph.id) { inclusive = true } }
                })
            }
            composable<OdometerEditorRoute> { OdometerEditorScreen(back) }
            composable<OdometerHistoryRoute> { OdometerHistoryScreen(back, navigate) }
            composable<SpecsRoute> { SpecsScreen(back, navigate) }

            composable<RefuelEditorRoute> { RefuelEditorScreen(back) }

            composable<MaintenanceEditorRoute> { MaintenanceEditorScreen(back, navigate) }
            composable<MaintenanceDetailRoute> { MaintenanceDetailScreen(back, navigate) }
            composable<ComponentDetailRoute> { ComponentDetailScreen(back, navigate) }
            composable<ComponentEditorRoute> {
                ComponentEditorScreen(back, onDeleted = {
                    navController.popBackStack(MaintenanceRoute::class, inclusive = false)
                })
            }
            composable<InstallationEditorRoute> { InstallationEditorScreen(back) }
            composable<WorkshopEditorRoute> { WorkshopEditorScreen(back) }

            composable<ExpenseEditorRoute> { ExpenseEditorScreen(back, navigate) }
            composable<LoanEditorRoute> { LoanEditorScreen(back) }
            composable<PaymentEditorRoute> { PaymentEditorScreen(back) }
            composable<DebtSimulatorRoute> { DebtSimulatorScreen(back) }

            composable<TripsRoute> { TripsScreen(back, navigate) }
            composable<TripEditorRoute> { TripEditorScreen(back) }
            composable<NotesRoute> { NotesScreen(back, navigate) }
            composable<NoteEditorRoute> { NoteEditorScreen(back) }
            composable<ItvRoute> { ItvScreen(back, navigate) }
            composable<ItvEditorRoute> { ItvEditorScreen(back, navigate) }
            composable<InsuranceRoute> { InsuranceScreen(back, navigate) }
            composable<InsuranceEditorRoute> { InsuranceEditorScreen(back, navigate) }
            composable<TiresRoute> { TiresScreen(back, navigate) }
            composable<TireEditorRoute> { TireEditorScreen(back) }
            composable<DamagesRoute> { DamagesScreen(back, navigate) }
            composable<DamageEditorRoute> { DamageEditorScreen(back, navigate) }
            composable<DocumentsRoute> { DocumentsScreen(back, navigate) }
            composable<DocumentEditorRoute> { DocumentEditorScreen(back, navigate) }
            composable<StatsRoute> { StatsScreen(back) }
            composable<ReportRoute> { ReportScreen(back) }
            composable<BackupRoute> { BackupScreen(back) }
            composable<SettingsRoute> { SettingsScreen(back, navigate) }
            composable<PhotoViewerRoute> { PhotoViewerScreen(it.toRoute<PhotoViewerRoute>().fileName, back) }
        }
    }
}
