import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import data.auth.AuthManager
import ui.screens.MainScreen
import ui.theme.AppTheme

@Composable
fun App() {
    val isLoggedIn by AuthManager.isLoggedInState()

    AppTheme {
        if (isLoggedIn) {
            MainScreen(
                onLogout = {
                    AuthManager.clearToken()
                }
            )
        } else {
            LoginScreen(
                onLoginSuccess = { /* No need to set state here, AuthManager will handle it */ }
            )
        }
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
