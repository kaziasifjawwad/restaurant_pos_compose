package ui.screens.dashboard.pro

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import data.model.DashboardFullResponse
import kotlin.math.max

@Composable
fun DashboardHeaderCard(
    dateRangeLabel: String,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onExportCsv: () -> Unit
) {
    AppDashboardCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Restaurant Analytics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "$dateRangeLabel · Timezone: ${DashboardFormatters.timezoneLabel()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(onClick = onExportCsv, enabled = !isLoading) {
                Icon(Icons.Filled.Analytics, contentDescription = "Export CSV", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Export CSV")
            }
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(onClick = onRefresh, enabled = !isLoading) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else Icon(Icons.Filled.Refresh, contentDescription = "Refresh", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Refresh")
            }
        }
    }
}

@Composable
fun KpiMetricCard(metric: KpiMetric, modifier: Modifier = Modifier) {
    val positive = (metric.deltaPercent ?: 0.0) > 0.0
    val negative = (metric.deltaPercent ?: 0.0) < 0.0
    val icon = when {
        positive -> Icons.Filled.TrendingUp
        negative -> Icons.Filled.TrendingDown
        else -> Icons.Filled.TrendingFlat
    }

    Card(
        modifier = modifier.height(118.dp).semantics { contentDescription = metric.contentDescription },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.10f))
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.SpaceBetween) {
                Text(metric.title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Text(metric.value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = if (negative) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Text(DashboardFormatters.deltaLabel(metric.deltaPercent), style = MaterialTheme.typography.labelSmall, color = if (negative) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                }
                Text(metric.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            MiniSparkline(values = metric.sparkline, modifier = Modifier.width(82.dp).height(54.dp))
        }
    }
}

@Composable
fun EmptyDashboardState(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().height(154.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f), modifier = Modifier.size(54.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.TableRestaurant, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun DashboardSkeleton() {
    val transition = rememberInfiniteTransition(label = "dashboard-shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.62f,
        animationSpec = infiniteRepeatable(animation = tween(850), repeatMode = RepeatMode.Reverse),
        label = "dashboard-shimmer-alpha"
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                repeat(4) { SkeletonBlock(Modifier.weight(1f).height(118.dp), alpha) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            repeat(3) { SkeletonBlock(Modifier.weight(1f).height(270.dp), alpha) }
        }
    }
}

@Composable
private fun SkeletonBlock(modifier: Modifier, alpha: Float) {
    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha), RoundedCornerShape(16.dp)))
}

@Composable
fun AppDashboardCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.10f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

@Composable
private fun MiniSparkline(values: List<Double>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    Canvas(modifier = modifier.semantics { contentDescription = "Trend sparkline with ${values.size} points" }) {
        if (values.size < 2) return@Canvas
        val minValue = values.minOrNull() ?: 0.0
        val maxValue = max(values.maxOrNull() ?: 0.0, minValue + 1.0)
        val step = size.width / (values.size - 1)
        val points = values.mapIndexed { index, value ->
            val ratio = ((value - minValue) / (maxValue - minValue)).toFloat()
            Offset(index * step, size.height - (ratio * size.height))
        }
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        val fillPath = Path().apply {
            moveTo(points.first().x, size.height)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, size.height)
            close()
        }
        drawPath(fillPath, fillColor)
        drawPath(path, lineColor, style = Stroke(width = 2.2f))
    }
}

fun buildKpis(data: DashboardFullResponse): List<KpiMetric> {
    val overview = data.overview
    val trend = timelineValues(data.salesTimeline)
    return listOf(
        KpiMetric("Total Sales", DashboardFormatters.money(overview.totalSales), "Today ${DashboardFormatters.money(overview.todaySales)}", DashboardFormatters.deltaPercent(overview.todaySales, overview.yesterdaySales), trend, "Total sales ${DashboardFormatters.money(overview.totalSales)}"),
        KpiMetric("Paid Orders", overview.totalPaidOrders.toString(), "${overview.totalCanceledOrders} canceled", null, data.salesTimeline.map { it.paidOrderCount.toDouble() }, "Paid orders ${overview.totalPaidOrders}"),
        KpiMetric("Average Order", DashboardFormatters.money(overview.averageOrderValue), "Per paid order", null, trend, "Average order value ${DashboardFormatters.money(overview.averageOrderValue)}"),
        KpiMetric("Cancellation", DashboardFormatters.percent(overview.cancellationRate), "Rate", null, listOf(overview.cancellationRate), "Cancellation rate ${DashboardFormatters.percent(overview.cancellationRate)}"),
        KpiMetric("Active Orders", overview.activeOrders.toString(), "Open now", null, listOf(overview.activeOrders.toDouble()), "Active orders ${overview.activeOrders}"),
        KpiMetric("Tables", "${overview.occupiedTables}/${overview.freeTables}", "Occupied / free", null, listOf(overview.occupiedTables.toDouble(), overview.freeTables.toDouble()), "Tables occupied ${overview.occupiedTables}, free ${overview.freeTables}"),
        KpiMetric("Menu Items", (overview.totalFoodItems + overview.totalBeverages).toString(), "${overview.totalFoodItems} food, ${overview.totalBeverages} beverages", null, listOf(overview.totalFoodItems.toDouble(), overview.totalBeverages.toDouble()), "Menu items ${overview.totalFoodItems + overview.totalBeverages}"),
        KpiMetric("Low Stock", overview.lowStockIngredientCount.toString(), "Ingredients", null, listOf(overview.lowStockIngredientCount.toDouble()), "Low stock ingredients ${overview.lowStockIngredientCount}")
    )
}
