package ui.screens.report

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.LastPage
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import data.model.PosReportDashboardResponse
import data.model.PosReportResponse
import data.model.PosReportWaiterResponse
import data.network.ReportApiService
import java.io.File
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlinx.coroutines.launch

private val PosReportGold = Color(0xFFD4AF37)
private val PosReportGreen = Color(0xFF16A34A)
private val PosReportBkash = Color(0xFFE2136E)
private val PosReportRocket = Color(0xFF7B1FA2)
private val PosReportNagad = Color(0xFFF97316)
private val PosReportCard = Color(0xFF38BDF8)

@Composable
fun PosReportPaymentFilterScreen(
    onNavigateToDetail: (String) -> Unit
) {
    val api = remember { ReportApiService() }
    val scope = rememberCoroutineScope()

    var dashboard by remember { mutableStateOf<PosReportDashboardResponse?>(null) }
    var reports by remember { mutableStateOf<List<PosReportResponse>>(emptyList()) }
    var waiters by remember { mutableStateOf<List<PosReportWaiterResponse>>(emptyList()) }

    var currentPage by remember { mutableStateOf(0) }
    var pageSize by remember { mutableStateOf(10) }
    var totalPages by remember { mutableStateOf(0) }
    var totalElements by remember { mutableStateOf(0) }

    var dateFrom by remember { mutableStateOf("") }
    var dateTo by remember { mutableStateOf("") }
    var waiterId by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf("ALL") }
    var selectedPaymentMethod by remember { mutableStateOf("ALL") }

    var isLoading by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    fun loadReports(pageOverride: Int? = null) {
        scope.launch {
            val targetPage = pageOverride ?: currentPage
            isLoading = true
            message = null
            try {
                val response = api.getPosReports(
                    page = targetPage,
                    size = pageSize,
                    dateFrom = posReportIsoStart(dateFrom),
                    dateTo = posReportIsoEnd(dateTo),
                    waiterId = waiterId.trim().ifBlank { null },
                    orderStatus = selectedStatus.takeIf { it != "ALL" },
                    paymentMethod = selectedPaymentMethod.takeIf { it != "ALL" }
                )
                dashboard = response
                reports = response.orders.content
                totalPages = response.orders.totalPages
                totalElements = response.orders.totalElements
            } catch (e: Exception) {
                message = "Failed to load POS reports: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            waiters = api.getWaiters().filter { it.enabled }
        } catch (e: Exception) {
            message = "Failed to load waiter list: ${e.message}"
        }
        loadReports(pageOverride = 0)
    }

    LaunchedEffect(currentPage, pageSize) {
        loadReports()
    }

    fun resetFilters() {
        dateFrom = ""
        dateTo = ""
        waiterId = ""
        selectedStatus = "ALL"
        selectedPaymentMethod = "ALL"
        currentPage = 0
        loadReports(pageOverride = 0)
    }

    fun downloadPdf() {
        scope.launch {
            isDownloading = true
            message = null
            try {
                val fileChooser = JFileChooser()
                fileChooser.dialogTitle = "Save POS Report PDF"
                fileChooser.selectedFile = File("pos_report_${LocalDate.now()}.pdf")
                fileChooser.fileFilter = FileNameExtensionFilter("PDF Files", "pdf")
                val result = fileChooser.showSaveDialog(null)
                if (result == JFileChooser.APPROVE_OPTION) {
                    var file = fileChooser.selectedFile
                    if (!file.name.endsWith(".pdf")) file = File(file.absolutePath + ".pdf")
                    val success = api.downloadPosReportPdf(posReportIsoStart(dateFrom), posReportIsoEnd(dateTo), file)
                    message = if (success) "PDF saved successfully to ${file.name}" else "Failed to download PDF"
                }
            } catch (e: Exception) {
                message = "Error downloading PDF: ${e.message}"
            } finally {
                isDownloading = false
            }
        }
    }

    Scaffold(
        topBar = {
            PosReportHeader(
                isDownloading = isDownloading,
                onDownloadPdf = ::downloadPdf,
                onRefresh = { loadReports() }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PosReportFilterPanel(
                dateFrom = dateFrom,
                onDateFromChange = { dateFrom = it },
                dateTo = dateTo,
                onDateToChange = { dateTo = it },
                waiterId = waiterId,
                onWaiterIdChange = { waiterId = it },
                waiters = waiters,
                selectedStatus = selectedStatus,
                onStatusChange = {
                    selectedStatus = it
                    currentPage = 0
                    loadReports(pageOverride = 0)
                },
                selectedPaymentMethod = selectedPaymentMethod,
                onPaymentMethodChange = {
                    selectedPaymentMethod = it
                    currentPage = 0
                    loadReports(pageOverride = 0)
                },
                onApply = {
                    currentPage = 0
                    loadReports(pageOverride = 0)
                },
                onReset = ::resetFilters
            )

            PosReportStats(dashboard)

            message?.let {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(it, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            PosReportTable(
                reports = reports,
                isLoading = isLoading,
                currentPage = currentPage,
                totalPages = totalPages,
                pageSize = pageSize,
                totalElements = totalElements,
                onNavigateToDetail = onNavigateToDetail,
                onPageChange = { currentPage = it },
                onPageSizeChange = {
                    pageSize = it
                    currentPage = 0
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PosReportHeader(
    isDownloading: Boolean,
    onDownloadPdf: () -> Unit,
    onRefresh: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(shape = RoundedCornerShape(14.dp), color = PosReportGold.copy(alpha = 0.14f)) {
                    Icon(Icons.Filled.Assessment, contentDescription = null, modifier = Modifier.padding(12.dp).size(28.dp), tint = PosReportGold)
                }
                Column {
                    Text("POS Reports", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text("Filter transactions by payment method", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onRefresh) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Refresh")
                }
                Button(
                    onClick = onDownloadPdf,
                    enabled = !isDownloading,
                    colors = ButtonDefaults.buttonColors(containerColor = PosReportGold, contentColor = Color(0xFF111827))
                ) {
                    if (isDownloading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isDownloading) "Downloading..." else "Download PDF", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PosReportFilterPanel(
    dateFrom: String,
    onDateFromChange: (String) -> Unit,
    dateTo: String,
    onDateToChange: (String) -> Unit,
    waiterId: String,
    onWaiterIdChange: (String) -> Unit,
    waiters: List<PosReportWaiterResponse>,
    selectedStatus: String,
    onStatusChange: (String) -> Unit,
    selectedPaymentMethod: String,
    onPaymentMethodChange: (String) -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("FILTER", modifier = Modifier.width(54.dp), fontWeight = FontWeight.Black, color = PosReportGold)
            PosReportTextField(dateFrom, onDateFromChange, "From yyyy-MM-dd", Modifier.weight(1f))
            PosReportTextField(dateTo, onDateToChange, "To yyyy-MM-dd", Modifier.weight(1f))
            PosReportWaiterFilter(waiterId, onWaiterIdChange, waiters, Modifier.weight(1.25f))
            PosReportStatusFilter(selectedStatus, onStatusChange, Modifier.weight(0.95f))
            PosReportPaymentMethodFilter(selectedPaymentMethod, onPaymentMethodChange, Modifier.weight(1.1f))
            OutlinedButton(onClick = onReset, modifier = Modifier.height(44.dp), contentPadding = PaddingValues(horizontal = 14.dp)) {
                Text("Reset")
            }
            Button(
                onClick = onApply,
                modifier = Modifier.height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PosReportGold, contentColor = Color(0xFF111827)),
                contentPadding = PaddingValues(horizontal = 18.dp)
            ) {
                Text("Apply", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PosReportTextField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        modifier = modifier.height(44.dp),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun PosReportWaiterFilter(
    selectedWaiterId: String,
    onWaiterIdChange: (String) -> Unit,
    waiters: List<PosReportWaiterResponse>,
    modifier: Modifier
) {
    val selectedName = waiters.firstOrNull { it.id == selectedWaiterId }?.fullName ?: "All Waiters"
    PosReportDropdown(
        value = selectedName,
        options = listOf("" to "All Waiters") + waiters.map { it.id to it.fullName },
        onSelect = onWaiterIdChange,
        modifier = modifier
    )
}

@Composable
private fun PosReportStatusFilter(selectedStatus: String, onStatusChange: (String) -> Unit, modifier: Modifier) {
    PosReportDropdown(
        value = posReportStatusDisplay(selectedStatus),
        options = listOf("ALL" to "All Status", "PAID" to "Paid", "CANCELED" to "Canceled"),
        onSelect = onStatusChange,
        modifier = modifier
    )
}

@Composable
private fun PosReportPaymentMethodFilter(selectedPaymentMethod: String, onPaymentMethodChange: (String) -> Unit, modifier: Modifier) {
    PosReportDropdown(
        value = posReportPaymentDisplay(selectedPaymentMethod),
        options = listOf(
            "ALL" to "All Payments",
            "CASH" to "Cash",
            "BKASH" to "bKash",
            "CREDIT_CARD" to "Credit Card",
            "ROCKET" to "Rocket",
            "NAGAD" to "Nagad"
        ),
        onSelect = onPaymentMethodChange,
        modifier = modifier
    )
}

@Composable
private fun PosReportDropdown(
    value: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    modifier: Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            Text(value, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
            Icon(if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.second) },
                    onClick = {
                        onSelect(option.first)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun PosReportStats(dashboard: PosReportDashboardResponse?) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            PosReportStatCard("Total Sales", posReportTaka(dashboard?.totalSales ?: 0.0), Icons.Filled.Assessment, PosReportGold, Modifier.weight(1f))
            PosReportStatCard("Total Orders", "${dashboard?.totalOrders ?: 0}", Icons.Outlined.ReceiptLong, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PosReportMiniPayment("Cash", dashboard?.cashTotal ?: 0.0, Icons.Outlined.Payments, PosReportGold, Modifier.weight(1f))
            PosReportMiniPayment("Card", dashboard?.cardTotal ?: 0.0, Icons.Outlined.CreditCard, PosReportCard, Modifier.weight(1f))
            PosReportMiniPayment("bKash", dashboard?.bkashTotal ?: 0.0, Icons.Outlined.ReceiptLong, PosReportBkash, Modifier.weight(1f))
            PosReportMiniPayment("Rocket", dashboard?.rocketTotal ?: 0.0, Icons.Outlined.ReceiptLong, PosReportRocket, Modifier.weight(1f))
            PosReportMiniPayment("Nagad", dashboard?.nagadTotal ?: 0.0, Icons.Outlined.ReceiptLong, PosReportNagad, Modifier.weight(1f))
        }
    }
}

@Composable
private fun PosReportStatCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f))) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp), tint = color)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = color)
            }
        }
    }
}

@Composable
private fun PosReportMiniPayment(title: String, amount: Double, icon: ImageVector, color: Color, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(13.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, color.copy(alpha = 0.18f))) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(posReportTaka(amount), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PosReportTable(
    reports: List<PosReportResponse>,
    isLoading: Boolean,
    currentPage: Int,
    totalPages: Int,
    pageSize: Int,
    totalElements: Int,
    onNavigateToDetail: (String) -> Unit,
    onPageChange: (Int) -> Unit,
    onPageSizeChange: (Int) -> Unit,
    modifier: Modifier
) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text("TRANSACTION HISTORY", modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
            PosReportTableHeader()
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
            when {
                isLoading -> Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PosReportGold)
                }
                reports.isEmpty() -> Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No reports found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> LazyColumn(modifier = Modifier.weight(1f)) {
                    items(reports) { report ->
                        PosReportRow(report = report, onClick = { onNavigateToDetail(report.id) })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f))
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
            PosReportPagination(currentPage, totalPages, pageSize, totalElements, onPageChange, onPageSizeChange)
        }
    }
}

@Composable
private fun PosReportTableHeader() {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        PosReportHeaderText("Waiter", Modifier.weight(1.55f))
        PosReportHeaderText("Table", Modifier.weight(0.65f))
        PosReportHeaderText("Payment", Modifier.weight(1.0f))
        PosReportHeaderText("Status", Modifier.weight(0.85f))
        PosReportHeaderText("Amount", Modifier.weight(0.95f))
        PosReportHeaderText("Date & Time", Modifier.weight(1.45f))
        Box(Modifier.weight(0.4f))
    }
}

@Composable
private fun PosReportHeaderText(text: String, modifier: Modifier) {
    Text(text, modifier = modifier, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
}

@Composable
private fun PosReportRow(report: PosReportResponse, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(report.waiterName, modifier = Modifier.weight(1.55f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("Table ${report.tableNumber}", modifier = Modifier.weight(0.65f))
        Text(posReportPaymentDisplay(report.paymentMethod ?: ""), modifier = Modifier.weight(1.0f), color = PosReportGold, fontWeight = FontWeight.Bold)
        Text(report.orderStatus, modifier = Modifier.weight(0.85f), color = if (report.orderStatus == "PAID") PosReportGreen else MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
        Text(posReportTaka(report.totalAmount), modifier = Modifier.weight(0.95f), fontWeight = FontWeight.Bold)
        Text(posReportDateTime(report.createdDateTime), modifier = Modifier.weight(1.45f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        IconButton(onClick = onClick, modifier = Modifier.weight(0.4f)) {
            Icon(Icons.Filled.Visibility, contentDescription = "View Details", tint = PosReportGold)
        }
    }
}

@Composable
private fun PosReportPagination(
    currentPage: Int,
    totalPages: Int,
    pageSize: Int,
    totalElements: Int,
    onPageChange: (Int) -> Unit,
    onPageSizeChange: (Int) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Show", color = MaterialTheme.colorScheme.onSurfaceVariant)
            PosReportPageSizeSelector(pageSize, onPageSizeChange)
            Text("entries", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(posReportPageInfo(currentPage, pageSize, totalElements), color = MaterialTheme.colorScheme.onSurfaceVariant)
            IconButton(onClick = { onPageChange(0) }, enabled = currentPage > 0) { Icon(Icons.Filled.FirstPage, contentDescription = "First Page") }
            IconButton(onClick = { onPageChange(currentPage - 1) }, enabled = currentPage > 0) { Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous Page") }
            Text("${currentPage + 1}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
            IconButton(onClick = { onPageChange(currentPage + 1) }, enabled = currentPage < totalPages - 1) { Icon(Icons.Filled.ChevronRight, contentDescription = "Next Page") }
            IconButton(onClick = { onPageChange(totalPages - 1) }, enabled = totalPages > 0 && currentPage < totalPages - 1) { Icon(Icons.Filled.LastPage, contentDescription = "Last Page") }
        }
    }
}

@Composable
private fun PosReportPageSizeSelector(pageSize: Int, onPageSizeChange: (Int) -> Unit) {
    PosReportDropdown(
        value = pageSize.toString(),
        options = listOf(5, 10, 20, 50).map { it.toString() to it.toString() },
        onSelect = { onPageSizeChange(it.toInt()) },
        modifier = Modifier.width(84.dp)
    )
}

private fun posReportIsoStart(value: String): String? = posReportIso(value, LocalTime.MIN)
private fun posReportIsoEnd(value: String): String? = posReportIso(value, LocalTime.MAX)

private fun posReportIso(value: String, time: LocalTime): String? {
    if (value.isBlank()) return null
    return runCatching {
        OffsetDateTime.of(LocalDate.parse(value.trim()), time, ZoneId.systemDefault().rules.getOffset(java.time.Instant.now()))
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }.getOrNull()
}

private fun posReportTaka(amount: Double): String = "৳ ${"%,.2f".format(amount)}"

private fun posReportDateTime(value: String): String = runCatching {
    OffsetDateTime.parse(value).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
}.getOrDefault(value)

private fun posReportPageInfo(currentPage: Int, pageSize: Int, totalElements: Int): String {
    if (totalElements <= 0) return "0 of 0"
    val from = currentPage * pageSize + 1
    val to = minOf((currentPage + 1) * pageSize, totalElements)
    return "$from-$to of $totalElements"
}

private fun posReportStatusDisplay(status: String): String = when (status) {
    "PAID" -> "Paid"
    "CANCELED" -> "Canceled"
    else -> "All Status"
}

private fun posReportPaymentDisplay(paymentMethod: String): String = when (paymentMethod) {
    "CASH" -> "Cash"
    "BKASH" -> "bKash"
    "CREDIT_CARD" -> "Credit Card"
    "ROCKET" -> "Rocket"
    "NAGAD" -> "Nagad"
    else -> "All Payments"
}
