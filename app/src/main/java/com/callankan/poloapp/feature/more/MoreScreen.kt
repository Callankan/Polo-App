package com.callankan.poloapp.feature.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CarCrash
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.ListAlt
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.TireRepair
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callankan.poloapp.data.repository.VehicleOverview
import com.callankan.poloapp.feature.dashboard.DashboardViewModel
import com.callankan.poloapp.navigation.BackupRoute
import com.callankan.poloapp.navigation.DamagesRoute
import com.callankan.poloapp.navigation.DocumentsRoute
import com.callankan.poloapp.navigation.InsuranceRoute
import com.callankan.poloapp.navigation.ItvRoute
import com.callankan.poloapp.navigation.NotesRoute
import com.callankan.poloapp.navigation.OdometerHistoryRoute
import com.callankan.poloapp.navigation.ReportRoute
import com.callankan.poloapp.navigation.SettingsRoute
import com.callankan.poloapp.navigation.SpecsRoute
import com.callankan.poloapp.navigation.StatsRoute
import com.callankan.poloapp.navigation.TiresRoute
import com.callankan.poloapp.navigation.TripsRoute
import com.callankan.poloapp.navigation.VehiclesRoute
import com.callankan.poloapp.ui.components.IconBadge
import com.callankan.poloapp.ui.components.PoloCard
import com.callankan.poloapp.ui.components.SectionHeader
import com.callankan.poloapp.ui.components.TabScaffold
import com.callankan.poloapp.ui.format.Fmt
import com.callankan.poloapp.ui.theme.PoloTheme

private data class Entry(val title: String, val subtitle: String, val icon: ImageVector, val tint: Color, val route: Any)

@Composable
fun MoreScreen(onNavigate: (Any) -> Unit, vm: DashboardViewModel = hiltViewModel()) {
    val overview by vm.overview.collectAsStateWithLifecycle()
    MoreContent(overview, onNavigate)
}

@Composable
fun MoreContent(o: VehicleOverview?, onNavigate: (Any) -> Unit) {
    val c = PoloTheme.colors
    val sections = listOf(
        "Diario del coche" to listOf(
            Entry("Viajes", "Trayectos largos", Icons.Rounded.Map, c.info, TripsRoute),
            Entry("Notas", "Y recordatorios", Icons.Rounded.StickyNote2, c.warning, NotesRoute),
            Entry(
                "Presiones", o?.lastTireCheck?.let { "Última ${Fmt.since(it.date, o.today)}" } ?: "Neumáticos",
                Icons.Rounded.TireRepair, c.teal, TiresRoute,
            ),
            Entry(
                "Daños", o?.openDamages?.size?.takeIf { it > 0 }?.let { "$it sin reparar" } ?: "Golpes y rayones",
                Icons.Rounded.CarCrash, c.danger, DamagesRoute,
            ),
        ),
        "Papeles" to listOf(
            Entry("ITV", o?.itv?.daysLeft?.let { if (it >= 0) "En $it días" else "Caducada" } ?: "Inspecciones", Icons.Rounded.TaskAlt, c.success, ItvRoute),
            Entry("Seguro", o?.insurance?.daysLeft?.let { if (it >= 0) "Vence en $it días" else "Vencido" } ?: "Póliza", Icons.Rounded.Shield, c.info, InsuranceRoute),
            Entry("Guantera", "Documentos", Icons.Rounded.Folder, c.violet, DocumentsRoute),
            Entry("Ficha rápida", "Datos técnicos", Icons.Rounded.ListAlt, c.teal, SpecsRoute),
        ),
        "Herramientas" to listOf(
            Entry("Informe PDF", "Para vender", Icons.Rounded.PictureAsPdf, MaterialTheme.colorScheme.primary, ReportRoute),
            Entry("Estadísticas", "Costes y consumo", Icons.Rounded.BarChart, c.warning, StatsRoute),
            Entry("Kilometraje", o?.currentKm?.let { Fmt.km(it) } ?: "Historial", Icons.Rounded.Speed, c.info, OdometerHistoryRoute),
            Entry("Copia", "Seguridad", Icons.Rounded.Save, c.success, BackupRoute),
        ),
        "App" to listOf(
            Entry("Vehículos", o?.vehicle?.alias ?: "Garaje", Icons.Rounded.DirectionsCar, MaterialTheme.colorScheme.primary, VehiclesRoute),
            Entry("Ajustes", "Tema y avisos", Icons.Rounded.Settings, MaterialTheme.colorScheme.onSurfaceVariant, SettingsRoute),
        ),
    )
    TabScaffold(title = "Más", subtitle = "Todo lo demás sobre tu coche") {
        sections.forEach { (title, entries) ->
            item(key = title) { SectionHeader(title) }
            entries.chunked(2).forEachIndexed { i, row ->
                item(key = "$title$i") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { e ->
                            PoloCard(Modifier.weight(1f), onClick = { onNavigate(e.route) }, contentPadding = PaddingValues(16.dp)) {
                                IconBadge(e.icon, e.tint)
                                Spacer(Modifier.height(12.dp))
                                Text(e.title, style = MaterialTheme.typography.titleSmall)
                                Text(e.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
