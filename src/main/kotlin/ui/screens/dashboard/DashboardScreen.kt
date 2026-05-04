package ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import data.model.DashboardFullResponse
import data.model.DashboardInsightResponse
import data.model.DashboardPeakHourResponse
import data.model.DashboardRecentActivityResponse
import data.model.DashboardTimelineResponse
import data.network.DashboardApiService
import java.time.LocalDate
import kotlinx.coroutines.launch
import kotlin.math.max

@Composable
fun DashboardScreen() {
    val api = remember { DashboardApiService() }
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }
    var dateFrom by remember { mutableStateOf(today.withDayOfMonth(1).toString()) }
    var dateTo by remember { mutableStateOf(today.withDayOfMonth(today.lengthOfMonth()).toString()) }
    var dashboard by remember { mutableStateOf<DashboardFullResponse?>(null) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            loading = true
            try {
                dashboard = api.getFull(dateFrom.ifBlank { null }, dateTo.ifBlank { null })
                message = null
            } catch (e: Exception) {
                message = "Failed to load dashboard: ${e.message}"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            DashboardTopBar(
                dateFrom = dateFrom,
                dateTo = dateTo,
                loading = loading,
                onRefresh = { load() }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                DashboardFilters(
                    dateFrom = dateFrom,
                    dateTo = dateTo,
                    onDateFrom = { dateFrom = it },
                    onDateTo = { dateTo = it },
                    onToday = {
                        dateFrom = today.toString()
                        dateTo = today.toString()
                        load()
                    },
                    onThisWeek = {
                        dateFrom = today.minusDays(6).toString()
                        dateTo = today.toString()
                        load()
                    },
                    onThisMonth = {
                        dateFrom = today.withDayOfMonth(1).toString()
                        dateTo = today.withDayOfMonth(today.lengthOfMonth()).toString()
                        load()
                    },
                    onApply = { load() }
                )
            }

            message?.let { item { MessagePanel(it) } }

            if (loading) {
                item {
                    Box(Modifier.fillMaxWidth().height(360.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                dashboard?.let { data ->
                    item { ExecutiveSummary(data) }
                    item { SalesTrendCard(data.salesTimeline, data.overview.totalSales, data.overview.averageOrderValue) }
                    item {
                        TwoColumnRow(
                            left = {
                                DonutChartCard(
                                    title = "Payment Mix",
                                    subtitle = "Share of paid sales by payment method",
                                    slices = data.paymentDistribution.items.map { ChartSlice(it.displayName, it.amount, it.percentage) }
                                )
                            },
                            right = {
                                DonutChartCard(
                                    title = "Order Status",
                                    subtitle = "Operational order distribution",
                                    slices = data.orderStatusDistribution.items.map { ChartSlice(it.status.replace('_', ' '), it.count.toDouble(), it.percentage) }
                                )
                            }
                        )
                    }
                    item {
                        TwoColumnRow(
                            left = {
                                HorizontalBarCard(
                                    title = "Top Food Items",
                                    subtitle = "Best sellers by net sales",
                                    items = data.topFoodItems.map { BarItem(it.foodName, it.netSales, "${decimal(it.quantitySold)} sold") }
                                )
                            },
                            right = {
                                HorizontalBarCard(
                                    title = "Top Beverages",
                                    subtitle = "Beverage contribution by net sales",
                                    items = data.topBeverages.map { BarItem(it.beverageName, it.netSales, "${decimal(it.quantity)} ${it.unit}") }
                                )
                            }
                        )
                    }
                    item {
                        TwoColumnRow(
                            left = {
                                HorizontalBarCard(
                                    title = "Waiter Performance",
                                    subtitle = "Sales handled by waiter",
                                    items = data.waiterPerformance.map { BarItem(it.waiterName ?: "Unknown", it.totalSales, "${it.paidOrderCount} paid orders") }
                                )
                            },
                            right = {
                                TablePerformanceCard(data)
                            }
                        )
                    }
                    item { PeakHourCard(data.peakHours) }
                    item {
                        TwoColumnRow(
                            left = { InsightCard(data.insights) },
                            right = { LowStockCard(data) }
                        )
                    }
                    item { RecentActivityCard(data.recentActivity) }
                }
            }
        }
    }
}

@Composable
private fun DashboardTopBar(
    dateFrom: String,
    dateTo: String,
    loading: Boolean,
    onRefresh: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 4.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(
                        Icons.Filled.Dashboard,
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp).size(28.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column {
                    Text("Restaurant Analytics", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text("$dateFrom → $dateTo", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            OutlinedButton(onClick = onRefresh, enabled = !loading) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (loading) "Refreshing" else "Refresh")
            }
        }
    }
}

@Composable
private fun DashboardFilters(
    dateFrom: String,
    dateTo: String,
    onDateFrom: (String) -> Unit,
    onDateTo: (String) -> Unit,
    onToday: () -> Unit,
    onThisWeek: () -> Unit,
    onThisMonth: () -> Unit,
    onApply: () -> Unit
) {
    DashboardCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(dateFrom, onDateFrom, label = { Text("Date From") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(dateTo, onDateTo, label = { Text("Date To") }, singleLine = true, modifier = Modifier.weight(1f))
            Button(onClick = onApply) { Text("Apply") }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onToday) { Text("Today") }
            OutlinedButton(onClick = onThisWeek) { Text("Last 7 Days") }
            OutlinedButton(onClick = onThisMonth) { Text("This Month") }
            Text("Use yyyy-MM-dd", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ExecutiveSummary(data: DashboardFullResponse) {
    val overview = data.overview
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("Total Sales", taka(overview.totalSales), "Paid order revenue", Modifier.weight(1f))
            MetricCard("Paid Orders", overview.totalPaidOrders.toString(), "Completed payments", Modifier.weight(1f))
            MetricCard("Average Order", taka(overview.averageOrderValue), "Revenue / paid order", Modifier.weight(1f))
            MetricCard("Cancellation", "${decimal(overview.cancellationRate)}%", "Canceled vs finished", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("Active Orders", overview.activeOrders.toString(), "Placed or bill printed", Modifier.weight(1f))
            MetricCard("Occupied Tables", overview.occupiedTables.toString(), "${overview.freeTables} free", Modifier.weight(1f))
            MetricCard("Menu Items", (overview.totalFoodItems + overview.totalBeverages).toString(), "Food + beverage", Modifier.weight(1f))
            MetricCard("Low Stock", overview.lowStockIngredientCount.toString(), "Inventory warnings", Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, caption: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SalesTrendCard(items: List<DashboardTimelineResponse>, totalSales: Double, averageOrder: Double) {
    val primary = MaterialTheme.colorScheme.primary
    val fill = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    val grid = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    val textMuted = MaterialTheme.colorScheme.onSurfaceVariant
    val maxSales = max(1.0, items.maxOfOrNull { it.salesAmount } ?: 0.0)
    DashboardCard {
        SectionHeader("Sales Trend", "Time-series revenue movement. Line chart is used because order sales change over time.")
        Spacer(Modifier.height(12.dp))
        Canvas(Modifier.fillMaxWidth().height(260.dp)) {
            val leftPadding = 4f
            val bottomPadding = 20f
            val chartHeight = size.height - bottomPadding
            repeat(4) { index ->
                val y = chartHeight * index / 3f
                drawLine(grid, Offset(leftPadding, y), Offset(size.width, y), strokeWidth = 1.4f)
            }
            if (items.size > 1) {
                val step = size.width / (items.size - 1)
                val points = items.mapIndexed { index, item ->
                    Offset(index * step, chartHeight - ((item.salesAmount / maxSales).toFloat() * chartHeight))
                }
                val area = Path().apply {
                    moveTo(points.first().x, chartHeight)
                    points.forEach { lineTo(it.x, it.y) }
                    lineTo(points.last().x, chartHeight)
                    close()
                }
                drawPath(area, fill)
                points.zipWithNext().forEach { (a, b) -> drawLine(primary, a, b, strokeWidth = 4f, cap = StrokeCap.Round) }
                points.forEach { drawCircle(primary, radius = 5f, center = it) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total: ${taka(totalSales)}", fontWeight = FontWeight.Bold)
            Text("AOV: ${taka(averageOrder)}", color = textMuted)
            Text("Points: ${items.size}", color = textMuted)
        }
    }
}

@Composable
private fun DonutChartCard(title: String, subtitle: String, slices: List<ChartSlice>) {
    val colors = chartColors()
    val validSlices = slices.filter { it.value > 0.0 }
    val total = validSlices.sumOf { it.value }.coerceAtLeast(1.0)
    DashboardCard {
        SectionHeader(title, subtitle)
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = size.minDimension * 0.16f
                    val diameter = size.minDimension - stroke
                    val topLeft = Offset(stroke / 2f, stroke / 2f)
                    var startAngle = -90f
                    if (validSlices.isEmpty()) {
                        drawArc(colors.first().copy(alpha = 0.25f), startAngle, 360f, false, topLeft, Size(diameter, diameter), style = Stroke(stroke, cap = StrokeCap.Round))
                    } else {
                        validSlices.forEachIndexed { index, slice ->
                            val sweep = (slice.value / total * 360.0).toFloat()
                            drawArc(colors[index % colors.size], startAngle, sweep, false, topLeft, Size(diameter, diameter), style = Stroke(stroke, cap = StrokeCap.Butt))
                            startAngle += sweep
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(taka(total), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                if (validSlices.isEmpty()) {
                    Text("No distribution data found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    validSlices.take(6).forEachIndexed { index, slice ->
                        LegendRow(color = colors[index % colors.size], label = slice.label, value = "${decimal(slice.percentage)}% • ${compact(slice.value)}")
                    }
                }
            }
        }
    }
}

@Composable
private fun HorizontalBarCard(title: String, subtitle: String, items: List<BarItem>) {
    val primary = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.surfaceVariant
    val maxValue = max(1.0, items.maxOfOrNull { it.value } ?: 0.0)
    DashboardCard {
        SectionHeader(title, subtitle)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
            if (items.isEmpty()) {
                Text("No data found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items.take(8).forEach { item ->
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Text(item.label.ifBlank { "Unknown" }, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(item.caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(taka(item.value), fontWeight = FontWeight.Bold)
                    }
                    Canvas(Modifier.fillMaxWidth().height(10.dp)) {
                        drawRoundRect(background, cornerRadius = CornerRadius(12f, 12f))
                        drawRoundRect(primary, size = Size(size.width * (item.value / maxValue).toFloat(), size.height), cornerRadius = CornerRadius(12f, 12f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PeakHourCard(items: List<DashboardPeakHourResponse>) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val axis = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
    val maxSales = max(1.0, items.maxOfOrNull { it.totalSales } ?: 0.0)
    DashboardCard {
        SectionHeader("Peak Hours", "Column chart is used to compare hourly revenue and identify rush periods.")
        Spacer(Modifier.height(12.dp))
        Canvas(Modifier.fillMaxWidth().height(260.dp)) {
            val gap = 5f
            val barWidth = (size.width - gap * 25) / 24f
            val chartHeight = size.height - 28f
            drawLine(axis, Offset(0f, chartHeight), Offset(size.width, chartHeight), strokeWidth = 1.4f)
            items.take(24).forEachIndexed { index, hour ->
                val height = (hour.totalSales / maxSales).toFloat() * chartHeight
                val x = gap + index * (barWidth + gap)
                val y = chartHeight - height
                val color = if (hour.orderCount > 0) primary else secondary.copy(alpha = 0.25f)
                drawRoundRect(color, topLeft = Offset(x, y), size = Size(barWidth, height.coerceAtLeast(2f)), cornerRadius = CornerRadius(8f, 8f))
            }
        }
        val best = items.maxByOrNull { it.totalSales }
        Text("Busiest: ${best?.label ?: "N/A"} • ${taka(best?.totalSales ?: 0.0)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TablePerformanceCard(data: DashboardFullResponse) {
    HorizontalBarCard(
        title = "Table Performance",
        subtitle = "Revenue by table. Useful for floor/table utilization.",
        items = data.tablePerformance.map {
            BarItem(
                label = "Table ${it.tableNumber ?: "-"}",
                value = it.totalSales,
                caption = "Floor ${it.floor ?: "-"} • ${it.paidOrderCount} paid orders"
            )
        }
    )
}

@Composable
private fun InsightCard(items: List<DashboardInsightResponse>) {
    DashboardCard {
        SectionHeader("Insights", "Actionable warnings generated from the selected range.")
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (items.isEmpty()) {
                Text("No critical insight for this range", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items.forEach { insight ->
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(insight.title, fontWeight = FontWeight.Bold)
                        Text(insight.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        insight.recommendedAction?.let { Text("Action: $it", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
        }
    }
}

@Composable
private fun LowStockCard(data: DashboardFullResponse) {
    DashboardCard {
        SectionHeader("Low Stock", "Inventory items that need purchasing attention.")
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            if (data.lowStockSummary.items.isEmpty()) {
                Text("No low-stock ingredients", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            data.lowStockSummary.items.take(8).forEach { item ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.ingredientName ?: "Ingredient", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${decimal(item.currentAmount)} ${item.unit ?: ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    SeverityPill(item.severity)
                }
            }
        }
    }
}

@Composable
private fun RecentActivityCard(items: List<DashboardRecentActivityResponse>) {
    DashboardCard {
        SectionHeader("Recent Activity", "Latest operational events from POS orders.")
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (items.isEmpty()) {
                Text("No recent activity", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items.take(10).forEach { activity ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(activity.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(activity.description, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(taka(activity.amount), fontWeight = FontWeight.Bold)
                        Text(activity.createdDateTime ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun TwoColumnRow(left: @Composable () -> Unit, right: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.weight(1f)) { left() }
        Box(Modifier.weight(1f)) { right() }
    }
}

@Composable
private fun DashboardCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), content = content)
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LegendRow(color: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Text(label, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SeverityPill(severity: String) {
    val background = if (severity == "DANGER") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer
    val foreground = if (severity == "DANGER") MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer
    Surface(shape = RoundedCornerShape(999.dp), color = background) {
        Text(severity, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = foreground, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MessagePanel(text: String) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.errorContainer) {
        Text(text, modifier = Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

@Composable
private fun chartColors(): List<Color> = listOf(
    MaterialTheme.colorScheme.primary,
    MaterialTheme.colorScheme.tertiary,
    MaterialTheme.colorScheme.secondary,
    MaterialTheme.colorScheme.error,
    MaterialTheme.colorScheme.inversePrimary,
    MaterialTheme.colorScheme.outline
)

private data class ChartSlice(val label: String, val value: Double, val percentage: Double)
private data class BarItem(val label: String, val value: Double, val caption: String)

private fun taka(value: Double): String = "৳" + String.format("%,.2f", value)
private fun compact(value: Double): String = if (value >= 1000.0) taka(value) else decimal(value)
private fun decimal(value: Double): String = String.format("%,.2f", value)
