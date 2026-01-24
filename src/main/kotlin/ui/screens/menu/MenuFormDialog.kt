package ui.screens.menu

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import data.model.RoleResponse

@Composable
fun MenuFormDialog(
    title: String,
    menuName: String,
    onMenuNameChange: (String) -> Unit,
    menuPath: String,
    onMenuPathChange: (String) -> Unit,
    menuIcon: String,
    onMenuIconChange: (String) -> Unit,
    menuOrder: Int,
    onMenuOrderChange: (Int) -> Unit,
    localizationCode: String,
    onLocalizationCodeChange: (String) -> Unit,
    showInMenuList: Boolean,
    onShowInMenuListChange: (Boolean) -> Unit,
    isSubMenu: Boolean,
    onIsSubMenuChange: (Boolean) -> Unit,
    selectedRoles: List<Long>,
    onSelectedRolesChange: (List<Long>) -> Unit,
    roles: List<RoleResponse>,
    parentName: String? = null,
    showStatus: Boolean = false,
    menuStatus: Boolean = true,
    onMenuStatusChange: (Boolean) -> Unit = {},
    menuCode: String? = null,
    isLoading: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Form content
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Name
                    OutlinedTextField(
                        value = menuName,
                        onValueChange = onMenuNameChange,
                        label = { Text("Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = menuName.isBlank()
                    )
                    
                    // Icon
                    OutlinedTextField(
                        value = menuIcon,
                        onValueChange = onMenuIconChange,
                        label = { Text("Icon Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    // Path
                    OutlinedTextField(
                        value = menuPath,
                        onValueChange = onMenuPathChange,
                        label = { Text("Path") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Order
                        OutlinedTextField(
                            value = menuOrder.toString(),
                            onValueChange = { 
                                it.toIntOrNull()?.let { value -> 
                                    if (value in 0..999) onMenuOrderChange(value)
                                }
                            },
                            label = { Text("Order") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        
                        // Status (only in edit mode)
                        if (showStatus) {
                            var expanded by remember { mutableStateOf(false) }
                            @OptIn(ExperimentalMaterial3Api::class)
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = if (menuStatus) "Active" else "Inactive",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Status") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Active") },
                                        onClick = {
                                            onMenuStatusChange(true)
                                            expanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Inactive") },
                                        onClick = {
                                            onMenuStatusChange(false)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    // Localization Code
                    OutlinedTextField(
                        value = localizationCode,
                        onValueChange = onLocalizationCodeChange,
                        label = { Text("Localization Code") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    // Restrict to Roles
                    MultiSelectRolesField(
                        selectedRoles = selectedRoles,
                        onSelectedRolesChange = onSelectedRolesChange,
                        roles = roles
                    )
                    
                    // Parent info (if submenu)
                    if (isSubMenu && parentName != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = "Parent: $parentName",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    
                    // Checkboxes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Checkbox(
                                checked = showInMenuList,
                                onCheckedChange = onShowInMenuListChange
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Show in Menu List")
                        }
                        
                        if (!showStatus) { // Only show in add mode
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Checkbox(
                                    checked = isSubMenu,
                                    onCheckedChange = onIsSubMenuChange
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Is it SubMenu?")
                            }
                        }
                    }
                    
                    // Menu Code (only in edit mode)
                    if (menuCode != null) {
                        Text(
                            text = "Code: $menuCode",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading && menuName.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MultiSelectRolesField(
    selectedRoles: List<Long>,
    onSelectedRolesChange: (List<Long>) -> Unit,
    roles: List<RoleResponse>
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column {
        OutlinedTextField(
            value = if (selectedRoles.isEmpty()) "" else "${selectedRoles.size} role(s) selected",
            onValueChange = {},
            readOnly = true,
            label = { Text("Restrict to Roles") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.ArrowDropDown,
                        contentDescription = null
                    )
                }
            }
        )
        
        if (expanded) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp)
                ) {
                    roles.forEach { role ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedRoles.contains(role.id),
                                onCheckedChange = { checked ->
                                    onSelectedRolesChange(
                                        if (checked) {
                                            selectedRoles + role.id
                                        } else {
                                            selectedRoles - role.id
                                        }
                                    )
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(role.name)
                        }
                    }
                }
            }
        }
    }
}
