package ui.screens.pos

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import data.model.*
import kotlinx.coroutines.delay
import ui.theme.AppAnimations
import ui.theme.ExtendedTypography
import ui.viewmodel.PosUiEvent
import ui.viewmodel.PosViewModel
import java.text.DecimalFormat

/**
 * POS Order Detail Screen - shows full order details with status actions
 */
@Composable
fun PosOrderDetailScreen(
    orderId: Long,
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var isContentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(orderId) {
        viewModel.onEvent(PosUiEvent.LoadOrderDetail(orderId))
        delay(100)
        isContentVisible = true
    }

    // Handle success messages that should navigate back
    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null && uiState.selectedOrder == null) {
            delay(1000)
            viewModel.onEvent(PosUiEvent.ClearSuccess)
            onNavigateBack()
        }
    }

    Box(
        modifier = Modifier
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
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            PosDetailHeader(
                order = uiState.selectedOrder,
                onBack = {
                    viewModel.onEvent(PosUiEvent.ClearSelectedOrder)
                    onNavigateBack()
                },
                onEdit = { onNavigateToEdit(orderId) }
            )

            when {
                uiState.isLoadingDetail -> PosDetailLoadingContent()
                uiState.selectedOrder != null -> {
                    AnimatedVisibility(
                        visible = isContentVisible,
                        enter = fadeIn(animationSpec = tween(AppAnimations.DURATION_ENTRANCE))
                    ) {
                        PosDetailContent(
                            order = uiState.selectedOrder!!,
                            viewModel = viewModel
                        )
                    }
                }
                else -> PosDetailErrorContent(onRetry = { viewModel.onEvent(PosUiEvent.LoadOrderDetail(orderId)) })
            }
        }

        // Error Toast
        AnimatedVisibility(
            visible = uiState.errorMessage != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            PosToast(
                message = uiState.errorMessage ?: "",
                isError = true,
                modifier = Modifier.padding(16.dp)
            )
        }

        // Success Toast
        AnimatedVisibility(
            visible = uiState.successMessage != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            PosToast(
                message = uiState.successMessage ?: "",
                isError = false,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
private fun PosDetailHeader(
    order: FoodOrderByCustomer?,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = order?.let { "Order #${it.id}" } ?: "Loading...",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Order Details",
                        style = ExtendedTypography.caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (order != null && order.orderStatus != OrderStatus.PAID && order.orderStatus != OrderStatus.CANCELED) {
                val interactionSource = remember { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()
                val scale by animateFloatAsState(
                    targetValue = if (isHovered) 1.05f else 1f,
                    animationSpec = tween(AppAnimations.DURATION_FAST)
                )

                Button(
                    onClick = onEdit,
                    modifier = Modifier
                        .hoverable(interactionSource)
                        .graphicsLayer { scaleX = scale; scaleY = scale },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit")
                }
            }
        }
    }
}

@Composable
private fun PosDetailContent(
    order: FoodOrderByCustomer,
    viewModel: PosViewModel
) {
    val df = remember { DecimalFormat("#,##0.00") }

    val statusColor = when (order.orderStatus) {
        OrderStatus.ORDER_PLACED -> MaterialTheme.colorScheme.primary
        OrderStatus.BILL_PRINTED -> MaterialTheme.colorScheme.tertiary
        OrderStatus.PAID -> MaterialTheme.colorScheme.secondary
        OrderStatus.CANCELED -> MaterialTheme.colorScheme.error
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(vertical = 24.dp)
    ) {
        // Order Info Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    // Status bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(statusColor)
                    )

                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "Table #${order.tableNumber}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = statusColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = order.orderStatus.displayName,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = statusColor,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            PosInfoItem(
                                icon = Icons.Outlined.Person,
                                label = "Waiter",
                                value = order.waiterName ?: "Unknown"
                            )
                            PosInfoItem(
                                icon = Icons.Outlined.AttachMoney,
                                label = "Total",
                                value = "৳ ${df.format(order.totalAmount)}"
                            )
                            if (order.discount > 0) {
                                PosInfoItem(
                                    icon = Icons.Outlined.Discount,
                                    label = "Discount",
                                    value = if (order.discountType == DiscountType.PERCENTAGE)
                                        "${df.format(order.discount)}%"
                                    else
                                        "৳ ${df.format(order.discount)}"
                                )
                            }
                        }
                    }
                }
            }
        }

        // Action Buttons
        item {
            PosDetailActionButtons(
                order = order,
                viewModel = viewModel
            )
        }

        // Food Orders
        if (order.foodOrders.isNotEmpty()) {
            item {
                Text(
                    text = "Food Items",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            itemsIndexed(order.foodOrders) { index, foodOrder ->
                PosOrderItemCard(
                    name = foodOrder.foodName ?: "Food #${foodOrder.itemNumber}",
                    subtitle = "${foodOrder.foodSize.name} × ${foodOrder.foodQuantity}",
                    price = foodOrder.foodPrice * foodOrder.foodQuantity,
                    df = df,
                    icon = Icons.Outlined.Restaurant
                )
            }
        }

        // Beverage Orders
        if (order.beverageOrders.isNotEmpty()) {
            item {
                Text(
                    text = "Beverages",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            itemsIndexed(order.beverageOrders) { index, bevOrder ->
                PosOrderItemCard(
                    name = bevOrder.beverageName ?: "Beverage #${bevOrder.beverageId}",
                    subtitle = "${bevOrder.quantity} ${bevOrder.unit?.name ?: ""} × ${bevOrder.amount}",
                    price = bevOrder.price * bevOrder.amount,
                    df = df,
                    icon = Icons.Outlined.LocalDrink
                )
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun PosInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(10.dp).size(24.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PosDetailActionButtons(
    order: FoodOrderByCustomer,
    viewModel: PosViewModel
) {
    val orderShort = FoodOrderShortInfo(
        id = order.id,
        waiterName = order.waiterName,
        waiterId = order.waiterId,
        totalAmount = order.totalAmount,
        orderStatus = order.orderStatus,
        tableId = order.tableId,
        tableNumber = order.tableNumber
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Print Bill button
        if (viewModel.canPrintBill(orderShort)) {
            Button(
                onClick = { viewModel.onEvent(PosUiEvent.PrintBill(order.id)) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Icon(Icons.Outlined.Print, null, Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Print Bill")
            }
        }

        // Mark Paid button
        if (viewModel.canMarkPaid(orderShort)) {
            Button(
                onClick = { viewModel.onEvent(PosUiEvent.MarkPaid(order.id)) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.CheckCircle, null, Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mark Paid")
            }
        }

        // Cancel button
        if (viewModel.canCancelOrder(orderShort)) {
            OutlinedButton(
                onClick = { viewModel.onEvent(PosUiEvent.CancelOrder(order.id)) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Outlined.Cancel, null, Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun PosOrderItemCard(
    name: String,
    subtitle: String,
    price: Double,
    df: DecimalFormat,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bgColor by animateColorAsState(
        targetValue = if (isHovered) 
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else 
            MaterialTheme.colorScheme.surface,
        animationSpec = tween(AppAnimations.DURATION_FAST)
    )

    Card(
        modifier = Modifier.fillMaxWidth().hoverable(interactionSource),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp).size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "৳ ${df.format(price)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun PosDetailLoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp), color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Loading order...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PosDetailErrorContent(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.errorContainer) {
            Icon(Icons.Default.Error, null, Modifier.padding(16.dp).size(40.dp), tint = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Failed to load order", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
            Icon(Icons.Outlined.Refresh, null, Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry")
        }
    }
}

@Composable
private fun PosToast(
    message: String,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}
