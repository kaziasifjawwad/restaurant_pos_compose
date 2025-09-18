package data.auth

import androidx.compose.runtime.mutableStateOf
import data.model.LoginResponse

object UserState {
    var currentUser = mutableStateOf<LoginResponse?>(null)
        private set

    fun login(user: LoginResponse) {
        currentUser.value = user
    }

    fun logout() {
        currentUser.value = null
    }

    fun isLoggedIn(): Boolean {
        return currentUser.value != null
    }
}
