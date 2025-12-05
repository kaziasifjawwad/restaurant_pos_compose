package ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FoodBank
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalDining
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import data.model.MenuItem
import data.network.ApiService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.theme.AppAnimations
import ui.theme.ExtendedTypography

@Composable
fun SidebarMenu(
    modifier: Modifier = Modifier,
    onMenuItemClick: (MenuItem) -> Unit = {},
    selectedMenuCode: String? = null
) {
    var menuItems by remember { mutableStateOf<List<MenuItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isVisible by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { ApiService() }

    LaunchedEffect(Unit) {
        isLoading = true
        coroutineScope.launch {
            try {
                val result = apiService.getMenu()
                if (result.isSuccess) {
                    menuItems = result.getOrThrow()
                    error = null
                } else {
                    error = "Failed to load menu: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                error = "Failed to load menu: ${e.message}"
            } finally {
                isLoading = false
                delay(100) // Small delay for smooth entrance
                isVisible = true
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .shadow(
                elevation = 8.dp,
                spotColor = Color.Black.copy(alpha = 0.1f)
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
                )
            )
    ) {
        // Sidebar Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Restaurant,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = "Navigation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Select a module",
                        style = ExtendedTypography.caption,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
        
        // Divider with gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    )
                )
        )

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "Loading menu...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    itemsIndexed(menuItems) { index, menuItem ->
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(
                                animationSpec = tween(
                                    durationMillis = AppAnimations.DURATION_NORMAL,
                                    delayMillis = index * 50
                                )
                            ) + slideInHorizontally(
                                initialOffsetX = { -it / 2 },
                                animationSpec = tween(
                                    durationMillis = AppAnimations.DURATION_NORMAL,
                                    delayMillis = index * 50,
                                    easing = AppAnimations.EaseOutQuart
                                )
                            )
                        ) {
                            MenuItemView(
                                menuItem = menuItem,
                                onItemClick = onMenuItemClick,
                                isSelected = menuItem.menuCode == selectedMenuCode
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuItemView(
    menuItem: MenuItem,
    onItemClick: (MenuItem) -> Unit,
    isSelected: Boolean = false,
    level: Int = 0
) {
    var isExpanded by remember { mutableStateOf(false) }
    val hasChildren = menuItem.children.isNotEmpty()
    val icon = getIconForMenuCode(menuItem.menuCode)
    
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    // Animated states
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer
            isHovered -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            else -> Color.Transparent
        },
        animationSpec = tween(AppAnimations.DURATION_FAST)
    )
    
    val indicatorWidth by animateDpAsState(
        targetValue = if (isSelected) 4.dp else 0.dp,
        animationSpec = tween(AppAnimations.DURATION_FAST)
    )
    
    val contentColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
            isHovered -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(AppAnimations.DURATION_FAST)
    )
    
    val iconRotation by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        animationSpec = tween(AppAnimations.DURATION_FAST, easing = AppAnimations.EaseOutQuart)
    )
    
    val elevation by animateFloatAsState(
        targetValue = if (isHovered && !isSelected) 2f else 0f,
        animationSpec = tween(AppAnimations.DURATION_FAST)
    )

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .clip(RoundedCornerShape(10.dp))
                .graphicsLayer { shadowElevation = elevation }
                .background(backgroundColor)
                .hoverable(interactionSource)
                .clickable {
                    if (hasChildren) {
                        isExpanded = !isExpanded
                    } else {
                        onItemClick(menuItem)
                    }
                }
                .padding(
                    start = (12 + level * 16).dp,
                    top = 12.dp,
                    bottom = 12.dp,
                    end = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection indicator
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .width(indicatorWidth)
                        .height(24.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            // Icon
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else contentColor
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            // Text
            Text(
                text = menuItem.name,
                style = if (isSelected) ExtendedTypography.sidebarItemSelected else ExtendedTypography.sidebarItem,
                modifier = Modifier.weight(1f),
                color = contentColor
            )

            // Expand/collapse indicator
            if (hasChildren) {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(iconRotation),
                    tint = contentColor.copy(alpha = 0.6f)
                )
            }
        }

        // Children with animated visibility
        AnimatedVisibility(
            visible = isExpanded && hasChildren,
            enter = expandVertically(
                animationSpec = tween(AppAnimations.DURATION_NORMAL, easing = AppAnimations.EaseOutQuart)
            ) + fadeIn(
                animationSpec = tween(AppAnimations.DURATION_NORMAL)
            ),
            exit = shrinkVertically(
                animationSpec = tween(AppAnimations.DURATION_FAST)
            ) + fadeOut(
                animationSpec = tween(AppAnimations.DURATION_FAST)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp)
            ) {
                menuItem.children.forEach { childItem ->
                    MenuItemView(
                        menuItem = childItem,
                        onItemClick = onItemClick,
                        level = level + 1
                    )
                }
            }
        }
    }
}

private fun getIconForMenuCode(menuCode: String): ImageVector? {
    return when (menuCode) {
        "INVENTORY", "INGREDIENTS" -> Icons.Outlined.Inventory2
        "FOOD_INFORMATION", "FOOD_ITEM" -> Icons.Outlined.FoodBank
        "BEVERAGE" -> Icons.Outlined.LocalDining
        else -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuItemCard(
    menuItem: MenuItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    val elevation by animateDpAsState(
        targetValue = if (isHovered) 8.dp else 2.dp,
        animationSpec = tween(AppAnimations.DURATION_FAST)
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.02f else 1f,
        animationSpec = tween(AppAnimations.DURATION_FAST, easing = AppAnimations.EaseOutQuart)
    )
    
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .hoverable(interactionSource)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(elevation, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = getIconForMenuCode(menuItem.menuCode)
            if (icon != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = menuItem.name,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = menuItem.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (menuItem.children.isNotEmpty()) {
                    Text(
                        text = "${menuItem.children.size} sub-items",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}
