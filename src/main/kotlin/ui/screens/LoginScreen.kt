import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import data.auth.AuthManager
import data.auth.UserState
import data.network.ApiService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.theme.AppAnimations
import ui.theme.ExtendedTypography

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isContentVisible by remember { mutableStateOf(false) }

    // Initialize ApiService and remember it
    val apiService = remember { ApiService() }
    
    // Trigger entrance animation
    LaunchedEffect(Unit) {
        delay(100)
        isContentVisible = true
    }
    
    // Add to the composition's onDispose
    DisposableEffect(apiService) {
        onDispose {
            apiService.close()
        }
    }
    
    val coroutineScope = rememberCoroutineScope()

    // Background with gradient
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        // Decorative circles in background
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-100).dp, y = (-50).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 100.dp, y = 100.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Logo and Title
            AnimatedVisibility(
                visible = isContentVisible,
                enter = fadeIn(
                    animationSpec = tween(AppAnimations.DURATION_ENTRANCE)
                ) + slideInVertically(
                    initialOffsetY = { -it / 2 },
                    animationSpec = tween(AppAnimations.DURATION_ENTRANCE, easing = AppAnimations.EaseOutQuart)
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 40.dp)
                ) {
                    // Logo icon with animated glow
                    Box(contentAlignment = Alignment.Center) {
                        // Glow effect
                        val infiniteTransition = rememberInfiniteTransition()
                        val glowAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 0.6f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(2000, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            )
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = CircleShape
                                )
                        )
                        
                        Surface(
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shadowElevation = 8.dp
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Restaurant,
                                contentDescription = "Restaurant Logo",
                                modifier = Modifier
                                    .padding(16.dp)
                                    .size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Restaurant Management",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "Point of Sale System",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Animated Login Card
            AnimatedVisibility(
                visible = isContentVisible,
                enter = fadeIn(
                    animationSpec = tween(AppAnimations.DURATION_ENTRANCE, delayMillis = 150)
                ) + slideInVertically(
                    initialOffsetY = { it / 3 },
                    animationSpec = tween(AppAnimations.DURATION_ENTRANCE, delayMillis = 150, easing = AppAnimations.EaseOutQuart)
                )
            ) {
                LoginCard(
                    email = email,
                    onEmailChange = { email = it },
                    password = password,
                    onPasswordChange = { password = it },
                    isPasswordVisible = isPasswordVisible,
                    onPasswordVisibilityToggle = { isPasswordVisible = !isPasswordVisible },
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    onLogin = {
                        if (email.isBlank() || password.isBlank()) {
                            errorMessage = "Please enter both email and password"
                            return@LoginCard
                        }

                        isLoading = true
                        errorMessage = null

                        coroutineScope.launch {
                            try {
                                val result = apiService.login(email, password)
                                result.onSuccess { response ->
                                    if (response.jwtToken.isNotEmpty()) {
                                        AuthManager.saveToken(response.jwtToken)
                                        UserState.login(response)
                                        onLoginSuccess()
                                    } else {
                                        errorMessage = "Login failed: Invalid token received"
                                        isLoading = false
                                    }
                                }.onFailure { exception ->
                                    errorMessage = exception.message
                                    isLoading = false
                                }
                            } catch (e: Exception) {
                                errorMessage = "Login failed: ${e.message}"
                                isLoading = false
                            }
                        }
                    }
                )
            }
            
            // Footer
            AnimatedVisibility(
                visible = isContentVisible,
                enter = fadeIn(
                    animationSpec = tween(AppAnimations.DURATION_ENTRANCE, delayMillis = 300)
                )
            ) {
                Text(
                    text = "© 2024 Restaurant POS System",
                    style = ExtendedTypography.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 32.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginCard(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    isPasswordVisible: Boolean,
    onPasswordVisibilityToggle: () -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    onLogin: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    val elevation by animateDpAsState(
        targetValue = if (isHovered) 24.dp else 12.dp,
        animationSpec = tween(AppAnimations.DURATION_FAST)
    )
    
    Card(
        modifier = Modifier
            .widthIn(min = 320.dp, max = 420.dp)
            .hoverable(interactionSource)
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(24.dp),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Card Header
            Text(
                text = "Welcome Back",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = "Sign in to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
            )

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Email Address") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = Color.Transparent
                ),
                singleLine = true
            )

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("Password") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    IconButton(onClick = onPasswordVisibilityToggle) {
                        Icon(
                            imageVector = if (isPasswordVisible) 
                                Icons.Default.VisibilityOff 
                            else 
                                Icons.Default.Visibility,
                            contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                visualTransformation = if (isPasswordVisible) 
                    VisualTransformation.None 
                else 
                    PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = Color.Transparent
                ),
                singleLine = true
            )

            // Error Message with animation
            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Login Button
            LoginButton(
                isLoading = isLoading,
                onClick = onLogin,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LoginButton(
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isHovered && !isLoading) 1.02f else 1f,
        animationSpec = tween(AppAnimations.DURATION_FAST)
    )
    
    Button(
        onClick = onClick,
        modifier = modifier
            .height(52.dp)
            .hoverable(interactionSource)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        enabled = !isLoading,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp,
            hoveredElevation = 6.dp
        )
    ) {
        AnimatedContent(
            targetState = isLoading,
            transitionSpec = {
                fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
            }
        ) { loading ->
            if (loading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Signing in...",
                        style = ExtendedTypography.buttonText
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Login,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Sign In",
                        style = ExtendedTypography.buttonText
                    )
                }
            }
        }
    }
}
