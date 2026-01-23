package ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val columns = when {
            maxWidth < 500.dp -> 1
            maxWidth < 900.dp -> 2
            maxWidth < 1300.dp -> 3
            else -> 4
        }
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            beverages.chunked(columns).forEachIndexed { rowIndex, rowItems ->
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for ((index, beverage) in rowItems.withIndex()) {
                            var isVisible by remember { mutableStateOf(false) }
                            
                            LaunchedEffect(Unit) {
                                delay((rowIndex * columns + index) * 50L)
                                isVisible = true
                            }
                            
                            AnimatedVisibility(
                                visible = isVisible,
                                enter = fadeIn(animationSpec = tween(AppAnimations.DURATION_NORMAL)) +
                                    slideInVertically(
                                        initialOffsetY = { it / 4 },
                                        animationSpec = tween(AppAnimations.DURATION_NORMAL, easing = AppAnimations.EaseOutQuart)
                                    ),
                                modifier = Modifier.weight(1f)
                            ) {
                                BeverageCard(
                                    beverage = beverage,
                                    onView = { onView(beverage.id) },
                                    onEdit = { onEdit(beverage.id) },
                                    onDelete = { onDelete(beverage) }
                                )
                            }
                        }
                        repeat(columns - rowItems.size) { 
                            Spacer(Modifier.weight(1f)) 
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
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
        targetValue = if (isHovered) 16.dp else 4.dp,
        animationSpec = tween(AppAnimations.DURATION_FAST)
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.02f else 1f,
        animationSpec = tween(AppAnimations.DURATION_FAST)
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isHovered) 
            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        else 
            Color.Transparent,
        animationSpec = tween(AppAnimations.DURATION_FAST)
    )
    
    val df = remember { DecimalFormat("#,##0.##") }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .hoverable(interactionSource)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(16.dp),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with gradient background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Beverage icon
                    Icon(
                        imageVector = Icons.Outlined.LocalDrink,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            // Beverage name
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = beverage.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Divider with subtle gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .padding(horizontal = 16.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.outlineVariant,
                                Color.Transparent
                            )
                        )
                    )
            )

            // Price table
            BeveragePriceTable(
                prices = beverage.prices,
                df = df,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
            
            // Action Buttons
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            
            BeverageActionButtonsRow(
                onView = onView,
                onEdit = onEdit,
                onDelete = onDelete
            )
        }
    }
}

@Composable
private fun BeveragePriceTable(
    prices: List<data.model.BeveragePrice>,
    df: DecimalFormat,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text(
                text = "SIZE",
                modifier = Modifier.weight(1f),
                style = ExtendedTypography.overline,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Text(
                text = "PRICE",
                modifier = Modifier.weight(1f),
                style = ExtendedTypography.overline,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.End
            )
        }

        // Divider line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        )
        
        Spacer(modifier = Modifier.height(6.dp))

        // Body
        if (prices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No prices available",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(prices) { price ->
                    BeveragePriceRow(price = price, df = df)
                }
            }
        }
    }
}

@Composable
private fun BeveragePriceRow(
    price: data.model.BeveragePrice,
    df: DecimalFormat
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isHovered) 
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) 
        else 
            Color.Transparent,
        animationSpec = tween(AppAnimations.DURATION_FAST)
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource)
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Size with badge styling - aligned to left with weight
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
            ) {
                Text(
                    text = "${price.quantity}${price.unit.name.first()}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
        
        // Price with emphasis - aligned to right with weight
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = "৳${df.format(price.price)}",
                style = ExtendedTypography.priceTag,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun BeverageActionButtonsRow(
    onView: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // View Button
        BeverageActionButton(
            onClick = onView,
            icon = Icons.Default.Visibility,
            label = "View",
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f)
        )
        
        // Edit Button
        BeverageActionButton(
            onClick = onEdit,
            icon = Icons.Default.Edit,
            label = "Edit",
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f)
        )
        
        // Delete Button
        BeverageActionButton(
            onClick = onDelete,
            icon = Icons.Default.Delete,
            label = "Delete",
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BeverageActionButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.05f else 1f,
        animationSpec = tween(AppAnimations.DURATION_FAST)
    )
    
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
            .height(36.dp)
            .hoverable(interactionSource)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
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


