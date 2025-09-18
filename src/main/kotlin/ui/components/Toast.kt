package ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun ToastHost(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.padding(16.dp),
            snackbar = { data ->
                Snackbar(
                    backgroundColor = Color.Red.copy(alpha = 0.9f),
                    contentColor = Color.White,
                    content = {
                        Text(
                            text = data.message,
                            style = androidx.compose.material.MaterialTheme.typography.body2
                        )
                    }
                )
            }
        )
    }
}

@Composable
fun rememberToastState(): SnackbarHostState {
    return remember { SnackbarHostState() }
}

@Composable
fun ShowToast(
    message: String,
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(message) {
        if (message.isNotBlank()) {
            scope.launch {
                snackbarHostState.showSnackbar(message = message)
            }
        }
    }
}