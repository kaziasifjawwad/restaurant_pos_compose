package ui.screens.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.LockPerson
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import data.model.*
import data.network.MenuApiService
import kotlinx.coroutines.launch
import ui.theme.ExtendedTypography

@Composable
fun MenuPermissionsScreen() {
    val api = remember { MenuApiService() }
    val scope = rememberCoroutineScope()
    
    var roles by remember { mutableStateOf<List<RoleResponse>>(emptyList()) }
    var selectedRole by remember { mutableStateOf<RoleResponse?>(null) }
    var allPermissionsMap by remember { mutableStateOf<Map<String, List<PermissionResponse>>>(emptyMap()) }
    var assignedPermissionIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Load initial data
    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
             try {
                roles = api.getAllRoles()
                if (roles.isNotEmpty()) {
                    selectedRole = roles.first()
                }

                allPermissionsMap = api.getAllPermissions()
                
                selectedRole?.let { role ->
                    loadRolePermissions(api, role.id) { ids ->
                         assignedPermissionIds = ids
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load initial data: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // Effect to reload when role changes
    LaunchedEffect(selectedRole) {
        if (selectedRole != null && allPermissionsMap.isNotEmpty()) {
             scope.launch {
                isLoading = true
                try {
                    loadRolePermissions(api, selectedRole!!.id) { ids ->
                        assignedPermissionIds = ids
                    }
                } catch (e: Exception) {
                     errorMessage = "Failed to load role permissions: ${e.message}"
                } finally {
                    isLoading = false
                }
             }
        }
    }

    fun handleSave() {
        if (selectedRole == null) return
        
        scope.launch {
            isLoading = true
            errorMessage = null
            successMessage = null
            try {
                val requestList = assignedPermissionIds.map { permId ->
                    PermissionRoleRequest(
                        permissionId = permId,
                        roleId = selectedRole!!.id,
                        active = true 
                    )
                }
                
                api.updatePermissionRoles(requestList)
                successMessage = "Permissions saved successfully"
            } catch (e: Exception) {
                errorMessage = "Failed to save permissions: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun togglePermission(permId: Long, isChecked: Boolean) {
        assignedPermissionIds = if (isChecked) {
            assignedPermissionIds + permId
        } else {
            assignedPermissionIds - permId
        }
    }

    Scaffold(
        topBar = {
            PermissionAssignTopBar()
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { handleSave() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Save, "Save Permissions")
            }
        }
    ) { paddingValues ->
        Column(
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
                .padding(16.dp)
        ) {
            // Role Selector
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Select Role",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (roles.isNotEmpty()) {
                        ScrollableTabRow(
                            selectedTabIndex = roles.indexOf(selectedRole).coerceAtLeast(0),
                            edgePadding = 0.dp,
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary,
                            indicator = { tabPositions ->
                                val index = roles.indexOf(selectedRole).coerceAtLeast(0)
                                if (index < tabPositions.size) {
                                    TabRowDefaults.SecondaryIndicator(
                                        Modifier.tabIndicatorOffset(tabPositions[index])
                                    )
                                }
                            }
                        ) {
                            roles.forEach { role ->
                                Tab(
                                    selected = selectedRole == role,
                                    onClick = { selectedRole = role },
                                    text = { Text(role.name) }
                                )
                            }
                        }
                    } else if (!isLoading) {
                        Text(
                            "No roles found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // Permissions List
                Card(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                         Text(
                            "Permissions Access",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            allPermissionsMap.forEach { (groupName, permissions) ->
                                item {
                                    Column {
                                        Text(
                                            groupName, 
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                                        permissions.forEach { perm ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                            ) {
                                                Checkbox(
                                                    checked = assignedPermissionIds.contains(perm.id),
                                                    onCheckedChange = { togglePermission(perm.id, it) }
                                                )
                                                Column {
                                                    Text(perm.name, style = MaterialTheme.typography.bodyMedium)
                                                    perm.localizationCode?.let { code ->
                                                        if (code.isNotBlank()) {
                                                            Text(code, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Messages
            errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            successMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

suspend fun loadRolePermissions(api: MenuApiService, roleId: Long, onResult: (Set<Long>) -> Unit) {
    val assignments = api.getPermissionRolesByRoleId(roleId)
    val ids = assignments.filter { it.active }.map { it.permissionId }.toSet()
    onResult(ids)
}


@Composable
fun PermissionAssignTopBar() {
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
                    imageVector = Icons.Outlined.LockPerson,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    "Permission Assignment",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Assign permissions to user roles",
                    style = ExtendedTypography.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
