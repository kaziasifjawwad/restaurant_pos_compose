package ui.screens.user

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import data.model.RoleResponse
import data.model.UserInformationResponse
import data.model.UserInformationResponseWithRole
import data.model.UserRegistrationRequest
import data.network.UserApiService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.theme.ExtendedTypography

private val UserGold = Color(0xFFD4AF37)
private val UserGreen = Color(0xFF16A34A)
private val UserRed = Color(0xFFDC2626)
private val TextDark = Color(0xFF111827)

private enum class UserScreenMode { LIST, CREATE }
private enum class LockFilter(val title: String, val value: Boolean?) {
    ALL("All Users", null), LOCKED("Locked", true), UNLOCKED("Unlocked", false)
}

@Composable
fun UserRegistrationScreen() {
    val api = remember { UserApiService() }
    var mode by remember { mutableStateOf(UserScreenMode.LIST) }
    var toast by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            UserTopBar(
                isCreate = mode == UserScreenMode.CREATE,
                onBack = { mode = UserScreenMode.LIST },
                onCreate = { mode = UserScreenMode.CREATE }
            )
        }
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
                    )
                )
        ) {
            when (mode) {
                UserScreenMode.LIST -> UserDashboard(api, { mode = UserScreenMode.CREATE }, { toast = it })
                UserScreenMode.CREATE -> UserCreateForm(
                    api = api,
                    onCancel = { mode = UserScreenMode.LIST },
                    onCreated = {
                        toast = it
                        mode = UserScreenMode.LIST
                    }
                )
            }
            toast?.let { UserToast(it, Modifier.align(Alignment.BottomCenter)) { toast = null } }
        }
    }
}

@Composable
private fun UserTopBar(isCreate: Boolean, onBack: () -> Unit, onCreate: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isCreate) IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                Surface(shape = RoundedCornerShape(14.dp), color = UserGold.copy(alpha = 0.14f)) {
                    Icon(
                        if (isCreate) Icons.Outlined.PersonAdd else Icons.Outlined.ManageAccounts,
                        null,
                        modifier = Modifier.padding(12.dp).size(28.dp),
                        tint = UserGold
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(if (isCreate) "Create User" else "User Management", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text(
                        if (isCreate) "Create account and assign roles" else "Search, view, lock and unlock system users",
                        style = ExtendedTypography.caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!isCreate) {
                Button(
                    onClick = onCreate,
                    colors = ButtonDefaults.buttonColors(containerColor = UserGold, contentColor = TextDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Create User", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun UserDashboard(api: UserApiService, onCreate: () -> Unit, onMessage: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var users by remember { mutableStateOf<List<UserInformationResponse>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var emailFilter by remember { mutableStateOf("") }
    var mobileFilter by remember { mutableStateOf("") }
    var lockFilter by remember { mutableStateOf(LockFilter.ALL) }
    var selectedUser by remember { mutableStateOf<UserInformationResponseWithRole?>(null) }
    var viewLoading by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(0) }
    var pageSize by remember { mutableStateOf(10) }
    var totalPages by remember { mutableStateOf(0) }
    var totalElements by remember { mutableStateOf(0L) }

    fun loadUsers(pageOverride: Int? = null) {
        scope.launch {
            val targetPage = pageOverride ?: currentPage
            loading = true
            error = null
            try {
                val response = api.getAllUsers(
                    page = targetPage,
                    size = pageSize,
                    lockType = lockFilter.value,
                    email = emailFilter,
                    mobileNumber = mobileFilter
                )
                users = response.content
                currentPage = response.number
                totalPages = response.totalPages
                totalElements = response.totalElements
            } catch (e: Exception) {
                error = e.message ?: "Failed to load users"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadUsers(0) }
    LaunchedEffect(pageSize) { loadUsers(0) }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        UserFilterPanel(
            email = emailFilter,
            mobile = mobileFilter,
            lockFilter = lockFilter,
            onEmailChange = { emailFilter = it },
            onMobileChange = { mobileFilter = it.filter(Char::isDigit) },
            onLockChange = {
                lockFilter = it
                loadUsers(0)
            },
            onApply = { loadUsers(0) },
            onReset = {
                emailFilter = ""
                mobileFilter = ""
                lockFilter = LockFilter.ALL
                loadUsers(0)
            }
        )
        UserStatsRow(users, totalElements)
        AnimatedVisibility(error != null) { ErrorCard(error ?: "", onDismiss = { error = null }) }
        UserTableCard(
            users = users,
            loading = loading,
            currentPage = currentPage,
            totalPages = totalPages,
            pageSize = pageSize,
            totalElements = totalElements,
            onRefresh = { loadUsers() },
            onPageChange = { loadUsers(it) },
            onPageSizeChange = { pageSize = it },
            onView = { id ->
                scope.launch {
                    viewLoading = true
                    try { selectedUser = api.getUserById(id) }
                    catch (e: Exception) { onMessage(e.message ?: "Failed to load user") }
                    finally { viewLoading = false }
                }
            },
            onToggleLock = { user ->
                scope.launch {
                    val lockType = user.enabled
                    try {
                        api.lockUnlockUser(user.username, lockType)
                        onMessage(if (lockType) "User locked successfully" else "User unlocked successfully")
                        loadUsers()
                    } catch (e: Exception) {
                        onMessage(e.message ?: "Lock/unlock failed")
                    }
                }
            },
            modifier = Modifier.weight(1f)
        )
    }

    if (viewLoading) LoadingOverlay()
    selectedUser?.let { UserViewDialog(it, onDismiss = { selectedUser = null }) }
}

@Composable
private fun UserFilterPanel(
    email: String,
    mobile: String,
    lockFilter: LockFilter,
    onEmailChange: (String) -> Unit,
    onMobileChange: (String) -> Unit,
    onLockChange: (LockFilter) -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit
) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)),
        shadowElevation = 1.dp
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
            if (maxWidth < 900.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("FILTER", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = UserGold)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        CompactInput(email, onEmailChange, "Email", Icons.Outlined.Email, Modifier.weight(1.35f))
                        CompactInput(mobile, onMobileChange, "Mobile", Icons.Outlined.Phone, Modifier.weight(1.1f), KeyboardType.Number)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        LockFilterButton(lockFilter, onLockChange, Modifier.weight(1f))
                        OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(28.dp), contentPadding = PaddingValues(horizontal = 18.dp)) {
                            Text("Reset", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onApply,
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            contentPadding = PaddingValues(horizontal = 22.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = UserGold, contentColor = TextDark)
                        ) {
                            Text("Apply", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("FILTER", Modifier.width(54.dp), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = UserGold)
                    CompactInput(email, onEmailChange, "Email", Icons.Outlined.Email, Modifier.weight(1.35f))
                    CompactInput(mobile, onMobileChange, "Mobile", Icons.Outlined.Phone, Modifier.weight(1.1f), KeyboardType.Number)
                    LockFilterButton(lockFilter, onLockChange, Modifier.width(155.dp))
                    OutlinedButton(onClick = onReset, modifier = Modifier.width(110.dp).height(56.dp), shape = RoundedCornerShape(28.dp), contentPadding = PaddingValues(horizontal = 18.dp)) {
                        Text("Reset", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onApply,
                        modifier = Modifier.width(130.dp).height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        contentPadding = PaddingValues(horizontal = 22.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = UserGold, contentColor = TextDark)
                    ) {
                        Text("Apply", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactInput(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    modifier: Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(icon, null, modifier = Modifier.size(18.dp), tint = UserGold) },
        modifier = modifier.heightIn(min = 56.dp),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
private fun LockFilterButton(selected: LockFilter, onSelected: (LockFilter) -> Unit, modifier: Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            Text(selected.title, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, null, Modifier.size(18.dp), tint = UserGold)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            LockFilter.entries.forEach { item -> DropdownMenuItem(text = { Text(item.title) }, onClick = { onSelected(item); expanded = false }) }
        }
    }
}

@Composable
private fun UserStatsRow(users: List<UserInformationResponse>, totalElements: Long) {
    val enabled = users.count { it.enabled }
    val disabled = users.size - enabled
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        StatCard("Total Users", totalElements.toString(), Icons.Outlined.Group, UserGold, Modifier.weight(1f))
        StatCard("Visible Enabled", enabled.toString(), Icons.Outlined.VerifiedUser, UserGreen, Modifier.weight(1f))
        StatCard("Visible Locked", disabled.toString(), Icons.Outlined.Lock, UserRed, Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f))) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = color.copy(alpha = 0.18f)) { Icon(icon, null, modifier = Modifier.padding(10.dp).size(22.dp), tint = color) }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = color)
            }
        }
    }
}

@Composable
private fun UserTableCard(
    users: List<UserInformationResponse>,
    loading: Boolean,
    currentPage: Int,
    totalPages: Int,
    pageSize: Int,
    totalElements: Long,
    onRefresh: () -> Unit,
    onPageChange: (Int) -> Unit,
    onPageSizeChange: (Int) -> Unit,
    onView: (Long) -> Unit,
    onToggleLock: (UserInformationResponse) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("USER DIRECTORY", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = onRefresh, modifier = Modifier.height(40.dp), contentPadding = PaddingValues(horizontal = 12.dp)) {
                    Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Refresh")
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
            UserTableHeader()
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
            when {
                loading -> LoadingContent("Loading users...")
                users.isEmpty() -> EmptyContent()
                else -> LazyColumn(Modifier.weight(1f)) {
                    items(users, key = { it.id }) { user ->
                        UserRow(user, onView = { onView(user.id) }, onToggleLock = { onToggleLock(user) })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f))
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
            PaginationBar(currentPage, totalPages, pageSize, totalElements, onPageChange, onPageSizeChange)
        }
    }
}

@Composable
private fun ColumnScope.LoadingContent(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = UserGold)
            Spacer(Modifier.height(16.dp))
            Text(text)
        }
    }
}

@Composable
private fun ColumnScope.EmptyContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.GroupOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "No users found",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UserTableHeader() {
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)).padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        HeaderText("Full Name", Modifier.weight(1.55f))
        HeaderText("Mobile Number", Modifier.weight(1.05f))
        HeaderText("User Name", Modifier.weight(1.45f))
        HeaderText("Status", Modifier.weight(0.75f))
        Box(Modifier.weight(0.75f))
    }
}

@Composable private fun HeaderText(text: String, modifier: Modifier) = Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = modifier)

@Composable
private fun UserRow(user: UserInformationResponse, onView: () -> Unit, onToggleLock: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onView).padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(user.fullName.ifBlank { "${user.firstName} ${user.lastName}" }.ifBlank { "Unknown" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1.55f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(user.mobileNumber.ifBlank { "-" }, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1.05f))
        Text(user.username.ifBlank { "-" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.45f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box(Modifier.weight(0.75f), contentAlignment = Alignment.CenterStart) { StatusChip(user.enabled) }
        Row(Modifier.weight(0.75f), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onView) { Icon(Icons.Filled.Visibility, "View", tint = UserGold) }
            IconButton(onClick = onToggleLock) { Icon(if (user.enabled) Icons.Outlined.Lock else Icons.Outlined.LockOpen, if (user.enabled) "Lock" else "Unlock", tint = if (user.enabled) UserRed else UserGreen) }
        }
    }
}

@Composable
private fun StatusChip(enabled: Boolean) {
    val color = if (enabled) UserGreen else UserRed
    Surface(shape = RoundedCornerShape(999.dp), color = color.copy(alpha = 0.12f), border = BorderStroke(1.dp, color.copy(alpha = 0.22f))) {
        Text(if (enabled) "ENABLED" else "LOCKED", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun PaginationBar(currentPage: Int, totalPages: Int, pageSize: Int, totalElements: Long, onPageChange: (Int) -> Unit, onPageSizeChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Show", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            PageSizeSelector(pageSize, onPageSizeChange)
            Spacer(Modifier.width(8.dp))
            Text("entries", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(pageInfo(currentPage, pageSize, totalElements), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
            IconButton(onClick = { onPageChange(0) }, enabled = currentPage > 0) { Icon(Icons.Default.FirstPage, "First") }
            IconButton(onClick = { onPageChange(currentPage - 1) }, enabled = currentPage > 0) { Icon(Icons.Default.ChevronLeft, "Previous") }
            Surface(shape = CircleShape, color = UserGold, modifier = Modifier.size(36.dp)) { Box(contentAlignment = Alignment.Center) { Text("${currentPage + 1}", fontWeight = FontWeight.Bold, color = TextDark) } }
            IconButton(onClick = { onPageChange(currentPage + 1) }, enabled = currentPage < totalPages - 1) { Icon(Icons.Default.ChevronRight, "Next") }
            IconButton(onClick = { onPageChange(totalPages - 1) }, enabled = currentPage < totalPages - 1 && totalPages > 0) { Icon(Icons.Default.LastPage, "Last") }
        }
    }
}

@Composable
private fun PageSizeSelector(pageSize: Int, onPageSizeChange: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
            Text("$pageSize")
            Spacer(Modifier.width(4.dp))
            Icon(if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, null, Modifier.size(18.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(5, 10, 20, 50).forEach { size -> DropdownMenuItem(text = { Text("$size") }, onClick = { onPageSizeChange(size); expanded = false }) }
        }
    }
}

private fun pageInfo(currentPage: Int, pageSize: Int, totalElements: Long): String {
    if (totalElements <= 0) return "Showing 0 entries"
    val from = currentPage * pageSize + 1
    val to = minOf((currentPage + 1) * pageSize, totalElements.toInt())
    return "Showing $from to $to of $totalElements"
}

@Composable
private fun UserCreateForm(api: UserApiService, onCancel: () -> Unit, onCreated: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var roles by remember { mutableStateOf<List<RoleResponse>>(emptyList()) }
    var selectedRoles by remember { mutableStateOf<List<RoleResponse>>(emptyList()) }
    var roleMenuOpen by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var presentAddress by remember { mutableStateOf("") }
    var permanentAddress by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var emailLookupMessage by remember { mutableStateOf<String?>(null) }
    var emailExists by remember { mutableStateOf(false) }
    var checkingEmail by remember { mutableStateOf(false) }

    val emailValid = email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
    val mobileValid = mobile.isNotBlank() && mobile.all(Char::isDigit)
    val formValid = emailValid &&
            !emailExists &&
            !checkingEmail &&
            password.length >= 6 &&
            firstName.isNotBlank() &&
            mobileValid &&
            selectedRoles.isNotEmpty()

    LaunchedEffect(Unit) {
        try { roles = api.getAllRoles() }
        catch (e: Exception) { error = e.message ?: "Failed to load roles" }
        finally { loading = false }
    }

    LaunchedEffect(email) {
        emailLookupMessage = null
        emailExists = false
        if (!emailValid) return@LaunchedEffect
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
        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = UserGold) }
        return
    }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 24.dp), contentPadding = PaddingValues(vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item { error?.let { ErrorCard(it, onDismiss = { error = null }) } }
        item {
            FormSection("Account Information", Icons.Outlined.Badge) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    RoundedField(
                        value = email,
                        onChange = { email = it.trim() },
                        label = "Email *",
                        modifier = Modifier.weight(1f),
                        isError = email.isNotBlank() && (!emailValid || emailExists),
                        errorText = when {
                            email.isNotBlank() && !emailValid -> "Enter a valid email"
                            emailExists -> emailLookupMessage ?: "Email already exists"
                            checkingEmail -> "Checking email..."
                            emailLookupMessage != null -> emailLookupMessage
                            else -> null
                        },
                        success = emailValid && !emailExists && !checkingEmail && emailLookupMessage != null
                    )
                    RoundedPasswordField(password, { password = it }, showPassword, { showPassword = !showPassword }, Modifier.weight(1f), password.isNotBlank() && password.length < 6)
                }
            }
        }
        item {
            FormSection("Personal Information", Icons.Outlined.Person) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    RoundedField(firstName, { firstName = it }, "First Name *", Modifier.weight(1f))
                    RoundedField(lastName, { lastName = it }, "Last Name", Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
                RoundedField(mobile, { mobile = it.filter(Char::isDigit) }, "Mobile Number *", Modifier.fillMaxWidth(), mobile.isNotBlank() && !mobileValid, "Only digits are allowed", KeyboardType.Number)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    RoundedField(presentAddress, { presentAddress = it }, "Present Address", Modifier.weight(1f), singleLine = false)
                    RoundedField(permanentAddress, { permanentAddress = it }, "Permanent Address", Modifier.weight(1f), singleLine = false)
                }
            }
        }
        item {
            FormSection("Role Assignment", Icons.Outlined.AdminPanelSettings) {
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.TouchApp, null, tint = MaterialTheme.colorScheme.primary)
                        Text("Select one or more roles. Selected roles are listed below and can be removed before saving.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Box {
                    OutlinedButton(onClick = { roleMenuOpen = true }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp)) {
                        Text(if (selectedRoles.isEmpty()) "Select Role *" else "Add another role", Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = roleMenuOpen, onDismissRequest = { roleMenuOpen = false }) {
                        val available = roles.filterNot { role -> selectedRoles.any { it.id == role.id } }
                        if (available.isEmpty()) DropdownMenuItem(text = { Text("No more roles") }, enabled = false, onClick = {})
                        available.forEach { role -> DropdownMenuItem(text = { Text(role.name) }, onClick = { selectedRoles = selectedRoles + role; roleMenuOpen = false }) }
                    }
                }
                Spacer(Modifier.height(16.dp))
                SelectedRoleTable(selectedRoles, onDelete = { selectedRoles = selectedRoles - it })
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.height(48.dp), shape = RoundedCornerShape(12.dp), enabled = !submitting) { Text("Cancel") }
                Spacer(Modifier.width(16.dp))
                Button(
                    onClick = {
                        scope.launch {
                            submitting = true
                            error = null
                            try {
                                val response = api.registerUser(UserRegistrationRequest(email, password, firstName.trim(), lastName.trim(), mobile, presentAddress.trim(), permanentAddress.trim(), selectedRoles.map { it.id }))
                                onCreated(response.message.ifBlank { "User created successfully" })
                            } catch (e: Exception) {
                                error = e.message ?: "Failed to create user"
                            } finally {
                                submitting = false
                            }
                        }
                    },
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = formValid && !submitting,
                    colors = ButtonDefaults.buttonColors(containerColor = UserGold, contentColor = TextDark)
                ) {
                    if (submitting) CircularProgressIndicator(Modifier.size(20.dp), color = TextDark, strokeWidth = 2.dp) else Icon(Icons.Default.Save, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (submitting) "Saving..." else "Save User", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RoundedField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier, isError: Boolean = false, errorText: String? = null, keyboardType: KeyboardType = KeyboardType.Text, singleLine: Boolean = true, success: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        shape = RoundedCornerShape(12.dp),
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        supportingText = {
            errorText?.let { Text(it, color = if (success) UserGreen else if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) }
        },
        trailingIcon = { if (success) Icon(Icons.Default.CheckCircle, null, tint = UserGreen) }
    )
}

@Composable
private fun RoundedPasswordField(value: String, onChange: (String) -> Unit, visible: Boolean, onVisibleChange: () -> Unit, modifier: Modifier, isError: Boolean) {
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
private fun FormSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) { Icon(icon, null, modifier = Modifier.padding(8.dp), tint = MaterialTheme.colorScheme.primary) }
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun SelectedRoleTable(roles: List<RoleResponse>, onDelete: (RoleResponse) -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)) {
        Column {
            Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)).padding(horizontal = 14.dp, vertical = 10.dp)) {
                HeaderText("Role Name", Modifier.weight(1f))
                HeaderText("Delete", Modifier.width(80.dp))
            }
            if (roles.isEmpty()) Text("No role selected", Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            roles.forEach { role ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(role.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    IconButton(onClick = { onDelete(role) }, modifier = Modifier.width(80.dp)) { Icon(Icons.Default.Delete, "Delete", tint = UserRed) }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f))
            }
        }
    }
}

@Composable
private fun UserViewDialog(user: UserInformationResponseWithRole, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        icon = { Icon(Icons.Outlined.ManageAccounts, null, tint = UserGold) },
        title = { Text("User Details", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailLine("Full Name", user.fullName.ifBlank { "${user.firstName} ${user.lastName}" })
                DetailLine("Username", user.username)
                DetailLine("Mobile", user.mobileNumber)
                DetailLine("Present Address", user.presetAddress ?: "-")
                DetailLine("Permanent Address", user.permanentAddress ?: "-")
                DetailLine("Status", if (user.enabled) "Enabled" else "Locked")
                DetailLine("Roles", user.userRoles.joinToString { it.name }.ifBlank { "-" })
            }
        }
    )
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, Modifier.width(120.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value.ifBlank { "-" }, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ErrorCard(message: String, onDismiss: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(10.dp))
            Text(message, Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
        }
    }
}

@Composable
private fun UserToast(message: String, modifier: Modifier, onDismiss: () -> Unit) {
    Snackbar(modifier = modifier.padding(16.dp), action = { TextButton(onClick = onDismiss) { Text("OK") } }) { Text(message) }
}

@Composable
private fun LoadingOverlay() {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.15f)), Alignment.Center) { CircularProgressIndicator(color = UserGold) }
}
