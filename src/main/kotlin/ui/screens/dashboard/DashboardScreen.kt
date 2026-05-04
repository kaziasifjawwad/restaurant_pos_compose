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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.awaitPointerEventScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import data.model.DashboardFullResponse
import data.model.DashboardInsightResponse
import data.model.DashboardPeakHourResponse
import data.model.DashboardRecentActivityResponse
import data.network.DashboardApiService
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.sqrt

@Composable
fun DashboardScreen() {
    val api = remember { DashboardApiService() }
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }
    var dateFrom by remember { mutableStateOf(today.withDayOfMonth(1)) }
    var dateTo by remember { mutableStateOf(today.withDayOfMonth(today.lengthOfMonth())) }
    var dashboard by remember { mutableStateOf<DashboardFullResponse?>(null) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            loading = true
            try {
                dashboard = api.getFull(dateFrom.toString(), dateTo.toString())
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
            modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                DashboardFilters(
                    dateFrom = dateFrom,
                    dateTo = dateTo,
                    onDateFrom = { dateFrom = it },
                    onDateTo = { dateTo = it },
                    onToday = {
                        dateFrom = today
                        dateTo = today
                        load()
                    },
                    onThisWeek = {
                        dateFrom = today.minusDays(6)
                        dateTo = today
                        load()
                    },
                    onThisMonth = {
                        dateFrom = today.withDayOfMonth(1)
                        dateTo = today.withDayOfMonth(today.lengthOfMonth())
                        load()
                    },
                    onApply = { load() }
                )
            }

            message?.let { item { MessagePanel(it) } }

            if (loading) {
                item {
                    Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                dashboard?.let { data ->
                    item { ExecutiveSummary(data) }
                    item {
                        TwoColumnRow(
                            left = {
                                PieChartCard(
                                    title = "Payment Mix",
                                    subtitle = "Top 4 payment methods + others",
                                    slices = topFourPlusOthers(data.paymentDistribution.items.map { ChartSlice(it.displayName, it.amount) })
                                )
                            },
                            right = {
                                PieChartCard(
                                    title = "Order Status",
                                    subtitle = "Top 4 order states + others",
                                    slices = topFourPlusOthers(data.orderStatusDistribution.items.map { ChartSlice(it.status.replace('_', ' '), it.count.toDouble()) })
                                )
                            }
                        )
                    }
                    item {
                        TwoColumnRow(
                            left = {
                                PieChartCard(
                                    title = "Top Food Items",
                                    subtitle = "Top 4 food items by net sales + others",
                                    slices = topFourPlusOthers(data.topFoodItems.map { ChartSlice(it.foodName, it.netSales) })
                                )
                            },
                            right = {
                                PieChartCard(
                                    title = "Top Beverages",
                                    subtitle = "Top 4 beverages by net sales + others",
                                    slices = topFourPlusOthers(data.topBeverages.map { ChartSlice(it.beverageName, it.netSales) })
                                )
                            }
                        )
                    }
                    item {
                        TwoColumnRow(
                            left = {
                                PieChartCard(
                                    title = "Waiter Performance",
                                    subtitle = "Top 4 waiters by sales + others",
                                    slices = topFourPlusOthers(data.waiterPerformance.map { ChartSlice(it.waiterName ?: "Unknown", it.totalSales) })
                                )
                            },
                            right = {
                                PieChartCard(
                                    title = "Table Performance",
                                    subtitle = "Top 4 tables by sales + others",
                                    slices = topFourPlusOthers(data.tablePerformance.map { ChartSlice("Table ${it.tableNumber ?: "-"}", it.totalSales) })
                                )
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
    dateFrom: LocalDate,
    dateTo: LocalDate,
    loading: Boolean,
    onRefresh: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 3.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(
                        Icons.Filled.Dashboard,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp).size(24.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column {
                    Text("Restaurant Analytics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("${formatDate(dateFrom)} → ${formatDate(dateTo)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            OutlinedButton(onClick = onRefresh, enabled = !loading) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (loading) "Refreshing" else "Refresh")
            }
        }
    }
}

@Composable
private fun DashboardFilters(
    dateFrom: LocalDate,
    dateTo: LocalDate,
    onDateFrom: (LocalDate) -> Unit,
    onDateTo: (LocalDate) -> Unit,
    onToday: () -> Unit,
    onThisWeek: () -> Unit,
    onThisMonth: () -> Unit,
    onApply: () -> Unit
) {
    DashboardCard {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CalendarDateButton("Date From", dateFrom, onDateFrom, Modifier.weight(1f))
            CalendarDateButton("Date To", dateTo, onDateTo, Modifier.weight(1f))
            Button(onClick = onApply) { Text("Apply") }
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onToday) { Text("Today") }
            OutlinedButton(onClick = onThisWeek) { Text("Last 7 Days") }
            OutlinedButton(onClick = onThisMonth) { Text("This Month") }
            Text("Dates are selected from calendar view", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CalendarDateButton(
    label: String,
    selectedDate: LocalDate,
    onSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var visibleMonth by remember(selectedDate) { mutableStateOf(YearMonth.from(selectedDate)) }

    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatDate(selectedDate), fontWeight = FontWeight.Bold)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            CalendarView(
                month = visibleMonth,
                selectedDate = selectedDate,
                onPreviousMonth = { visibleMonth = visibleMonth.minusMonths(1) },
                onNextMonth = { visibleMonth = visibleMonth.plusMonths(1) },
                onDateSelected = {
                    onSelected(it)
                    expanded = false
                }
            )
        }
    }
}

@Composable
private fun CalendarView(
    month: YearMonth,
    selectedDate: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    Column(Modifier.width(310.dp).padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onPreviousMonth) { Text("‹") }
            Text(
                "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}",
                fontWeight = FontWeight.Black
            )
            TextButton(onClick = onNextMonth) { Text("›") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach {
                Text(it, modifier = Modifier.width(38.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val firstDay = month.atDay(1)
        val daysInMonth = month.lengthOfMonth()
        val leadingBlank = firstDay.dayOfWeek.value - 1
        val cells = List(leadingBlank) { null } + (1..daysInMonth).map { month.atDay(it) }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                week.forEach { date ->
                    if (date == null) {
                        Spacer(Modifier.size(38.dp))
                    } else {
                        val selected = date == selectedDate
                        Surface(
                            modifier = Modifier.size(38.dp),
                            shape = CircleShape,
                            color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            onClick = { onDateSelected(date) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    date.dayOfMonth.toString(),
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.size(38.dp)) }
            }
        }
    }
}

@Composable
private fun ExecutiveSummary(data: DashboardFullResponse) {
    val overview = data.overview
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("Total Sales", taka(overview.totalSales), "Paid revenue", Modifier.weight(1f))
            MetricCard("Paid Orders", overview.totalPaidOrders.toString(), "Completed", Modifier.weight(1f))
            MetricCard("Average Order", taka(overview.averageOrderValue), "AOV", Modifier.weight(1f))
            MetricCard("Cancellation", "${decimal(overview.cancellationRate)}%", "Canceled", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("Active Orders", overview.activeOrders.toString(), "Open", Modifier.weight(1f))
            MetricCard("Occupied Tables", overview.occupiedTables.toString(), "${overview.freeTables} free", Modifier.weight(1f))
            MetricCard("Menu Items", (overview.totalFoodItems + overview.totalBeverages).toString(), "Food + beverage", Modifier.weight(1f))
            MetricCard("Low Stock", overview.lowStockIngredientCount.toString(), "Warnings", Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, caption: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun PieChartCard(title: String, subtitle: String, slices: List<ChartSlice>) {
    val colors = chartColors()
    val validSlices = slices.filter { it.value > 0.0 }
    val total = validSlices.sumOf { it.value }.coerceAtLeast(1.0)
    var hoveredIndex by remember { mutableStateOf<Int?>(null) }
    var pieSizePx by remember { mutableStateOf(0f) }
    val hoveredSlice = hoveredIndex?.let { validSlices.getOrNull(it) }

    DashboardCard {
        SectionHeader(title, subtitle)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(156.dp), contentAlignment = Alignment.Center) {
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .onSizeChanged { pieSizePx = it.width.toFloat() }
                        .pointerInput(validSlices) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    hoveredIndex = if (event.type == PointerEventType.Exit) {
                                        null
                                    } else {
                                        event.changes.firstOrNull()?.position?.let { position ->
                                            detectPieSlice(position, validSlices.map { slice -> slice.value }, pieSizePx)
                                        }
                                    }
                                }
                            }
                        }
                ) {
                    val diameter = size.minDimension
                    var startAngle = -90f
                    if (validSlices.isEmpty()) {
                        drawArc(colors.first().copy(alpha = 0.20f), startAngle, 360f, true, Offset.Zero, Size(diameter, diameter))
                    } else {
                        validSlices.forEachIndexed { index, slice ->
                            val sweep = (slice.value / total * 360.0).toFloat()
                            val color = colors[index % colors.size]
                            drawArc(
                                color = if (hoveredIndex == null || hoveredIndex == index) color else color.copy(alpha = 0.45f),
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = true,
                                topLeft = Offset.Zero,
                                size = Size(diameter, diameter)
                            )
                            drawArc(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.70f),
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = true,
                                topLeft = Offset.Zero,
                                size = Size(diameter, diameter),
                                style = Stroke(width = 1.2f)
                            )
                            startAngle += sweep
                        }
                    }
                }
                if (hoveredSlice != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.92f)
                    ) {
                        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(hoveredSlice.label, color = MaterialTheme.colorScheme.inverseOnSurface, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text("${compact(hoveredSlice.value)} • ${decimal(hoveredSlice.value / total * 100)}%", color = MaterialTheme.colorScheme.inverseOnSurface)
                        }
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (validSlices.isEmpty()) {
                    Text("No data found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    validSlices.forEachIndexed { index, slice ->
                        LegendRow(
                            color = colors[index % colors.size],
                            label = slice.label,
                            value = "${decimal(slice.value / total * 100)}% • ${compact(slice.value)}"
                        )
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
    val axis = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)
    val maxSales = max(1.0, items.maxOfOrNull { it.totalSales } ?: 0.0)
    DashboardCard {
        SectionHeader("Peak Hours", "Hourly revenue comparison.")
        Spacer(Modifier.height(8.dp))
        Canvas(Modifier.fillMaxWidth().height(180.dp)) {
            val gap = 4f
            val barWidth = (size.width - gap * 25) / 24f
            val chartHeight = size.height - 22f
            drawLine(axis, Offset(0f, chartHeight), Offset(size.width, chartHeight), strokeWidth = 1.2f)
            items.take(24).forEachIndexed { index, hour ->
                val height = (hour.totalSales / maxSales).toFloat() * chartHeight
                val x = gap + index * (barWidth + gap)
                val y = chartHeight - height
                val color = if (hour.orderCount > 0) primary else secondary.copy(alpha = 0.18f)
                drawRoundRect(color, topLeft = Offset(x, y), size = Size(barWidth, height.coerceAtLeast(2f)), cornerRadius = CornerRadius(7f, 7f))
            }
        }
        val best = items.maxByOrNull { it.totalSales }
        Text("Busiest: ${best?.label ?: "N/A"} • ${taka(best?.totalSales ?: 0.0)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InsightCard(items: List<DashboardInsightResponse>) {
    DashboardCard {
        SectionHeader("Insights", "Actionable warnings")
        Spacer(Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (items.isEmpty()) {
                Text("No critical insight for this range", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items.forEach { insight ->
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.60f)) {
                    Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
        SectionHeader("Low Stock", "Inventory attention")
        Spacer(Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
        SectionHeader("Recent Activity", "Latest POS movement")
        Spacer(Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            if (items.isEmpty()) {
                Text("No recent activity", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items.take(10).forEach { activity ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(shape = CircleShape, color = activityColor(activity.severity).copy(alpha = 0.18f)) {
                                Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                                    Text(activityIcon(activity.type), color = activityColor(activity.severity), fontWeight = FontWeight.Black)
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Text(activity.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(activity.description, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(taka(activity.amount), fontWeight = FontWeight.Black)
                            Text(humanDateTime(activity.createdDateTime), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TwoColumnRow(left: @Composable () -> Unit, right: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.weight(1f)) { left() }
        Box(Modifier.weight(1f)) { right() }
    }
}

@Composable
private fun DashboardCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), content = content)
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LegendRow(color: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.size(9.dp).background(color, CircleShape))
        Text(label, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SeverityPill(severity: String) {
    val background = if (severity == "DANGER") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer
    val foreground = if (severity == "DANGER") MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer
    Surface(shape = RoundedCornerShape(999.dp), color = background) {
        Text(severity, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = foreground, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MessagePanel(text: String) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer) {
        Text(text, modifier = Modifier.padding(10.dp), color = MaterialTheme.colorScheme.onErrorContainer)
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

@Composable
private fun activityColor(severity: String): Color = when (severity) {
    "SUCCESS" -> MaterialTheme.colorScheme.primary
    "DANGER" -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.tertiary
}

private data class ChartSlice(val label: String, val value: Double)

private fun topFourPlusOthers(items: List<ChartSlice>): List<ChartSlice> {
    val sorted = items.filter { it.value > 0.0 }.sortedByDescending { it.value }
    val top = sorted.take(4)
    val others = sorted.drop(4).sumOf { it.value }
    return if (others > 0.0) top + ChartSlice("Others", others) else top
}

private fun detectPieSlice(position: Offset, values: List<Double>, canvasSize: Float): Int? {
    val total = values.sum().takeIf { it > 0.0 } ?: return null
    val radius = canvasSize / 2f
    if (radius <= 0f) return null
    val center = Offset(radius, radius)
    val dx = position.x - center.x
    val dy = position.y - center.y
    if (sqrt(dx * dx + dy * dy) > radius) return null
    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 90.0
    if (angle < 0) angle += 360.0
    var start = 0.0
    values.forEachIndexed { index, value ->
        val sweep = value / total * 360.0
        if (angle >= start && angle < start + sweep) return index
        start += sweep
    }
    return null
}

private fun activityIcon(type: String): String = when (type) {
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

private fun formatDate(value: LocalDate): String = value.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
private fun taka(value: Double): String = "৳" + String.format("%,.2f", value)
private fun compact(value: Double): String = if (value >= 1000.0) taka(value) else decimal(value)
private fun decimal(value: Double): String = String.format("%,.2f", value)
