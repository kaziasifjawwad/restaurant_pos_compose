package ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Restaurant
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
import ui.theme.AppAnimations

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantTopAppBar(
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit = {},
    onThemeToggle: (Boolean) -> Unit = {},
    onLogoutClick: () -> Unit = {},
    showSidebarToggle: Boolean = false
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left section: Menu toggle and branding
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sidebar toggle button
                if (showSidebarToggle) {
                    AnimatedIconButton(
                        onClick = onMenuClick,
                        icon = Icons.Filled.Menu,
                        contentDescription = "Toggle Sidebar"
                    )
                }
                
                // Logo and title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // App Icon
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Restaurant,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Column {
                        Text(
                            text = "Restaurant POS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Management System",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Right section: Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Theme Toggle
                ThemeToggle(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    onThemeToggle = onThemeToggle,
                    showLabel = false
                )
                
                // Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(32.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                
                // Logout Button with hover effect
                LogoutButton(onClick = onLogoutClick)
                
                // User Profile
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = "User Profile",
                        modifier = Modifier
                            .padding(4.dp)
                            .size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedIconButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isHovered) 
            MaterialTheme.colorScheme.surfaceVariant 
        else 
            MaterialTheme.colorScheme.surface,
        animationSpec = tween(AppAnimations.DURATION_FAST)
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.1f else 1f,
        animationSpec = tween(AppAnimations.DURATION_FAST)
    )
    
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .hoverable(interactionSource)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(10.dp),
        color = backgroundColor
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun LogoutButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isHovered) 
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f) 
        else 
            MaterialTheme.colorScheme.surface,
        animationSpec = tween(AppAnimations.DURATION_FAST)
    )
    
    val iconTint by animateColorAsState(
        targetValue = if (isHovered) 
            MaterialTheme.colorScheme.error 
        else 
            MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(AppAnimations.DURATION_FAST)
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.1f else 1f,
        animationSpec = tween(AppAnimations.DURATION_FAST)
    )
    
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .hoverable(interactionSource)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(10.dp),
        color = backgroundColor
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = Icons.Filled.ExitToApp,
                contentDescription = "Logout",
                tint = iconTint
            )
        }
    }
}

@Composable
fun CompactTopAppBar(
    modifier: Modifier = Modifier,
    onThemeToggle: (Boolean) -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Restaurant,
                        contentDescription = null,
                        modifier = Modifier.padding(6.dp).size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "Restaurant POS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeToggle(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    onThemeToggle = onThemeToggle,
                    showLabel = false
                )

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = "User",
                        modifier = Modifier
                            .padding(4.dp)
                            .size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
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
