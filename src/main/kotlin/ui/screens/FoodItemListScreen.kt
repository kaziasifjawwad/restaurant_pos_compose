package ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import data.model.FoodItemResponse
import data.network.FoodItemApiService
import kotlinx.coroutines.delay
import ui.components.FoodCard
import ui.theme.AppAnimations
import ui.theme.ExtendedTypography
import ui.viewmodel.FoodItemState

/**
 * Screen for displaying list of food items with CRUD operations
 */
@Composable
fun FoodItemListScreen(
    onNavigateToCreate: () -> Unit = {},
    onNavigateToView: (Long) -> Unit = {},
    onNavigateToEdit: (Long) -> Unit = {}
) {
    // Use shared API (uses HttpClientProvider internally)
    val api = remember { FoodItemApiService() }
    val state = remember { FoodItemState(api) }
    var isContentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { 
        println("[FoodItemListScreen] Loading initial page")
        state.loadPage(0, 20)
        delay(100)
        isContentVisible = true
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
                TopBar(
                    filterText = state.filterText,
                    onFilterChange = { state.applyFilter(it) },
                    onRefresh = { 
                        println("[FoodItemListScreen] Refreshing...")
                        state.refresh() 
                    },
                    onCreateNew = {
                        println("[FoodItemListScreen] Navigating to create")
                        onNavigateToCreate()
                    }
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isContentVisible,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(AppAnimations.DURATION_NORMAL, easing = AppAnimations.EaseOutQuart)
                ) + fadeIn()
            ) {
                BottomPagination(
                    currentPage = state.currentPage,
                    totalPages = state.totalPages,
                    onPrev = { state.previousPage() },
                    onNext = { state.nextPage() }
                )
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = isContentVisible,
                enter = scaleIn(animationSpec = tween(AppAnimations.DURATION_NORMAL)) + fadeIn()
            ) {
                CreateFAB(onClick = onNavigateToCreate)
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
                state.isLoading -> {
                    LoadingContent()
                }
                state.errorMessage != null -> {
                    ErrorContent(
                        message = state.errorMessage ?: "Unknown error",
                        onRetry = { state.refresh() }
                    )
                }
                else -> {
                    AnimatedVisibility(
                        visible = isContentVisible,
                        enter = fadeIn(animationSpec = tween(AppAnimations.DURATION_ENTRANCE))
                    ) {
                        FoodGrid(
                            items = state.filteredFoodItems(),
                            onView = { id ->
                                println("[FoodItemListScreen] Viewing item id=$id")
                                onNavigateToView(id)
                            },
                            onEdit = { id ->
                                println("[FoodItemListScreen] Editing item id=$id")
                                onNavigateToEdit(id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateFAB(onClick: () -> Unit) {
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
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Create New",
            style = ExtendedTypography.buttonText
        )
    }
}

@Composable
private fun LoadingContent() {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(48.dp)
                .graphicsLayer { rotationZ = rotation },
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 4.dp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Loading food items...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.errorContainer
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier
                    .padding(16.dp)
                    .size(40.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Error: $message",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
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
            // Title with icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column {
                    Text(
                        "Food Items",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Manage your menu items",
                        style = ExtendedTypography.caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Search and actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search field
                OutlinedTextField(
                    value = filterText,
                    onValueChange = onFilterChange,
                    singleLine = true,
                    placeholder = { 
                        Text(
                            "Search by name or serial...",
                            style = MaterialTheme.typography.bodyMedium
                        ) 
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier
                        .widthIn(min = 220.dp, max = 320.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    )
                )
                
                // Refresh button with hover effect
                RefreshButton(onClick = onRefresh)
            }
        }
    }
}

@Composable
private fun RefreshButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    val rotation by animateFloatAsState(
        targetValue = if (isHovered) 45f else 0f,
        animationSpec = tween(AppAnimations.DURATION_FAST)
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.1f else 1f,
        animationSpec = tween(AppAnimations.DURATION_FAST)
    )
    
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .hoverable(interactionSource)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = rotation
            },
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Refresh,
            contentDescription = "Refresh",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun FoodGrid(
    items: List<FoodItemResponse>,
    onView: (Long) -> Unit,
    onEdit: (Long) -> Unit
) {
    if (items.isEmpty()) {
        EmptyState()
        return
    }
    
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
            items.chunked(columns).forEachIndexed { rowIndex, rowItems ->
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for ((index, food) in rowItems.withIndex()) {
                            // Staggered animation for each card
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
                                FoodCard(
                                    foodItem = food,
                                    onView = onView,
                                    onEdit = onEdit
                                )
                            }
                        }
                        repeat(columns - rowItems.size) { 
                            Spacer(Modifier.weight(1f)) 
                        }
                    }
                }
            }
            
            // Bottom padding for FAB
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Icon(
                imageVector = Icons.Default.RestaurantMenu,
                contentDescription = null,
                modifier = Modifier
                    .padding(24.dp)
                    .size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No Food Items Found",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Try adjusting your search or add new items",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BottomPagination(
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
            // Previous button
            PaginationButton(
                onClick = onPrev,
                enabled = currentPage > 0,
                isNext = false
            )
            
            // Page info
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
            
            // Next button
            PaginationButton(
                onClick = onNext,
                enabled = currentPage < totalPages - 1,
                isNext = true
            )
        }
    }
}

@Composable
private fun PaginationButton(
    onClick: () -> Unit,
    enabled: Boolean,
    isNext: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isHovered && enabled) 1.05f else 1f,
        animationSpec = tween(AppAnimations.DURATION_FAST)
    )
    
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .hoverable(interactionSource)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (!isNext) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = if (isNext) "Next" else "Previous",
            style = ExtendedTypography.buttonText
        )
        if (isNext) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
