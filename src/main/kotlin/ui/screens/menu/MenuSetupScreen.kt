package ui.screens.menu

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import data.model.*
import data.network.MenuApiService
import kotlinx.coroutines.launch
import ui.theme.AppAnimations
import ui.theme.ExtendedTypography

/**
 * Menu Setup Screen - Tree view with CRUD operations
 */
@Composable
fun MenuSetupScreen() {
    val api = remember { MenuApiService() }
    val scope = rememberCoroutineScope()
    
    var menuList by remember { mutableStateOf<List<MenuTreeNode>>(emptyList()) }
    var roles by remember { mutableStateOf<List<RoleResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchValue by remember { mutableStateOf("") }
    
    // Dialog states
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedParent by remember { mutableStateOf<MenuTreeNode?>(null) }
    var selectedMenu by remember { mutableStateOf<MenuResponse?>(null) }
    
    // Form state
    var menuName by remember { mutableStateOf("") }
    var menuPath by remember { mutableStateOf("") }
    var menuIcon by remember { mutableStateOf("") }
    var menuOrder by remember { mutableStateOf(0) }
    var localizationCode by remember { mutableStateOf("") }
    var showInMenuList by remember { mutableStateOf(true) }
    var isSubMenu by remember { mutableStateOf(false) }
    var menuStatus by remember { mutableStateOf(true) }
    var selectedRoles by remember { mutableStateOf<List<Long>>(emptyList()) }
    
    fun loadMenus() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val menus = api.getAllMenus()
                menuList = menus.map { menu ->
                    MenuTreeNode(
                        key = menu.id.toString(),
                        title = "${menu.name} ${menu.path?.let { "($it)" } ?: ""}",
                        origin = menu,
                        children = menu.children.map { child ->
                            MenuTreeNode(
                                key = child.id.toString(),
                                title = "${child.name} ${child.path?.let { "($it)" } ?: ""}",
                                origin = child
                            )
                        },
                        restrictedToRoles = menu.restrictedToRoles.map { it.id }
                    )
                }
            } catch (e: Exception) {
                errorMessage = e.message
            } finally {
                isLoading = false
            }
        }
    }
    
    fun loadRoles() {
        scope.launch {
            try {
                roles = api.getAllRoles()
            } catch (e: Exception) {
                println("Error loading roles: ${e.message}")
            }
        }
    }
    
    fun resetForm() {
        menuName = ""
        menuPath = ""
        menuIcon = ""
        menuOrder = 0
        localizationCode = ""
        showInMenuList = true
        isSubMenu = false
        menuStatus = true
        selectedRoles = emptyList()
        selectedParent = null
        selectedMenu = null
    }
    
    fun addMenu() {
        if (menuName.isBlank()) return
        
        scope.launch {
            isLoading = true
            try {
                val menuRequest = MenuRequest(
                    name = menuName,
                    path = menuPath.takeIf { it.isNotBlank() },
                    iconSrc = menuIcon.takeIf { it.isNotBlank() },
                    menuOrder = menuOrder,
                    localizationCode = localizationCode.takeIf { it.isNotBlank() },
                    showInMenuList = showInMenuList,
                    status = true,
                    parentId = if (isSubMenu) selectedParent?.origin?.id else null,
                    restrictedToRoles = selectedRoles.map { roleId ->
                        roles.find { it.id == roleId }?.let { RoleRequest(roleName = it.name) }
                    }.filterNotNull()
                )
                
                api.createMenu(menuRequest)
                loadMenus()
                showAddDialog = false
                resetForm()
            } catch (e: Exception) {
                errorMessage = e.message
            } finally {
                isLoading = false
            }
        }
    }
    
    fun editMenu() {
        if (menuName.isBlank() || selectedMenu == null) return
        
        scope.launch {
            isLoading = true
            try {
                val menuRequest = MenuRequest(
                    id = selectedMenu!!.id,
                    name = menuName,
                    path = menuPath.takeIf { it.isNotBlank() },
                    iconSrc = menuIcon.takeIf { it.isNotBlank() },
                    menuOrder = menuOrder,
                    localizationCode = localizationCode.takeIf { it.isNotBlank() },
                    showInMenuList = showInMenuList,
                    status = menuStatus,
                    parentId = selectedMenu!!.parentId,
                    restrictedToRoles = selectedRoles.map { roleId ->
                        roles.find { it.id == roleId }?.let { RoleRequest(roleName = it.name) }
                    }.filterNotNull()
                )
                
                api.updateMenu(menuRequest)
                loadMenus()
                showEditDialog = false
                resetForm()
            } catch (e: Exception) {
                errorMessage = e.message
            } finally {
                isLoading = false
            }
        }
    }
    
    fun deleteMenu(menuId: Long) {
        scope.launch {
            isLoading = true
            try {
                api.deleteMenu(menuId)
                loadMenus()
            } catch (e: Exception) {
                errorMessage = e.message
            } finally {
                isLoading = false
            }
        }
    }
    
    fun openEditDialog(node: MenuTreeNode) {
        scope.launch {
            isLoading = true
            try {
                val menu = api.getMenuById(node.origin.id)
                selectedMenu = menu
                menuName = menu.name
                menuPath = menu.path ?: ""
                menuIcon = menu.iconSrc ?: ""
                menuOrder = menu.menuOrder
                localizationCode = menu.localizationCode ?: ""
                showInMenuList = menu.showInMenuList
                menuStatus = menu.status
                selectedRoles = menu.restrictedToRoles.map { it.id }
                showEditDialog = true
            } catch (e: Exception) {
                errorMessage = e.message
            } finally {
                isLoading = false
            }
        }
    }
    
    LaunchedEffect(Unit) {
        loadMenus()
        loadRoles()
    }
    
    // Filter menus by search
    val filteredMenus = remember(menuList, searchValue) {
        if (searchValue.isBlank()) {
            menuList
        } else {
            menuList.filter { 
                it.title.contains(searchValue, ignoreCase = true) ||
                it.children.any { child -> child.title.contains(searchValue, ignoreCase = true) }
            }
        }
    }
    
    Scaffold(
        topBar = {
            MenuSetupTopBar(
                searchValue = searchValue,
                onSearchChange = { searchValue = it }
            )
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (errorMessage != null) {
                    ErrorContent(
                        message = errorMessage ?: "Unknown error",
                        onRetry = { loadMenus() }
                    )
                } else {
                    // Menu tree
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredMenus) { node ->
                                MenuTreeItem(
                                    node = node,
                                    onExpand = { /* Toggle expansion */ },
                                    onEdit = { openEditDialog(it) },
                                    onDelete = { deleteMenu(it.origin.id) },
                                    onAddChild = {
                                        selectedParent = it
                                        isSubMenu = true
                                        showAddDialog = true
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Add button
                    Button(
                        onClick = {
                            isSubMenu = false
                            showAddDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (menuList.isEmpty()) "Add Menu" else "Add New")
                    }
                }
            }
        }
    }
    
    // Add Dialog
    if (showAddDialog) {
        MenuFormDialog(
            title = "Add Menu",
            menuName = menuName,
            onMenuNameChange = { menuName = it },
            menuPath = menuPath,
            onMenuPathChange = { menuPath = it },
            menuIcon = menuIcon,
            onMenuIconChange = { menuIcon = it },
            menuOrder = menuOrder,
            onMenuOrderChange = { menuOrder = it },
            localizationCode = localizationCode,
            onLocalizationCodeChange = { localizationCode = it },
            showInMenuList = showInMenuList,
            onShowInMenuListChange = { showInMenuList = it },
            isSubMenu = isSubMenu,
            onIsSubMenuChange = { isSubMenu = it },
            selectedRoles = selectedRoles,
            onSelectedRolesChange = { selectedRoles = it },
            roles = roles,
            parentName = selectedParent?.origin?.name,
            showStatus = false,
            menuStatus = true,
            onMenuStatusChange = {},
            isLoading = isLoading,
            onConfirm = { addMenu() },
            onDismiss = {
                showAddDialog = false
                resetForm()
            }
        )
    }
    
    // Edit Dialog
    if (showEditDialog) {
        MenuFormDialog(
            title = "Edit Menu",
            menuName = menuName,
            onMenuNameChange = { menuName = it },
            menuPath = menuPath,
            onMenuPathChange = { menuPath = it },
            menuIcon = menuIcon,
            onMenuIconChange = { menuIcon = it },
            menuOrder = menuOrder,
            onMenuOrderChange = { menuOrder = it },
            localizationCode = localizationCode,
            onLocalizationCodeChange = { localizationCode = it },
            showInMenuList = showInMenuList,
            onShowInMenuListChange = { showInMenuList = it },
            isSubMenu = false,
            onIsSubMenuChange = {},
            selectedRoles = selectedRoles,
            onSelectedRolesChange = { selectedRoles = it },
            roles = roles,
            parentName = null,
            showStatus = true,
            menuStatus = menuStatus,
            onMenuStatusChange = { menuStatus = it },
            menuCode = selectedMenu?.menuCode,
            isLoading = isLoading,
            onConfirm = { editMenu() },
            onDismiss = {
                showEditDialog = false
                resetForm()
            }
        )
    }
}

@Composable
private fun MenuSetupTopBar(
    searchValue: String,
    onSearchChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Menu,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column {
                    Text(
                        "Menu Setup",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Manage application menus",
                        style = ExtendedTypography.caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            OutlinedTextField(
                value = searchValue,
                onValueChange = onSearchChange,
                singleLine = true,
                placeholder = { Text("Search") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null
                    )
                },
                modifier = Modifier.width(200.dp),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
private fun MenuTreeItem(
    node: MenuTreeNode,
    onExpand: (MenuTreeNode) -> Unit,
    onEdit: (MenuTreeNode) -> Unit,
    onDelete: (MenuTreeNode) -> Unit,
    onAddChild: (MenuTreeNode) -> Unit
) {
    var expanded by remember { mutableStateOf(node.isExpanded) }
    
    Column {
        // Parent node
        MenuNodeRow(
            title = node.title,
            isParent = node.children.isNotEmpty(),
            isExpanded = expanded,
            onExpand = { expanded = !expanded },
            onEdit = { onEdit(node) },
            onDelete = { onDelete(node) },
            onAddChild = if (node.children.isEmpty()) null else { { onAddChild(node) } }
        )
        
        // Children
        if (expanded && node.children.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(start = 32.dp)
            ) {
                node.children.forEach { child ->
                    MenuNodeRow(
                        title = child.title,
                        isParent = false,
                        isExpanded = false,
                        onExpand = {},
                        onEdit = { onEdit(child) },
                        onDelete = { onDelete(child) },
                        onAddChild = null
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuNodeRow(
    title: String,
    isParent: Boolean,
    isExpanded: Boolean,
    onExpand: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddChild: (() -> Unit)?
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource),
        color = if (isHovered) 
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else 
            MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Expand/collapse icon
                if (isParent) {
                    IconButton(
                        onClick = onExpand,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Text(
                            text = if (isExpanded) "−" else "+",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(24.dp))
                }
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isParent) FontWeight.SemiBold else FontWeight.Normal
                )
            }
            
            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (onAddChild != null) {
                    IconButton(onClick = onAddChild, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add child",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Error,
            null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Error: $message", color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}
