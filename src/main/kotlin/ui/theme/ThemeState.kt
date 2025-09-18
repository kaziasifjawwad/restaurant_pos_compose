package ui.theme

import androidx.compose.runtime.mutableStateOf

object ThemeState {
    var isDarkTheme = mutableStateOf(false)
        private set

    fun toggleTheme() {
        isDarkTheme.value = !isDarkTheme.value
    }

    fun setDarkTheme(isDark: Boolean) {
        isDarkTheme.value = isDark
    }
}
