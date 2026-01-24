package ui.screens.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.AdminPanelSettings
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
fun MenuAssignScreen() {
    val api = remember { MenuApiService() }
    val scope = rememberCoroutineScope()
    
    var roles by remember { mutableStateOf<List<RoleResponse>>(emptyList()) }
    var selectedRole by remember { mutableStateOf<RoleResponse?>(null) }
    var allMenus by remember { mutableStateOf<List<MenuTreeNode>>(emptyList()) }
    var userMenus by remember { mutableStateOf<List<MenuTreeNode>>(emptyList()) }
    var assignedMenuIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Load initial data (roles and all menus)
    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
             try {
                // Fetch roles
                roles = api.getAllRoles()
                if (roles.isNotEmpty()) {
                    selectedRole = roles.first()
                }

                // Fetch all menus structure
                val menus = api.getAllMenus()
                allMenus = mapToTreeNodes(menus)
                
                // If we have a selected role, load its assignments
                selectedRole?.let { role ->
                    loadRoleAssignments(api, role.id) { ids ->
                         assignedMenuIds = ids
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load initial data: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // Effect to reload assignments when role changes
    LaunchedEffect(selectedRole) {
        if (selectedRole != null && allMenus.isNotEmpty()) {
             scope.launch {
                isLoading = true
                try {
                    loadRoleAssignments(api, selectedRole!!.id) { ids ->
                        assignedMenuIds = ids
                    }
                } catch (e: Exception) {
                     errorMessage = "Failed to load role assignments: ${e.message}"
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
                // Prepare request list
                // We need to send ALL assigned menu IDs for this role
                // The API expects a list of MenuRoleRequest objects
                // Based on previous code: updateMenuRoles(List<MenuRoleRequest>)
                
                val requestList = assignedMenuIds.map { menuId ->
                    MenuRoleRequest(
                        menuId = menuId,
                        roleId = selectedRole!!.id,
                        active = true 
                    )
                }
                
                // Note: The API might expect a diff or a full replace. 
                // Usually "update" implies replacing the set or updating statuses.
                // If the backend wipes existing and adds new, this is fine.
                // If it updates existing, we might need to handle unticked ones too if we want to remove them.
                // Assuming typical "save assignments" behavior where we send what's active.
                
                // However, check api.updateMenuRoles signature.
                // It takes List<MenuRoleRequest>.
                // If the backend expects only the changes, or the full state, depends on implementation.
                // Given "updateMenuRoles" name, let's assume it updates the passed records.
                // If we want to REMOVE a role, we might need to send status=false or the backend might handle "missing = deleted".
                // Let's assume for now we send all 'checked' items. 
                
                // Let's check MenuManagementApiService again mentally...
                // It just calls PUT /menu-role.
                
                api.updateMenuRoles(requestList)
                successMessage = "Menu assignments saved successfully"
            } catch (e: Exception) {
                errorMessage = "Failed to save assignments: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun toggleMenu(menuId: Long, isChecked: Boolean) {
        assignedMenuIds = if (isChecked) {
            assignedMenuIds + menuId
        } else {
            assignedMenuIds - menuId
        }
    }

    Scaffold(
        topBar = {
            MenuAssignTopBar()
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { handleSave() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Save, "Save Assignments")
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
                // Menu Tree
                Card(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                         Text(
                            "Menu Access",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(allMenus) { node ->
                                MenuCheckItem(
                                    node = node,
                                    assignedIds = assignedMenuIds,
                                    onToggle = { id, checked -> toggleMenu(id, checked) }
                                )
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

// Helper to loading assigments
suspend fun loadRoleAssignments(api: MenuApiService, roleId: Long, onResult: (Set<Long>) -> Unit) {
    val assignments = api.getMenuRolesByRoleId(roleId)
    val ids = assignments.filter { it.active }.map { it.menuId }.toSet()
    onResult(ids)
}

// Helper to map MenuResponse to MenuTreeNode
fun mapToTreeNodes(menus: List<MenuResponse>): List<MenuTreeNode> {
    return menus.map { menu ->
        MenuTreeNode(
            key = menu.id.toString(),
            title = menu.name,
            origin = menu,
            children = mapToTreeNodes(menu.children),
            restrictedToRoles = menu.restrictedToRoles.map { it.id }
        )
    }
}


@Composable
fun MenuAssignTopBar() {
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
                    imageVector = Icons.Outlined.AdminPanelSettings,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    "Menu Assignment",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Assign menus to user roles",
                    style = ExtendedTypography.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MenuCheckItem(
    node: MenuTreeNode,
    assignedIds: Set<Long>,
    onToggle: (Long, Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(true) } // Default expanded to see submenus
    val isChecked = assignedIds.contains(node.origin.id)
    
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            // Expand toggle
            if (node.children.isNotEmpty()) {
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        if (expanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                        null
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(24.dp))
            }
            
            // Checkbox
            Checkbox(
                checked = isChecked,
                onCheckedChange = { onToggle(node.origin.id, it) }
            )
            
            Text(
                text = node.title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
        }
        
        if (expanded && node.children.isNotEmpty()) {
            Column(modifier = Modifier.padding(start = 32.dp)) {
                node.children.forEach { child ->
                    MenuCheckItem(
                        node = child,
                        assignedIds = assignedIds,
                        onToggle = onToggle
                    )
                }
            }
        }
    }
}
