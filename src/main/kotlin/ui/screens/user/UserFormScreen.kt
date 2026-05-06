package ui.screens.user

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import data.model.RoleResponse
import data.model.UserInformationResponseWithRole
import data.model.UserRegistrationRequest
import data.model.UserUpdateRequest
import data.network.UserApiService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val FormGold = Color(0xFFD4AF37)
private val FormGreen = Color(0xFF16A34A)
private val FormRed = Color(0xFFDC2626)
private val FormDark = Color(0xFF111827)

enum class UserFormMode { CREATE, VIEW, EDIT }

@Composable
fun UserFormScreen(
    api: UserApiService,
    mode: UserFormMode,
    userId: Long? = null,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onSaved: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var roles by remember { mutableStateOf<List<RoleResponse>>(emptyList()) }
    var selectedRoles by remember { mutableStateOf<List<RoleResponse>>(emptyList()) }
    var roleMenuOpen by remember { mutableStateOf(false) }
    var user by remember { mutableStateOf<UserInformationResponseWithRole?>(null) }
    var email by remember { mutableStateOf("") }
    var originalEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var presentAddress by remember { mutableStateOf("") }
    var permanentAddress by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var emailLookupMessage by remember { mutableStateOf<String?>(null) }
    var emailExists by remember { mutableStateOf(false) }
    var checkingEmail by remember { mutableStateOf(false) }

    val editable = mode != UserFormMode.VIEW
    val isCreate = mode == UserFormMode.CREATE
    val emailValid = email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
    val mobileValid = mobile.isNotBlank() && mobile.all(Char::isDigit)
    val ownEmail = email.equals(originalEmail, ignoreCase = true)
    val formValid = emailValid && !emailExists && !checkingEmail && firstName.isNotBlank() &&
        mobileValid && selectedRoles.isNotEmpty() && (!isCreate || password.length >= 6)

    LaunchedEffect(mode, userId) {
        loading = true
        error = null
        try {
            roles = api.getAllRoles()
            if (userId != null) {
                val response = api.getUserById(userId)
                user = response
                email = response.username
                originalEmail = response.username
                firstName = response.firstName
                lastName = response.lastName
                mobile = response.mobileNumber
                presentAddress = response.presetAddress.orEmpty()
                permanentAddress = response.permanentAddress.orEmpty()
                selectedRoles = response.userRoles
            }
        } catch (e: Exception) {
            error = e.message ?: "Failed to load user form"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(email, originalEmail, editable) {
        emailLookupMessage = null
        emailExists = false
        if (!editable || !emailValid || ownEmail) return@LaunchedEffect
        delay(600)
        checkingEmail = true
        try {
            val response = api.lookupEmail(email)
            emailExists = response.exists
            emailLookupMessage = response.message
        } catch (e: Exception) {
            emailLookupMessage = e.message ?: "Could not check email"
        } finally {
            checkingEmail = false
        }
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = FormGold)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            UserFormHeader(
                title = when (mode) {
                    UserFormMode.CREATE -> "Create User"
                    UserFormMode.VIEW -> "View User"
                    UserFormMode.EDIT -> "Edit User"
                },
                subtitle = when (mode) {
                    UserFormMode.CREATE -> "Create account and assign roles"
                    UserFormMode.VIEW -> "Review account information and roles"
                    UserFormMode.EDIT -> "Update user information and role assignment"
                },
                icon = when (mode) {
                    UserFormMode.CREATE -> Icons.Outlined.PersonAdd
                    UserFormMode.VIEW -> Icons.Outlined.Visibility
                    UserFormMode.EDIT -> Icons.Outlined.Edit
                },
                onBack = onBack,
                action = {
                    if (mode == UserFormMode.VIEW && userId != null) {
                        Button(
                            onClick = { onEdit(userId) },
                            colors = ButtonDefaults.buttonColors(containerColor = FormGold, contentColor = FormDark),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Edit, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Edit", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }

        item { error?.let { FormError(it) { error = null } } }

        item {
            FormSection("Account Information", Icons.Outlined.Badge) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    RoundedField(
                        value = email,
                        onChange = { email = it.trim() },
                        label = "Email *",
                        modifier = Modifier.weight(1f),
                        enabled = editable,
                        isError = editable && email.isNotBlank() && (!emailValid || emailExists),
                        helper = when {
                            !editable -> null
                            email.isNotBlank() && !emailValid -> "Enter a valid email"
                            ownEmail && mode == UserFormMode.EDIT -> "Current email"
                            emailExists -> emailLookupMessage ?: "Email already exists"
                            checkingEmail -> "Checking email..."
                            emailLookupMessage != null -> emailLookupMessage
                            else -> null
                        },
                        success = editable && emailValid && !emailExists && !checkingEmail && (ownEmail || emailLookupMessage != null)
                    )
                    if (isCreate) {
                        PasswordField(
                            value = password,
                            onChange = { password = it },
                            visible = showPassword,
                            onVisibleChange = { showPassword = !showPassword },
                            modifier = Modifier.weight(1f),
                            isError = password.isNotBlank() && password.length < 6
                        )
                    } else {
                        ReadOnlyField("Status", if (user?.enabled == true) "Enabled" else "Locked", Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            FormSection("Personal Information", Icons.Outlined.Person) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    RoundedField(firstName, { firstName = it }, "First Name *", Modifier.weight(1f), enabled = editable)
                    RoundedField(lastName, { lastName = it }, "Last Name", Modifier.weight(1f), enabled = editable)
                }
                Spacer(Modifier.height(16.dp))
                RoundedField(
                    value = mobile,
                    onChange = { mobile = it.filter(Char::isDigit) },
                    label = "Mobile Number *",
                    modifier = Modifier.fillMaxWidth(),
                    enabled = editable,
                    isError = editable && mobile.isNotBlank() && !mobileValid,
                    helper = "Only digits are allowed",
                    keyboardType = KeyboardType.Number
                )
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    RoundedField(presentAddress, { presentAddress = it }, "Present Address", Modifier.weight(1f), enabled = editable, singleLine = false)
                    RoundedField(permanentAddress, { permanentAddress = it }, "Permanent Address", Modifier.weight(1f), enabled = editable, singleLine = false)
                }
            }
        }

        item {
            FormSection("Role Assignment", Icons.Outlined.AdminPanelSettings) {
                if (editable) {
                    Box {
                        OutlinedButton(
                            onClick = { roleMenuOpen = true },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (selectedRoles.isEmpty()) "Select Role *" else "Add another role", Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                        DropdownMenu(expanded = roleMenuOpen, onDismissRequest = { roleMenuOpen = false }) {
                            val available = roles.filterNot { role -> selectedRoles.any { it.id == role.id } }
                            if (available.isEmpty()) DropdownMenuItem(text = { Text("No more roles") }, enabled = false, onClick = {})
                            available.forEach { role ->
                                DropdownMenuItem(text = { Text(role.name) }, onClick = {
                                    selectedRoles = selectedRoles + role
                                    roleMenuOpen = false
                                })
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                RoleTable(roles = selectedRoles, editable = editable, onDelete = { selectedRoles = selectedRoles - it })
            }
        }

        if (editable) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onBack, enabled = !saving, modifier = Modifier.height(48.dp), shape = RoundedCornerShape(12.dp)) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(16.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                saving = true
                                error = null
                                try {
                                    if (isCreate) {
                                        api.registerUser(
                                            UserRegistrationRequest(
                                                email = email,
                                                password = password,
                                                firstName = firstName.trim(),
                                                lastName = lastName.trim(),
                                                mobileNumber = mobile,
                                                presetAddress = presentAddress.trim(),
                                                permanentAddress = permanentAddress.trim(),
                                                roleId = selectedRoles.map { it.id }
                                            )
                                        )
                                        onSaved("User created successfully")
                                    } else if (userId != null) {
                                        api.updateUser(
                                            userId,
                                            UserUpdateRequest(
                                                email = email,
                                                firstName = firstName.trim(),
                                                lastName = lastName.trim(),
                                                mobileNumber = mobile,
                                                presetAddress = presentAddress.trim(),
                                                permanentAddress = permanentAddress.trim(),
                                                roleId = selectedRoles.map { it.id }
                                            )
                                        )
                                        onSaved("User updated successfully")
                                    }
                                } catch (e: Exception) {
                                    error = e.message ?: "Failed to save user"
                                } finally {
                                    saving = false
                                }
                            }
                        },
                        enabled = formValid && !saving,
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FormGold, contentColor = FormDark)
                    ) {
                        if (saving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = FormDark)
                        else Icon(Icons.Default.Save, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (saving) "Saving..." else if (isCreate) "Save User" else "Update User", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun UserFormHeader(title: String, subtitle: String, icon: ImageVector, onBack: () -> Unit, action: @Composable () -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
            Surface(shape = RoundedCornerShape(12.dp), color = FormGold.copy(alpha = 0.14f)) {
                Icon(icon, null, Modifier.padding(12.dp).size(28.dp), tint = FormGold)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            action()
        }
    }
}

@Composable
private fun FormSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(icon, null, modifier = Modifier.padding(8.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun RoundedField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    helper: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    success: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = modifier,
        enabled = enabled,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        shape = RoundedCornerShape(12.dp),
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        supportingText = { helper?.let { Text(it, color = if (success) FormGreen else if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) } },
        trailingIcon = { if (success) Icon(Icons.Default.CheckCircle, null, tint = FormGreen) }
    )
}

@Composable
private fun PasswordField(value: String, onChange: (String) -> Unit, visible: Boolean, onVisibleChange: () -> Unit, modifier: Modifier, isError: Boolean) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text("Password *") },
        modifier = modifier,
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        isError = isError,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = { IconButton(onClick = onVisibleChange) { Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } },
        supportingText = { if (isError) Text("Minimum 6 characters") }
    )
}

@Composable
private fun ReadOnlyField(label: String, value: String, modifier: Modifier) {
    OutlinedTextField(value = value, onValueChange = {}, label = { Text(label) }, modifier = modifier, enabled = false, singleLine = true, shape = RoundedCornerShape(12.dp))
}

@Composable
private fun RoleTable(roles: List<RoleResponse>, editable: Boolean, onDelete: (RoleResponse) -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)) {
        Column {
            Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)).padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text("Role Name", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                if (editable) Text("Delete", Modifier.width(80.dp), fontWeight = FontWeight.Bold)
            }
            if (roles.isEmpty()) Text("No role selected", Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            roles.forEach { role ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(role.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    if (editable) IconButton(onClick = { onDelete(role) }, modifier = Modifier.width(80.dp)) { Icon(Icons.Default.Delete, "Delete", tint = FormRed) }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f))
            }
        }
    }
}

@Composable
private fun FormError(message: String, onDismiss: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(10.dp))
            Text(message, Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
        }
    }
}
