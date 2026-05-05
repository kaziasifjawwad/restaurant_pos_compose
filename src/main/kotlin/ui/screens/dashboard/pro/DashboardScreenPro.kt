package ui.screens.dashboard.pro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import data.model.DashboardFullResponse
import data.network.DashboardApiService
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun DashboardScreenPro() {
    val api = remember { DashboardApiService() }
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }
    val defaultFrom = remember(today) { today.minusDays(6) }
    var draftFrom by remember { mutableStateOf(defaultFrom) }
    var draftTo by remember { mutableStateOf(today) }
    var appliedFrom by remember { mutableStateOf(defaultFrom) }
    var appliedTo by remember { mutableStateOf(today) }
    var dashboard by remember { mutableStateOf<DashboardFullResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            isLoading = true
            error = null
            try {
                dashboard = api.getFull(DashboardFormatters.apiDate(appliedFrom), DashboardFormatters.apiDate(appliedTo))
            } catch (throwable: Throwable) {
                error = throwable.message ?: "Unable to load dashboard"
            } finally {
                isLoading = false
            }
        }
    }

    fun applyRange(from: LocalDate, to: LocalDate) {
        val normalizedFrom = if (from <= to) from else to
        val normalizedTo = if (from <= to) to else from
        draftFrom = normalizedFrom
        draftTo = normalizedTo
        appliedFrom = normalizedFrom
        appliedTo = normalizedTo
    }

    LaunchedEffect(appliedFrom, appliedTo) { load() }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                DashboardHeaderCard(
                    dateRangeLabel = "${DashboardFormatters.date(appliedFrom)} - ${DashboardFormatters.date(appliedTo)}",
                    isLoading = isLoading,
                    onRefresh = { load() },
                    onExportCsv = {
                        val data = dashboard
                        toast = if (data == null) "No dashboard data to export" else "CSV exported: ${exportDashboardCsv(data)}"
                    }
                )
            }
            item {
                DateFilterCard(
                    dateRange = DashboardDateRange(draftFrom, draftTo, appliedFrom, appliedTo),
                    onFromChange = { draftFrom = it },
                    onToChange = { draftTo = it },
                    onToday = { val now = LocalDate.now(); applyRange(now, now) },
                    onLastSevenDays = { val now = LocalDate.now(); applyRange(now.minusDays(6), now) },
                    onThisMonth = { val now = LocalDate.now(); applyRange(now.withDayOfMonth(1), now) },
                    onApply = { applyRange(draftFrom, draftTo) }
                )
            }
            if (error != null) {
                item { ErrorDashboardCard(error.orEmpty(), onRetry = { load() }) }
            }
            if (isLoading && dashboard == null) {
                item { DashboardSkeleton() }
            }
            dashboard?.let { data ->
                item { KpiAdaptiveGrid(buildKpis(data)) }
                item { AnomalyPanel(data.detectAnomalies()) }
                item {
                    DashboardAdaptiveThree(
                        first = {
                            DonutChartCard(
                                title = "Payment Mix",
                                entries = data.paymentDistribution.items.map {
                                    ChartEntry(
                                        label = it.displayName.ifBlank { it.paymentMethod.ifBlank { "Unknown" } },
                                        value = it.amount,
                                        secondary = "${it.orderCount} orders"
                                    )
                                },
                                valueFormatter = DashboardFormatters::money,
                                onEntryClick = { toast = "Payment drill-down: ${it.label}" }
                            )
                        },
                        second = { StackedStatusBarCard(data) },
                        third = {
                            DonutChartCard(
                                title = "Top Food Items",
                                entries = data.topFoodItems.map {
                                    ChartEntry(
                                        label = it.foodName.ifBlank { "Food item" },
                                        value = it.netSales,
                                        secondary = "${DashboardFormatters.decimal(it.quantitySold)} sold, ${it.orderCount} orders",
                                        referenceId = it.foodId
                                    )
                                },
                                valueFormatter = DashboardFormatters::money,
                                onEntryClick = { toast = "Open food item detail: ${it.label}" }
                            )
                        }
                    )
                }
                item {
                    DashboardAdaptiveThree(
                        first = {
                            DonutChartCard(
                                title = "Top Beverages",
                                entries = data.topBeverages.map {
                                    ChartEntry(
                                        label = it.beverageName.ifBlank { "Beverage" },
                                        value = it.netSales,
                                        secondary = "${DashboardFormatters.decimal(it.quantity)} ${it.unit}, ${it.orderCount} orders",
                                        referenceId = it.beverageId
                                    )
                                },
                                valueFormatter = DashboardFormatters::money,
                                onEntryClick = { toast = "Open beverage detail: ${it.label}" }
                            )
                        },
                        second = {
                            HorizontalBarChartCard(
                                title = "Waiter Performance",
                                entries = data.waiterPerformance.map {
                                    ChartEntry(
                                        label = it.waiterName?.takeIf { name -> name.isNotBlank() } ?: "Unknown",
                                        value = it.totalSales,
                                        secondary = "${it.paidOrderCount} paid, AOV ${DashboardFormatters.money(it.averageOrderValue)}",
                                        referenceId = it.waiterId
                                    )
                                },
                                valueFormatter = DashboardFormatters::money,
                                onEntryClick = { toast = "Waiter performance drill-down: ${it.label}" }
                            )
                        },
                        third = {
                            HorizontalBarChartCard(
                                title = "Table Performance",
                                entries = data.tablePerformance.map {
                                    ChartEntry(
                                        label = "Table ${it.tableNumber ?: "-"}",
                                        value = it.totalSales,
                                        secondary = "Floor ${it.floor ?: "-"}, ${it.paidOrderCount} paid",
                                        referenceId = it.tableId
                                    )
                                },
                                valueFormatter = DashboardFormatters::money,
                                onEntryClick = { toast = "Table performance drill-down: ${it.label}" }
                            )
                        }
                    )
                }
                item { PeakHoursBarCard(data) }
                item {
                    DashboardAdaptiveTwo(
                        left = { InsightsPanel(data) },
                        right = { LowStockPanel(data, onIngredientClick = { toast = "Open inventory detail for ingredient id: ${it ?: "unknown"}" }) }
                    )
                }
                item { RecentActivityPanel(data, onActivityClick = { toast = "Open report/order detail id: ${it ?: "unknown"}" }) }
            }
        }
        Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)) {
            DashboardToast(toast, onDismiss = { toast = null })
        }
    }
}

@Composable
private fun KpiAdaptiveGrid(metrics: List<KpiMetric>) {
    BoundedAdaptiveGrid(itemCount = metrics.size, minCellWidth = 236.dp, rowHeight = 132.dp) {
        items(metrics) { metric -> KpiMetricCard(metric) }
    }
}

@Composable
private fun DashboardAdaptiveThree(
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
    third: @Composable () -> Unit
) {
    BoundedAdaptiveGrid(itemCount = 3, minCellWidth = 360.dp, rowHeight = 338.dp) {
        item { first() }
        item { second() }
        item { third() }
    }
}

@Composable
private fun DashboardAdaptiveTwo(left: @Composable () -> Unit, right: @Composable () -> Unit) {
    BoundedAdaptiveGrid(itemCount = 2, minCellWidth = 460.dp, rowHeight = 380.dp) {
        item { left() }
        item { right() }
    }
}

@Composable
private fun BoundedAdaptiveGrid(
    itemCount: Int,
    minCellWidth: Dp,
    rowHeight: Dp,
    content: androidx.compose.foundation.lazy.grid.LazyGridScope.() -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = maxOf(1, (maxWidth / minCellWidth).toInt())
        val rows = ((itemCount + columns - 1) / columns).coerceAtLeast(1)
        val gridHeight = (rowHeight * rows.toFloat()) + (12.dp * (rows - 1).toFloat())
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = minCellWidth),
            modifier = Modifier.fillMaxWidth().height(gridHeight),
            userScrollEnabled = false,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun ErrorDashboardCard(message: String, onRetry: () -> Unit) {
    AppDashboardCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Dashboard unavailable: $message", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}
