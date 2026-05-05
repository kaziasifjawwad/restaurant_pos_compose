package ui.screens.dashboard.pro

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import kotlin.math.pow

@Composable
fun dashboardChartColors(): List<Color> {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.45f
    return if (isDark) {
        listOf(
            Color(0xFFFFC83D),
            Color(0xFF5CE1E6),
            Color(0xFFFF8A3D),
            Color(0xFF8AE66E),
            Color(0xFFFF5D8F),
            Color(0xFFB28DFF)
        )
    } else {
        listOf(
            Color(0xFFB77900),
            Color(0xFF007C89),
            Color(0xFFD35400),
            Color(0xFF2E7D32),
            Color(0xFFC2185B),
            Color(0xFF6A1B9A)
        )
    }
}

@Composable
fun statusColor(status: String, fallbackIndex: Int): Color {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.45f
    return when (status.uppercase()) {
        "PAID" -> if (isDark) Color(0xFF63E66D) else Color(0xFF2E7D32)
        "CANCELED", "CANCELLED" -> if (isDark) Color(0xFFFF6B6B) else Color(0xFFC62828)
        "BILL_PRINTED" -> if (isDark) Color(0xFF5CE1E6) else Color(0xFF007C89)
        "ORDER_PLACED" -> if (isDark) Color(0xFFFFC83D) else Color(0xFFB77900)
        else -> dashboardChartColors()[fallbackIndex % dashboardChartColors().size]
    }
}

private fun Color.luminance(): Float {
    fun channel(value: Float): Float =
        if (value <= 0.03928f) {
            value / 12.92f
        } else {
            (((value + 0.055f) / 1.055f).toDouble().pow(2.4)).toFloat()
        }
    return 0.2126f * channel(red) + 0.7152f * channel(green) + 0.0722f * channel(blue)
}
