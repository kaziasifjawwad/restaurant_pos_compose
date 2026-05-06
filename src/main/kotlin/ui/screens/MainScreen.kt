package ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import data.model.MenuItem
import data.network.ApiService
import data.repository.PosRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.components.ResponsiveTopAppBar
import ui.components.SidebarMenu
import ui.navigation.NavigationHost
import ui.theme.AppAnimations
import ui.theme.ExtendedTypography
import ui.theme.ThemeState

@Composable
fun MainScreen(onLogout: () -> Unit) {
    var selectedMenuItem by remember { mutableStateOf<MenuItem?>(null) }
    var isSidebarVisible by remember { mutableStateOf(true) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isContentVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val posRepository = remember { PosRepository.getInstance() }
    
    LaunchedEffect(Unit) {
        coroutineScope.launch { posRepository.loadStartupConfiguration() }
        delay(100)
        isContentVisible = true
    }
    
    val apiService = remember { ApiService() }
    
    DisposableEffect(apiService) {
        onDispose { apiService.close() }
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = isContentVisible,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(AppAnimations.DURATION_NORMAL, easing = AppAnimations.EaseOutQuart)
                ) + fadeIn(animationSpec = tween(AppAnimations.DURATION_NORMAL))
            ) {
                BoxWithConstraints {
                    ResponsiveTopAppBar(
                        screenWidth = maxWidth.value.toInt(),
                        onMenuClick = { isSidebarVisible = !isSidebarVisible },
                        onThemeToggle = { ThemeState.setDarkTheme(it) },
                        onLogoutClick = { showLogoutDialog = true },
                        showSidebarToggle = true
                    )
                }
            }
        },
        content = { innerPadding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                val sidebarWidth = when {
                    maxWidth >= 1500.dp -> 280.dp
                    maxWidth >= 1200.dp -> 240.dp
                    else -> 216.dp
                }

                Row(modifier = Modifier.fillMaxSize()) {
                    AnimatedVisibility(
                        visible = isSidebarVisible && isContentVisible,
                        enter = slideInHorizontally(
                            initialOffsetX = { -it },
                            animationSpec = tween(AppAnimations.DURATION_NORMAL, easing = AppAnimations.EaseOutQuart)
                        ) + fadeIn(animationSpec = tween(AppAnimations.DURATION_NORMAL)),
                        exit = slideOutHorizontally(
                            targetOffsetX = { -it },
                            animationSpec = tween(AppAnimations.DURATION_FAST)
                        ) + fadeOut(animationSpec = tween(AppAnimations.DURATION_FAST))
                    ) {
                        SidebarMenu(
                            modifier = Modifier
                                .width(sidebarWidth)
                                .fillMaxHeight(),
                            onMenuItemClick = { menuItem -> selectedMenuItem = menuItem },
                            selectedMenuCode = selectedMenuItem?.menuCode
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.background,
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                                    )
                                )
                            )
                    ) {
                        AnimatedContent(
                            targetState = selectedMenuItem,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(AppAnimations.DURATION_NORMAL)) +
                                    slideInHorizontally(
                                        initialOffsetX = { it / 6 },
                                        animationSpec = tween(AppAnimations.DURATION_NORMAL, easing = AppAnimations.EaseOutQuart)
                                    )).togetherWith(
                                    fadeOut(animationSpec = tween(AppAnimations.DURATION_FAST)) +
                                        slideOutHorizontally(
                                            targetOffsetX = { -it / 6 },
                                            animationSpec = tween(AppAnimations.DURATION_FAST)
                                        )
                                )
                            }
                        ) { menuItem ->
                            menuItem?.let {
                                NavigationHost(currentMenuCode = it.menuCode, currentMenuItem = it)
                            } ?: WelcomeScreen()
                        }
                    }
                }
            }
        }
    )
    
    if (showLogoutDialog) {
        LogoutConfirmationDialog(
            onConfirm = {
                showLogoutDialog = false
                posRepository.clearCaches()
                onLogout()
            },
            onDismiss = { showLogoutDialog = false }
        )
    }
}

@Composable
private fun LogoutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Icon(
                    imageVector = Icons.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        },
        title = { Text("Confirm Logout", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold) },
        text = {
            Text(
                text = "Are you sure you want to logout? You will need to sign in again to access the system.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun WelcomeScreen() {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(AppAnimations.DURATION_ENTRANCE)) +
                slideInVertically(
                    initialOffsetY = { it / 4 },
                    animationSpec = tween(AppAnimations.DURATION_ENTRANCE, easing = AppAnimations.EaseOutQuart)
                )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 8.dp
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Dashboard,
                        contentDescription = null,
                        modifier = Modifier.padding(20.dp).size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Welcome to Restaurant POS",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Select a menu item from the sidebar to get started",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 40.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(AppAnimations.DURATION_ENTRANCE, delayMillis = 200))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
            ) {
                StatCard("Inventory", "0", "Items", Icons.Outlined.Inventory2, Modifier.weight(1f, fill = false).widthIn(max = 200.dp), 0)
                StatCard("Menu Items", "0", "Available", Icons.Outlined.Restaurant, Modifier.weight(1f, fill = false).widthIn(max = 200.dp), 100)
                StatCard("Users", "1", "Active", Icons.Default.Person, Modifier.weight(1f, fill = false).widthIn(max = 200.dp), 200)
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector = Icons.Default.Info,
    modifier: Modifier = Modifier,
    delay: Int = 0
) {
    var isVisible by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        isVisible = true
    }
    
    val elevation by animateDpAsState(if (isHovered) 16.dp else 4.dp, tween(AppAnimations.DURATION_FAST))
    val scale by animateFloatAsState(if (isHovered) 1.03f else 1f, tween(AppAnimations.DURATION_FAST, easing = AppAnimations.EaseOutQuart))
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(AppAnimations.DURATION_NORMAL)) +
            scaleIn(initialScale = 0.8f, animationSpec = tween(AppAnimations.DURATION_NORMAL, easing = AppAnimations.EaseOutQuart))
    ) {
        Card(
            modifier = modifier
                .hoverable(interactionSource)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .shadow(elevation = elevation, shape = RoundedCornerShape(16.dp), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)) {
                    Icon(icon, contentDescription = null, modifier = Modifier.padding(10.dp).size(24.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = ExtendedTypography.caption, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }
        }
    }
}
