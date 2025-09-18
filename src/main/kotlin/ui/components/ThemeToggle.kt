package ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ui.theme.ThemeState

@Composable
fun ThemeToggle(
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    onThemeToggle: (Boolean) -> Unit
) {
    val isDarkTheme = ThemeState.isDarkTheme.value

    Row(
        modifier = modifier.clickable { onThemeToggle(!isDarkTheme) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isDarkTheme) Icons.Filled.Brightness7 else Icons.Filled.Brightness4,
            contentDescription = if (isDarkTheme) "Light Mode" else "Dark Mode",
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )

        if (showLabel) {
            Text(
                text = if (isDarkTheme) "Light Mode" else "Dark Mode",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Switch(
            checked = isDarkTheme,
            onCheckedChange = onThemeToggle,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
fun CompactThemeToggle(
    modifier: Modifier = Modifier
) {
    val isDarkTheme = ThemeState.isDarkTheme.value
    val icon: ImageVector = if (isDarkTheme) Icons.Filled.Brightness7 else Icons.Filled.Brightness4
    val description = if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Mode"

    Icon(
        imageVector = icon,
        contentDescription = description,
        modifier = modifier
            .size(24.dp)
            .clickable { ThemeState.toggleTheme() },
        tint = MaterialTheme.colorScheme.onSurface
    )
}
