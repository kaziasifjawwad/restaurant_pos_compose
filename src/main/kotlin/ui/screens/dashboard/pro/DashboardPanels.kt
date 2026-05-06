package ui.screens.dashboard.pro

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import data.model.DashboardFullResponse
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun DateFilterCard(
    dateRange: DashboardDateRange,
    onFromChange: (java.time.LocalDate) -> Unit,
    onToChange: (java.time.LocalDate) -> Unit,
    onToday: () -> Unit,
    onLastSevenDays: () -> Unit,
    onThisMonth: () -> Unit,
    onApply: () -> Unit
) {
    AppDashboardCard {
        BoxWithConstraints {
            if (maxWidth < 900.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        DashboardDateButton("From", dateRange.draftFrom, onFromChange, Modifier.weight(1f))
                        DashboardDateButton("To", dateRange.draftTo, onToChange, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = onToday, modifier = Modifier.weight(1f)) { Text("Today") }
                        OutlinedButton(onClick = onLastSevenDays, modifier = Modifier.weight(1f)) { Text("Last 7 Days") }
                        OutlinedButton(onClick = onThisMonth, modifier = Modifier.weight(1f)) { Text("This Month") }
                        Button(onClick = onApply, enabled = dateRange.hasChanges, modifier = Modifier.weight(1f)) { Text("Apply") }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DashboardDateButton("From", dateRange.draftFrom, onFromChange, Modifier.weight(1f))
                    DashboardDateButton("To", dateRange.draftTo, onToChange, Modifier.weight(1f))
                    OutlinedButton(onClick = onToday) { Text("Today") }
                    OutlinedButton(onClick = onLastSevenDays) { Text("Last 7 Days") }
                    OutlinedButton(onClick = onThisMonth) { Text("This Month") }
                    Button(onClick = onApply, enabled = dateRange.hasChanges) { Text("Apply") }
                }
            }
        }
    }
}

@Composable
fun AnomalyPanel(anomalies: List<DashboardAnomaly>) {
    if (anomalies.isEmpty()) return
    AppDashboardCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Text("Smart anomaly alerts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        anomalies.forEach { anomaly ->
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(anomaly.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Text(anomaly.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun InsightsPanel(data: DashboardFullResponse) {
    AppDashboardCard {
        Text("Insights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (data.insights.isEmpty()) {
            EmptyDashboardState("No insights for this period")
        } else {
            data.insights.take(5).forEach { insight ->
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(insight.title.ifBlank { "Insight" }, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(insight.message, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        insight.recommendedAction?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LowStockPanel(data: DashboardFullResponse, onIngredientClick: (Long?) -> Unit) {
    AppDashboardCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Low Stock", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(data.lowStockSummary.lowStockCount.toString(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
        }
        if (data.lowStockSummary.items.isEmpty()) {
            EmptyDashboardState("No low stock ingredients")
        } else {
            data.lowStockSummary.items.take(7).forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onIngredientClick(item.ingredientId) },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.ingredientName ?: "Ingredient", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${DashboardFormatters.decimal(item.currentAmount)} ${item.unit.orEmpty()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(item.severity, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun RecentActivityPanel(data: DashboardFullResponse, onActivityClick: (Long?) -> Unit) {
    AppDashboardCard {
        Text("Recent Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (data.recentActivity.isEmpty()) {
            EmptyDashboardState("No recent activity")
        } else {
            data.recentActivity.take(10).forEach { activity ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onActivityClick(activity.referenceId) }.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(activity.title.ifBlank { "Activity" }, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(activity.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        if (activity.amount != 0.0) Text(DashboardFormatters.money(activity.amount), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text(DashboardFormatters.humanDateTime(activity.createdDateTime), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardToast(message: String?, onDismiss: () -> Unit) {
    if (message.isNullOrBlank()) return
    Surface(shape = RoundedCornerShape(99.dp), color = MaterialTheme.colorScheme.inverseSurface) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, color = MaterialTheme.colorScheme.inverseOnSurface, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    }
}

fun exportDashboardCsv(data: DashboardFullResponse): String {
    val fileName = "restaurant-dashboard-${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))}.csv"
    val file = File(System.getProperty("user.home"), fileName)
    val lines = mutableListOf<String>()
    lines += "Section,Label,Value,Extra"
    lines += "Overview,Total Sales,${data.overview.totalSales},Paid Orders ${data.overview.totalPaidOrders}"
    lines += "Overview,Average Order,${data.overview.averageOrderValue},Cancellation ${data.overview.cancellationRate}"
    data.topFoodItems.forEach { lines += "Top Food,${it.foodName},${it.netSales},${it.orderCount} orders" }
    data.topBeverages.forEach { lines += "Top Beverage,${it.beverageName},${it.netSales},${it.orderCount} orders" }
    data.lowStockSummary.items.forEach { lines += "Low Stock,${it.ingredientName ?: "Ingredient"},${it.currentAmount},${it.severity}" }
    file.writeText(lines.joinToString("\n"))
    return file.absolutePath
}
