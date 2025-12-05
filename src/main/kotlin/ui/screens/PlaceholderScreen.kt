package ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.Refresh
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.model.MenuItem
import kotlinx.coroutines.delay
import ui.theme.AppAnimations
import ui.theme.ExtendedTypography

@Composable
fun PlaceholderScreen(
    menuItem: MenuItem? = null,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    )
                )
            )
    ) {
        // Decorative background elements
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopEnd)
                .offset(x = 100.dp, y = (-50).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated icon with pulse effect
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(AppAnimations.DURATION_ENTRANCE)) +
                    scaleIn(
                        initialScale = 0.8f,
                        animationSpec = tween(AppAnimations.DURATION_ENTRANCE, easing = AppAnimations.EaseOutQuart)
                    )
            ) {
                val infiniteTransition = rememberInfiniteTransition()
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.05f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
                
                Surface(
                    modifier = Modifier
                        .size(100.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        },
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shadowElevation = 8.dp
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Construction,
                        contentDescription = "Under Construction",
                        modifier = Modifier
                            .padding(24.dp)
                            .size(52.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Title with animation
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(AppAnimations.DURATION_ENTRANCE, delayMillis = 100)) +
                    slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = tween(AppAnimations.DURATION_ENTRANCE, delayMillis = 100, easing = AppAnimations.EaseOutQuart)
                    )
            ) {
                Text(
                    text = "Coming Soon",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(AppAnimations.DURATION_ENTRANCE, delayMillis = 200))
            ) {
                Text(
                    text = menuItem?.let {
                        "The ${it.name} module is currently under development.\nThis feature will be available in a future update."
                    } ?: "This feature is currently under development.\nPlease check back later for updates.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Menu Item Details Card
            if (menuItem != null) {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(AppAnimations.DURATION_ENTRANCE, delayMillis = 300)) +
                        slideInVertically(
                            initialOffsetY = { it / 3 },
                            animationSpec = tween(AppAnimations.DURATION_ENTRANCE, delayMillis = 300, easing = AppAnimations.EaseOutQuart)
                        )
                ) {
                    MenuItemDetailsCard(menuItem = menuItem)
                }
            }
        }
    }
}

@Composable
private fun MenuItemDetailsCard(menuItem: MenuItem) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    val elevation by animateDpAsState(
        targetValue = if (isHovered) 16.dp else 6.dp,
        animationSpec = tween(AppAnimations.DURATION_FAST)
    )
    
    Card(
        modifier = Modifier
            .widthIn(max = 400.dp)
            .hoverable(interactionSource)
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(20.dp),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "Module Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Details grid
            DetailRow(label = "Name", value = menuItem.name)
            DetailRow(label = "Code", value = menuItem.menuCode)
            DetailRow(label = "Path", value = menuItem.path ?: "N/A")
            DetailRow(label = "Sub-modules", value = "${menuItem.children.size} items", isLast = true)
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    isLast: Boolean = false
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (!isLast) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun LoadingScreen(
    message: String = "Loading...",
    modifier: Modifier = Modifier
) {
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
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Custom loading indicator
        Box(contentAlignment = Alignment.Center) {
            // Outer glow
            val glowAlpha by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 0.5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
            
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
            
            CircularProgressIndicator(
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer { rotationZ = rotation },
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ErrorScreen(
    errorMessage: String,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Error icon with shake effect
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn() + scaleIn(initialScale = 0.8f)
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                shadowElevation = 8.dp
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = "Error",
                    modifier = Modifier
                        .padding(20.dp)
                        .size(40.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(delayMillis = 100)) +
                slideInVertically(initialOffsetY = { it / 4 })
        ) {
            Text(
                text = "Something Went Wrong",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(delayMillis = 200))
        ) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Retry button
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(delayMillis = 300)) +
                scaleIn(initialScale = 0.9f)
        ) {
            val interactionSource = remember { MutableInteractionSource() }
            val isHovered by interactionSource.collectIsHoveredAsState()
            
            val scale by animateFloatAsState(
                targetValue = if (isHovered) 1.05f else 1f,
                animationSpec = tween(AppAnimations.DURATION_FAST)
            )
            
            Button(
                onClick = onRetry,
                modifier = Modifier
                    .hoverable(interactionSource)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Try Again",
                    style = ExtendedTypography.buttonText
                )
            }
        }
    }
}
