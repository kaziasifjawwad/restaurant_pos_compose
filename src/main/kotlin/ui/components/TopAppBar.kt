package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantTopAppBar(
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit = {},
    onThemeToggle: (Boolean) -> Unit = {},
    onLogoutClick: () -> Unit = {},
    showSidebarToggle: Boolean = false
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "Restaurant Management System",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            if (showSidebarToggle) {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = "Menu"
                    )
                }
            }
        },
        actions = {
            // Theme Toggle
            ThemeToggle(
                modifier = Modifier
                    .padding(horizontal = 8.dp),
                onThemeToggle = onThemeToggle,
                showLabel = false
            )

            // User Info and Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Logout Button
                IconButton(
                    onClick = onLogoutClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ExitToApp,
                        contentDescription = "Logout",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // User Profile
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = "User Profile",
                    modifier = Modifier
                        .size(32.dp)
                        .padding(end = 8.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        modifier = modifier,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun CompactTopAppBar(
    modifier: Modifier = Modifier,
    onThemeToggle: (Boolean) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Restaurant POS",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThemeToggle(
                modifier = Modifier
                    .padding(horizontal = 8.dp),
                onThemeToggle = onThemeToggle,
                showLabel = false
            )

            Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = "User",
                modifier = Modifier
                    .size(24.dp)
                    .padding(start = 8.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ResponsiveTopAppBar(
    modifier: Modifier = Modifier,
    screenWidth: Int,
    onMenuClick: () -> Unit = {},
    onThemeToggle: (Boolean) -> Unit = {},
    onLogoutClick: () -> Unit = {},
    showSidebarToggle: Boolean = false
) {
    // Use full app bar for wider screens, compact for narrower screens
    if (screenWidth >= 1024) {
        RestaurantTopAppBar(
            modifier = modifier,
            onMenuClick = onMenuClick,
            onThemeToggle = onThemeToggle,
            onLogoutClick = onLogoutClick,
            showSidebarToggle = showSidebarToggle
        )
    } else {
        CompactTopAppBar(
            modifier = modifier,
            onThemeToggle = onThemeToggle
        )
    }
}
