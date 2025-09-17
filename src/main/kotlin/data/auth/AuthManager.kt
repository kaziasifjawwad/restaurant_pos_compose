package data.auth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class AuthManager {
    private val tokenFile = File("auth_token.txt")
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    private val _isLoggedIn = MutableStateFlow(isLoggedInSync())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    
    fun saveToken(jwtToken: String) {
        coroutineScope.launch {
            tokenFile.writeText(jwtToken)
            _isLoggedIn.value = true
        }
    }
    
    fun getToken(): String? {
        return if (tokenFile.exists() && tokenFile.isFile) {
            tokenFile.readText().takeIf { it.isNotBlank() }
        } else {
            null
        }
    }
    
    fun clearToken() {
        coroutineScope.launch {
            if (tokenFile.exists()) {
                tokenFile.delete()
            }
            _isLoggedIn.value = false
        }
    }
    
    fun isLoggedInSync(): Boolean {
        return getToken() != null
    }
}