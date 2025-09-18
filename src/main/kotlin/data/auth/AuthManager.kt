package data.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

object AuthManager {
    private val appDataDir = File(System.getProperty("user.home"), ".restaurant-pos")
    private val tokenFile = File(appDataDir, "auth_token.txt")
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val _isLoggedIn = MutableStateFlow(tokenFile.exists() && tokenFile.isFile && tokenFile.readText().isNotBlank())
    val isLoggedInState: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    init {
        // Create app directory if it doesn't exist
        if (!appDataDir.exists()) {
            appDataDir.mkdirs()
        }
    }

    fun saveToken(jwtToken: String) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                tokenFile.writeText(jwtToken)
                _isLoggedIn.value = true
            } catch (e: Exception) {
                e.printStackTrace()
                _isLoggedIn.value = false
            }
        }
    }

    fun getToken(): String? {
        return if (tokenFile.exists() && tokenFile.isFile) {
            try {
                tokenFile.readText().takeIf { it.isNotBlank() }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }

    fun clearToken() {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                if (tokenFile.exists()) {
                    tokenFile.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoggedIn.value = false
            }
        }
    }

    @Composable
    fun isLoggedInState(): State<Boolean> {
        return isLoggedInState.collectAsState()
    }
}
