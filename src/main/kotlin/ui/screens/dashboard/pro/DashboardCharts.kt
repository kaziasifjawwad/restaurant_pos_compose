package ui.screens.dashboard.pro

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import data.model.DashboardFullResponse
import kotlin.math.atan2
import kotlin.math.max

@Composable
fun DonutChartCard(
    title: String,
    entries: List<ChartEntry>,
    valueFormatter: (Double) -> String,
    onEntryClick: (ChartEntry) -> Unit = {}
) {
    val compactEntries = entries.filter { it.value.isFinite() && it.value > 0.0 }.sortedByDescending { it.value }.take(5)
    val total = compactEntries.sumOf { it.value }
    val colors = dashboardChartColors()
    var selected by remember(entries) { mutableStateOf<ChartEntry?>(null) }

    AppDashboardCard(modifier = Modifier.height(338.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (total <= 0.0 || compactEntries.isEmpty()) {
            EmptyDashboardState("No data for this period")
        } else {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Canvas(
                    modifier = Modifier.size(156.dp).clipToBounds().semantics {
                        contentDescription = "$title donut chart total ${valueFormatter(total)}"
                    }.pointerInput(compactEntries) {
                        detectTapGestures { offset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val dx = offset.x - center.x
                            val dy = offset.y - center.y
                            var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                            if (angle < 0) angle += 360f
                            var start = 0f
                            compactEntries.forEach { entry ->
                                val sweep = ((entry.value / total) * 360.0).toFloat()
                                if (angle in start..(start + sweep)) {
                                    selected = entry
                                    onEntryClick(entry)
                                    return@detectTapGestures
                                }
                                start += sweep
                            }
                        }
                    }
                ) {
                    val strokeWidth = size.minDimension * 0.18f
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                    var start = -90f
                    compactEntries.forEachIndexed { index, entry ->
                        val sweep = ((entry.value / total) * 360.0).toFloat()
                        if (sweep.isFinite() && sweep > 0f) {
                            drawArc(
                                color = colors[index % colors.size],
                                startAngle = start,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = Size(diameter, diameter),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                        start += sweep
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(valueFormatter(total), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            selected?.let {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)) {
                    Text("${it.label}: ${valueFormatter(it.value)}", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                compactEntries.forEachIndexed { index, entry ->
                    LegendRow(colors[index % colors.size], entry, entry.value / total * 100.0, valueFormatter, onEntryClick)
                }
            }
        }
    }
}

@Composable
fun HorizontalBarChartCard(
    title: String,
    entries: List<ChartEntry>,
    valueFormatter: (Double) -> String,
    onEntryClick: (ChartEntry) -> Unit = {}
) {
    val items = entries.filter { it.value.isFinite() && it.value > 0.0 }.sortedByDescending { it.value }.take(7)
    val maxValue = max(items.maxOfOrNull { it.value } ?: 0.0, 1.0)
    val barColor = dashboardChartColors().first()
    AppDashboardCard(modifier = Modifier.height(338.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (items.isEmpty()) {
            EmptyDashboardState("No data for this period")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items.forEach { item ->
                    Column(modifier = Modifier.fillMaxWidth().clickable { onEntryClick(item) }) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(item.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(valueFormatter(item.value), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(5.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(99.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))) {
                            Box(modifier = Modifier.fillMaxWidth((item.value / maxValue).toFloat().coerceIn(0.02f, 1f)).height(10.dp).clip(RoundedCornerShape(99.dp)).background(barColor))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StackedStatusBarCard(data: DashboardFullResponse) {
    val items = data.orderStatusDistribution.items.filter { it.count > 0 }
    val totalRaw = items.sumOf { it.count }.toDouble()
    AppDashboardCard(modifier = Modifier.height(338.dp)) {
        Text("Order Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (items.isEmpty() || totalRaw <= 0.0) {
            EmptyDashboardState("No order status data")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().height(28.dp).clip(RoundedCornerShape(99.dp))) {
                    items.forEachIndexed { index, item ->
                        Box(
                            modifier = Modifier
                                .weight((item.count.toDouble() / totalRaw).toFloat().coerceAtLeast(0.02f))
                                .background(statusColor(item.status, index))
                        )
                    }
                }
                items.forEachIndexed { index, item ->
                    val percentage = item.count / totalRaw * 100.0
                    StatusBarRow(
                        label = item.status.ifBlank { "Unknown" },
                        count = item.count,
                        percentage = percentage,
                        color = statusColor(item.status, index)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBarRow(label: String, count: Long, percentage: Double, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$count orders", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(DashboardFormatters.percent(percentage), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
        Box(modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(99.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f))) {
            Box(modifier = Modifier.fillMaxWidth((percentage / 100.0).toFloat().coerceIn(0.02f, 1f)).height(12.dp).clip(RoundedCornerShape(99.dp)).background(color))
        }
    }
}

@Composable
fun PeakHoursBarCard(data: DashboardFullResponse) {
    val byHour = data.peakHours.associateBy { it.hourOfDay.coerceIn(0, 23) }
    val values = (0..23).map { hour -> byHour[hour]?.totalSales ?: 0.0 }
    val maxValue = max(values.maxOrNull() ?: 0.0, 1.0)
    val busiest = data.peakHours.maxByOrNull { it.totalSales }
    var selectedHour by remember(data.peakHours) { mutableStateOf<Int?>(null) }
    val barColor = dashboardChartColors().first()
    val axis = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)

    AppDashboardCard {
        Text("Peak Hours", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            if (busiest != null && busiest.totalSales > 0.0) "Busiest: ${busiest.label} (${DashboardFormatters.money(busiest.totalSales)})" else "No hourly data",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (data.peakHours.isEmpty() || values.all { it <= 0.0 }) {
            EmptyDashboardState("No peak hour sales for this period")
        } else {
            selectedHour?.let { hour ->
                Text("$hour:00 · ${DashboardFormatters.money(values[hour])}", style = MaterialTheme.typography.labelMedium, color = barColor)
            }
            Canvas(
                modifier = Modifier.fillMaxWidth().height(190.dp).clipToBounds().semantics {
                    contentDescription = "Peak hours bar chart. Busiest hour ${busiest?.label ?: "not available"}."
                }.pointerInput(values) {
                    detectTapGestures { offset ->
                        val slot = size.width / 24f
                        selectedHour = (offset.x / slot).toInt().coerceIn(0, 23)
                    }
                }
            ) {
                val chartHeight = size.height - 24f
                val slot = size.width / 24f
                val barWidth = slot * 0.58f
                drawLine(axis, Offset(0f, chartHeight), Offset(size.width, chartHeight), strokeWidth = 1.2f)
                values.forEachIndexed { index, value ->
                    val height = ((value / maxValue) * (chartHeight - 8f)).toFloat()
                    val left = index * slot + (slot - barWidth) / 2f
                    drawRect(barColor, Offset(left, chartHeight - height), Size(barWidth, height))
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("00", "04", "08", "12", "16", "20", "23").forEach {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun LegendRow(
    color: Color,
    entry: ChartEntry,
    percentage: Double,
    valueFormatter: (Double) -> String,
    onClick: (ChartEntry) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick(entry) },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(Modifier.padding(top = 4.dp).size(10.dp).clip(CircleShape).background(color))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(entry.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(DashboardFormatters.percent(percentage), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(listOf(valueFormatter(entry.value), entry.secondary).filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun chartColors(): List<Color> = dashboardChartColors()
