import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.auth.AuthManager
import androidx.compose.ui.graphics.Color

@Composable
fun MainScreen(onLogout: () -> Unit) {
    val authManager = remember { AuthManager() }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Restaurant Management System") },
                actions = {
                    Button(
                        onClick = {
                            authManager.clearToken()
                            onLogout()
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)
                    ) {
                        Text("Logout", color = Color.White)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome to Admin Panel",
                fontSize = 24.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "This is the main dashboard",
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            // Placeholder for future features
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Dashboard Features",
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Text(
                        text = "• Inventory Management\n• Menu Management\n• Order Tracking\n• Reports & Analytics",
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Text(
                        text = "Will implement these features later",
                        fontStyle = MaterialTheme.typography.body2.fontStyle,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}