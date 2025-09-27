package ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import data.model.MenuItem
import data.network.ApiService
import kotlinx.coroutines.launch
import ui.components.RestaurantTopAppBar
import ui.components.SidebarMenu
import ui.navigation.NavigationHost
import ui.theme.ThemeState

@Composable
fun MainScreen(onLogout: () -> Unit) {
    var selectedMenuItem by remember { mutableStateOf<MenuItem?>(null) }
    var isSidebarVisible by remember { mutableStateOf(true) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // Initialize ApiService and remember it
    val apiService = remember {
        ApiService()
    }
    
    // Add to the composition's onDispose
    DisposableEffect(apiService) {
        onDispose {
            apiService.close()
        }
    }

    Scaffold(
        topBar = {
            RestaurantTopAppBar(
                onMenuClick = { isSidebarVisible = !isSidebarVisible },
                onThemeToggle = { ThemeState.setDarkTheme(it) },
                onLogoutClick = { showLogoutDialog = true },
                showSidebarToggle = true
            )
        },
        content = { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Sidebar
                if (isSidebarVisible) {
                    SidebarMenu(
                        modifier = Modifier
                            .width(280.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surface),
                        onMenuItemClick = { menuItem ->
                            selectedMenuItem = menuItem
                        }
                    )
                }

                // Main Content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    selectedMenuItem?.let { menuItem ->
                        NavigationHost(
                            currentMenuCode = menuItem.menuCode,
                            currentMenuItem = menuItem
                        )
                    } ?: run {
                        WelcomeScreen()
                    }
                }
            }
        }
    )
    
    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Confirm Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun WelcomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to Restaurant Management System",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Select a menu item from the sidebar to get started",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Quick Stats Cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatCard(
                title = "Inventory",
                value = "0",
                subtitle = "Items",
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            StatCard(
                title = "Menu Items",
                value = "0",
                subtitle = "Available",
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            StatCard(
                title = "Users",
                value = "1",
                subtitle = "Active",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
