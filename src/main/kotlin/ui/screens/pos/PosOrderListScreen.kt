package ui.screens.pos

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
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
import data.model.FoodOrderShortInfo
import data.model.OrderStatus
import kotlinx.coroutines.delay
import ui.theme.AppAnimations
import ui.theme.ExtendedTypography
import ui.viewmodel.PosUiEvent
import ui.viewmodel.PosViewModel
import java.text.DecimalFormat

/**
 * POS Order List Screen - displays active orders in a responsive grid
 */
@Composable
fun PosOrderListScreen(
    viewModel: PosViewModel,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var isContentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.onEvent(PosUiEvent.LoadOrders)
        delay(100)
        isContentVisible = true
    }

    uiState.errorMessage?.let {
        LaunchedEffect(it) {
            delay(4000)
            viewModel.onEvent(PosUiEvent.ClearError)
        }
    }
    uiState.successMessage?.let {
        LaunchedEffect(it) {
            delay(3000)
            viewModel.onEvent(PosUiEvent.ClearSuccess)
        }
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = isContentVisible,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(AppAnimations.DURATION_NORMAL, easing = AppAnimations.EaseOutQuart)
                ) + fadeIn()
            ) {
                PosTopBar(
                    onRefresh = { viewModel.onEvent(PosUiEvent.RefreshOrders) },
                    onCreateNew = onNavigateToCreate
                )
            }
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
            when {
                uiState.isLoadingOrders && uiState.orders.isEmpty() -> PosLoadingContent()
                uiState.orders.isEmpty() -> PosEmptyState(onCreateNew = onNavigateToCreate)
                else -> {
                    AnimatedVisibility(
                        visible = isContentVisible,
                        enter = fadeIn(animationSpec = tween(AppAnimations.DURATION_ENTRANCE))
                    ) {
                        PosOrderGrid(
                            orders = uiState.orders,
                            viewModel = viewModel,
                            onView = onNavigateToDetail,
                            onEdit = onNavigateToEdit
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = uiState.errorMessage != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                PosToast(message = uiState.errorMessage ?: "", isError = true, modifier = Modifier.padding(16.dp))
            }

            AnimatedVisibility(
                visible = uiState.successMessage != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                PosToast(message = uiState.successMessage ?: "", isError = false, modifier = Modifier.padding(16.dp))
            }

            if (uiState.isLoadingOrders && uiState.orders.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp), color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun PosTopBar(onRefresh: () -> Unit, onCreateNew: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(imageVector = Icons.Outlined.Receipt, contentDescription = null, modifier = Modifier.padding(8.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Column {
                    Text("POS Orders", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Manage active orders", style = ExtendedTypography.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val interactionSource = remember { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()
                val scale by animateFloatAsState(targetValue = if (isHovered) 1.1f else 1f, animationSpec = tween(AppAnimations.DURATION_FAST))

                FilledTonalIconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(44.dp).hoverable(interactionSource).graphicsLayer { scaleX = scale; scaleY = scale },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary)
                }

                Button(onClick = onCreateNew, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Order")
                }
            }
        }
    }
}

@Composable
private fun PosOrderGrid(orders: List<FoodOrderShortInfo>, viewModel: PosViewModel, onView: (Long) -> Unit, onEdit: (Long) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 280.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(orders) { index, order ->
            var isVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(index * 40L)
                isVisible = true
            }
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(AppAnimations.DURATION_NORMAL)) + scaleIn(initialScale = 0.9f, animationSpec = tween(AppAnimations.DURATION_NORMAL, easing = AppAnimations.EaseOutQuart))
            ) {
                PosOrderCard(order = order, viewModel = viewModel, onView = { onView(order.id) }, onEdit = { onEdit(order.id) })
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun PosOrderCard(order: FoodOrderShortInfo, viewModel: PosViewModel, onView: () -> Unit, onEdit: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val elevation by animateDpAsState(targetValue = if (isHovered) 12.dp else 4.dp, animationSpec = tween(AppAnimations.DURATION_FAST))
    val scale by animateFloatAsState(targetValue = if (isHovered) 1.02f else 1f, animationSpec = tween(AppAnimations.DURATION_FAST))
    val df = remember { DecimalFormat("#,##0.00") }
    val statusColor = when (order.orderStatus) {
        OrderStatus.ORDER_PLACED -> MaterialTheme.colorScheme.primary
        OrderStatus.BILL_PRINTED -> MaterialTheme.colorScheme.tertiary
        OrderStatus.PAID -> MaterialTheme.colorScheme.secondary
        OrderStatus.CANCELED -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth().hoverable(interactionSource).graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(statusColor))
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(
                            text = "Table #${order.tableNumber}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    Text(
                        text = "৳ ${df.format(order.totalAmount)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = order.waiterName ?: "Unknown Waiter", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(6.dp), color = statusColor.copy(alpha = 0.15f)) {
                        Text(
                            text = order.orderStatus.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onView, modifier = Modifier.size(36.dp)) {
                            Icon(imageVector = Icons.Outlined.Visibility, contentDescription = "View", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        if (viewModel.canEditOrder(order)) {
                            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                                Icon(imageVector = Icons.Outlined.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                            }
                        }
                        if (viewModel.canPrintBill(order)) {
                            IconButton(onClick = { viewModel.onEvent(PosUiEvent.PrintBill(order.id)) }, modifier = Modifier.size(36.dp)) {
                                Icon(imageVector = Icons.Outlined.Print, contentDescription = "Print Bill", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                            }
                        }
                        if (viewModel.canMarkPaid(order)) {
                            IconButton(onClick = onView, modifier = Modifier.size(36.dp)) {
                                Icon(imageVector = Icons.Outlined.CheckCircle, contentDescription = "Complete Order", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PosLoadingContent() {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp), color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Loading orders...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PosEmptyState(onCreateNew: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Icon(imageVector = Icons.Outlined.Receipt, contentDescription = null, modifier = Modifier.padding(24.dp).size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("No Active Orders", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Create a new order to get started", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onCreateNew, shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Default.Add, null, Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("New Order")
        }
    }
}

@Composable
private fun PosToast(message: String, isError: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
        shadowElevation = 8.dp
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(imageVector = if (isError) Icons.Default.Error else Icons.Default.CheckCircle, contentDescription = null, tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary)
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}
