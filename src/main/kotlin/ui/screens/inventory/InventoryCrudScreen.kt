package ui.screens.inventory

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ui.theme.AppAnimations
import ui.theme.ExtendedTypography

// ──────────────────────────────────────────────────────────────────────────────
// Domain model used only for display inside this generic screen
// ──────────────────────────────────────────────────────────────────────────────

data class InventoryItem(
    val id: Long,
    val name: String,
    val description: String?
)

// ──────────────────────────────────────────────────────────────────────────────
// Generic CRUD screen
// ──────────────────────────────────────────────────────────────────────────────

/**
 * A fully self-contained CRUD list/form screen for entities that share the
 * (id, name, description) shape — currently Ingredients and Food Categories.
 *
 * All I/O is driven by caller-supplied suspend lambdas so the composable
 * stays completely decoupled from the concrete API service.
 *
 * @param title          Screen title shown in the top bar.
 * @param subtitle       Secondary line shown beneath the title.
 * @param icon           Icon shown next to the title.
 * @param pageSize       Items per page for the paginated list.
 * @param onLoadPage     Fetches one page; returns (items, totalPages).
 * @param onGetById      Fetches a single item for the detail/edit dialog.
 * @param onCreate       Creates a new item; returns the created item.
 * @param onUpdate       Updates an existing item; returns the updated item.
 * @param onDelete       Deletes an item by id.
 */
@Composable
fun InventoryCrudScreen(
    title: String,
    subtitle: String,
    icon: ImageVector,
    pageSize: Int = 20,
    onLoadPage: suspend (page: Int, size: Int) -> Pair<List<InventoryItem>, Int>,
    onGetById: suspend (id: Long) -> InventoryItem,
    onCreate: suspend (name: String, description: String) -> InventoryItem,
    onUpdate: suspend (id: Long, name: String, description: String) -> InventoryItem,
    onDelete: suspend (id: Long) -> Unit
) {
    val scope = rememberCoroutineScope()

    // ─── List state ────────────────────────────────────────────────────────
    var items          by remember { mutableStateOf<List<InventoryItem>>(emptyList()) }
    var isLoading      by remember { mutableStateOf(true) }
    var loadError      by remember { mutableStateOf<String?>(null) }
    var currentPage    by remember { mutableStateOf(0) }
    var totalPages     by remember { mutableStateOf(1) }
    var searchQuery    by remember { mutableStateOf("") }

    // ─── Dialog state ──────────────────────────────────────────────────────
    var showForm       by remember { mutableStateOf(false) }
    var showDetail     by remember { mutableStateOf(false) }
    var showDeleteConf by remember { mutableStateOf(false) }
    var selectedItem   by remember { mutableStateOf<InventoryItem?>(null) }
    var isEditMode     by remember { mutableStateOf(false) }

    // ─── Notification state ────────────────────────────────────────────────
    var snackMsg       by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // msg, isError

    // ─── Helpers ───────────────────────────────────────────────────────────
    fun loadPage(page: Int) {
        scope.launch {
            isLoading = true
            loadError = null
            try {
                val (pageItems, pages) = onLoadPage(page, pageSize)
                items = pageItems
                totalPages = pages.coerceAtLeast(1)
                currentPage = page
            } catch (e: Exception) {
                loadError = e.message ?: "Failed to load data"
            } finally {
                isLoading = false
            }
        }
    }

    fun showSnack(msg: String, isError: Boolean = false) {
        snackMsg = Pair(msg, isError)
        scope.launch {
            kotlinx.coroutines.delay(3000)
            snackMsg = null
        }
    }

    // Initial load
    LaunchedEffect(Unit) { loadPage(0) }

    val filteredItems = remember(items, searchQuery) {
        if (searchQuery.isBlank()) items
        else items.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.description?.contains(searchQuery, ignoreCase = true) == true
        }
    }

    // ─── Scaffold ─────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            InventoryTopBar(
                title       = title,
                subtitle    = subtitle,
                icon        = icon,
                searchQuery = searchQuery,
                onSearch    = { searchQuery = it },
                onRefresh   = { loadPage(currentPage) },
                onCreateNew = { isEditMode = false; selectedItem = null; showForm = true }
            )
        },
        bottomBar = {
            if (totalPages > 1) {
                InventoryPagination(
                    currentPage = currentPage,
                    totalPages  = totalPages,
                    onPrev      = { loadPage(currentPage - 1) },
                    onNext      = { loadPage(currentPage + 1) }
                )
            }
        },
        snackbarHost = {
            snackMsg?.let { (msg, isError) ->
                AnimatedVisibility(
                    visible = true,
                    enter   = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit    = slideOutVertically(targetOffsetY  = { it }) + fadeOut()
                ) {
                    Surface(
                        modifier  = Modifier.fillMaxWidth().padding(16.dp),
                        color     = if (isError) MaterialTheme.colorScheme.errorContainer
                                    else MaterialTheme.colorScheme.primaryContainer,
                        shape     = RoundedCornerShape(12.dp),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier            = Modifier.padding(16.dp),
                            verticalAlignment   = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isError) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                msg,
                                color = if (isError) MaterialTheme.colorScheme.onErrorContainer
                                        else MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f)
                        )
                    )
                )
        ) {
            when {
                isLoading -> InventoryLoadingContent(title)
                loadError != null -> InventoryErrorContent(loadError!!) { loadPage(currentPage) }
                filteredItems.isEmpty() -> InventoryEmptyState(searchQuery, title) {
                    isEditMode = false; selectedItem = null; showForm = true
                }
                else -> InventoryGrid(
                    items    = filteredItems,
                    onView   = { item -> selectedItem = item; showDetail = true },
                    onEdit   = { item -> selectedItem = item; isEditMode = true; showForm = true },
                    onDelete = { item -> selectedItem = item; showDeleteConf = true }
                )
            }
        }
    }

    // ─── Dialogs ──────────────────────────────────────────────────────────

    if (showForm) {
        InventoryFormDialog(
            title    = if (isEditMode) "Edit $title" else "Create $title",
            initial  = if (isEditMode) selectedItem else null,
            onDismiss = { showForm = false },
            onSave   = { name, desc ->
                scope.launch {
                    try {
                        if (isEditMode && selectedItem != null) {
                            onUpdate(selectedItem!!.id, name, desc)
                            showSnack("$title updated successfully")
                        } else {
                            onCreate(name, desc)
                            showSnack("$title created successfully")
                        }
                        showForm = false
                        loadPage(currentPage)
                    } catch (e: Exception) {
                        showSnack(e.message ?: "Operation failed", isError = true)
                    }
                }
            }
        )
    }

    if (showDetail && selectedItem != null) {
        InventoryDetailDialog(
            item     = selectedItem!!,
            title    = title,
            onDismiss = { showDetail = false },
            onEdit   = {
                showDetail = false
                isEditMode = true
                showForm = true
            }
        )
    }

    if (showDeleteConf && selectedItem != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConf = false },
            icon  = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete ${selectedItem!!.name}?") },
            text  = {
                Text(
                    "This action cannot be undone. The entry will be permanently removed.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toDelete = selectedItem!!
                        showDeleteConf = false
                        scope.launch {
                            try {
                                onDelete(toDelete.id)
                                showSnack("\"${toDelete.name}\" deleted")
                                loadPage(currentPage)
                            } catch (e: Exception) {
                                showSnack(e.message ?: "Delete failed", isError = true)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConf = false }) { Text("Cancel") }
            }
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Top bar
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun InventoryTopBar(
    title: String,
    subtitle: String,
    icon: ImageVector,
    searchQuery: String,
    onSearch: (String) -> Unit,
    onRefresh: () -> Unit,
    onCreateNew: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val refreshRotation by animateFloatAsState(
        targetValue     = if (isHovered) 180f else 0f,
        animationSpec   = tween(AppAnimations.DURATION_FAST),
        label           = "refresh_rotation"
    )

    Surface(
        modifier        = Modifier.fillMaxWidth(),
        color           = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier            = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Title block
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector        = icon,
                        contentDescription = null,
                        modifier           = Modifier.padding(8.dp),
                        tint               = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        title,
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        subtitle,
                        style = ExtendedTypography.caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Search + actions
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value         = searchQuery,
                    onValueChange = onSearch,
                    singleLine    = true,
                    placeholder   = { Text("Search...", style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon   = { Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingIcon  = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearch("") }) {
                                Icon(Icons.Default.Clear, null)
                            }
                        }
                    },
                    modifier      = Modifier.widthIn(min = 200.dp, max = 300.dp),
                    shape         = RoundedCornerShape(12.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    )
                )

                // Refresh button
                FilledTonalIconButton(
                    onClick   = onRefresh,
                    modifier  = Modifier
                        .size(44.dp)
                        .hoverable(interactionSource)
                        .graphicsLayer { rotationZ = refreshRotation },
                    shape     = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.Refresh, "Refresh", tint = MaterialTheme.colorScheme.primary)
                }

                // Create button
                Button(
                    onClick          = onCreateNew,
                    shape            = RoundedCornerShape(10.dp),
                    contentPadding   = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Create New", style = ExtendedTypography.buttonText)
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Grid
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun InventoryGrid(
    items: List<InventoryItem>,
    onView: (InventoryItem) -> Unit,
    onEdit: (InventoryItem) -> Unit,
    onDelete: (InventoryItem) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val columns = when {
            maxWidth < 500.dp  -> 1
            maxWidth < 900.dp  -> 2
            maxWidth < 1300.dp -> 3
            else               -> 4
        }

        LazyColumn(
            modifier        = Modifier.fillMaxSize(),
            contentPadding  = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val chunked = items.chunked(columns)
            items(chunked.size) { rowIndex ->
                val row = chunked[rowIndex]
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEachIndexed { colIndex, item ->
                        var visible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay((rowIndex * columns + colIndex) * 40L)
                            visible = true
                        }
                        AnimatedVisibility(
                            visible  = visible,
                            enter    = fadeIn(tween(AppAnimations.DURATION_NORMAL)) +
                                       slideInVertically(
                                           initialOffsetY = { it / 4 },
                                           animationSpec  = tween(AppAnimations.DURATION_NORMAL, easing = AppAnimations.EaseOutQuart)
                                       ),
                            modifier = Modifier.weight(1f)
                        ) {
                            InventoryCard(
                                item     = item,
                                onView   = { onView(item) },
                                onEdit   = { onEdit(item) },
                                onDelete = { onDelete(item) }
                            )
                        }
                    }
                    // Fill remaining spots in a partial row
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Card
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun InventoryCard(
    item: InventoryItem,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val elevation by animateDpAsState(
        targetValue   = if (isHovered) 8.dp else 2.dp,
        animationSpec = tween(AppAnimations.DURATION_FAST),
        label         = "card_elevation"
    )
    val scale by animateFloatAsState(
        targetValue   = if (isHovered) 1.015f else 1f,
        animationSpec = tween(AppAnimations.DURATION_FAST),
        label         = "card_scale"
    )

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // ID badge + name row
            Row(
                modifier            = Modifier.fillMaxWidth(),
                verticalAlignment   = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.name,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onSurface,
                        maxLines   = 2
                    )
                    if (!item.description.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            item.description,
                            style   = MaterialTheme.typography.bodySmall,
                            color   = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3
                        )
                    }
                }
                // ID chip
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        "#${item.id % 1_000_000}", // Short display ID
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(Modifier.height(8.dp))

            // Action icons
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // View
                TooltipIconButton(
                    tooltip = "View Details",
                    icon    = Icons.Outlined.Visibility,
                    tint    = MaterialTheme.colorScheme.primary,
                    onClick = onView
                )
                // Edit
                TooltipIconButton(
                    tooltip = "Edit",
                    icon    = Icons.Outlined.Edit,
                    tint    = MaterialTheme.colorScheme.secondary,
                    onClick = onEdit
                )
                // Delete
                TooltipIconButton(
                    tooltip = "Delete",
                    icon    = Icons.Outlined.Delete,
                    tint    = MaterialTheme.colorScheme.error,
                    onClick = onDelete
                )
            }
        }
    }
}

@Composable
private fun TooltipIconButton(
    tooltip: String,
    icon: ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue   = if (isHovered) 1.2f else 1f,
        animationSpec = tween(AppAnimations.DURATION_FAST),
        label         = "icon_scale"
    )
    IconButton(
        onClick  = onClick,
        modifier = Modifier.hoverable(interactionSource).graphicsLayer { scaleX = scale; scaleY = scale }
    ) {
        Icon(icon, tooltip, tint = tint)
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Dialogs
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun InventoryFormDialog(
    title: String,
    initial: InventoryItem?,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String) -> Unit
) {
    var name        by remember { mutableStateOf(initial?.name ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var isSaving    by remember { mutableStateOf(false) }
    var touched     by remember { mutableStateOf(false) }

    val isNameValid = name.isNotBlank()

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        icon  = {
            Icon(
                if (initial == null) Icons.Outlined.Add else Icons.Outlined.Edit,
                null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text  = {
            Column(
                modifier              = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement   = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it; touched = true },
                    label         = { Text("Name *") },
                    singleLine    = true,
                    isError       = touched && !isNameValid,
                    supportingText = {
                        if (touched && !isNameValid) Text("Name is required")
                    },
                    leadingIcon   = { Icon(Icons.Outlined.Label, null) },
                    modifier      = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value         = description,
                    onValueChange = { description = it },
                    label         = { Text("Description") },
                    leadingIcon   = { Icon(Icons.Outlined.Notes, null) },
                    minLines      = 2,
                    maxLines      = 5,
                    modifier      = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick  = {
                    touched = true
                    if (isNameValid) {
                        isSaving = true
                        onSave(name.trim(), description.trim())
                    }
                },
                enabled  = !isSaving,
                shape    = RoundedCornerShape(8.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color       = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Saving…")
                } else {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (initial == null) "Create" else "Update")
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick  = onDismiss,
                enabled  = !isSaving,
                shape    = RoundedCornerShape(8.dp)
            ) { Text("Cancel") }
        }
    )
}

@Composable
private fun InventoryDetailDialog(
    item: InventoryItem,
    title: String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon  = {
            Icon(Icons.Outlined.Visibility, null, tint = MaterialTheme.colorScheme.primary)
        },
        title = {
            Text("$title Details", fontWeight = FontWeight.Bold)
        },
        text  = {
            Column(
                modifier            = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailRow("ID", item.id.toString())
                DetailRow("Name", item.name)
                if (!item.description.isNullOrBlank()) {
                    DetailRow("Description", item.description)
                }
            }
        },
        confirmButton = {
            Button(onClick = onEdit, shape = RoundedCornerShape(8.dp)) {
                Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Edit")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(
            label,
            style      = MaterialTheme.typography.labelSmall,
            color      = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(2.dp))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Text(
                value,
                style    = MaterialTheme.typography.bodyMedium,
                color    = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// State composables
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun InventoryLoadingContent(entityName: String) {
    val transition = rememberInfiniteTransition(label = "loading")
    val rotation by transition.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label         = "rotation"
    )
    Column(
        modifier               = Modifier.fillMaxSize(),
        horizontalAlignment    = Alignment.CenterHorizontally,
        verticalArrangement    = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier    = Modifier.size(48.dp).graphicsLayer { rotationZ = rotation },
            color       = MaterialTheme.colorScheme.primary,
            strokeWidth = 4.dp
        )
        Spacer(Modifier.height(16.dp))
        Text("Loading $entityName…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InventoryErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier            = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.errorContainer) {
            Icon(
                Icons.Default.Error, null,
                modifier = Modifier.padding(16.dp).size(40.dp),
                tint     = MaterialTheme.colorScheme.error
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            shape   = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Retry")
        }
    }
}

@Composable
private fun InventoryEmptyState(searchQuery: String, entityName: String, onCreateNew: () -> Unit) {
    Column(
        modifier            = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Icon(
                Icons.Outlined.SearchOff, null,
                modifier = Modifier.padding(24.dp).size(64.dp),
                tint     = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            if (searchQuery.isNotBlank()) "No results for \"$searchQuery\"" else "No $entityName Found",
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (searchQuery.isNotBlank()) "Try a different search term" else "Get started by adding your first entry",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (searchQuery.isBlank()) {
            Spacer(Modifier.height(24.dp))
            Button(onClick = onCreateNew, shape = RoundedCornerShape(10.dp)) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Create New")
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Pagination
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun InventoryPagination(
    currentPage: Int,
    totalPages: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        modifier        = Modifier.fillMaxWidth(),
        color           = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            PaginationButton(enabled = currentPage > 0, isNext = false, onClick = onPrev)

            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Text(
                    "Page ${currentPage + 1} of $totalPages",
                    style    = MaterialTheme.typography.labelLarge,
                    color    = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            PaginationButton(enabled = currentPage < totalPages - 1, isNext = true, onClick = onNext)
        }
    }
}

@Composable
private fun PaginationButton(enabled: Boolean, isNext: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue   = if (isHovered && enabled) 1.05f else 1f,
        animationSpec = tween(AppAnimations.DURATION_FAST),
        label         = "page_btn_scale"
    )
    Button(
        onClick  = onClick,
        enabled  = enabled,
        modifier = Modifier
            .hoverable(interactionSource)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        shape    = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (!isNext) {
            Icon(Icons.Default.ChevronLeft, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(if (isNext) "Next" else "Previous", style = ExtendedTypography.buttonText)
        if (isNext) {
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(18.dp))
        }
    }
}
