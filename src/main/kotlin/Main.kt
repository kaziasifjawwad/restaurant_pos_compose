import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import data.auth.AuthManager

@Composable
fun App() {
    val authManager = remember { AuthManager() }
    var isLoggedIn by remember { mutableStateOf(authManager.isLoggedInSync()) }

    if (isLoggedIn) {
        MainScreen(
            onLogout = {
                isLoggedIn = false
            }
        )
    } else {
        LoginScreen(
            onLoginSuccess = {
                isLoggedIn = true
            }
        )
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Restaurant Management System"
    ) {
        App()
    }
}
