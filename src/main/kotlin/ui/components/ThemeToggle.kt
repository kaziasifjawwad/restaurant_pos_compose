package ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import ui.theme.AppAnimations
import ui.theme.ThemeState

@Composable
fun ThemeToggle(
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    onThemeToggle: (Boolean) -> Unit
) {
    val isDarkTheme = ThemeState.isDarkTheme.value
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    // Animated values
    val rotation by animateFloatAsState(
        targetValue = if (isDarkTheme) 180f else 0f,
        animationSpec = tween(AppAnimations.DURATION_NORMAL, easing = AppAnimations.EaseOutQuart)
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.05f else 1f,
        animationSpec = tween(AppAnimations.DURATION_FAST)
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isDarkTheme) 
            MaterialTheme.colorScheme.primaryContainer 
        else 
            MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(AppAnimations.DURATION_NORMAL)
    )

    Row(
        modifier = modifier
            .hoverable(interactionSource)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor.copy(alpha = 0.5f))
            .clickable { onThemeToggle(!isDarkTheme) }
            .padding(8.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Animated icon
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    if (isDarkTheme) 
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    else 
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = isDarkTheme,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(200)) + 
                        scaleIn(initialScale = 0.8f, animationSpec = tween(200)))
                        .togetherWith(
                            fadeOut(animationSpec = tween(200)) + 
                            scaleOut(targetScale = 0.8f, animationSpec = tween(200))
                        )
                }
            ) { dark ->
                Icon(
                    imageVector = if (dark) Icons.Default.DarkMode else Icons.Default.LightMode,
                    contentDescription = if (dark) "Dark Mode" else "Light Mode",
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(rotation),
                    tint = if (dark) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.tertiary
                )
            }
        }

        if (showLabel) {
            AnimatedContent(
                targetState = isDarkTheme,
                transitionSpec = {
                    (slideInVertically { it } + fadeIn())
                        .togetherWith(slideOutVertically { -it } + fadeOut())
                }
            ) { dark ->
                Text(
                    text = if (dark) "Dark" else "Light",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Custom animated switch
        ThemeSwitch(
            checked = isDarkTheme,
            onCheckedChange = onThemeToggle
        )
    }
}

@Composable
private fun ThemeSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = tween(AppAnimations.DURATION_FAST, easing = AppAnimations.EaseOutQuart)
    )
    
    val trackColor by animateColorAsState(
        targetValue = if (checked) 
            MaterialTheme.colorScheme.primary 
        else 
            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        animationSpec = tween(AppAnimations.DURATION_FAST)
    )
    
    val thumbColor by animateColorAsState(
        targetValue = if (checked) 
            MaterialTheme.colorScheme.onPrimary 
        else 
            MaterialTheme.colorScheme.surface,
        animationSpec = tween(AppAnimations.DURATION_FAST)
    )
    
    Box(
        modifier = Modifier
            .width(44.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(trackColor)
            .clickable { onCheckedChange(!checked) }
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(20.dp)
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}

@Composable
fun CompactThemeToggle(
    modifier: Modifier = Modifier
) {
    val isDarkTheme = ThemeState.isDarkTheme.value
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    val rotation by animateFloatAsState(
        targetValue = if (isDarkTheme) 180f else 0f,
        animationSpec = tween(AppAnimations.DURATION_NORMAL, easing = AppAnimations.EaseOutQuart)
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.15f else 1f,
        animationSpec = tween(AppAnimations.DURATION_FAST)
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isHovered) 
            MaterialTheme.colorScheme.surfaceVariant 
        else 
            MaterialTheme.colorScheme.surface,
        animationSpec = tween(AppAnimations.DURATION_FAST)
    )

    Surface(
        onClick = { ThemeState.toggleTheme() },
        modifier = modifier
            .size(36.dp)
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
            AnimatedContent(
                targetState = isDarkTheme,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(200)) + 
                        scaleIn(initialScale = 0.7f, animationSpec = tween(200)))
                        .togetherWith(
                            fadeOut(animationSpec = tween(200)) + 
                            scaleOut(targetScale = 0.7f, animationSpec = tween(200))
                        )
                }
            ) { dark ->
                Icon(
                    imageVector = if (dark) Icons.Default.DarkMode else Icons.Default.LightMode,
                    contentDescription = if (dark) "Switch to Light Mode" else "Switch to Dark Mode",
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(rotation),
                    tint = if (dark) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}
