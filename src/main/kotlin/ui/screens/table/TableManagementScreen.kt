package ui.screens.table

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.EventSeat
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TableRestaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import data.model.TableRequest
import data.model.TableResponse
import data.model.TableUpdateRequest
import data.network.TableApiService
import kotlinx.coroutines.launch
import ui.theme.ExtendedTypography

private val TableGold = Color(0xFFD4AF37)
private val TableGreen = Color(0xFF16A34A)
private val TableRed = Color(0xFFDC2626)
private val TableDark = Color(0xFF111827)

private enum class TableRoute { LIST, CREATE, EDIT }
private enum class TableStatusFilter(val label: String) { ALL("All Status"), FREE("Available"), OCCUPIED("Occupied") }

@Composable
fun TableManagementScreen() {
    val api = remember { TableApiService() }
    var route by remember { mutableStateOf(TableRoute.LIST) }
    var selectedTable by remember { mutableStateOf<TableResponse?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }
    var refreshToken by remember { mutableStateOf(0) }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)))
        )
    ) {
        when (route) {
            TableRoute.LIST -> TableListScreen(
                api = api,
                refreshToken = refreshToken,
                onCreate = { route = TableRoute.CREATE },
                onEdit = { selectedTable = it; route = TableRoute.EDIT },
                onMessage = { toast = it }
            )
            TableRoute.CREATE -> TableCreateScreen(
                api = api,
                onBack = { route = TableRoute.LIST },
                onSaved = { toast = it; refreshToken++; route = TableRoute.LIST }
            )
            TableRoute.EDIT -> TableEditScreen(
                api = api,
                table = selectedTable,
                onBack = { route = TableRoute.LIST },
                onSaved = { toast = it; refreshToken++; route = TableRoute.LIST }
            )
        }
        toast?.let { TableToast(it, Modifier.align(Alignment.BottomCenter)) { toast = null } }
    }
}

@Composable
private fun TableListScreen(
    api: TableApiService,
    refreshToken: Int,
    onCreate: () -> Unit,
    onEdit: (TableResponse) -> Unit,
    onMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var tables by remember { mutableStateOf<List<TableResponse>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf(TableStatusFilter.ALL) }
    var currentPage by remember { mutableStateOf(0) }
    var pageSize by remember { mutableStateOf(10) }
    var deleteCandidate by remember { mutableStateOf<TableResponse?>(null) }

    fun loadTables() {
        scope.launch {
            loading = true
            error = null
            try {
                val firstPage = api.getTables(page = 0, size = 200)
                tables = firstPage.content.sortedBy { it.tableNumber }
                currentPage = 0
            } catch (e: Exception) {
                error = e.message ?: "Failed to load tables"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(refreshToken) { loadTables() }

    val filtered = remember(tables, search, statusFilter) {
        tables.filter { table ->
            val query = search.trim()
            val matchesSearch = query.isBlank()
                    || table.tableNumber.toString().contains(query, ignoreCase = true)
                    || table.description.contains(query, ignoreCase = true)
            val matchesStatus = when (statusFilter) {
                TableStatusFilter.ALL -> true
                TableStatusFilter.FREE -> table.isAvailable
                TableStatusFilter.OCCUPIED -> !table.isAvailable
            }
            matchesSearch && matchesStatus
        }
    }
    LaunchedEffect(search, statusFilter, pageSize, filtered.size) {
        currentPage = currentPage.coerceAtMost(lastPage(filtered.size, pageSize))
    }

    Column(Modifier.fillMaxSize()) {
        TableTopBar(onCreate)
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            TableFilterPanel(
                search = search,
                statusFilter = statusFilter,
                total = filtered.size,
                onSearchChange = { search = it },
                onStatusChange = { statusFilter = it },
                onRefresh = { loadTables() }
            )
            error?.let { ErrorCard(it) { error = null } }
            TableGridCard(
                tables = filtered.drop(currentPage * pageSize).take(pageSize),
                loading = loading,
                currentPage = currentPage,
                totalPages = lastPage(filtered.size, pageSize) + 1,
                pageSize = pageSize,
                totalElements = filtered.size,
                onPageChange = { currentPage = it.coerceIn(0, lastPage(filtered.size, pageSize)) },
                onPageSizeChange = { pageSize = it; currentPage = 0 },
                onEdit = onEdit,
                onDelete = { deleteCandidate = it },
                modifier = Modifier.weight(1f)
            )
        }
    }

    deleteCandidate?.let { table ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Delete Table #${table.tableNumber}?") },
            text = { Text("This will remove the table from table management.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                api.deleteTable(table.id)
                                onMessage("Table #${table.tableNumber} deleted successfully")
                                deleteCandidate = null
                                loadTables()
                            } catch (e: Exception) {
                                onMessage(e.message ?: "Failed to delete table")
                                deleteCandidate = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TableRed)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun TableTopBar(onCreate: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(14.dp), color = TableGold.copy(alpha = 0.14f)) {
                    Icon(Icons.Outlined.TableRestaurant, null, Modifier.padding(12.dp).size(28.dp), tint = TableGold)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Table Management", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text("Create, edit and monitor restaurant table availability", style = ExtendedTypography.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Button(onClick = onCreate, colors = ButtonDefaults.buttonColors(containerColor = TableGold, contentColor = TableDark), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Add New Table", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TableFilterPanel(
    search: String,
    statusFilter: TableStatusFilter,
    total: Int,
    onSearchChange: (String) -> Unit,
    onStatusChange: (TableStatusFilter) -> Unit,
    onRefresh: () -> Unit
) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = search,
                onValueChange = onSearchChange,
                placeholder = { Text("Search table number or description") },
                leadingIcon = { Icon(Icons.Outlined.Search, null, tint = TableGold) },
                trailingIcon = { if (search.isNotBlank()) IconButton(onClick = { onSearchChange("") }) { Icon(Icons.Default.Close, null) } },
                singleLine = true,
                modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                shape = RoundedCornerShape(8.dp)
            )
            StatusFilterButton(statusFilter, onStatusChange, Modifier.width(170.dp))
            Surface(shape = RoundedCornerShape(999.dp), color = TableGold.copy(alpha = 0.12f)) {
                Text("$total Tables Found", modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), fontWeight = FontWeight.Bold, color = TableGold)
            }
            OutlinedButton(onClick = onRefresh, modifier = Modifier.height(48.dp), shape = RoundedCornerShape(24.dp)) {
                Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Refresh")
            }
        }
    }
}

@Composable
private fun StatusFilterButton(selected: TableStatusFilter, onSelected: (TableStatusFilter) -> Unit, modifier: Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(8.dp)) {
            Text(selected.label, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Default.ArrowDropDown, null, Modifier.size(18.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TableStatusFilter.entries.forEach { item ->
                DropdownMenuItem(text = { Text(item.label) }, onClick = { onSelected(item); expanded = false })
            }
        }
    }
}

@Composable
private fun TableGridCard(
    tables: List<TableResponse>, loading: Boolean, currentPage: Int, totalPages: Int, pageSize: Int, totalElements: Int,
    onPageChange: (Int) -> Unit, onPageSizeChange: (Int) -> Unit, onEdit: (TableResponse) -> Unit, onDelete: (TableResponse) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.fillMaxSize()) {
            TableHeader()
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
            when {
                loading -> LoadingContent("Loading tables...")
                tables.isEmpty() -> EmptyContent()
                else -> LazyColumn(Modifier.weight(1f)) {
                    items(tables, key = { it.id }) { table ->
                        TableRow(table, onEdit = { onEdit(table) }, onDelete = { onDelete(table) })
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
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)).padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        HeaderText("NUM", Modifier.weight(0.55f))
        HeaderText("DESCRIPTION", Modifier.weight(2.2f))
        HeaderText("CAP", Modifier.weight(0.55f))
        HeaderText("STATUS", Modifier.weight(0.9f))
        HeaderText("ACTIONS", Modifier.weight(1.05f))
    }
}

@Composable
private fun HeaderText(text: String, modifier: Modifier) = Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = modifier)

@Composable
private fun TableRow(table: TableResponse, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onEdit).padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(table.tableNumber.toString().padStart(2, '0'), modifier = Modifier.weight(0.55f), fontWeight = FontWeight.Bold)
        Text(table.description.ifBlank { "No description" }, modifier = Modifier.weight(2.2f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(table.capacity.toString().padStart(2, '0'), modifier = Modifier.weight(0.55f))
        Box(Modifier.weight(0.9f), contentAlignment = Alignment.CenterStart) { TableStatusChip(table.isAvailable) }
        Row(Modifier.weight(1.05f), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onEdit) { Icon(Icons.Default.Edit, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Edit") }
            TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = TableRed)) { Icon(Icons.Default.Delete, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Delete") }
        }
    }
}

@Composable
private fun TableStatusChip(isAvailable: Boolean) {
    val color = if (isAvailable) TableGreen else TableRed
    Surface(shape = RoundedCornerShape(999.dp), color = color.copy(alpha = 0.12f), border = BorderStroke(1.dp, color.copy(alpha = 0.22f))) {
        Text(if (isAvailable) "FREE" else "OCCUPIED", modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun ColumnScope.LoadingContent(text: String) {
    Box(Modifier.fillMaxWidth().weight(1f), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(color = TableGold); Spacer(Modifier.height(16.dp)); Text(text) }
    }
}

@Composable
private fun ColumnScope.EmptyContent() {
    Box(Modifier.fillMaxWidth().weight(1f), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.EventSeat, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(16.dp))
            Text("No tables found", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TableCreateScreen(api: TableApiService, onBack: () -> Unit, onSaved: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var tableNumber by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf(4) }
    var description by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    TableFormScaffold(title = "Create New Table", onBack = onBack, error = error) {
        NumberInput("Table Number", tableNumber, { tableNumber = it.filter(Char::isDigit) })
        CapacityStepper(capacity) { capacity = it }
        DescriptionInput(description) { description = it }
        FormButtons(onCancel = onBack, primaryText = "Save Table", saving = saving) {
            val number = tableNumber.toIntOrNull()
            if (number == null || number <= 0) { error = "Table number must be positive"; return@FormButtons }
            scope.launch {
                saving = true
                error = null
                try {
                    api.createTable(TableRequest(tableNumber = number, description = description.trim(), capacity = capacity))
                    onSaved("Table #$number created successfully")
                } catch (e: Exception) {
                    error = e.message ?: "Failed to create table"
                } finally {
                    saving = false
                }
            }
        }
    }
}

@Composable
private fun TableEditScreen(api: TableApiService, table: TableResponse?, onBack: () -> Unit, onSaved: (String) -> Unit) {
    if (table == null) {
        TableFormScaffold(title = "Edit Table", onBack = onBack, error = "No table selected") { FormButtons(onBack, "Back", false, onBack) }
        return
    }

    val scope = rememberCoroutineScope()
    var capacity by remember(table.id) { mutableStateOf(table.capacity.coerceAtLeast(1)) }
    var description by remember(table.id) { mutableStateOf(table.description) }
    var isAvailable by remember(table.id) { mutableStateOf(table.isAvailable) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    TableFormScaffold(title = "Edit Table #${table.tableNumber}", onBack = onBack, error = error) {
        Text("ID: ${table.id} (Read Only)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        Text("Status:", fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(selected = !isAvailable, onClick = { isAvailable = false }, label = { Text("Occupied") })
            FilterChip(selected = isAvailable, onClick = { isAvailable = true }, label = { Text("Available") })
        }
        CapacityStepper(capacity) { capacity = it }
        DescriptionInput(description) { description = it }
        FormButtons(onCancel = onBack, primaryText = "Update", saving = saving) {
            scope.launch {
                saving = true
                error = null
                try {
                    api.updateTable(table.id, TableUpdateRequest(description = description.trim(), isAvailable = isAvailable, capacity = capacity))
                    onSaved("Table #${table.tableNumber} updated successfully")
                } catch (e: Exception) {
                    error = e.message ?: "Failed to update table"
                } finally {
                    saving = false
                }
            }
        }
    }
}

@Composable
private fun TableFormScaffold(title: String, onBack: () -> Unit, error: String?, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                Surface(shape = RoundedCornerShape(14.dp), color = TableGold.copy(alpha = 0.14f)) { Icon(Icons.Outlined.TableRestaurant, null, Modifier.padding(12.dp).size(28.dp), tint = TableGold) }
                Spacer(Modifier.width(16.dp))
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            }
        }
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.TopCenter) {
            Card(Modifier.widthIn(max = 520.dp).fillMaxWidth(), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(3.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    error?.let { ErrorCard(it) {} }
                    content()
                }
            }
        }
    }
}

@Composable
private fun NumberInput(label: String, value: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("$label:", fontWeight = FontWeight.Bold)
        OutlinedTextField(value = value, onValueChange = onChange, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
    }
}

@Composable
private fun CapacityStepper(capacity: Int, onChange: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Capacity:", fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedIconButton(onClick = { onChange((capacity - 1).coerceAtLeast(1)) }) { Icon(Icons.Default.Remove, "Decrease") }
            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Box(Modifier.width(72.dp).padding(vertical = 12.dp), contentAlignment = Alignment.Center) { Text(capacity.toString(), fontWeight = FontWeight.Bold) }
            }
            OutlinedIconButton(onClick = { onChange((capacity + 1).coerceAtMost(99)) }) { Icon(Icons.Default.Add, "Increase") }
        }
    }
}

@Composable
private fun DescriptionInput(value: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Description:", fontWeight = FontWeight.Bold)
        OutlinedTextField(value = value, onValueChange = onChange, placeholder = { Text("Enter table details here...") }, modifier = Modifier.fillMaxWidth().height(120.dp), shape = RoundedCornerShape(10.dp))
    }
}

@Composable
private fun FormButtons(onCancel: () -> Unit, primaryText: String, saving: Boolean, onPrimary: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = onCancel, enabled = !saving, shape = RoundedCornerShape(10.dp)) { Text("Cancel") }
        Spacer(Modifier.width(12.dp))
        Button(onClick = onPrimary, enabled = !saving, colors = ButtonDefaults.buttonColors(containerColor = TableGold, contentColor = TableDark), shape = RoundedCornerShape(10.dp)) {
            if (saving) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            else Text(primaryText, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PaginationBar(currentPage: Int, totalPages: Int, pageSize: Int, totalElements: Int, onPageChange: (Int) -> Unit, onPageSizeChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) { Text("Show", color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.width(8.dp)); PageSizeSelector(pageSize, onPageSizeChange) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Page ${if (totalElements == 0) 0 else currentPage + 1} of ${totalPages.coerceAtLeast(1)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            IconButton(onClick = { onPageChange(currentPage - 1) }, enabled = currentPage > 0) { Icon(Icons.Default.ChevronLeft, "Previous") }
            Surface(shape = CircleShape, color = TableGold, modifier = Modifier.size(36.dp)) { Box(contentAlignment = Alignment.Center) { Text("${currentPage + 1}", fontWeight = FontWeight.Bold, color = TableDark) } }
            IconButton(onClick = { onPageChange(currentPage + 1) }, enabled = currentPage < totalPages - 1) { Icon(Icons.Default.ChevronRight, "Next") }
        }
    }
}

@Composable
private fun PageSizeSelector(pageSize: Int, onPageSizeChange: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, shape = RoundedCornerShape(8.dp)) { Text("$pageSize") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(5, 10, 20, 50).forEach { size -> DropdownMenuItem(text = { Text("$size") }, onClick = { onPageSizeChange(size); expanded = false }) }
        }
    }
}

@Composable
private fun ErrorCard(message: String, onDismiss: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
        }
    }
}

@Composable
private fun TableToast(message: String, modifier: Modifier, onDismiss: () -> Unit) {
    Snackbar(modifier = modifier.padding(16.dp), action = { TextButton(onClick = onDismiss) { Text("OK") } }) { Text(message) }
}

private fun lastPage(total: Int, pageSize: Int): Int = if (total <= 0) 0 else (total - 1) / pageSize
