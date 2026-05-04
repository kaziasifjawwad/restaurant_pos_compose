package ui.screens.report

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.LastPage
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Assessment
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import data.model.PosReportDashboardResponse
import data.model.PosReportResponse
import data.model.PosReportWaiterResponse
import data.network.ReportApiService
import kotlinx.coroutines.launch
import ui.theme.ExtendedTypography
import java.io.File
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

private val Goldenrod = Color(0xFFD4AF37)
private val PaidGreen = Color(0xFF16A34A)
private val BkashPink = Color(0xFFE2136E)
private val RocketPurple = Color(0xFF7B1FA2)
private val NagadOrange = Color(0xFFF97316)

@Composable
fun PosReportScreen(
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
    var amountFrom by remember { mutableStateOf("") }
    var amountTo by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf("ALL") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isDownloading by remember { mutableStateOf(false) }

    fun loadReports(pageOverride: Int? = null) {
        scope.launch {
            val targetPage = pageOverride ?: currentPage
            isLoading = true
            errorMessage = null
            try {
                val response = api.getPosReports(
                    page = targetPage,
                    size = pageSize,
                    dateFrom = toIsoStart(dateFrom),
                    dateTo = toIsoEnd(dateTo),
                    waiterId = waiterId.trim().ifBlank { null },
                    amountFrom = amountFrom.trim().ifBlank { null },
                    amountTo = amountTo.trim().ifBlank { null },
                    orderStatus = selectedStatus.takeIf { it != "ALL" }
                )
                dashboard = response
                reports = response.orders.content
                totalPages = response.orders.totalPages
                totalElements = response.orders.totalElements
            } catch (e: Exception) {
                errorMessage = "Failed to load reports: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            waiters = api.getWaiters().filter { it.enabled }
        } catch (e: Exception) {
            errorMessage = "Failed to load waiter list: ${e.message}"
        }
    }

    LaunchedEffect(currentPage, pageSize) {
        loadReports()
    }

    fun applyFilters() {
        currentPage = 0
        loadReports(pageOverride = 0)
    }

    fun resetFilters() {
        dateFrom = ""
        dateTo = ""
        waiterId = ""
        amountFrom = ""
        amountTo = ""
        selectedStatus = "ALL"
        currentPage = 0
        loadReports(pageOverride = 0)
    }

    fun downloadPdf() {
        scope.launch {
            isDownloading = true
            errorMessage = null
            successMessage = null
            try {
                val fileChooser = JFileChooser()
                fileChooser.dialogTitle = "Save POS Report PDF"
                val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                fileChooser.selectedFile = File("pos_report_$today.pdf")
                fileChooser.fileFilter = FileNameExtensionFilter("PDF Files", "pdf")

                val result = fileChooser.showSaveDialog(null)
                if (result == JFileChooser.APPROVE_OPTION) {
                    var file = fileChooser.selectedFile
                    if (!file.name.endsWith(".pdf")) file = File(file.absolutePath + ".pdf")
                    val success = api.downloadPosReportPdf(toIsoStart(dateFrom), toIsoEnd(dateTo), file)
                    if (success) successMessage = "PDF saved successfully to ${file.name}"
                    else errorMessage = "Failed to download PDF"
                }
            } catch (e: Exception) {
                errorMessage = "Error downloading PDF: ${e.message}"
            } finally {
                isDownloading = false
            }
        }
    }

    Scaffold(
        topBar = {
            ReportTopBar(
                onDownloadPdf = { downloadPdf() },
                isDownloading = isDownloading
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            FilterPanel(
                dateFrom = dateFrom,
                onDateFromChange = { dateFrom = it },
                dateTo = dateTo,
                onDateToChange = { dateTo = it },
                waiterId = waiterId,
                onWaiterIdChange = { waiterId = it },
                waiters = waiters,
                amountFrom = amountFrom,
                onAmountFromChange = { amountFrom = it },
                amountTo = amountTo,
                onAmountToChange = { amountTo = it },
                selectedStatus = selectedStatus,
                onStatusChange = {
                    selectedStatus = it
                    currentPage = 0
                    loadReports(pageOverride = 0)
                },
                onApply = { applyFilters() },
                onReset = { resetFilters() }
            )

            Spacer(modifier = Modifier.height(14.dp))
            StatsRow(dashboard = dashboard)
            Spacer(modifier = Modifier.height(14.dp))

            AnimatedVisibility(visible = errorMessage != null) {
                ErrorCard(message = errorMessage ?: "", onDismiss = { errorMessage = null })
            }
            AnimatedVisibility(visible = successMessage != null) {
                SuccessCard(message = successMessage ?: "", onDismiss = { successMessage = null })
            }
            if (errorMessage != null || successMessage != null) Spacer(modifier = Modifier.height(12.dp))

            TransactionHistoryCard(
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
private fun ReportTopBar(
    onDownloadPdf: () -> Unit,
    isDownloading: Boolean
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(14.dp), color = Goldenrod.copy(alpha = 0.14f)) {
                    Icon(
                        imageVector = Icons.Outlined.Assessment,
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp).size(28.dp),
                        tint = Goldenrod
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("POS Reports", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text("View and analyze sales data", style = ExtendedTypography.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Button(
                onClick = onDownloadPdf,
                enabled = !isDownloading,
                colors = ButtonDefaults.buttonColors(containerColor = Goldenrod, contentColor = Color(0xFF111827)),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isDownloading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF111827), strokeWidth = 2.dp)
                else Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isDownloading) "Downloading..." else "Download PDF", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FilterPanel(
    dateFrom: String,
    onDateFromChange: (String) -> Unit,
    dateTo: String,
    onDateToChange: (String) -> Unit,
    waiterId: String,
    onWaiterIdChange: (String) -> Unit,
    waiters: List<PosReportWaiterResponse>,
    amountFrom: String,
    onAmountFromChange: (String) -> Unit,
    amountTo: String,
    onAmountToChange: (String) -> Unit,
    selectedStatus: String,
    onStatusChange: (String) -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "FILTER",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = Goldenrod,
                modifier = Modifier.width(54.dp)
            )
            DateFilterButton("From", dateFrom, onDateFromChange, Modifier.weight(1f))
            DateFilterButton("To", dateTo, onDateToChange, Modifier.weight(1f))
            WaiterFilter(waiterId, onWaiterIdChange, waiters, Modifier.weight(1.15f))
            CompactTextField(amountFrom, onAmountFromChange, "৳ From", Modifier.weight(0.85f))
            CompactTextField(amountTo, onAmountToChange, "৳ To", Modifier.weight(0.85f))
            StatusFilter(selectedStatus = selectedStatus, onStatusChange = onStatusChange, modifier = Modifier.weight(0.95f))
            OutlinedButton(onClick = onReset, modifier = Modifier.height(44.dp), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)) {
                Text("Reset")
            }
            Button(
                onClick = onApply,
                modifier = Modifier.height(44.dp),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Goldenrod, contentColor = Color(0xFF111827))
            ) {
                Text("Apply", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DateFilterButton(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    var visibleMonth by remember(value) { mutableStateOf(parseDate(value)?.let { YearMonth.from(it) } ?: YearMonth.now()) }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
        ) {
            Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(17.dp), tint = Goldenrod)
            Spacer(Modifier.width(6.dp))
            Text(value.ifBlank { label }, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            CalendarPicker(
                month = visibleMonth,
                selected = parseDate(value),
                onPreviousMonth = { visibleMonth = visibleMonth.minusMonths(1) },
                onNextMonth = { visibleMonth = visibleMonth.plusMonths(1) },
                onClear = {
                    onValueChange("")
                    expanded = false
                },
                onSelect = {
                    onValueChange(it.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    expanded = false
                }
            )
        }
    }
}

@Composable
private fun CalendarPicker(
    month: YearMonth,
    selected: LocalDate?,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onClear: () -> Unit,
    onSelect: (LocalDate) -> Unit
) {
    Column(modifier = Modifier.width(292.dp).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onPreviousMonth) { Icon(Icons.Filled.ArrowBack, contentDescription = "Previous month") }
            Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            IconButton(onClick = onNextMonth) { Icon(Icons.Filled.ChevronRight, contentDescription = "Next month") }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach {
                Text(it, modifier = Modifier.width(36.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        val isSelected = selected == date
                        Surface(
                            modifier = Modifier.size(36.dp).clickable { onSelect(date) },
                            shape = CircleShape,
                            color = if (isSelected) Goldenrod else Color.Transparent
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    day.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFF111827) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.size(36.dp))
                    }
                    day++
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onClear) { Text("Clear") }
            TextButton(onClick = { onSelect(LocalDate.now()) }) { Text("Today") }
        }
    }
}

@Composable
private fun WaiterFilter(
    selectedWaiterId: String,
    onWaiterIdChange: (String) -> Unit,
    waiters: List<PosReportWaiterResponse>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = waiters.firstOrNull { it.id == selectedWaiterId }?.fullName ?: "All Waiters"
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
        ) {
            Text(selectedName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("All Waiters") }, onClick = {
                onWaiterIdChange("")
                expanded = false
            })
            waiters.forEach { waiter ->
                DropdownMenuItem(text = { Text(waiter.fullName) }, onClick = {
                    onWaiterIdChange(waiter.id)
                    expanded = false
                })
            }
        }
    }
}

@Composable
private fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
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
private fun StatusFilter(selectedStatus: String, onStatusChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val statuses = listOf("ALL", "PAID", "CANCELED")
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
        ) {
            Text(statusDisplay(selectedStatus), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            Icon(if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            statuses.forEach { status ->
                DropdownMenuItem(text = { Text(statusDisplay(status)) }, onClick = {
                    onStatusChange(status)
                    expanded = false
                })
            }
        }
    }
}

@Composable
private fun StatsRow(dashboard: PosReportDashboardResponse?) {
    val totalSales = dashboard?.totalSales ?: 0.0
    val totalOrders = dashboard?.totalOrders ?: 0
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            StatCard("Total Sales", taka(totalSales), Icons.Filled.Assessment, Goldenrod, Modifier.weight(1f))
            StatCard("Total Orders", totalOrders.toString(), Icons.Filled.Receipt, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniPaymentCard("Cash", dashboard?.cashTotal ?: 0.0, Icons.Outlined.Payments, Goldenrod, Modifier.weight(1f))
            MiniPaymentCard("Card", dashboard?.cardTotal ?: 0.0, Icons.Outlined.CreditCard, Color(0xFF38BDF8), Modifier.weight(1f))
            MiniPaymentCard("bKash", dashboard?.bkashTotal ?: 0.0, Icons.Outlined.ReceiptLong, BkashPink, Modifier.weight(1f))
            MiniPaymentCard("Rocket", dashboard?.rocketTotal ?: 0.0, Icons.Outlined.ReceiptLong, RocketPurple, Modifier.weight(1f))
            MiniPaymentCard("Nagad", dashboard?.nagadTotal ?: 0.0, Icons.Outlined.ReceiptLong, NagadOrange, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f))) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = color.copy(alpha = 0.18f)) {
                Icon(icon, contentDescription = null, modifier = Modifier.padding(10.dp).size(22.dp), tint = color)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = color)
            }
        }
    }
}

@Composable
private fun MiniPaymentCard(title: String, amount: Double, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(13.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, color.copy(alpha = 0.18f))) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(taka(amount), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun TransactionHistoryCard(
    reports: List<PosReportResponse>,
    isLoading: Boolean,
    currentPage: Int,
    totalPages: Int,
    pageSize: Int,
    totalElements: Int,
    onNavigateToDetail: (String) -> Unit,
    onPageChange: (Int) -> Unit,
    onPageSizeChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text("TRANSACTION HISTORY", modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
            TableHeader()
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
            when {
                isLoading -> Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Goldenrod)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading reports...")
                    }
                }
                reports.isEmpty() -> Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Assessment, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No reports found", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> LazyColumn(modifier = Modifier.weight(1f)) {
                    items(reports) { report ->
                        ReportRow(report = report, onClick = { onNavigateToDetail(report.id) })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f))
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
            PaginationBar(currentPage, totalPages, pageSize, totalElements, onPageChange, onPageSizeChange)
        }
    }
}

@Composable
private fun TableHeader() {
    Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)).padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        HeaderText("Waiter Name", Modifier.weight(1.65f))
        HeaderText("Table", Modifier.weight(0.70f))
        HeaderText("Payment", Modifier.weight(0.90f))
        HeaderText("Status", Modifier.weight(0.90f))
        HeaderText("Amount", Modifier.weight(1.00f))
        HeaderText("Date & Time", Modifier.weight(1.45f))
        Box(modifier = Modifier.weight(0.45f))
    }
}

@Composable
private fun HeaderText(text: String, modifier: Modifier) {
    Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = modifier)
}

@Composable
private fun ReportRow(report: PosReportResponse, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(report.waiterName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1.65f))
        Text("Table ${report.tableNumber}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.70f))
        Box(modifier = Modifier.weight(0.90f), contentAlignment = Alignment.CenterStart) { PaymentChip(report.paymentMethod ?: "N/A") }
        Box(modifier = Modifier.weight(0.90f), contentAlignment = Alignment.CenterStart) { StatusChip(report.orderStatus) }
        Text(amountText(report), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (report.orderStatus == "CANCELED") MaterialTheme.colorScheme.error else Goldenrod, modifier = Modifier.weight(1.00f))
        Text(formatDateTime(report.createdDateTime), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.45f))
        IconButton(onClick = onClick, modifier = Modifier.weight(0.45f)) {
            Icon(Icons.Filled.Visibility, contentDescription = "View Details", tint = Goldenrod)
        }
    }
}

@Composable
private fun PaymentChip(payment: String) {
    Surface(shape = RoundedCornerShape(999.dp), color = Goldenrod.copy(alpha = 0.12f), border = BorderStroke(1.dp, Goldenrod.copy(alpha = 0.22f))) {
        Text(payment.uppercase(), modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Goldenrod)
    }
}

@Composable
private fun StatusChip(status: String) {
    val color = if (status == "PAID") PaidGreen else MaterialTheme.colorScheme.error
    Surface(shape = RoundedCornerShape(999.dp), color = color.copy(alpha = 0.12f), border = BorderStroke(1.dp, color.copy(alpha = 0.22f))) {
        Text(status.uppercase(), modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun PaginationBar(currentPage: Int, totalPages: Int, pageSize: Int, totalElements: Int, onPageChange: (Int) -> Unit, onPageSizeChange: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Show", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(8.dp))
            PageSizeSelector(pageSize, onPageSizeChange)
            Spacer(modifier = Modifier.width(8.dp))
            Text("entries", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(pageInfo(currentPage, pageSize, totalElements), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(onClick = { onPageChange(0) }, enabled = currentPage > 0) { Icon(Icons.Filled.FirstPage, contentDescription = "First Page") }
            IconButton(onClick = { onPageChange(currentPage - 1) }, enabled = currentPage > 0) { Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous Page") }
            Surface(shape = CircleShape, color = Goldenrod, modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center) { Text("${currentPage + 1}", fontWeight = FontWeight.Bold, color = Color(0xFF111827)) }
            }
            IconButton(onClick = { onPageChange(currentPage + 1) }, enabled = currentPage < totalPages - 1) { Icon(Icons.Filled.ChevronRight, contentDescription = "Next Page") }
            IconButton(onClick = { onPageChange(totalPages - 1) }, enabled = currentPage < totalPages - 1 && totalPages > 0) { Icon(Icons.Filled.LastPage, contentDescription = "Last Page") }
        }
    }
}

@Composable
private fun PageSizeSelector(pageSize: Int, onPageSizeChange: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val pageSizes = listOf(5, 10, 20, 50)
    Box {
        OutlinedButton(onClick = { expanded = true }, shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
            Text("$pageSize")
            Spacer(modifier = Modifier.width(4.dp))
            Icon(if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(20.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            pageSizes.forEach { size ->
                DropdownMenuItem(text = { Text("$size per page") }, onClick = {
                    onPageSizeChange(size)
                    expanded = false
                })
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String, onDismiss: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.width(12.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Dismiss") }
        }
    }
}

@Composable
private fun SuccessCard(message: String, onDismiss: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = PaidGreen.copy(alpha = 0.12f))) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = PaidGreen)
            Spacer(modifier = Modifier.width(12.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = PaidGreen, modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Dismiss") }
        }
    }
}

private fun taka(value: Double): String = "৳${String.format("%,.2f", value)}"

private fun amountText(report: PosReportResponse): String {
    val amount = taka(report.totalAmount)
    return if (report.orderStatus == "CANCELED") "-$amount" else amount
}

private fun pageInfo(currentPage: Int, pageSize: Int, totalElements: Int): String {
    if (totalElements == 0) return "Showing 0 of 0"
    val start = currentPage * pageSize + 1
    val end = minOf((currentPage + 1) * pageSize, totalElements)
    return "Showing $start-$end of $totalElements"
}

private fun statusDisplay(status: String): String = when (status) {
    "PAID" -> "Paid"
    "CANCELED" -> "Canceled"
    else -> "All"
}

private fun toIsoStart(value: String): String? = parseDate(value)?.atStartOfDay()?.atOffset(OffsetDateTime.now().offset)?.toString()

private fun toIsoEnd(value: String): String? = parseDate(value)?.atTime(23, 59, 59)?.atOffset(OffsetDateTime.now().offset)?.toString()

private fun parseDate(value: String): LocalDate? = try {
    if (value.isBlank()) null else LocalDate.parse(value.trim(), DateTimeFormatter.ISO_LOCAL_DATE)
} catch (_: Exception) {
    null
}

fun formatDateTime(dateTimeString: String): String {
    return try {
        val dateTime = OffsetDateTime.parse(dateTimeString)
        dateTime.format(DateTimeFormatter.ofPattern("dd-MMM-yy h:mm a"))
    } catch (e: Exception) {
        dateTimeString
    }
}
