package ui.screens.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import data.model.PermissionRequest
import data.model.PermissionResponse
import data.network.MenuApiService
import kotlinx.coroutines.launch
import ui.theme.ExtendedTypography

@Composable
fun MenuPermissionTypesScreen() {
    val api = remember { MenuApiService() }
    val scope = rememberCoroutineScope()
    
    var permissionsMap by remember { mutableStateOf<Map<String, List<PermissionResponse>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    var showAddDialog by remember { mutableStateOf(false) }

    fun loadPermissions() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                permissionsMap = api.getAllPermissions()
            } catch (e: Exception) {
                errorMessage = "Failed to load permissions: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadPermissions()
    }

    Scaffold(
        topBar = {
            PermissionTypesTopBar()
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "Add Permission")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                        )
                    )
                )
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    permissionsMap.forEach { (groupName, permissions) ->
                        item {
                            PermissionGroupCard(groupName, permissions)
                        }
                    }
                }
            }
        }
    }
    
    if (showAddDialog) {
        PermissionAddDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { group, name, code ->
                scope.launch {
                    val req = PermissionRequest(
                        groupName = group,
                        name = name,
                        localizationCode = code
                    )
                    try {
                        api.createPermission(req)
                        loadPermissions()
                        showAddDialog = false
                    } catch (e: Exception) {
                        // Handle error (maybe show in dialog)
                        println("Error creating permission: ${e.message}")
                    }
                }
            }
        )
    }
}

@Composable
fun PermissionTypesTopBar() {
     Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = Icons.Outlined.Security,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    "Permission Types",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Manage system permission definitions",
                    style = ExtendedTypography.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PermissionGroupCard(groupName: String, permissions: List<PermissionResponse>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = groupName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            permissions.forEach { perm ->
                Row(
                   modifier = Modifier
                       .fillMaxWidth()
                       .padding(vertical = 4.dp),
                   horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(perm.name, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        perm.localizationCode ?: "", 
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionAddDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Add Permission Type",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Permission Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Localization Code") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { 
                            if (groupName.isNotBlank() && name.isNotBlank()) {
                                onConfirm(groupName, name, code)
                            }
                        },
                        enabled = groupName.isNotBlank() && name.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
