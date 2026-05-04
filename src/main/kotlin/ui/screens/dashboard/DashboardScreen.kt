package ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import data.model.DashboardFullResponse
import data.model.DashboardRecentActivityResponse
import data.network.DashboardApiService
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch
import kotlin.math.max

@Composable
fun DashboardScreen() {
    val api = remember { DashboardApiService() }
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }
    var from by remember { mutableStateOf(today.withDayOfMonth(1)) }
    var to by remember { mutableStateOf(today.withDayOfMonth(today.lengthOfMonth())) }
    var data by remember { mutableStateOf<DashboardFullResponse?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            loading = true
            try {
                data = api.getFull(from.toString(), to.toString())
                error = null
            } catch (ex: Exception) {
                error = ex.message ?: "Dashboard load failed"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize()) {
        Header(from, to, loading, ::load)
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                FilterCard(
                    from = from,
                    to = to,
                    onFrom = { from = it },
                    onTo = { to = it },
                    onToday = { from = today; to = today; load() },
                    onWeek = { from = today.minusDays(6); to = today; load() },
                    onMonth = { from = today.withDayOfMonth(1); to = today.withDayOfMonth(today.lengthOfMonth()); load() },
                    onApply = ::load
                )
            }
            error?.let { item { MessageCard(it) } }
            if (loading) {
                item { Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            }
            data?.let { d ->
                item { KpiGrid(d) }
                item { TwoColumns({ PieCard("Payment Mix", topFourPlusOthers(d.paymentDistribution.items.map { Slice(it.displayName, it.amount) })) }, { PieCard("Order Status", topFourPlusOthers(d.orderStatusDistribution.items.map { Slice(it.status.replace('_', ' '), it.count.toDouble()) })) }) }
                item { TwoColumns({ PieCard("Top Food Items", topFourPlusOthers(d.topFoodItems.map { Slice(it.foodName, it.netSales) })) }, { PieCard("Top Beverages", topFourPlusOthers(d.topBeverages.map { Slice(it.beverageName, it.netSales) })) }) }
                item { TwoColumns({ PieCard("Waiter Performance", topFourPlusOthers(d.waiterPerformance.map { Slice(it.waiterName ?: "Unknown", it.totalSales) })) }, { PieCard("Table Performance", topFourPlusOthers(d.tablePerformance.map { Slice("Table ${it.tableNumber ?: "-"}", it.totalSales) })) }) }
                item { PeakHoursCard(d) }
                item { RecentActivityCard(d.recentActivity) }
            }
        }
    }
}

@Composable
private fun Header(from: LocalDate, to: LocalDate, loading: Boolean, onRefresh: () -> Unit) {
    Surface(shadowElevation = 3.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(Icons.Filled.Dashboard, null, Modifier.padding(8.dp).size(24.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Column {
                    Text("Restaurant Analytics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("${fmtDate(from)} → ${fmtDate(to)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            OutlinedButton(onClick = onRefresh, enabled = !loading) {
                Icon(Icons.Filled.Refresh, null)
                Spacer(Modifier.width(6.dp))
                Text("Refresh")
            }
        }
    }
}

@Composable
private fun FilterCard(
    from: LocalDate,
    to: LocalDate,
    onFrom: (LocalDate) -> Unit,
    onTo: (LocalDate) -> Unit,
    onToday: () -> Unit,
    onWeek: () -> Unit,
    onMonth: () -> Unit,
    onApply: () -> Unit
) {
    AppCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            CalendarButton("Date From", from, onFrom, Modifier.weight(1f))
            CalendarButton("Date To", to, onTo, Modifier.weight(1f))
            Button(onClick = onApply) { Text("Apply") }
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onToday) { Text("Today") }
            OutlinedButton(onClick = onWeek) { Text("Last 7 Days") }
            OutlinedButton(onClick = onMonth) { Text("This Month") }
            Text("Calendar date selection", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CalendarButton(label: String, selected: LocalDate, onSelect: (LocalDate) -> Unit, modifier: Modifier) {
    var open by remember { mutableStateOf(false) }
    var month by remember(selected) { mutableStateOf(YearMonth.from(selected)) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(fmtDate(selected), fontWeight = FontWeight.Bold)
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Calendar(month, selected, { month = month.minusMonths(1) }, { month = month.plusMonths(1) }) {
                onSelect(it)
                open = false
            }
        }
    }
}

@Composable
private fun Calendar(month: YearMonth, selected: LocalDate, prev: () -> Unit, next: () -> Unit, pick: (LocalDate) -> Unit) {
    Column(Modifier.width(310.dp).padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(prev) { Text("‹") }
            Text("${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}", fontWeight = FontWeight.Black)
            TextButton(next) { Text("›") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { Text(it, Modifier.width(38.dp), style = MaterialTheme.typography.labelSmall) }
        }
        val first = month.atDay(1)
        val cells = List(first.dayOfWeek.value - 1) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                week.forEach { day ->
                    if (day == null) Spacer(Modifier.size(38.dp)) else {
                        TextButton(onClick = { pick(day) }, modifier = Modifier.size(38.dp)) {
                            Text(day.dayOfMonth.toString(), fontWeight = if (day == selected) FontWeight.Black else FontWeight.Normal)
                        }
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.size(38.dp)) }
            }
        }
    }
}

@Composable
private fun KpiGrid(data: DashboardFullResponse) {
    val o = data.overview
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Kpi("Total Sales", money(o.totalSales), "Paid revenue", Modifier.weight(1f))
            Kpi("Paid Orders", o.totalPaidOrders.toString(), "Completed", Modifier.weight(1f))
            Kpi("Average Order", money(o.averageOrderValue), "AOV", Modifier.weight(1f))
            Kpi("Cancellation", "${dec(o.cancellationRate)}%", "Canceled", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Kpi("Active Orders", o.activeOrders.toString(), "Open", Modifier.weight(1f))
            Kpi("Occupied Tables", o.occupiedTables.toString(), "${o.freeTables} free", Modifier.weight(1f))
            Kpi("Menu Items", (o.totalFoodItems + o.totalBeverages).toString(), "Food + beverage", Modifier.weight(1f))
            Kpi("Low Stock", o.lowStockIngredientCount.toString(), "Warnings", Modifier.weight(1f))
        }
    }
}

@Composable
private fun Kpi(title: String, value: String, caption: String, modifier: Modifier) {
    Card(modifier, RoundedCornerShape(16.dp), CardDefaults.cardColors(MaterialTheme.colorScheme.surface), CardDefaults.cardElevation(1.dp), BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(.10f))) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun PieCard(title: String, slices: List<Slice>) {
    val colors = chartColors()
    val valid = slices.filter { it.value > 0.0 }
    val total = valid.sumOf { it.value }.coerceAtLeast(1.0)
    AppCard {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        Text("Top 4 + Others", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    var start = -90f
                    val diameter = size.minDimension
                    if (valid.isEmpty()) {
                        drawArc(colors.first().copy(alpha = .2f), start, 360f, true, Offset.Zero, Size(diameter, diameter))
                    } else {
                        valid.forEachIndexed { i, slice ->
                            val sweep = (slice.value / total * 360.0).toFloat()
                            drawArc(colors[i % colors.size], start, sweep, true, Offset.Zero, Size(diameter, diameter))
                            drawArc(MaterialTheme.colorScheme.surface.copy(alpha = .7f), start, sweep, true, Offset.Zero, Size(diameter, diameter), style = Stroke(1.2f))
                            start += sweep
                        }
                    }
                }
                Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .88f)) {
                    Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(money(total), fontWeight = FontWeight.Black, maxLines = 1)
                        Text("total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (valid.isEmpty()) Text("No data found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                valid.forEachIndexed { i, slice -> Legend(colors[i % colors.size], slice.label, "${dec(slice.value / total * 100)}% • ${compact(slice.value)}") }
            }
        }
    }
}

@Composable
private fun PeakHoursCard(data: DashboardFullResponse) {
    val items = data.peakHours
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary.copy(alpha = .18f)
    val axis = MaterialTheme.colorScheme.outline.copy(alpha = .2f)
    val maxSales = max(1.0, items.maxOfOrNull { it.totalSales } ?: 0.0)
    AppCard {
        Text("Peak Hours", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        Text("Hourly revenue comparison", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Canvas(Modifier.fillMaxWidth().height(170.dp)) {
            val gap = 4f
            val barWidth = (size.width - gap * 25) / 24f
            val chartHeight = size.height - 20f
            drawLine(axis, Offset(0f, chartHeight), Offset(size.width, chartHeight), 1.2f)
            items.take(24).forEachIndexed { i, h ->
                val height = (h.totalSales / maxSales).toFloat() * chartHeight
                val x = gap + i * (barWidth + gap)
                drawRoundRect(if (h.orderCount > 0) primary else secondary, Offset(x, chartHeight - height), Size(barWidth, height.coerceAtLeast(2f)), CornerRadius(7f, 7f))
            }
        }
        val best = items.maxByOrNull { it.totalSales }
        Text("Busiest: ${best?.label ?: "N/A"} • ${money(best?.totalSales ?: 0.0)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RecentActivityCard(items: List<DashboardRecentActivityResponse>) {
    AppCard {
        Text("Recent Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        Text("Latest POS movement", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            if (items.isEmpty()) Text("No recent activity", color = MaterialTheme.colorScheme.onSurfaceVariant)
            items.take(10).forEach { a ->
                Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .48f)) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(shape = CircleShape, color = activityColor(a.severity).copy(alpha = .18f)) {
                                Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) { Text(activityIcon(a.type), color = activityColor(a.severity), fontWeight = FontWeight.Black) }
                            }
                            Column(Modifier.weight(1f)) {
                                Text(a.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(a.description, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(money(a.amount), fontWeight = FontWeight.Black)
                            Text(humanDateTime(a.createdDateTime), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TwoColumns(left: @Composable () -> Unit, right: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.weight(1f)) { left() }
        Box(Modifier.weight(1f)) { right() }
    }
}

@Composable
private fun AppCard(content: @Composable Column.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .10f)),
        elevation = CardDefaults.cardElevation(1.dp)
    ) { Column(Modifier.fillMaxWidth().padding(12.dp), content = content) }
}

@Composable
private fun Legend(color: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.size(9.dp).background(color, CircleShape))
        Text(label, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MessageCard(text: String) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer) {
        Text(text, Modifier.padding(10.dp), color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

@Composable
private fun chartColors() = listOf(
    MaterialTheme.colorScheme.primary,
    MaterialTheme.colorScheme.tertiary,
    MaterialTheme.colorScheme.secondary,
    MaterialTheme.colorScheme.error,
    MaterialTheme.colorScheme.inversePrimary,
    MaterialTheme.colorScheme.outline
)

@Composable
private fun activityColor(severity: String) = when (severity) {
    "SUCCESS" -> MaterialTheme.colorScheme.primary
    "DANGER" -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.tertiary
}

private data class Slice(val label: String, val value: Double)

private fun topFourPlusOthers(items: List<Slice>): List<Slice> {
    val sorted = items.filter { it.value > 0.0 }.sortedByDescending { it.value }
    val top = sorted.take(4)
    val rest = sorted.drop(4).sumOf { it.value }
    return if (rest > 0.0) top + Slice("Others", rest) else top
}

private fun activityIcon(type: String) = when (type) {
    "ORDER_PAID" -> "✓"
    "ORDER_CANCELED" -> "!"
    "BILL_PRINTED" -> "#"
    else -> "+"
}

private fun humanDateTime(value: String?): String {
    if (value.isNullOrBlank()) return ""
    return try {
        OffsetDateTime.parse(value).format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))
    } catch (_: Exception) {
        value.replace('T', ' ').substringBefore('.')
    }
}

private fun fmtDate(value: LocalDate) = value.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
private fun money(value: Double) = "৳" + String.format("%,.2f", value)
private fun compact(value: Double) = if (value >= 1000.0) money(value) else dec(value)
private fun dec(value: Double) = String.format("%,.2f", value)
