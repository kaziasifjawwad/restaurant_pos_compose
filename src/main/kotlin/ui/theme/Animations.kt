package ui.theme

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Animation constants and utilities for the Restaurant POS app
 */
object AppAnimations {
    // Duration constants
    const val DURATION_INSTANT = 100
    const val DURATION_FAST = 200
    const val DURATION_NORMAL = 300
    const val DURATION_SLOW = 500
    const val DURATION_ENTRANCE = 400
    
    // Easing curves
    val EaseOutQuart = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)
    val EaseInOutQuart = CubicBezierEasing(0.76f, 0f, 0.24f, 1f)
    val EaseOutBack = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
    val EaseOutExpo = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    
    // Standard animation specs
    val fastTween = tween<Float>(DURATION_FAST, easing = EaseOutQuart)
    val normalTween = tween<Float>(DURATION_NORMAL, easing = EaseOutQuart)
    val slowTween = tween<Float>(DURATION_SLOW, easing = EaseOutQuart)
    
    // For color transitions
    fun <T> colorTween(durationMillis: Int = DURATION_NORMAL) = tween<T>(
        durationMillis = durationMillis,
        easing = LinearEasing
    )
    
    // Spring animations
    val gentleSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
    
    val snappySpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
}

/**
 * Modifier for hover scale effect on desktop
 */
fun Modifier.hoverScale(
    normalScale: Float = 1f,
    hoverScale: Float = 1.02f,
    pressedScale: Float = 0.98f
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> pressedScale
            isHovered -> hoverScale
            else -> normalScale
        },
        animationSpec = tween(AppAnimations.DURATION_FAST, easing = AppAnimations.EaseOutQuart)
    )
    
    this
        .hoverable(interactionSource)
        .scale(scale)
}

/**
 * Modifier for hover elevation effect
 */
fun Modifier.hoverElevation(
    normalElevation: Dp = 2.dp,
    hoverElevation: Dp = 8.dp,
    shadowColor: Color = Color.Black.copy(alpha = 0.2f)
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    val elevation by animateDpAsState(
        targetValue = if (isHovered) hoverElevation else normalElevation,
        animationSpec = tween(AppAnimations.DURATION_FAST, easing = AppAnimations.EaseOutQuart)
    )
    
    this
        .hoverable(interactionSource)
        .shadow(elevation, spotColor = shadowColor, ambientColor = shadowColor)
}

/**
 * Combined hover effect with scale, elevation and optional background color change
 */
@Composable
fun Modifier.hoverInteraction(
    normalScale: Float = 1f,
    hoverScale: Float = 1.02f,
    normalElevation: Dp = 4.dp,
    hoverElevation: Dp = 12.dp,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
): Modifier {
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.98f
            isHovered -> hoverScale
            else -> normalScale
        },
        animationSpec = tween(AppAnimations.DURATION_FAST, easing = AppAnimations.EaseOutQuart)
    )
    
    val elevation by animateDpAsState(
        targetValue = when {
            isPressed -> normalElevation
            isHovered -> hoverElevation
            else -> normalElevation
        },
        animationSpec = tween(AppAnimations.DURATION_FAST, easing = AppAnimations.EaseOutQuart)
    )
    
    return this
        .hoverable(interactionSource)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.shadowElevation = elevation.toPx()
        }
}

/**
 * Fade and slide entrance animation
 */
@Composable
fun AnimatedEntranceColumn(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(AppAnimations.DURATION_ENTRANCE, easing = AppAnimations.EaseOutQuart)
        ) + slideInVertically(
            initialOffsetY = { it / 4 },
            animationSpec = tween(AppAnimations.DURATION_ENTRANCE, easing = AppAnimations.EaseOutQuart)
        ),
        exit = fadeOut(
            animationSpec = tween(AppAnimations.DURATION_FAST)
        ) + slideOutVertically(
            targetOffsetY = { -it / 4 },
            animationSpec = tween(AppAnimations.DURATION_FAST)
        ),
        modifier = modifier,
        content = content
    )
}

/**
 * Staggered animation delay calculator for list items
 */
fun staggeredDelay(index: Int, baseDelay: Int = 50, maxDelay: Int = 300): Int {
    return minOf(index * baseDelay, maxDelay)
}

/**
 * Pulse animation for loading or attention states
 */
@Composable
fun Modifier.pulseAnimation(
    enabled: Boolean = true,
    minScale: Float = 0.97f,
    maxScale: Float = 1.03f
): Modifier {
    if (!enabled) return this
    
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    return this.scale(scale)
}

/**
 * Shimmer effect for loading states
 */
@Composable
fun Modifier.shimmerEffect(
    enabled: Boolean = true
): Modifier = composed {
    if (!enabled) return@composed this
    
    val infiniteTransition = rememberInfiniteTransition()
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    this.graphicsLayer { alpha = shimmerAlpha }
}

/**
 * Smooth rotation animation
 */
@Composable
fun Modifier.rotateAnimation(
    enabled: Boolean = true,
    durationMs: Int = 1000
): Modifier = composed {
    if (!enabled) return@composed this
    
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    this.graphicsLayer { rotationZ = rotation }
}

/**
 * Content transition specs for screen changes
 */
object ContentTransitions {
    val fadeSlideIn = fadeIn(
        animationSpec = tween(AppAnimations.DURATION_NORMAL, easing = AppAnimations.EaseOutQuart)
    ) + slideInHorizontally(
        initialOffsetX = { it / 6 },
        animationSpec = tween(AppAnimations.DURATION_NORMAL, easing = AppAnimations.EaseOutQuart)
    )
    
    val fadeSlideOut = fadeOut(
        animationSpec = tween(AppAnimations.DURATION_FAST)
    ) + slideOutHorizontally(
        targetOffsetX = { -it / 6 },
        animationSpec = tween(AppAnimations.DURATION_FAST)
    )
    
    val scaleIn = scaleIn(
        initialScale = 0.92f,
        animationSpec = tween(AppAnimations.DURATION_NORMAL, easing = AppAnimations.EaseOutQuart)
    ) + fadeIn(
        animationSpec = tween(AppAnimations.DURATION_NORMAL)
    )
    
    val scaleOut = scaleOut(
        targetScale = 0.92f,
        animationSpec = tween(AppAnimations.DURATION_FAST)
    ) + fadeOut(
        animationSpec = tween(AppAnimations.DURATION_FAST)
    )
}


