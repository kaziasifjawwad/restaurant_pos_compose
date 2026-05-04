package ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import data.model.DashboardFullResponse
import data.model.DashboardInsightResponse
import data.model.DashboardPaymentDistributionItemResponse
import data.model.DashboardRecentActivityResponse
import data.model.DashboardTimelineResponse
import data.network.DashboardApiService
import java.time.LocalDate
import kotlinx.coroutines.launch

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

    Scaffold(topBar = {
        Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 3.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.Dashboard, contentDescription = null, modifier = Modifier.size(30.dp), tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("Dashboard", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                        Text("$dateFrom to $dateTo", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                OutlinedButton(onClick = { load() }, enabled = !loading) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Refresh")
                }
            }
        }
    }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                DashboardFilters(
                    dateFrom = dateFrom,
                    dateTo = dateTo,
                    onDateFrom = { dateFrom = it },
                    onDateTo = { dateTo = it },
                    onToday = {
                        dateFrom = today.toString(); dateTo = today.toString(); load()
                    },
                    onThisWeek = {
                        dateFrom = today.minusDays(6).toString(); dateTo = today.toString(); load()
                    },
                    onThisMonth = {
                        dateFrom = today.withDayOfMonth(1).toString(); dateTo = today.withDayOfMonth(today.lengthOfMonth()).toString(); load()
                    },
                    onApply = { load() }
                )
            }

            message?.let { item { InfoPanel(it) } }

            if (loading) {
                item { Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            } else {
                dashboard?.let { data ->
                    item { KpiGrid(data) }
                    item { SectionTitle("Sales Timeline") }
                    item { LineChart(data.salesTimeline) }
                    item { SectionTitle("Payment Distribution") }
                    item { HorizontalBars(data.paymentDistribution.items.map { it.displayName to it.amount }) }
                    item { SectionTitle("Top Food Items") }
                    item { HorizontalBars(data.topFoodItems.map { it.foodName to it.netSales }) }
                    item { SectionTitle("Top Beverages") }
                    item { HorizontalBars(data.topBeverages.map { it.beverageName to it.netSales }) }
                    item { SectionTitle("Waiter Performance") }
                    item { HorizontalBars(data.waiterPerformance.map { (it.waiterName ?: "Unknown") to it.totalSales }) }
                    item { SectionTitle("Peak Hours") }
                    item { HorizontalBars(data.peakHours.filter { it.orderCount > 0 }.map { it.label to it.totalSales }) }
                    item { SectionTitle("Low Stock") }
                    item {
                        SimpleList(
                            if (data.lowStockSummary.items.isEmpty()) listOf("No low-stock ingredients")
                            else data.lowStockSummary.items.map { "${it.ingredientName ?: "Ingredient"}: ${it.currentAmount} ${it.unit ?: ""} (${it.severity})" }
                        )
                    }
                    item { SectionTitle("Insights") }
                    item { InsightList(data.insights) }
                    item { SectionTitle("Recent Activity") }
                    item { RecentActivityList(data.recentActivity) }
                }
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
    Card(shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(dateFrom, onDateFrom, label = { Text("Date From") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(dateTo, onDateTo, label = { Text("Date To") }, singleLine = true, modifier = Modifier.weight(1f))
                Button(onClick = onApply) { Text("Apply") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onToday) { Text("Today") }
                OutlinedButton(onClick = onThisWeek) { Text("This Week") }
                OutlinedButton(onClick = onThisMonth) { Text("This Month") }
            }
            Text("Use yyyy-MM-dd only. No time input is required.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun KpiGrid(data: DashboardFullResponse) {
    val o = data.overview
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard("Total Sales", taka(o.totalSales), Modifier.weight(1f))
            MetricCard("Paid Orders", o.totalPaidOrders.toString(), Modifier.weight(1f))
            MetricCard("Active Orders", o.activeOrders.toString(), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard("Average Order", taka(o.averageOrderValue), Modifier.weight(1f))
            MetricCard("Cancellation", "${o.cancellationRate}%", Modifier.weight(1f))
            MetricCard("Low Stock", o.lowStockIngredientCount.toString(), Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
}

@Composable
private fun LineChart(items: List<DashboardTimelineResponse>) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val max = (items.maxOfOrNull { it.salesAmount } ?: 0.0).coerceAtLeast(1.0)
    Card(shape = RoundedCornerShape(18.dp)) {
        Canvas(Modifier.fillMaxWidth().height(220.dp).padding(18.dp)) {
            drawLine(outline, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 2f)
            if (items.size > 1) {
                val step = size.width / (items.size - 1)
                val points = items.mapIndexed { index, item -> Offset(index * step, size.height - ((item.salesAmount / max).toFloat() * size.height)) }
                points.zipWithNext().forEach { (a, b) -> drawLine(primary, a, b, strokeWidth = 4f, cap = StrokeCap.Round) }
                points.forEach { drawCircle(primary, radius = 5f, center = it) }
            }
        }
    }
}

@Composable
private fun HorizontalBars(items: List<Pair<String, Double>>) {
    val max = (items.maxOfOrNull { it.second } ?: 0.0).coerceAtLeast(1.0)
    Card(shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (items.isEmpty()) Text("No data found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            items.take(10).forEach { (label, value) ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(label.ifBlank { "Unknown" }, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text(taka(value), fontWeight = FontWeight.Bold)
                    }
                    val primary = MaterialTheme.colorScheme.primary
                    val background = MaterialTheme.colorScheme.surfaceVariant
                    Canvas(Modifier.fillMaxWidth().height(8.dp)) {
                        drawRoundRect(background)
                        drawRoundRect(primary, size = size.copy(width = size.width * (value / max).toFloat()))
                    }
                }
            }
        }
    }
}

@Composable
private fun SimpleList(items: List<String>) {
    Card(shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { Text(it) }
        }
    }
}

@Composable
private fun InsightList(items: List<DashboardInsightResponse>) {
    SimpleList(if (items.isEmpty()) listOf("No critical insight for this range") else items.map { "${it.title}: ${it.message}" })
}

@Composable
private fun RecentActivityList(items: List<DashboardRecentActivityResponse>) {
    SimpleList(if (items.isEmpty()) listOf("No recent activity") else items.take(10).map { "${it.title} • ${taka(it.amount)} • ${it.createdDateTime ?: ""}" })
}

@Composable
private fun InfoPanel(text: String) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Text(text, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun taka(value: Double): String = "৳" + String.format("%,.2f", value)
