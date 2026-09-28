package com.callankan.poloapp.feature.finance

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callankan.poloapp.data.model.ExpenseCategory
import com.callankan.poloapp.navigation.DebtSimulatorRoute
import com.callankan.poloapp.navigation.ExpenseEditorRoute
import com.callankan.poloapp.navigation.LoanEditorRoute
import com.callankan.poloapp.navigation.PaymentEditorRoute
import com.callankan.poloapp.navigation.StatsRoute
import com.callankan.poloapp.ui.components.ChartCard
import com.callankan.poloapp.ui.components.ChartColors
import com.callankan.poloapp.ui.components.EmptyState
import com.callankan.poloapp.ui.components.IconBadge
import com.callankan.poloapp.ui.components.Legend
import com.callankan.poloapp.ui.components.LineChart
import com.callankan.poloapp.ui.components.LinearMeter
import com.callankan.poloapp.ui.components.MetricText
import com.callankan.poloapp.ui.components.PoloCard
import com.callankan.poloapp.ui.components.PoloFab
import com.callankan.poloapp.ui.components.ProgressRing
import com.callankan.poloapp.ui.components.SectionHeader
import com.callankan.poloapp.ui.components.SegmentedBar
import com.callankan.poloapp.ui.components.SegmentedTabs
import com.callankan.poloapp.ui.components.StatTile
import com.callankan.poloapp.ui.components.TabScaffold
import com.callankan.poloapp.ui.components.icon
import com.callankan.poloapp.ui.format.Fmt
import com.callankan.poloapp.ui.theme.NumberStyles
import com.callankan.poloapp.ui.theme.PoloTheme
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun FinanceScreen(initialTab: Int, onNavigate: (Any) -> Unit, vm: FinanceViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val unlocked by vm.lock.unlocked.collectAsStateWithLifecycle()
    val s = state
    if (s == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val context = LocalContext.current
    FinanceContent(
        s, initialTab, locked = s.lockEnabled && !unlocked, onNavigate = onNavigate,
        onUnlock = {
            authenticate(context, "Desbloquear finanzas", onSuccess = vm.lock::unlock) {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
        },
    )
}

@Composable
fun FinanceContent(s: FinanceUiState, initialTab: Int, locked: Boolean, onNavigate: (Any) -> Unit, onUnlock: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }
    TabScaffold(
        title = "Finanzas",
        subtitle = "Deuda del coche y gastos del día a día",
        header = { SegmentedTabs(listOf("Deuda", "Gastos"), tab, { tab = it }) },
        actions = { IconButton(onClick = { onNavigate(StatsRoute) }) { Icon(Icons.Rounded.BarChart, "Estadísticas") } },
        fab = {
            AnimatedContent(tab, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "fab") { t ->
                val loan = s.loans.firstOrNull { !it.summary.isPaidOff } ?: s.loans.firstOrNull()
                when {
                    t == 1 -> PoloFab("Gasto", Icons.Rounded.Add) { onNavigate(ExpenseEditorRoute()) }
                    locked -> Unit
                    loan != null -> PoloFab("Pago", Icons.Rounded.Payments) { onNavigate(PaymentEditorRoute(loan.summary.loan.id)) }
                    else -> PoloFab("Deuda", Icons.Rounded.Add) { onNavigate(LoanEditorRoute()) }
                }
            }
        },
    ) {
        if (tab == 0) debtTab(s, locked, onNavigate, onUnlock) else expensesTab(s, onNavigate)
    }
}

private fun LazyListScope.debtTab(s: FinanceUiState, locked: Boolean, onNavigate: (Any) -> Unit, onUnlock: () -> Unit) {
    if (locked) {
        item {
            PoloCard {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    IconBadge(Icons.Rounded.Lock, PoloTheme.colors.violet, size = 64.dp, iconSize = 30.dp)
                    Spacer(Modifier.height(12.dp))
                    Text("Sección protegida", style = MaterialTheme.typography.titleMedium)
                    Text("Desbloquea con tu huella o el PIN del móvil", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onUnlock) { Text("Desbloquear") }
                }
            }
        }
        return
    }
    if (s.loans.isEmpty()) {
        item {
            EmptyState(
                Icons.Rounded.Savings, "Sin deudas registradas",
                "Apunta lo que te prestaron para el coche y ve añadiendo cada pago. Verás cuánto queda y cuándo terminarás.",
                actionLabel = "Registrar deuda", onAction = { onNavigate(LoanEditorRoute()) },
            )
        }
        return
    }
    s.loans.forEach { lw ->
        val d = lw.summary
        val violet = ChartColors.other
        item(key = "loan${d.loan.id}") {
            PoloCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Préstamo · ${d.loan.lender}", style = MaterialTheme.typography.titleMedium)
                        Text("Desde ${Fmt.date(d.loan.startDate)} · sin intereses", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { onNavigate(LoanEditorRoute(d.loan.id)) }) { Icon(Icons.Rounded.Edit, "Editar") }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (d.isPaidOff) "¡Deuda saldada!" else "Te queda por pagar", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        MetricText(Fmt.moneyPlain(d.remaining), "€", style = NumberStyles.hero)
                    }
                    ProgressRing(d.progress, violet, size = 78.dp, strokeWidth = 8.dp) {
                        Text(Fmt.percent(d.progress), style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(Modifier.height(12.dp))
                LinearMeter(d.progress, violet, height = 10.dp)
                Spacer(Modifier.height(8.dp))
                Text("Pagado ${Fmt.money(d.paid)} de ${Fmt.money(d.initial)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!d.isPaidOff) {
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(onClick = { onNavigate(PaymentEditorRoute(d.loan.id)) }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Payments, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Añadir pago")
                        }
                        Button(onClick = { onNavigate(DebtSimulatorRoute(d.loan.id)) }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Calculate, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Simulador")
                        }
                    }
                }
            }
        }
        item(key = "pace${d.loan.id}") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Media mensual", Fmt.moneyRound(d.averageMonthly), Modifier.weight(1f), icon = Icons.Rounded.Payments, tint = violet, caption = "${d.paymentsCount} pagos")
                StatTile(
                    "A tu ritmo", d.projectedPayoff?.let { Fmt.monthYear(it) } ?: "—", Modifier.weight(1f),
                    icon = Icons.AutoMirrored.Rounded.TrendingDown, tint = PoloTheme.colors.teal,
                    caption = d.projectedPayoff?.let { "en ${Fmt.duration(monthsUntil(it))}" } ?: "Añade pagos",
                )
            }
        }
        if (lw.payments.size >= 2) {
            item(key = "chart${d.loan.id}") {
                ChartCard("Evolución de la deuda", "Saldo pendiente tras cada pago") {
                    val sorted = lw.payments.sortedWith(compareBy({ it.date }, { it.id }))
                    var balance = d.initial
                    val values = listOf(d.initial) + sorted.map { balance -= it.amount; balance.coerceAtLeast(0.0) }
                    LineChart(
                        values, violet,
                        labels = listOf(Fmt.date(d.loan.startDate), Fmt.date(sorted.last().date)),
                        valueLabel = { Fmt.moneyRound(it) },
                    )
                }
            }
        }
        if (lw.payments.isNotEmpty()) {
            item(key = "ph${d.loan.id}") { SectionHeader("Historial de pagos") }
            val sortedAsc = lw.payments.sortedWith(compareBy({ it.date }, { it.id }))
            val remainingAfter = HashMap<Long, Double>()
            var bal = d.initial
            sortedAsc.forEach { bal -= it.amount; remainingAfter[it.id] = bal.coerceAtLeast(0.0) }
            items(lw.payments, key = { "p${it.id}" }) { p ->
                PoloCard(onClick = { onNavigate(PaymentEditorRoute(d.loan.id, p.id)) }, contentPadding = PaddingValues(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Rounded.Payments, violet, size = 36.dp, iconSize = 18.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(Fmt.longDate(p.date), style = MaterialTheme.typography.titleSmall)
                            Text(
                                listOf("Quedaban ${Fmt.money(remainingAfter[p.id])}", p.note).filter { it.isNotBlank() }.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text("−${Fmt.money(p.amount)}", style = NumberStyles.small, color = PoloTheme.colors.success)
                    }
                }
            }
        }
    }
}

private fun monthsUntil(month: YearMonth): Int =
    java.time.temporal.ChronoUnit.MONTHS.between(YearMonth.now(), month).toInt().coerceAtLeast(0)

private fun LazyListScope.expensesTab(s: FinanceUiState, onNavigate: (Any) -> Unit) {
    val expenses = s.expenses
    val year = LocalDate.now().year
    val thisMonth = YearMonth.now()
    item(key = "exp-summary") {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("Este mes", Fmt.moneyRound(expenses.filter { YearMonth.from(it.date) == thisMonth }.sumOf { it.amount }), Modifier.weight(1f), icon = Icons.Rounded.Receipt, tint = ChartColors.other)
            StatTile("En $year", Fmt.moneyRound(expenses.filter { it.date.year == year }.sumOf { it.amount }), Modifier.weight(1f), icon = Icons.Rounded.BarChart, tint = PoloTheme.colors.info)
        }
    }
    if (expenses.isEmpty()) {
        item {
            EmptyState(
                Icons.Rounded.Receipt, "Sin gastos", "Impuesto de circulación, parking, peajes, lavados, multas... todo suma al coste real del coche.",
                actionLabel = "Añadir gasto", onAction = { onNavigate(ExpenseEditorRoute()) },
            )
        }
        return
    }
    val byCategory = ExpenseCategory.entries.map { c -> c to expenses.filter { it.category == c }.sumOf { it.amount } }.filter { it.second > 0 }
    item(key = "exp-breakdown") {
        PoloCard {
            Text("Reparto por categoría", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            SegmentedBar(byCategory.map { (c, v) -> v to ChartColors.palette[(c.ordinal + 4) % ChartColors.palette.size] })
            Spacer(Modifier.height(12.dp))
            Legend(byCategory.map { (c, v) -> "${c.label} · ${Fmt.moneyRound(v)}" to ChartColors.palette[(c.ordinal + 4) % ChartColors.palette.size] })
        }
    }
    expenses.groupBy { YearMonth.from(it.date) }.forEach { (month, list) ->
        item(key = "em$month") { SectionHeader(Fmt.monthYear(month), subtitle = Fmt.money(list.sumOf { it.amount })) }
        items(list, key = { "e${it.id}" }) { e ->
            PoloCard(onClick = { onNavigate(ExpenseEditorRoute(e.id)) }, contentPadding = PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(e.category.icon, ChartColors.palette[(e.category.ordinal + 4) % ChartColors.palette.size])
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(e.description.ifBlank { e.category.label }, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${Fmt.date(e.date)} · ${e.category.label}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(Fmt.money(e.amount), style = NumberStyles.small)
                }
            }
        }
    }
}
