package ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import data.model.BeverageResponse
import data.model.PageBeverageResponse
import data.network.BeverageApiService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.theme.AppAnimations
import ui.theme.ExtendedTypography
import java.text.DecimalFormat

/**
 * Screen for displaying list of beverages with CRUD operations
 */
@Composable
fun BeverageListScreen(
    onNavigateToCreate: () -> Unit = {},
    onNavigateToView: (Long) -> Unit = {},
    onNavigateToEdit: (Long) -> Unit = {}
) {
    val api = remember { BeverageApiService() }
    val scope = rememberCoroutineScope()
    
    var beverages by remember { mutableStateOf<List<BeverageResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentPage by remember { mutableStateOf(0) }
    var totalPages by remember { mutableStateOf(1) }
    var filterText by remember { mutableStateOf("") }
    var isContentVisible by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<BeverageResponse?>(null) }

    fun loadBeverages(page: Int = 0) {
        println("[BeverageListScreen] Loading page $page")
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response: PageBeverageResponse = api.getBeverages(page, 20)
                beverages = response.content
                currentPage = response.number
                totalPages = response.totalPages
                println("[BeverageListScreen] Loaded ${beverages.size} beverages")
            } catch (e: Exception) {
                println("[BeverageListScreen] Error: ${e.message}")
                errorMessage = e.message
            } finally {
                isLoading = false
            }
        }
    }

    fun deleteBeverage(beverage: BeverageResponse) {
        println("[BeverageListScreen] Deleting beverage id=${beverage.id}")
        scope.launch {
            try {
                api.deleteBeverage(beverage.id)
                beverages = beverages.filter { it.id != beverage.id }
                println("[BeverageListScreen] Deleted successfully")
            } catch (e: Exception) {
                println("[BeverageListScreen] Delete error: ${e.message}")
                errorMessage = e.message
            }
        }
    }

    LaunchedEffect(Unit) {
        loadBeverages()
        delay(100)
        isContentVisible = true
    }

    // Filter beverages by name
    val filteredBeverages = if (filterText.isBlank()) {
        beverages
    } else {
        beverages.filter { it.name.contains(filterText, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = isContentVisible,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(AppAnimations.DURATION_NORMAL, easing = AppAnimations.EaseOutQuart)
                ) + fadeIn()
            ) {
                BeverageTopBar(
                    filterText = filterText,
                    onFilterChange = { filterText = it },
                    onRefresh = { loadBeverages(currentPage) },
                    onCreateNew = onNavigateToCreate
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isContentVisible && totalPages > 1,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(AppAnimations.DURATION_NORMAL, easing = AppAnimations.EaseOutQuart)
                ) + fadeIn()
            ) {
                BeveragePagination(
                    currentPage = currentPage,
                    totalPages = totalPages,
                    onPrev = { if (currentPage > 0) loadBeverages(currentPage - 1) },
                    onNext = { if (currentPage < totalPages - 1) loadBeverages(currentPage + 1) }
                )
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = isContentVisible,
                enter = scaleIn(animationSpec = tween(AppAnimations.DURATION_NORMAL)) + fadeIn()
            ) {
                BeverageCreateFAB(onClick = onNavigateToCreate)
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                        )
                    )
                )
        ) {
            when {
                isLoading -> BeverageLoadingContent()
                errorMessage != null -> BeverageErrorContent(
                    message = errorMessage ?: "Unknown error",
                    onRetry = { loadBeverages(currentPage) }
                )
                filteredBeverages.isEmpty() -> BeverageEmptyState()
                else -> {
                    AnimatedVisibility(
                        visible = isContentVisible,
                        enter = fadeIn(animationSpec = tween(AppAnimations.DURATION_ENTRANCE))
                    ) {
                        BeverageList(
                            beverages = filteredBeverages,
                            onView = onNavigateToView,
                            onEdit = onNavigateToEdit,
                            onDelete = { showDeleteDialog = it }
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    showDeleteDialog?.let { beverage ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon = {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            },
            title = { Text("Delete Beverage") },
            text = { 
                Text("Are you sure you want to delete '${beverage.name}'? This action cannot be undone.") 
            },
            confirmButton = {
                Button(
                    onClick = {
                        deleteBeverage(beverage)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun BeverageTopBar(
    filterText: String,
    onFilterChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onCreateNew: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocalDrink,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column {
                    Text(
                        "Beverages",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Manage your drinks menu",
                        style = ExtendedTypography.caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = filterText,
                    onValueChange = onFilterChange,
                    singleLine = true,
                    placeholder = { Text("Search beverages...", style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.widthIn(min = 200.dp, max = 280.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )
                
                val interactionSource = remember { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()
                val scale by animateFloatAsState(
                    targetValue = if (isHovered) 1.1f else 1f,
                    animationSpec = tween(AppAnimations.DURATION_FAST)
                )
                
                FilledTonalIconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(44.dp)
                        .hoverable(interactionSource)
                        .graphicsLayer { scaleX = scale; scaleY = scale },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun BeverageCreateFAB(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.1f else 1f,
        animationSpec = tween(AppAnimations.DURATION_FAST)
    )
    
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = Modifier
            .hoverable(interactionSource)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(imageVector = Icons.Default.Add, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Add Beverage", style = ExtendedTypography.buttonText)
    }
}

@Composable
private fun BeverageList(
    beverages: List<BeverageResponse>,
    onView: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (BeverageResponse) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(beverages) { index, beverage ->
            var isVisible by remember { mutableStateOf(false) }
            
            LaunchedEffect(Unit) {
                delay(index * 50L)
                isVisible = true
            }
            
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(AppAnimations.DURATION_NORMAL)) +
                    slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = tween(AppAnimations.DURATION_NORMAL, easing = AppAnimations.EaseOutQuart)
                    )
            ) {
                BeverageCard(
                    beverage = beverage,
                    onView = { onView(beverage.id) },
                    onEdit = { onEdit(beverage.id) },
                    onDelete = { onDelete(beverage) }
                )
            }
        }
        
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun BeverageCard(
    beverage: BeverageResponse,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    val elevation by animateDpAsState(
        targetValue = if (isHovered) 12.dp else 4.dp,
        animationSpec = tween(AppAnimations.DURATION_FAST)
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.01f else 1f,
        animationSpec = tween(AppAnimations.DURATION_FAST)
    )
    
    val df = remember { DecimalFormat("#,##0.##") }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Icon and Name
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocalDrink,
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp).size(28.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Column {
                    Text(
                        text = beverage.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${beverage.prices.size} price option${if (beverage.prices.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Center: Price chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                beverage.prices.take(3).forEach { price ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "৳${df.format(price.price)}/${price.quantity}${price.unit.name.first()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                if (beverage.prices.size > 3) {
                    Text(
                        text = "+${beverage.prices.size - 3}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Right: Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onView) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "View",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun BeveragePagination(
    currentPage: Int,
    totalPages: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onPrev,
                enabled = currentPage > 0,
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.ChevronLeft, null, Modifier.size(18.dp))
                Text("Previous")
            }
            
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "Page ${currentPage + 1} of $totalPages",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            Button(
                onClick = onNext,
                enabled = currentPage < totalPages - 1,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Next")
                Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun BeverageLoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp), color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Loading beverages...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BeverageErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.errorContainer) {
            Icon(Icons.Default.Error, null, Modifier.padding(16.dp).size(40.dp), tint = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Error: $message", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
            Icon(Icons.Outlined.Refresh, null, Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry")
        }
    }
}

@Composable
private fun BeverageEmptyState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Icon(
                imageVector = Icons.Outlined.LocalDrink,
                contentDescription = null,
                modifier = Modifier.padding(24.dp).size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("No Beverages Found", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Add beverages to your menu", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}


