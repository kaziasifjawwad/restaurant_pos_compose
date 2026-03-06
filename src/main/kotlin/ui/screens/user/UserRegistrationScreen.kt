package ui.screens.user

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import data.model.RoleResponse
import data.model.UserInformationResponse
import data.model.UserRegistrationRequest
import data.model.UserUpdateRequest
import data.network.UserApiService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.theme.ExtendedTypography

@Composable
fun UserRegistrationScreen() {
    val api = remember { UserApiService() }
    val scope = rememberCoroutineScope()
    
    // Overall Screen State
    var isViewingList by remember { mutableStateOf(true) }
    var editingUserId by remember { mutableStateOf<Long?>(null) }
    
    // Global notification
    var globalSuccessMsg by remember { mutableStateOf<String?>(null) }
    
    // Auto-dismiss success message
    LaunchedEffect(globalSuccessMsg) {
        if (globalSuccessMsg != null) {
            delay(3000)
            globalSuccessMsg = null
        }
    }

    Scaffold(
        topBar = {
            UserRegistrationTopBar(
                isList = isViewingList,
                isEdit = editingUserId != null,
                onBack = {
                    isViewingList = true
                    editingUserId = null
                },
                onCreateClick = {
                    isViewingList = false
                    editingUserId = null
                }
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
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f)
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Global notifications section
                AnimatedVisibility(visible = globalSuccessMsg != null) {
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            shadowElevation = 2.dp
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Text(globalSuccessMsg ?: "", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                // Main Views Flip
                AnimatedContent(
                    targetState = isViewingList,
                    transitionSpec = {
                        if (targetState) {
                            slideInHorizontally(initialOffsetX = { -it }) + fadeIn() togetherWith
                            slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                        } else {
                            slideInHorizontally(initialOffsetX = { it }) + fadeIn() togetherWith
                            slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
                        }
                    }
                ) { showList ->
                    if (showList) {
                        UserListView(
                            api = api,
                            onEditUser = { userId ->
                                editingUserId = userId
                                isViewingList = false
                            }
                        )
                    } else {
                        UserFormView(
                            api = api,
                            userId = editingUserId,
                            onSuccess = { msg ->
                                globalSuccessMsg = msg
                                isViewingList = true
                                editingUserId = null
                            },
                            onCancel = {
                                isViewingList = true
                                editingUserId = null
                            }
                        )
                    }
                }
            }
        }
    }
}

// ======================= User List View =======================

@Composable
private fun UserListView(
    api: UserApiService,
    onEditUser: (Long) -> Unit
) {
    var users by remember { mutableStateOf<List<UserInformationResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                isLoading = true
                val response = api.getAllUsers(page = 0, size = 100) // Getting a large chunk for now, or implement proper pagination later
                users = response.content
            } catch (e: Exception) {
                errorMessage = "Failed to load users: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (errorMessage != null) {
            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
            }
        } else if (users.isEmpty()) {
            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.Group, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text("No users found.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = MaterialTheme.typography.titleMedium.fontSize)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(users, key = { it.id }) { user ->
                    UserCard(user = user, onEditClick = { onEditUser(user.id) })
                }
            }
        }
    }
}

@Composable
private fun UserCard(
    user: UserInformationResponse,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar Placeholder
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = user.firstName.take(1).uppercase() + user.lastName.take(1).uppercase(),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            // Info block
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.fullName.ifBlank { "Unknown Name" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Email, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(user.username, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    Spacer(Modifier.width(16.dp))
                    
                    Icon(Icons.Outlined.Phone, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(user.mobileNumber.ifBlank { "N/A" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            // Status Badge
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (user.enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.padding(end = 16.dp)
            ) {
                Text(
                    text = if (user.enabled) "Active" else "Disabled",
                    color = if (user.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Edit Button
            IconButton(
                onClick = onEditClick,
                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Outlined.Edit, "Edit User")
            }
        }
    }
}

// ======================= User Form View (Create & Edit) =======================

@Composable
private fun UserFormView(
    api: UserApiService,
    userId: Long?,
    onSuccess: (String) -> Unit,
    onCancel: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val isEditMode = userId != null

    // Form fields
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }
    var presetAddress by remember { mutableStateOf("") }
    var permanentAddress by remember { mutableStateOf("") }
    var selectedRoles by remember { mutableStateOf<List<Long>>(emptyList()) }

    // State
    var availableRoles by remember { mutableStateOf<List<RoleResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) } // Loads roles and optionally user payload
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var rolesDropdownExpanded by remember { mutableStateOf(false) }

    // Validations
    val isEmailValid = email.isNotBlank() && "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-zA-Z]{2,}\$".toRegex().matches(email)
    val isPasswordValid = isEditMode || password.length >= 6 // Password optional in edit mode
    val isFirstNameValid = firstName.isNotBlank()
    val isLastNameValid = lastName.isNotBlank()
    val isMobileValid = mobileNumber.isNotBlank() && mobileNumber.all { it.isDigit() || it == '+' || it == '-' }
    
    val isFormValid = isEmailValid && isPasswordValid && isFirstNameValid && 
                      isLastNameValid && isMobileValid && selectedRoles.isNotEmpty()

    LaunchedEffect(userId) {
        scope.launch {
            isLoading = true
            try {
                availableRoles = api.getAllRoles()
                
                if (isEditMode) {
                    val userDetails = api.getUserById(userId!!)
                    email = userDetails.username
                    firstName = userDetails.firstName
                    lastName = userDetails.lastName
                    mobileNumber = userDetails.mobileNumber
                    presetAddress = userDetails.presetAddress ?: ""
                    permanentAddress = userDetails.permanentAddress ?: ""
                    selectedRoles = userDetails.userRoles.map { it.id }
                }
            } catch (e: Exception) {
                errorMessage = "Failed to initialization form: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun submitForm() {
        if (!isFormValid) return
        
        scope.launch {
            isSubmitting = true
            errorMessage = null
            
            try {
                if (isEditMode) {
                    val req = UserUpdateRequest(
                        email = email.trim(),
                        firstName = firstName.trim(),
                        lastName = lastName.trim(),
                        mobileNumber = mobileNumber.trim(),
                        presetAddress = presetAddress.trim(),
                        permanentAddress = permanentAddress.trim(),
                        roleId = selectedRoles
                    )
                    api.updateUser(userId!!, req)
                    onSuccess("User updated successfully!")
                } else {
                    val req = UserRegistrationRequest(
                        email = email.trim(),
                        password = password,
                        firstName = firstName.trim(),
                        lastName = lastName.trim(),
                        mobileNumber = mobileNumber.trim(),
                        presetAddress = presetAddress.trim(),
                        permanentAddress = permanentAddress.trim(),
                        roleId = selectedRoles
                    )
                    val response = api.registerUser(req)
                    onSuccess(response.message.ifBlank { "User registered successfully!" })
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Operation failed"
            } finally {
                isSubmitting = false
            }
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(visible = errorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).widthIn(max = 700.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(12.dp))
                    Text(errorMessage ?: "", color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                    IconButton(onClick = { errorMessage = null }) {
                        Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().widthIn(max = 700.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    "Account Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email *") },
                        isError = email.isNotEmpty() && !isEmailValid,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Outlined.Email, null) },
                        supportingText = { if (email.isNotEmpty() && !isEmailValid) Text("Invalid email address") }
                    )
                    
                    if (!isEditMode) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password *") },
                            isError = password.isNotEmpty() && !isPasswordValid,
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                            trailingIcon = {
                                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(imageVector = image, contentDescription = "Toggle password visibility")
                                }
                            },
                            supportingText = { if (password.isNotEmpty() && !isPasswordValid) Text("Min 6 characters") }
                        )
                    }
                }
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Text(
                    "Personal Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("First Name *") },
                        isError = firstName.isEmpty() && isSubmitting,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Outlined.Person, null) }
                    )
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Last Name *") },
                        isError = lastName.isEmpty() && isSubmitting,
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = mobileNumber,
                    onValueChange = { mobileNumber = it },
                    label = { Text("Mobile Number *") },
                    isError = mobileNumber.isNotEmpty() && !isMobileValid,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Phone, null) }
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = presetAddress,
                        onValueChange = { presetAddress = it },
                        label = { Text("Present Address") },
                        modifier = Modifier.weight(1f),
                        minLines = 2,
                        maxLines = 4,
                        leadingIcon = { Icon(Icons.Outlined.Home, null) }
                    )
                    OutlinedTextField(
                        value = permanentAddress,
                        onValueChange = { permanentAddress = it },
                        label = { Text("Permanent Address") },
                        modifier = Modifier.weight(1f),
                        minLines = 2,
                        maxLines = 4,
                        leadingIcon = { Icon(Icons.Outlined.LocationCity, null) }
                    )
                }
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Text(
                    "Role Assignment",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box {
                        OutlinedTextField(
                            value = if (selectedRoles.isEmpty()) "Select Roles *" else "${selectedRoles.size} role(s) selected",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Outlined.AdminPanelSettings, null) },
                            trailingIcon = {
                                IconButton(onClick = { rolesDropdownExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                            },
                            isError = selectedRoles.isEmpty() && isSubmitting,
                            supportingText = { if (selectedRoles.isEmpty()) Text("At least one role is required") }
                        )
                        
                        DropdownMenu(
                            expanded = rolesDropdownExpanded,
                            onDismissRequest = { rolesDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.8f).heightIn(max = 300.dp)
                        ) {
                            availableRoles.forEach { role ->
                                val isSelected = selectedRoles.contains(role.id)
                                DropdownMenuItem(
                                    text = { Text(role.name) },
                                    onClick = {
                                        selectedRoles = if (isSelected) {
                                            selectedRoles - role.id
                                        } else {
                                            selectedRoles + role.id
                                        }
                                    },
                                    leadingIcon = {
                                        Checkbox(checked = isSelected, onCheckedChange = null)
                                    }
                                )
                            }
                        }
                    }
                    
                    if (selectedRoles.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            selectedRoles.forEach { roleId ->
                                val roleName = availableRoles.find { it.id == roleId }?.name ?: "Unknown"
                                SuggestionChip(
                                    onClick = { selectedRoles = selectedRoles - roleId },
                                    label = { Text(roleName) },
                                    icon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp)) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSubmitting
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { submitForm() },
                        modifier = Modifier.weight(1f).height(52.dp),
                        enabled = isFormValid && !isSubmitting,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(if (isEditMode) "Updating..." else "Registering...")
                        } else {
                            Icon(if (isEditMode) Icons.Default.Save else Icons.Default.PersonAdd, null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (isEditMode) "Update User" else "Register User")
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

// ======================= Top Bar =======================

@Composable
private fun UserRegistrationTopBar(
    isList: Boolean,
    isEdit: Boolean,
    onBack: () -> Unit,
    onCreateClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isList) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back to List")
                }
                Spacer(Modifier.width(12.dp))
            } else {
                Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(
                        imageVector = Icons.Outlined.Group,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.width(12.dp))
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isList) "User Management" else if (isEdit) "Edit User" else "Register User",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isList) "Manage system users and their roles" else "Fill out the information below",
                    style = ExtendedTypography.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isList) {
                Button(
                    onClick = onCreateClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Create User", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
