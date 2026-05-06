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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import data.model.UserInformationResponse
import data.network.UserApiService
import kotlinx.coroutines.launch
import ui.theme.ExtendedTypography

private val UserMgGold = Color(0xFFD4AF37)
private val UserMgGreen = Color(0xFF16A34A)
private val UserMgRed = Color(0xFFDC2626)
private val UserMgDark = Color(0xFF111827)

private enum class UserRoute { LIST, CREATE, VIEW, EDIT }
private enum class UserLockFilter(val title: String, val value: Boolean?) {
    ALL("All", null), LOCKED("Locked", true), UNLOCKED("Unlocked", false)
}

@Composable
fun UserManagementScreen() {
    val api = remember { UserApiService() }
    var route by remember { mutableStateOf(UserRoute.LIST) }
    var selectedUserId by remember { mutableStateOf<Long?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
                )
            )
    ) {
        when (route) {
            UserRoute.LIST -> UserListScreen(
                api = api,
                onCreate = { route = UserRoute.CREATE },
                onView = { id -> selectedUserId = id; route = UserRoute.VIEW },
                onEdit = { id -> selectedUserId = id; route = UserRoute.EDIT },
                onMessage = { toast = it }
            )
            UserRoute.CREATE -> UserFormScreen(
                api = api,
                mode = UserFormMode.CREATE,
                onBack = { route = UserRoute.LIST },
                onEdit = {},
                onSaved = { toast = it; route = UserRoute.LIST }
            )
            UserRoute.VIEW -> UserFormScreen(
                api = api,
                mode = UserFormMode.VIEW,
                userId = selectedUserId,
                onBack = { route = UserRoute.LIST },
                onEdit = { id -> selectedUserId = id; route = UserRoute.EDIT },
                onSaved = { toast = it; route = UserRoute.LIST }
            )
            UserRoute.EDIT -> UserFormScreen(
                api = api,
                mode = UserFormMode.EDIT,
                userId = selectedUserId,
                onBack = { route = UserRoute.VIEW },
                onEdit = {},
                onSaved = { toast = it; route = UserRoute.LIST }
            )
        }
        toast?.let { UserToast(it, Modifier.align(Alignment.BottomCenter)) { toast = null } }
    }
}

@Composable
private fun UserListScreen(
    api: UserApiService,
    onCreate: () -> Unit,
    onView: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var users by remember { mutableStateOf<List<UserInformationResponse>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var emailFilter by remember { mutableStateOf("") }
    var mobileFilter by remember { mutableStateOf("") }
    var lockFilter by remember { mutableStateOf(UserLockFilter.ALL) }
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

    Column(Modifier.fillMaxSize()) {
        UserListTopBar(onCreate)
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            UserFilterPanel(
                email = emailFilter,
                mobile = mobileFilter,
                lockFilter = lockFilter,
                onEmailChange = { emailFilter = it },
                onMobileChange = { mobileFilter = it.filter(Char::isDigit) },
                onLockChange = { lockFilter = it; loadUsers(0) },
                onApply = { loadUsers(0) },
                onReset = {
                    emailFilter = ""
                    mobileFilter = ""
                    lockFilter = UserLockFilter.ALL
                    loadUsers(0)
                }
            )
            UserStatsRow(users, totalElements)
            AnimatedVisibility(error != null) { ErrorCard(error.orEmpty()) { error = null } }
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
                onView = onView,
                onEdit = onEdit,
                onToggleLock = { user ->
                    scope.launch {
                        try {
                            val lockType = user.enabled
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
    }
}

@Composable
private fun UserListTopBar(onCreate: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(14.dp), color = UserMgGold.copy(alpha = 0.14f)) {
                    Icon(Icons.Outlined.ManageAccounts, null, Modifier.padding(12.dp).size(28.dp), tint = UserMgGold)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("User Management", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text("Search, view, edit, lock and unlock system users", style = ExtendedTypography.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Button(onClick = onCreate, colors = ButtonDefaults.buttonColors(containerColor = UserMgGold, contentColor = UserMgDark), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.PersonAdd, null)
                Spacer(Modifier.width(8.dp))
                Text("Create User", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun UserFilterPanel(
    email: String,
    mobile: String,
    lockFilter: UserLockFilter,
    onEmailChange: (String) -> Unit,
    onMobileChange: (String) -> Unit,
    onLockChange: (UserLockFilter) -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit
) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)), shadowElevation = 1.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("FILTER", Modifier.width(54.dp), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = UserMgGold)
            CompactInput(email, onEmailChange, "Email", Icons.Outlined.Email, Modifier.weight(1.35f))
            CompactInput(mobile, onMobileChange, "Mobile", Icons.Outlined.Phone, Modifier.weight(1.1f), KeyboardType.Number)
            LockFilterButton(lockFilter, onLockChange, Modifier.width(155.dp))
            OutlinedButton(onClick = onReset, modifier = Modifier.width(110.dp).height(56.dp), shape = RoundedCornerShape(28.dp), contentPadding = PaddingValues(horizontal = 18.dp)) {
                Text("Reset", fontWeight = FontWeight.Bold)
            }
            Button(onClick = onApply, modifier = Modifier.width(130.dp).height(56.dp), shape = RoundedCornerShape(28.dp), contentPadding = PaddingValues(horizontal = 22.dp), colors = ButtonDefaults.buttonColors(containerColor = UserMgGold, contentColor = UserMgDark)) {
                Text("Apply", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CompactInput(value: String, onChange: (String) -> Unit, placeholder: String, icon: ImageVector, modifier: Modifier, keyboardType: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(icon, null, modifier = Modifier.size(18.dp), tint = UserMgGold) },
        modifier = modifier.heightIn(min = 56.dp),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
private fun LockFilterButton(selected: UserLockFilter, onSelected: (UserLockFilter) -> Unit, modifier: Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 12.dp)) {
            Text(selected.title, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, null, Modifier.size(18.dp), tint = UserMgGold)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            UserLockFilter.entries.forEach { item -> DropdownMenuItem(text = { Text(item.title) }, onClick = { onSelected(item); expanded = false }) }
        }
    }
}

@Composable
private fun UserStatsRow(users: List<UserInformationResponse>, totalElements: Long) {
    val enabled = users.count { it.enabled }
    val locked = users.size - enabled
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        StatCard("Total Users", totalElements.toString(), Icons.Outlined.Group, UserMgGold, Modifier.weight(1f))
        StatCard("Visible Enabled", enabled.toString(), Icons.Outlined.VerifiedUser, UserMgGreen, Modifier.weight(1f))
        StatCard("Visible Locked", locked.toString(), Icons.Outlined.Lock, UserMgRed, Modifier.weight(1f))
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
    users: List<UserInformationResponse>, loading: Boolean, currentPage: Int, totalPages: Int, pageSize: Int, totalElements: Long,
    onRefresh: () -> Unit, onPageChange: (Int) -> Unit, onPageSizeChange: (Int) -> Unit,
    onView: (Long) -> Unit, onEdit: (Long) -> Unit, onToggleLock: (UserInformationResponse) -> Unit, modifier: Modifier = Modifier
) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("USER DIRECTORY", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = onRefresh, modifier = Modifier.height(40.dp), contentPadding = PaddingValues(horizontal = 12.dp)) {
                    Icon(Icons.Default.Refresh, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Refresh")
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
                        UserRow(user, { onView(user.id) }, { onEdit(user.id) }, { onToggleLock(user) })
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
    Box(Modifier.fillMaxWidth().weight(1f), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(color = UserMgGold); Spacer(Modifier.height(16.dp)); Text(text) }
    }
}

@Composable
private fun ColumnScope.EmptyContent() {
    Box(Modifier.fillMaxWidth().weight(1f), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Outlined.GroupOff, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline); Spacer(Modifier.height(16.dp)); Text("No users found", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun UserTableHeader() {
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)).padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        HeaderText("Full Name", Modifier.weight(1.55f)); HeaderText("Mobile Number", Modifier.weight(1.05f)); HeaderText("User Name", Modifier.weight(1.45f)); HeaderText("Status", Modifier.weight(0.75f)); Box(Modifier.weight(0.90f))
    }
}

@Composable private fun HeaderText(text: String, modifier: Modifier) = Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = modifier)

@Composable
private fun UserRow(user: UserInformationResponse, onView: () -> Unit, onEdit: () -> Unit, onToggleLock: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onView).padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(user.fullName.ifBlank { "${user.firstName} ${user.lastName}" }.ifBlank { "Unknown" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1.55f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(user.mobileNumber.ifBlank { "-" }, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1.05f))
        Text(user.username.ifBlank { "-" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.45f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box(Modifier.weight(0.75f), contentAlignment = Alignment.CenterStart) { StatusChip(user.enabled) }
        Row(Modifier.weight(0.90f), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onView) { Icon(Icons.Filled.Visibility, "View", tint = UserMgGold) }
            IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, "Edit", tint = MaterialTheme.colorScheme.primary) }
            IconButton(onClick = onToggleLock) { Icon(if (user.enabled) Icons.Outlined.Lock else Icons.Outlined.LockOpen, if (user.enabled) "Lock" else "Unlock", tint = if (user.enabled) UserMgRed else UserMgGreen) }
        }
    }
}

@Composable
private fun StatusChip(enabled: Boolean) {
    val color = if (enabled) UserMgGreen else UserMgRed
    Surface(shape = RoundedCornerShape(999.dp), color = color.copy(alpha = 0.12f), border = BorderStroke(1.dp, color.copy(alpha = 0.22f))) {
        Text(if (enabled) "ENABLED" else "LOCKED", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun PaginationBar(currentPage: Int, totalPages: Int, pageSize: Int, totalElements: Long, onPageChange: (Int) -> Unit, onPageSizeChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) { Text("Show", color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.width(8.dp)); PageSizeSelector(pageSize, onPageSizeChange); Spacer(Modifier.width(8.dp)); Text("entries", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(pageInfo(currentPage, pageSize, totalElements), color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.width(16.dp))
            IconButton(onClick = { onPageChange(0) }, enabled = currentPage > 0) { Icon(Icons.Default.FirstPage, "First") }
            IconButton(onClick = { onPageChange(currentPage - 1) }, enabled = currentPage > 0) { Icon(Icons.Default.ChevronLeft, "Previous") }
            Surface(shape = CircleShape, color = UserMgGold, modifier = Modifier.size(36.dp)) { Box(contentAlignment = Alignment.Center) { Text("${currentPage + 1}", fontWeight = FontWeight.Bold, color = UserMgDark) } }
            IconButton(onClick = { onPageChange(currentPage + 1) }, enabled = currentPage < totalPages - 1) { Icon(Icons.Default.ChevronRight, "Next") }
            IconButton(onClick = { onPageChange(totalPages - 1) }, enabled = currentPage < totalPages - 1 && totalPages > 0) { Icon(Icons.Default.LastPage, "Last") }
        }
    }
}

@Composable
private fun PageSizeSelector(pageSize: Int, onPageSizeChange: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) { Text("$pageSize"); Spacer(Modifier.width(4.dp)); Icon(if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, null, Modifier.size(18.dp)) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) { listOf(5, 10, 20, 50).forEach { size -> DropdownMenuItem(text = { Text("$size") }, onClick = { onPageSizeChange(size); expanded = false }) } }
    }
}

private fun pageInfo(currentPage: Int, pageSize: Int, totalElements: Long): String {
    if (totalElements <= 0) return "Showing 0 entries"
    val from = currentPage * pageSize + 1
    val to = minOf((currentPage + 1) * pageSize, totalElements.toInt())
    return "Showing $from to $to of $totalElements"
}

@Composable
private fun ErrorCard(message: String, onDismiss: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error); Spacer(Modifier.width(10.dp)); Text(message, Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer); IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) } }
    }
}

@Composable
private fun UserToast(message: String, modifier: Modifier, onDismiss: () -> Unit) {
    Snackbar(modifier = modifier.padding(16.dp), action = { TextButton(onClick = onDismiss) { Text("OK") } }) { Text(message) }
}
