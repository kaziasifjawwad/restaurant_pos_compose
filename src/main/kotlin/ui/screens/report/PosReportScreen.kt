package ui.screens.report

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import data.model.PosReportResponse
import data.network.ReportApiService
import kotlinx.coroutines.launch
import ui.theme.ExtendedTypography
import java.io.File
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
fun PosReportScreen(
    onNavigateToDetail: (String) -> Unit
) {
    val api = remember { ReportApiService() }
    val scope = rememberCoroutineScope()
    
    var reports by remember { mutableStateOf<List<PosReportResponse>>(emptyList()) }
    var currentPage by remember { mutableStateOf(0) }
    var pageSize by remember { mutableStateOf(10) }
    var totalPages by remember { mutableStateOf(0) }
    var totalElements by remember { mutableStateOf(0) }
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isDownloading by remember { mutableStateOf(false) }

    fun loadReports() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = api.getPosReports(page = currentPage, size = pageSize)
                reports = response.content
                totalPages = response.totalPages
                totalElements = response.totalElements
            } catch (e: Exception) {
                errorMessage = "Failed to load reports: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(currentPage, pageSize) {
        loadReports()
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
                val defaultFileName = "pos_report_$today.pdf"
                fileChooser.selectedFile = File(defaultFileName)
                fileChooser.fileFilter = FileNameExtensionFilter("PDF Files", "pdf")
                
                val result = fileChooser.showSaveDialog(null)
                if (result == JFileChooser.APPROVE_OPTION) {
                    var file = fileChooser.selectedFile
                    if (!file.name.endsWith(".pdf")) {
                        file = File(file.absolutePath + ".pdf")
                    }
                    
                    // No date filter - download all reports
                    val success = api.downloadPosReportPdf(null, null, file)
                    if (success) {
                        successMessage = "PDF saved successfully to ${file.name}"
                    } else {
                        errorMessage = "Failed to download PDF"
                    }
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
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            // Stats Cards
            StatsRow(totalElements = totalElements)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Messages
            AnimatedVisibility(visible = errorMessage != null) {
                ErrorCard(message = errorMessage ?: "", onDismiss = { errorMessage = null })
            }
            
            AnimatedVisibility(visible = successMessage != null) {
                SuccessCard(message = successMessage ?: "", onDismiss = { successMessage = null })
            }
            
            if (errorMessage != null || successMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Reports Table
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Table Header
                    TableHeader()
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    
                    // Table Content
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Loading reports...", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    } else if (reports.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Filled.Assessment,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "No reports found",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f)
                        ) {
                            items(reports) { report ->
                                ReportRow(
                                    report = report,
                                    onClick = { onNavigateToDetail(report.id) }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    
                    // Pagination
                    PaginationBar(
                        currentPage = currentPage,
                        totalPages = totalPages,
                        pageSize = pageSize,
                        totalElements = totalElements,
                        onPageChange = { currentPage = it },
                        onPageSizeChange = { 
                            pageSize = it
                            currentPage = 0
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ReportTopBar(
    onDownloadPdf: () -> Unit,
    isDownloading: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Assessment,
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp).size(28.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        "POS Reports",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "View and download order reports",
                        style = ExtendedTypography.caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Button(
                onClick = onDownloadPdf,
                enabled = !isDownloading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onError,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isDownloading) "Downloading..." else "Download PDF")
            }
        }
    }
}

@Composable
fun StatsRow(totalElements: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatCard(
            title = "Total Orders",
            value = totalElements.toString(),
            icon = Icons.Filled.Receipt,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.2f)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp).size(24.dp),
                    tint = color
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }
}

@Composable
fun TableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Waiter Name",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(2f)
        )
        Text(
            "Table",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Text(
            "Amount",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1.5f)
        )
        Text(
            "Date & Time",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(2f)
        )
        Box(modifier = Modifier.weight(0.8f))
    }
}

@Composable
fun ReportRow(
    report: PosReportResponse,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            report.waiterName,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(2f)
        )
        
        Text(
            "Table ${report.tableNumber}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            "৳${String.format("%.2f", report.totalAmount)}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1.5f)
        )
        
        Text(
            formatDateTime(report.createdDateTime),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(2f)
        )
        
        IconButton(
            onClick = onClick,
            modifier = Modifier.weight(0.8f)
        ) {
            Icon(
                Icons.Filled.Visibility,
                contentDescription = "View Details",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun PaginationBar(
    currentPage: Int,
    totalPages: Int,
    pageSize: Int,
    totalElements: Int,
    onPageChange: (Int) -> Unit,
    onPageSizeChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Page size selector
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Show",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            
            var expanded by remember { mutableStateOf(false) }
            val pageSizes = listOf(5, 10, 20, 50, 100)
            
            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("$pageSize")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    pageSizes.forEach { size ->
                        DropdownMenuItem(
                            text = { Text("$size per page") },
                            onClick = {
                                onPageSizeChange(size)
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "entries",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Page info and navigation
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Showing ${currentPage * pageSize + 1}-${minOf((currentPage + 1) * pageSize, totalElements)} of $totalElements",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // First page
            IconButton(
                onClick = { onPageChange(0) },
                enabled = currentPage > 0
            ) {
                Icon(Icons.Filled.FirstPage, contentDescription = "First Page")
            }
            
            // Previous page
            IconButton(
                onClick = { onPageChange(currentPage - 1) },
                enabled = currentPage > 0
            ) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous Page")
            }
            
            // Page numbers
            val startPage = maxOf(0, currentPage - 2)
            val endPage = minOf(totalPages - 1, currentPage + 2)
            
            for (page in startPage..endPage) {
                if (page == currentPage) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "${page + 1}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = CircleShape,
                        color = Color.Transparent,
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { onPageChange(page) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "${page + 1}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Next page
            IconButton(
                onClick = { onPageChange(currentPage + 1) },
                enabled = currentPage < totalPages - 1
            ) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Next Page")
            }
            
            // Last page
            IconButton(
                onClick = { onPageChange(totalPages - 1) },
                enabled = currentPage < totalPages - 1
            ) {
                Icon(Icons.Filled.LastPage, contentDescription = "Last Page")
            }
        }
    }
}

@Composable
fun ErrorCard(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
fun SuccessCard(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF1B5E20),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Dismiss",
                    tint = Color(0xFF1B5E20)
                )
            }
        }
    }
}

fun formatDateTime(dateTimeString: String): String {
    return try {
        val dateTime = OffsetDateTime.parse(dateTimeString)
        dateTime.format(DateTimeFormatter.ofPattern("dd-MMM-yy h:mm a"))
    } catch (e: Exception) {
        dateTimeString
    }
}
