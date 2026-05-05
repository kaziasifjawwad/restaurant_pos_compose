package ui.screens.takeout.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import data.model.Result
import data.model.TakeoutMediumResponse
import data.model.TakeoutOrderStatus
import data.model.TakeoutReportDashboardResponse
import data.network.TakeoutApiService
import data.network.TakeoutReportApiService
import kotlinx.coroutines.launch
import ui.screens.takeout.TakeoutOrderDetailScreen
import ui.viewmodel.TakeoutViewModel

@Composable
fun TakeoutReportScreen() {
    var selectedOrderId by remember { mutableStateOf<Long?>(null) }
    val detailViewModel = remember { TakeoutViewModel() }
    if (selectedOrderId != null) {
        TakeoutOrderDetailScreen(
            orderId = selectedOrderId!!,
            viewModel = detailViewModel,
            onNavigateBack = { selectedOrderId = null },
            onNavigateToEdit = { },
            readOnly = true
        )
        return
    }
    TakeoutReportContent(onViewOrder = { selectedOrderId = it })
}

@Composable
private fun TakeoutReportContent(onViewOrder: (Long) -> Unit) {
    val scope = rememberCoroutineScope()
    val api = remember { TakeoutReportApiService() }
    val lookupApi = remember { TakeoutApiService() }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var report by remember { mutableStateOf<TakeoutReportDashboardResponse?>(null) }
    var mediums by remember { mutableStateOf<List<TakeoutMediumResponse>>(emptyList()) }
    var fromDate by remember { mutableStateOf("") }
    var fromTime by remember { mutableStateOf("") }
    var toDate by remember { mutableStateOf("") }
    var toTime by remember { mutableStateOf("") }
    var selectedMediumCode by remember { mutableStateOf<String?>(null) }
    var selectedStatus by remember { mutableStateOf<TakeoutOrderStatus?>(null) }

    fun loadReport() {
        val dateFrom = takeoutReportIsoBoundary(fromDate, fromTime, isStart = true)
        val dateTo = takeoutReportIsoBoundary(toDate, toTime, isStart = false)
        scope.launch {
            loading = true
            error = null
            when (val result = api.getReport(dateFrom = dateFrom, dateTo = dateTo, mediumCode = selectedMediumCode, orderStatus = selectedStatus)) {
                is Result.Success -> report = result.data
                is Result.Error -> error = result.message
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        when (val result = lookupApi.getAllTakeoutMediums()) {
            is Result.Success -> mediums = result.data
            is Result.Error -> error = result.message
        }
        loadReport()
    }

    Column(Modifier.fillMaxSize()) {
        TakeoutReportHeader(loading = loading, onRefresh = { loadReport() })
        LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                TakeoutReportFilterPanel(
                    fromDate = fromDate,
                    fromTime = fromTime,
                    onFromDateTimeChange = { d, t -> fromDate = d; fromTime = t },
                    toDate = toDate,
                    toTime = toTime,
                    onToDateTimeChange = { d, t -> toDate = d; toTime = t },
                    mediums = mediums,
                    selectedMediumCode = selectedMediumCode,
                    onMediumChange = { selectedMediumCode = it },
                    selectedStatus = selectedStatus,
                    onStatusChange = { selectedStatus = it },
                    onReset = {
                        fromDate = ""
                        fromTime = ""
                        toDate = ""
                        toTime = ""
                        selectedMediumCode = null
                        selectedStatus = null
                        loadReport()
                    },
                    onApply = { loadReport() }
                )
            }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            report?.let { data ->
                item { TakeoutReportKpiGrid(data) }
                item { TakeoutReportMiniCards(data) }
                item { TakeoutTransactionTable(data, onViewOrder) }
                item { TakeoutReportBreakdown(data) }
            }
        }
    }
}

@Composable
private fun TakeoutReportHeader(loading: Boolean, onRefresh: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Takeout Reports", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text("Filter takeout orders by date, medium and status", style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = onRefresh, enabled = !loading) {
                Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Refresh")
            }
        }
    }
}

@Composable
private fun TakeoutReportFilterPanel(
    fromDate: String,
    fromTime: String,
    onFromDateTimeChange: (String, String) -> Unit,
    toDate: String,
    toTime: String,
    onToDateTimeChange: (String, String) -> Unit,
    mediums: List<TakeoutMediumResponse>,
    selectedMediumCode: String?,
    onMediumChange: (String?) -> Unit,
    selectedStatus: TakeoutOrderStatus?,
    onStatusChange: (TakeoutOrderStatus?) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("DATE RANGE, MEDIUM & STATUS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                TakeoutReportDateTimePicker(dateValue = fromDate, timeValue = fromTime, onDateTimeChange = onFromDateTimeChange, placeholder = "Start Date & Time", modifier = Modifier.weight(1f))
                Text("to")
                TakeoutReportDateTimePicker(dateValue = toDate, timeValue = toTime, onDateTimeChange = onToDateTimeChange, placeholder = "End Date & Time", modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                MediumFilterDropdown(mediums, selectedMediumCode, onMediumChange, Modifier.weight(1f))
                StatusFilterDropdown(selectedStatus, onStatusChange, Modifier.weight(1f))
                OutlinedButton(onClick = onReset) { Text("Reset") }
                Button(onClick = onApply) { Text("Apply") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediumFilterDropdown(mediums: List<TakeoutMediumResponse>, selectedMediumCode: String?, onMediumChange: (String?) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val selected = mediums.firstOrNull { it.mediumCode == selectedMediumCode }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(value = selected?.displayName ?: "All Mediums", onValueChange = {}, readOnly = true, label = { Text("Medium") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(), singleLine = true)
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("All Mediums") }, onClick = { onMediumChange(null); expanded = false })
            mediums.forEach { medium -> DropdownMenuItem(text = { Text("${medium.displayName} (${medium.mediumCode})") }, onClick = { onMediumChange(medium.mediumCode); expanded = false }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusFilterDropdown(selectedStatus: TakeoutOrderStatus?, onStatusChange: (TakeoutOrderStatus?) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val reportStatuses = listOf(TakeoutOrderStatus.ORDER_RECEIVED, TakeoutOrderStatus.ACCEPTED, TakeoutOrderStatus.PREPARING, TakeoutOrderStatus.READY_FOR_PICKUP, TakeoutOrderStatus.PICKED_UP, TakeoutOrderStatus.COMPLETED, TakeoutOrderStatus.CANCELED)
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(value = selectedStatus?.displayName ?: "All Status", onValueChange = {}, readOnly = true, label = { Text("Status") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(), singleLine = true)
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("All Status") }, onClick = { onStatusChange(null); expanded = false })
            reportStatuses.forEach { status -> DropdownMenuItem(text = { Text(status.displayName) }, onClick = { onStatusChange(status); expanded = false }) }
        }
    }
}

@Composable
private fun TakeoutReportKpiGrid(report: TakeoutReportDashboardResponse) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TakeoutReportSummaryCard("TOTAL SALES", "৳ ${"%.2f".format(report.totalSales)}", "Gross customer payable", Modifier.weight(1f))
        TakeoutReportSummaryCard("TOTAL ORDERS", report.totalTakeoutOrders.toString(), "Completed ${report.totalCompletedOrders}", Modifier.weight(1f))
        TakeoutReportSummaryCard("RECEIVABLE", "৳ ${"%.2f".format(report.totalRestaurantReceivable)}", "After commission", Modifier.weight(1f))
    }
}

@Composable
private fun TakeoutReportMiniCards(report: TakeoutReportDashboardResponse) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TakeoutReportSummaryCard("Commission", "৳ ${"%.2f".format(report.totalPlatformCommission)}", "Platform deduction", Modifier.weight(1f))
        TakeoutReportSummaryCard("Pending Settlement", "৳ ${"%.2f".format(report.pendingSettlementAmount)}", null, Modifier.weight(1f))
        TakeoutReportSummaryCard("Canceled", report.totalCanceledOrders.toString(), null, Modifier.weight(1f))
        TakeoutReportSummaryCard("Avg Prep", "${report.averagePreparationMinutes} min", null, Modifier.weight(1f))
    }
}

@Composable
private fun TakeoutTransactionTable(report: TakeoutReportDashboardResponse, onViewOrder: (Long) -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            Text("TRANSACTION HISTORY", Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                HeaderCell("Order No", Modifier.weight(1.1f)); HeaderCell("Medium", Modifier.weight(0.9f)); HeaderCell("Status", Modifier.weight(0.9f)); HeaderCell("Gross", Modifier.weight(0.75f)); HeaderCell("Commission", Modifier.weight(0.8f)); HeaderCell("Receivable", Modifier.weight(0.85f)); HeaderCell("View", Modifier.width(54.dp))
            }
            (report.orders?.content ?: emptyList()).forEach { order ->
                val canceled = order.orderStatus == TakeoutOrderStatus.CANCELED
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    BodyCell(order.takeoutOrderNumber ?: "TO-${order.id}", Modifier.weight(1.1f))
                    BodyCell(order.mediumName ?: order.mediumCode, Modifier.weight(0.9f))
                    BodyCell(order.orderStatus.displayName, Modifier.weight(0.9f), bold = true)
                    BodyCell(if (canceled) "N/A" else "৳ ${"%.2f".format(order.totalAmount)}", Modifier.weight(0.75f), bold = true)
                    BodyCell(if (canceled) "N/A" else "৳ ${"%.2f".format(order.platformCommissionAmount)}", Modifier.weight(0.8f), bold = true)
                    BodyCell(if (canceled) "N/A" else "৳ ${"%.2f".format(order.restaurantReceivableAmount)}", Modifier.weight(0.85f), bold = true)
                    IconButton(onClick = { onViewOrder(order.id) }, modifier = Modifier.width(54.dp)) { Icon(Icons.Default.Visibility, contentDescription = "View") }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f))
            }
        }
    }
}

@Composable
private fun TakeoutReportBreakdown(report: TakeoutReportDashboardResponse) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BreakdownCard("Medium Wise Sales", report.mediumWiseSales.map { Triple(it.mediumName ?: it.mediumCode, "Orders ${it.orderCount} • Commission ৳ ${"%.2f".format(it.commissionAmount)}", "৳ ${"%.2f".format(it.totalSales)}") }, Modifier.weight(1f))
        BreakdownCard("Payment Wise Sales", report.paymentWiseSales.map { Triple(it.paymentMethod?.displayName ?: "Unknown", "Orders ${it.orderCount}", "৳ ${"%.2f".format(it.totalSales)}") }, Modifier.weight(1f))
    }
}

@Composable
private fun BreakdownCard(title: String, rows: List<Triple<String, String, String>>, modifier: Modifier = Modifier) {
    ElevatedCard(modifier) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black); rows.forEach { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text(row.first, fontWeight = FontWeight.SemiBold); Text(row.second, style = MaterialTheme.typography.bodySmall) }; Text(row.third, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) } } } }
}

@Composable private fun HeaderCell(text: String, modifier: Modifier = Modifier) { Text(text, modifier = modifier, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
@Composable private fun BodyCell(text: String, modifier: Modifier = Modifier, bold: Boolean = false) { Text(text, modifier = modifier, style = MaterialTheme.typography.bodyMedium, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal) }
