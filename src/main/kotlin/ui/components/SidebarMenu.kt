package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import data.model.MenuItem
import data.network.ApiService
import kotlinx.coroutines.launch

@Composable
fun SidebarMenu(
    modifier: Modifier = Modifier,
    onMenuItemClick: (MenuItem) -> Unit = {}
) {
    var menuItems by remember { mutableStateOf<List<MenuItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

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
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
    ) {
        Text(
            text = "Navigation",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp),
            color = MaterialTheme.colorScheme.onSurface
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else if (error != null) {
            Text(
                text = error!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(menuItems) { menuItem ->
                    MenuItemView(
                        menuItem = menuItem,
                        onItemClick = onMenuItemClick
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuItemView(
    menuItem: MenuItem,
    onItemClick: (MenuItem) -> Unit,
    level: Int = 0
) {
    var isExpanded by remember { mutableStateOf(false) }
    val hasChildren = menuItem.children.isNotEmpty()
    val icon = getIconForMenuCode(menuItem.menuCode)

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (hasChildren) {
                        isExpanded = !isExpanded
                    } else {
                        onItemClick(menuItem)
                    }
                }
                .padding(
                    start = (16 + level * 16).dp,
                    top = 8.dp,
                    bottom = 8.dp,
                    end = 16.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 8.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = menuItem.name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (hasChildren) {
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ArrowDropDown else Icons.Filled.ArrowForward,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isExpanded && hasChildren) {
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

private fun getIconForMenuCode(menuCode: String): ImageVector? {
    return when (menuCode) {
        "INVENTORY", "INGREDIENTS" -> Icons.Filled.Storage
        "FOOD_INFORMATION", "BEVERAGE" -> Icons.Filled.RestaurantMenu
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
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
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
                Icon(
                    imageVector = icon,
                    contentDescription = menuItem.name,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 12.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column {
                Text(
                    text = menuItem.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (menuItem.children.isNotEmpty()) {
                    Text(
                        text = "${menuItem.children.size} sub-items",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
