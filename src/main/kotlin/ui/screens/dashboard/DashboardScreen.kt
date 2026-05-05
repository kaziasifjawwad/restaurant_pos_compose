package ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import data.model.DashboardFullResponse
import data.network.DashboardApiService
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

@Composable
fun DashboardScreen() {
    val api = remember { DashboardApiService() }
    val scope = rememberCoroutineScope()
    var dateFrom by remember { mutableStateOf(LocalDate.now()) }
    var dateTo by remember { mutableStateOf(LocalDate.now()) }
    var appliedFrom by remember { mutableStateOf(dateFrom) }
    var appliedTo by remember { mutableStateOf(dateTo) }
    var dashboard by remember { mutableStateOf<DashboardFullResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            isLoading = true
            error = null
            try {
                dashboard = api.getFull(formatDate(appliedFrom), formatDate(appliedTo))
            } catch (throwable: Throwable) {
                error = throwable.message ?: "Unable to load dashboard"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(appliedFrom, appliedTo) {
        load()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            DashboardHeader(
                dateFrom = appliedFrom,
                dateTo = appliedTo,
                isLoading = isLoading,
                onRefresh = { load() }
            )
        }
        item {
            DateFilter(
                dateFrom = dateFrom,
                dateTo = dateTo,
                onDateFromChange = { dateFrom = it },
                onDateToChange = { dateTo = it },
                onToday = {
                    val today = LocalDate.now()
                    dateFrom = today
                    dateTo = today
                    appliedFrom = today
                    appliedTo = today
                },
                onLastSevenDays = {
                    val today = LocalDate.now()
                    val from = today.minusDays(6)
                    dateFrom = from
                    dateTo = today
                    appliedFrom = from
                    appliedTo = today
                },
                onThisMonth = {
                    val today = LocalDate.now()
                    val from = today.withDayOfMonth(1)
                    dateFrom = from
                    dateTo = today
                    appliedFrom = from
                    appliedTo = today
                },
                onApply = {
                    val normalizedFrom = if (dateFrom <= dateTo) dateFrom else dateTo
                    val normalizedTo = if (dateFrom <= dateTo) dateTo else dateFrom
                    dateFrom = normalizedFrom
                    dateTo = normalizedTo
                    appliedFrom = normalizedFrom
                    appliedTo = normalizedTo
                }
            )
        }
        if (error != null) {
            item { ErrorCard(message = error.orEmpty(), onRetry = { load() }) }
        }
        if (isLoading && dashboard == null) {
            item { LoadingCard() }
        }
        dashboard?.let { data ->
            item { KpiSection(data) }
            item {
                ThreeColumnRow(
                    first = {
                        DonutChartCard(
                            title = "Payment Mix",
                            slices = data.paymentDistribution.items.map {
                                ChartSlice(
                                    label = it.displayName.ifBlank { it.paymentMethod.ifBlank { "Unknown" } },
                                    value = it.amount,
                                    secondary = "${it.orderCount} orders"
                                )
                            },
                            valueFormatter = ::formatMoney
                        )
                    },
                    second = {
                        DonutChartCard(
                            title = "Order Status",
                            slices = data.orderStatusDistribution.items.map {
                                ChartSlice(
                                    label = it.status.ifBlank { "Unknown" },
                                    value = it.count.toDouble()
                                )
                            },
                            valueFormatter = { formatDecimal(it) }
                        )
                    },
                    third = {
                        DonutChartCard(
                            title = "Top Food Items",
                            slices = data.topFoodItems.map {
                                ChartSlice(
                                    label = it.foodName.ifBlank { "Food item" },
                                    value = it.netSales,
                                    secondary = "${formatDecimal(it.quantitySold)} sold, ${it.orderCount} orders"
                                )
                            },
                            valueFormatter = ::formatMoney
                        )
                    }
                )
            }
            item {
                ThreeColumnRow(
                    first = {
                        DonutChartCard(
                            title = "Top Beverages",
                            slices = data.topBeverages.map {
                                ChartSlice(
                                    label = it.beverageName.ifBlank { "Beverage" },
                                    value = it.netSales,
                                    secondary = "${formatDecimal(it.quantity)} ${it.unit}, ${it.orderCount} orders"
                                )
                            },
                            valueFormatter = ::formatMoney
                        )
                    },
                    second = {
                        DonutChartCard(
                            title = "Waiter Performance",
                            slices = data.waiterPerformance.map {
                                ChartSlice(
                                    label = it.waiterName?.takeIf { name -> name.isNotBlank() } ?: "Unknown",
                                    value = it.totalSales,
                                    secondary = "${it.paidOrderCount} paid, AOV ${formatMoney(it.averageOrderValue)}"
                                )
                            },
                            valueFormatter = ::formatMoney
                        )
                    },
                    third = {
                        DonutChartCard(
                            title = "Table Performance",
                            slices = data.tablePerformance.map {
                                ChartSlice(
                                    label = "Table ${it.tableNumber ?: "-"}",
                                    value = it.totalSales,
                                    secondary = "Floor ${it.floor ?: "-"}, cap ${it.capacity ?: "-"}, ${it.paidOrderCount} paid"
                                )
                            },
                            valueFormatter = ::formatMoney
                        )
                    }
                )
            }
            item { PeakHoursPanel(data) }
            item {
                TwoColumnRow(
                    left = { InsightsCard(data) },
                    right = { LowStockCard(data) }
                )
            }
            item { RecentActivityCard(data) }
        }
    }
}

@Composable
private fun DashboardHeader(
    dateFrom: LocalDate,
    dateTo: LocalDate,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Restaurant Analytics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${displayDate(dateFrom)} - ${displayDate(dateTo)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(
                onClick = onRefresh,
                enabled = !isLoading,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh",
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text("Refresh")
            }
        }
    }
}

@Composable
private fun DateFilter(
    dateFrom: LocalDate,
    dateTo: LocalDate,
    onDateFromChange: (LocalDate) -> Unit,
    onDateToChange: (LocalDate) -> Unit,
    onToday: () -> Unit,
    onLastSevenDays: () -> Unit,
    onThisMonth: () -> Unit,
    onApply: () -> Unit
) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CalendarDateButton(
                label = "From",
                selected = dateFrom,
                onSelect = onDateFromChange,
                modifier = Modifier.weight(1f)
            )
            CalendarDateButton(
                label = "To",
                selected = dateTo,
                onSelect = onDateToChange,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(onClick = onToday, contentPadding = PaddingValues(horizontal = 10.dp)) {
                Text("Today")
            }
            OutlinedButton(onClick = onLastSevenDays, contentPadding = PaddingValues(horizontal = 10.dp)) {
                Text("Last 7 Days")
            }
            OutlinedButton(onClick = onThisMonth, contentPadding = PaddingValues(horizontal = 10.dp)) {
                Text("This Month")
            }
            Button(
                onClick = onApply,
                contentPadding = PaddingValues(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Apply", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CalendarDateButton(
    label: String,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var month by remember(selected) { mutableStateOf(YearMonth.from(selected)) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().height(40.dp),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = "$label: ${displayDate(selected)}",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            CalendarPicker(
                month = month,
                selected = selected,
                onPreviousMonth = { month = month.minusMonths(1) },
                onNextMonth = { month = month.plusMonths(1) },
                onSelect = {
                    onSelect(it)
                    expanded = false
                }
            )
        }
    }
}

@Composable
private fun CalendarPicker(
    month: YearMonth,
    selected: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelect: (LocalDate) -> Unit
) {
    Column(
        modifier = Modifier.width(286.dp).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousMonth, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month")
            }
            Text(
                text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onNextMonth, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Next month")
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach {
                Text(
                    text = it,
                    modifier = Modifier.width(34.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        val first = month.atDay(1)
        val daysInMonth = month.lengthOfMonth()
        var day = 1 - (first.dayOfWeek.value % 7)
        repeat(6) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                repeat(7) {
                    if (day in 1..daysInMonth) {
                        val date = month.atDay(day)
                        val isSelected = date == selected
                        Surface(
                            modifier = Modifier.size(34.dp).clickable { onSelect(date) },
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = day.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.size(34.dp))
                    }
                    day++
                }
            }
        }
    }
}

@Composable
private fun KpiSection(data: DashboardFullResponse) {
    val overview = data.overview
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            KpiCard("Total Sales", formatMoney(overview.totalSales), "Today ${formatMoney(overview.todaySales)}", Modifier.weight(1f))
            KpiCard("Paid Orders", overview.totalPaidOrders.toString(), "${overview.totalCanceledOrders} canceled", Modifier.weight(1f))
            KpiCard("Average Order", formatMoney(overview.averageOrderValue), "Per paid order", Modifier.weight(1f))
            KpiCard("Cancellation", formatPercent(overview.cancellationRate), "Rate", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            KpiCard("Active Orders", overview.activeOrders.toString(), "Open now", Modifier.weight(1f))
            KpiCard("Tables", "${overview.occupiedTables}/${overview.freeTables}", "Occupied / free", Modifier.weight(1f))
            KpiCard("Menu Items", (overview.totalFoodItems + overview.totalBeverages).toString(), "${overview.totalFoodItems} food, ${overview.totalBeverages} beverages", Modifier.weight(1f))
            KpiCard("Low Stock", overview.lowStockIngredientCount.toString(), "Ingredients", Modifier.weight(1f))
        }
    }
}

@Composable
private fun KpiCard(title: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(86.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.10f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TwoColumnRow(
    left: @Composable () -> Unit,
    right: @Composable () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(modifier = Modifier.weight(1f)) { left() }
        Box(modifier = Modifier.weight(1f)) { right() }
    }
}

@Composable
private fun ThreeColumnRow(
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
    third: @Composable () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(modifier = Modifier.weight(1f)) { first() }
        Box(modifier = Modifier.weight(1f)) { second() }
        Box(modifier = Modifier.weight(1f)) { third() }
    }
}

@Composable
private fun DonutChartCard(
    title: String,
    slices: List<ChartSlice>,
    valueFormatter: (Double) -> String
) {
    val compactSlices = topFourPlusOthers(slices)
    val total = compactSlices.sumOf { it.value }
    val colors = chartColors()

    AppCard(modifier = Modifier.height(286.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(6.dp))
        if (total <= 0.0 || compactSlices.isEmpty()) {
            EmptyState("No data for this period")
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CompactDonut(
                    slices = compactSlices,
                    total = total,
                    colors = colors,
                    totalLabel = valueFormatter(total)
                )
                Spacer(Modifier.height(8.dp))
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    compactSlices.forEachIndexed { index, slice ->
                        CompactLegendRow(
                            color = colors[index % colors.size],
                            slice = slice,
                            percentage = slice.value / total * 100.0,
                            valueFormatter = valueFormatter
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactDonut(
    slices: List<ChartSlice>,
    total: Double,
    colors: List<Color>,
    totalLabel: String
) {
    Box(modifier = Modifier.size(118.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize().padding(7.dp)) {
            val strokeWidth = size.minDimension * 0.17f
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            var start = -90f
            slices.forEachIndexed { index, slice ->
                val sweep = ((slice.value / total) * 360.0).toFloat()
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = totalLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Total",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CompactLegendRow(
    color: Color,
    slice: ChartSlice,
    percentage: Double,
    valueFormatter: (Double) -> String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.padding(top = 3.dp).size(8.dp).clip(CircleShape).background(color)
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = slice.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatPercent(percentage),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Text(
                text = listOf(valueFormatter(slice.value), slice.secondary).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PeakHoursPanel(data: DashboardFullResponse) {
    val byHour = data.peakHours.associateBy { it.hourOfDay.coerceIn(0, 23) }
    val values = (0..23).map { hour -> byHour[hour]?.totalSales ?: 0.0 }
    val maxValue = max(values.maxOrNull() ?: 0.0, 1.0)
    val busiest = data.peakHours.maxByOrNull { it.totalSales }
    val barColor = MaterialTheme.colorScheme.primary
    val axis = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(text = "Peak Hours", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = if (busiest != null) "Busiest: ${busiest.label} (${formatMoney(busiest.totalSales)})" else "No hourly data",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        if (data.peakHours.isEmpty() || values.all { it <= 0.0 }) {
            EmptyState("No peak hour sales for this period")
        } else {
            Canvas(modifier = Modifier.fillMaxWidth().height(176.dp)) {
                val chartHeight = size.height - 22f
                val slot = size.width / 24f
                val barWidth = slot * 0.58f
                drawLine(
                    color = axis,
                    start = Offset(0f, chartHeight),
                    end = Offset(size.width, chartHeight),
                    strokeWidth = 1.2f
                )
                values.forEachIndexed { index, value ->
                    val height = ((value / maxValue) * (chartHeight - 8f)).toFloat()
                    val left = index * slot + (slot - barWidth) / 2f
                    drawRect(
                        color = barColor,
                        topLeft = Offset(left, chartHeight - height),
                        size = Size(barWidth, height)
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("00", "04", "08", "12", "16", "20", "23").forEach {
                    Text(text = it, style = MaterialTheme.typography.labelSmall, color = labelColor)
                }
            }
        }
    }
}

@Composable
private fun InsightsCard(data: DashboardFullResponse) {
    AppCard {
        Text(text = "Insights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (data.insights.isEmpty()) {
            EmptyState("No insights for this period")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                data.insights.take(5).forEach { insight ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SeverityDot(insight.severity)
                                Text(
                                    text = insight.title.ifBlank { "Insight" },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = insight.message,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            insight.recommendedAction?.takeIf { it.isNotBlank() }?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LowStockCard(data: DashboardFullResponse) {
    AppCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Low Stock", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = data.lowStockSummary.lowStockCount.toString(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        }
        Spacer(Modifier.height(8.dp))
        if (data.lowStockSummary.items.isEmpty()) {
            EmptyState("No low stock ingredients")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                data.lowStockSummary.items.take(7).forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.ingredientName ?: "Ingredient",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${formatDecimal(item.currentAmount)} ${item.unit.orEmpty()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        SeverityPill(item.severity)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentActivityCard(data: DashboardFullResponse) {
    AppCard {
        Text(text = "Recent Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (data.recentActivity.isEmpty()) {
            EmptyState("No recent activity")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                data.recentActivity.take(12).forEach { activity ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(34.dp).clip(CircleShape).background(severityColor(activity.severity).copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = activityIcon(activity.type),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = severityColor(activity.severity)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activity.title.ifBlank { "Activity" },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = activity.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                if (activity.amount != 0.0) {
                                    Text(
                                        text = formatMoney(activity.amount),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = humanDateTime(activity.createdDateTime),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingCard() {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.4.dp)
            Spacer(Modifier.width(10.dp))
            Text("Loading dashboard...")
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Dashboard unavailable", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth().height(84.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SeverityDot(severity: String) {
    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(severityColor(severity)))
}

@Composable
private fun SeverityPill(severity: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = severityColor(severity).copy(alpha = 0.14f)
    ) {
        Text(
            text = severity.ifBlank { "INFO" },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = severityColor(severity)
        )
    }
}

@Composable
private fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.10f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), content = content)
    }
}

private data class ChartSlice(
    val label: String,
    val value: Double,
    val secondary: String = ""
)

private fun topFourPlusOthers(items: List<ChartSlice>): List<ChartSlice> {
    val sorted = items.filter { it.value > 0.0 }.sortedByDescending { it.value }
    val top = sorted.take(4)
    val restValue = sorted.drop(4).sumOf { it.value }
    return if (restValue > 0.0) {
        top + ChartSlice("Others", restValue, "${sorted.size - 4} more")
    } else {
        top
    }
}

private fun formatMoney(value: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 0
    }
    return "Tk ${formatter.format(value)}"
}

private fun formatDecimal(value: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = if (value % 1.0 == 0.0) 0 else 2
    }
    return formatter.format(value)
}

private fun formatPercent(value: Double): String {
    val normalized = if (value in 0.0..1.0) value * 100.0 else value
    return "${formatDecimal(normalized)}%"
}

private fun formatDate(date: LocalDate): String = date.format(DateTimeFormatter.ISO_LOCAL_DATE)

private fun displayDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))

private fun humanDateTime(value: String?): String {
    if (value.isNullOrBlank()) return "-"
    val output = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
    return try {
        OffsetDateTime.parse(value).format(output)
    } catch (_: Exception) {
        try {
            Instant.parse(value).atZone(ZoneOffset.UTC).format(output)
        } catch (_: Exception) {
            try {
                LocalDateTime.parse(value.substringBefore("+").substringBefore("Z")).format(output)
            } catch (_: Exception) {
                value.replace('T', ' ').substringBefore(".")
            }
        }
    }
}

private fun activityIcon(type: String): String {
    val normalized = type.uppercase()
    return when {
        "PAY" in normalized || "SALE" in normalized -> "$"
        "CANCEL" in normalized || "VOID" in normalized -> "!"
        "STOCK" in normalized || "INVENTORY" in normalized -> "#"
        "ORDER" in normalized -> "O"
        else -> "i"
    }
}

@Composable
private fun severityColor(severity: String): Color {
    val normalized = severity.uppercase()
    return when {
        "CRITICAL" in normalized || "ERROR" in normalized || "HIGH" in normalized -> MaterialTheme.colorScheme.error
        "WARN" in normalized || "MEDIUM" in normalized -> Color(0xFFC88719)
        "SUCCESS" in normalized || "LOW" in normalized -> Color(0xFF2E7D32)
        else -> MaterialTheme.colorScheme.primary
    }
}

@Composable
private fun chartColors(): List<Color> {
    val scheme = MaterialTheme.colorScheme
    return listOf(
        scheme.primary,
        scheme.tertiary,
        Color(0xFF1976D2),
        Color(0xFFC88719),
        scheme.error
    )
}
