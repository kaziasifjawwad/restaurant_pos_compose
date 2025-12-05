package ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.theme.AppAnimations

/**
 * Toast message types with corresponding colors and icons
 */
enum class ToastType {
    SUCCESS, ERROR, WARNING, INFO
}

/**
 * Data class for toast messages
 */
data class ToastMessage(
    val message: String,
    val type: ToastType = ToastType.INFO,
    val durationMs: Long = 3000L
)

/**
 * State holder for toast messages
 */
class ToastState {
    var currentToast by mutableStateOf<ToastMessage?>(null)
        private set
    
    var isVisible by mutableStateOf(false)
        private set
    
    suspend fun showToast(message: String, type: ToastType = ToastType.INFO, durationMs: Long = 3000L) {
        currentToast = ToastMessage(message, type, durationMs)
        isVisible = true
        delay(durationMs)
        isVisible = false
        delay(300) // Wait for exit animation
        currentToast = null
    }
    
    fun dismiss() {
        isVisible = false
    }
}

@Composable
fun rememberToastState(): ToastState {
    return remember { ToastState() }
}

/**
 * Modern, themed toast host that displays toast messages
 */
@Composable
fun ToastHost(
    toastState: ToastState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = toastState.isVisible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(AppAnimations.DURATION_NORMAL, easing = AppAnimations.EaseOutQuart)
            ) + fadeIn(
                animationSpec = tween(AppAnimations.DURATION_NORMAL)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(AppAnimations.DURATION_FAST)
            ) + fadeOut(
                animationSpec = tween(AppAnimations.DURATION_FAST)
            )
        ) {
            toastState.currentToast?.let { toast ->
                ToastContent(
                    message = toast.message,
                    type = toast.type,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ToastContent(
    message: String,
    type: ToastType,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (type) {
        ToastType.SUCCESS -> MaterialTheme.colorScheme.tertiaryContainer
        ToastType.ERROR -> MaterialTheme.colorScheme.errorContainer
        ToastType.WARNING -> MaterialTheme.colorScheme.secondaryContainer
        ToastType.INFO -> MaterialTheme.colorScheme.primaryContainer
    }
    
    val contentColor = when (type) {
        ToastType.SUCCESS -> MaterialTheme.colorScheme.onTertiaryContainer
        ToastType.ERROR -> MaterialTheme.colorScheme.onErrorContainer
        ToastType.WARNING -> MaterialTheme.colorScheme.onSecondaryContainer
        ToastType.INFO -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    
    val icon: ImageVector = when (type) {
        ToastType.SUCCESS -> Icons.Default.CheckCircle
        ToastType.ERROR -> Icons.Default.Error
        ToastType.WARNING -> Icons.Default.Warning
        ToastType.INFO -> Icons.Default.Info
    }
    
    val accentColor = when (type) {
        ToastType.SUCCESS -> MaterialTheme.colorScheme.tertiary
        ToastType.ERROR -> MaterialTheme.colorScheme.error
        ToastType.WARNING -> MaterialTheme.colorScheme.secondary
        ToastType.INFO -> MaterialTheme.colorScheme.primary
    }
    
    Surface(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = Color.Black.copy(alpha = 0.25f)
            )
            .clip(RoundedCornerShape(12.dp)),
        color = backgroundColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .widthIn(min = 200.dp, max = 400.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )
            
            // Icon
            Icon(
                imageVector = icon,
                contentDescription = type.name,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
            
            // Message
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

/**
 * Simplified toast for quick error messages (backwards compatible)
 */
@Composable
fun SimpleToast(
    message: String,
    isVisible: Boolean,
    type: ToastType = ToastType.ERROR,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(3000)
            onDismiss()
        }
    }
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(AppAnimations.DURATION_NORMAL, easing = AppAnimations.EaseOutQuart)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(AppAnimations.DURATION_FAST)
            ) + fadeOut()
        ) {
            ToastContent(
                message = message,
                type = type,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

/**
 * Extension function for showing toast in a coroutine scope
 */
suspend fun ToastState.showSuccess(message: String) = showToast(message, ToastType.SUCCESS)
suspend fun ToastState.showError(message: String) = showToast(message, ToastType.ERROR)
suspend fun ToastState.showWarning(message: String) = showToast(message, ToastType.WARNING)
suspend fun ToastState.showInfo(message: String) = showToast(message, ToastType.INFO)
