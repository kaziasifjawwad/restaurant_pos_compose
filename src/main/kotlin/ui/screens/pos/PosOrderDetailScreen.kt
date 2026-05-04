package ui.screens.pos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import data.model.BeverageOrder
import data.model.DiscountType
import data.model.FoodOrder
import data.model.FoodOrderByCustomer
import data.model.OrderStatus
import data.model.PaymentMethod
import data.model.PaymentMethodResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.theme.AppAnimations
import ui.theme.ExtendedTypography
import ui.viewmodel.PosUiEvent
import ui.viewmodel.PosViewModel
import java.text.DecimalFormat

private val Goldenrod = Color(0xFFD4AF37)
private val Slate900 = Color(0xFF0F172A)
private val Slate800 = Color(0xFF1E293B)
private val Slate700 = Color(0xFF334155)
private val Slate100 = Color(0xFFF1F5F9)

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
        viewModel.onEvent(PosUiEvent.RefreshPaymentMethods)
        delay(100)
        isContentVisible = true
    }

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
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.10f)
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CheckoutHeader(
                order = uiState.selectedOrder,
                onBack = {
                    viewModel.onEvent(PosUiEvent.ClearSelectedOrder)
                    onNavigateBack()
                },
                onEdit = { onNavigateToEdit(orderId) }
            )

            val isClosingAfterSuccessfulAction = uiState.successMessage != null && uiState.selectedOrder == null
            when {
                uiState.isLoadingDetail -> PosDetailLoadingContent()
                uiState.selectedOrder != null -> {
                    AnimatedVisibility(
                        visible = isContentVisible,
                        enter = fadeIn(animationSpec = tween(AppAnimations.DURATION_ENTRANCE))
                    ) {
                        OrderDetailsCheckoutContent(
                            order = uiState.selectedOrder!!,
                            paymentMethods = uiState.paymentMethods,
                            viewModel = viewModel
                        )
                    }
                }
                isClosingAfterSuccessfulAction -> PosDetailClosingContent()
                else -> PosDetailErrorContent(onRetry = { viewModel.onEvent(PosUiEvent.LoadOrderDetail(orderId)) })
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
    }
}

@Composable
private fun CheckoutHeader(
    order: FoodOrderByCustomer?,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 5.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = order?.let { "Order #${it.id}" } ?: "Loading order...",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (order != null) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = Goldenrod.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, Goldenrod.copy(alpha = 0.45f))
                            ) {
                                Text(
                                    text = "Table #${order.tableNumber}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Goldenrod,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = order?.waiterName ?: "Waiter loading...",
                            style = ExtendedTypography.caption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (order != null) {
                            SoftStatusPill(order.orderStatus)
                        }
                    }
                }
            }

            if (order != null && order.orderStatus != OrderStatus.PAID && order.orderStatus != OrderStatus.CANCELED) {
                BistroSecondaryButton(text = "Edit", icon = Icons.Outlined.Edit, onClick = onEdit)
            }
        }
    }
}

@Composable
private fun SoftStatusPill(status: OrderStatus) {
    val color = when (status) {
        OrderStatus.ORDER_PLACED -> Goldenrod
        OrderStatus.BILL_PRINTED -> Color(0xFF38BDF8)
        OrderStatus.PAID -> Color(0xFF22C55E)
        OrderStatus.CANCELED -> Color(0xFFEF4444)
    }
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.14f),
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Text(
            text = status.displayName.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun OrderDetailsCheckoutContent(
    order: FoodOrderByCustomer,
    paymentMethods: List<PaymentMethodResponse>,
    viewModel: PosViewModel
) {
    val df = remember { DecimalFormat("#,##0.00") }
    Row(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        OrderFeedColumn(
            order = order,
            df = df,
            modifier = Modifier.weight(0.60f).fillMaxHeight()
        )
        CheckoutEngine(
            order = order,
            paymentMethods = paymentMethods,
            df = df,
            viewModel = viewModel,
            modifier = Modifier.weight(0.40f).fillMaxHeight()
        )
    }
}

@Composable
private fun OrderFeedColumn(order: FoodOrderByCustomer, df: DecimalFormat, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Order Feed",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Grouped kitchen and beverage items with live checkout summary",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (order.foodOrders.isNotEmpty()) {
            item { FeedSectionTitle("Food", order.foodOrders.size, Icons.Outlined.Restaurant) }
            itemsIndexed(order.foodOrders) { index, item ->
                FoodItemCard(index = index + 1, item = item, df = df)
            }
        }

        if (order.beverageOrders.isNotEmpty()) {
            item { FeedSectionTitle("Beverages", order.beverageOrders.size, Icons.Outlined.LocalDrink) }
            itemsIndexed(order.beverageOrders) { index, item ->
                BeverageItemCard(index = index + 1, item = item, df = df)
            }
        }
    }
}

@Composable
private fun FeedSectionTitle(title: String, count: Int, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(shape = RoundedCornerShape(12.dp), color = Goldenrod.copy(alpha = 0.13f)) {
            Icon(icon, null, tint = Goldenrod, modifier = Modifier.padding(8.dp).size(20.dp))
        }
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)) {
            Text(
                "$count item${if (count > 1) "s" else ""}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun FoodItemCard(index: Int, item: FoodOrder, df: DecimalFormat) {
    PremiumItemCard(
        index = index,
        icon = Icons.Outlined.Restaurant,
        title = item.foodName ?: "Food #${item.itemNumber}",
        meta = "${item.foodSize.name} package",
        quantity = item.foodQuantity.toString(),
        unitPrice = item.foodPrice,
        lineTotal = item.foodPrice * item.foodQuantity,
        df = df
    )
}

@Composable
private fun BeverageItemCard(index: Int, item: BeverageOrder, df: DecimalFormat) {
    PremiumItemCard(
        index = index,
        icon = Icons.Outlined.LocalDrink,
        title = item.beverageName ?: "Beverage #${item.beverageId}",
        meta = "${item.quantity} ${item.unit?.name ?: ""}",
        quantity = item.amount.toString(),
        unitPrice = item.price,
        lineTotal = item.price * item.amount,
        df = df
    )
}

@Composable
private fun PremiumItemCard(
    index: Int,
    icon: ImageVector,
    title: String,
    meta: String,
    quantity: String,
    unitPrice: Double,
    lineTotal: Double,
    df: DecimalFormat
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val elevation by animateDpAsState(if (isHovered) 8.dp else 2.dp, tween(AppAnimations.DURATION_FAST))
    val scale by animateFloatAsState(if (isPressed) 0.98f else if (isHovered) 1.01f else 1f, tween(AppAnimations.DURATION_FAST))
    val borderColor by animateColorAsState(
        if (isHovered) Goldenrod.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
        tween(AppAnimations.DURATION_FAST)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) {}
            .graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(shape = RoundedCornerShape(16.dp), color = Goldenrod.copy(alpha = 0.14f)) {
                Icon(icon, null, tint = Goldenrod, modifier = Modifier.padding(12.dp).size(24.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("#$index", style = MaterialTheme.typography.labelSmall, color = Goldenrod, fontWeight = FontWeight.Bold)
                    Text("৳ ${df.format(unitPrice)} each", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)) {
                    Text("× $quantity", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
                Text("৳ ${df.format(lineTotal)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Goldenrod)
            }
        }
    }
}

@Composable
private fun CheckoutEngine(
    order: FoodOrderByCustomer,
    paymentMethods: List<PaymentMethodResponse>,
    df: DecimalFormat,
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    var selectedPaymentMethod by remember(order.id, paymentMethods) { mutableStateOf<PaymentMethod?>(null) }
    var isCompleting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val subtotal = remember(order) {
        order.foodOrders.sumOf { it.foodPrice * it.foodQuantity } +
            order.beverageOrders.sumOf { it.price * it.amount }
    }
    val tax = 0.0
    val discount = order.discount
    val grandTotal = order.totalAmount

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = BorderStroke(1.dp, Goldenrod.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Checkout Engine", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Choose a payment method to unlock completion", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            FinancialBreakdown(subtotal = subtotal, tax = tax, discount = discount, grandTotal = grandTotal, df = df)

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Payment Method", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("No payment is captured until Complete Order is confirmed.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                PaymentActionTiles(
                    paymentMethods = paymentMethods,
                    selectedPaymentMethod = selectedPaymentMethod,
                    onSelect = { selectedPaymentMethod = it }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            BistroPrimaryButton(
                text = if (isCompleting) "Completing..." else "Complete Order",
                icon = Icons.Outlined.CheckCircle,
                enabled = selectedPaymentMethod != null && order.orderStatus != OrderStatus.PAID && order.orderStatus != OrderStatus.CANCELED && !isCompleting,
                loading = isCompleting,
                onClick = {
                    val paymentMethod = selectedPaymentMethod ?: return@BistroPrimaryButton
                    isCompleting = true
                    viewModel.onEvent(PosUiEvent.CompleteOrder(order.id, paymentMethod))
                    scope.launch {
                        delay(1200)
                        isCompleting = false
                    }
                }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                BistroSecondaryButton(
                    text = "Print Bill",
                    icon = Icons.Outlined.Print,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onEvent(PosUiEvent.PrintBill(order.id)) }
                )
                BistroSecondaryButton(
                    text = "Kitchen Memo",
                    icon = Icons.Outlined.Restaurant,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onEvent(PosUiEvent.PrintKitchenMemo(order.id)) }
                )
            }

            TextButtonLikeCancel(
                enabled = order.orderStatus != OrderStatus.PAID && order.orderStatus != OrderStatus.CANCELED,
                onClick = { viewModel.onEvent(PosUiEvent.CancelOrder(order.id)) }
            )
        }
    }
}

@Composable
private fun FinancialBreakdown(subtotal: Double, tax: Double, discount: Double, grandTotal: Double, df: DecimalFormat) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.10f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BreakdownLine("Subtotal", "৳ ${df.format(subtotal)}")
            BreakdownLine("Tax (0%)", "৳ ${df.format(tax)}")
            BreakdownLine("Discount", "৳ ${df.format(discount)}")
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text("Grand Total", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Payable amount", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f))
                }
                Text("৳ ${df.format(grandTotal)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Goldenrod)
            }
        }
    }
}

@Composable
private fun BreakdownLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PaymentActionTiles(
    paymentMethods: List<PaymentMethodResponse>,
    selectedPaymentMethod: PaymentMethod?,
    onSelect: (PaymentMethod) -> Unit
) {
    val cash = paymentMethods.firstOrNull { it.methodCode == PaymentMethod.CASH }
    val card = paymentMethods.firstOrNull { it.methodCode == PaymentMethod.CREDIT_CARD }
    val mobile = paymentMethods
        .filter { it.methodCode == PaymentMethod.BKASH || it.methodCode == PaymentMethod.ROCKET || it.methodCode == PaymentMethod.NAGAD }
        .let { methods -> methods.firstOrNull { it.defaultMethod } ?: methods.firstOrNull() }

    val options = listOfNotNull(
        cash?.let { PaymentTileOption("Cash", it.displayName, Icons.Outlined.Payments, it) },
        card?.let { PaymentTileOption("Card", it.displayName, Icons.Outlined.CreditCard, it) },
        mobile?.let { PaymentTileOption("Mobile Pay", it.displayName, Icons.Outlined.PhoneAndroid, it) }
    )

    if (options.isEmpty()) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
        ) {
            Text(
                "No active payment method found. Please configure payment methods first.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(14.dp)
            )
        }
        return
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { option ->
            PaymentTile(
                option = option,
                selected = selectedPaymentMethod == option.method.methodCode,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(option.method.methodCode) }
            )
        }
    }
}

private data class PaymentTileOption(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val method: PaymentMethodResponse
)

@Composable
private fun PaymentTile(option: PaymentTileOption, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val borderColor by animateColorAsState(if (selected) Goldenrod else if (isHovered) Goldenrod.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.16f), tween(AppAnimations.DURATION_FAST))
    val backgroundColor by animateColorAsState(if (selected) Goldenrod.copy(alpha = 0.14f) else if (isHovered) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f), tween(AppAnimations.DURATION_FAST))
    val scale by animateFloatAsState(if (isPressed) 0.98f else if (selected) 1.04f else 1f, tween(AppAnimations.DURATION_FAST))

    Surface(
        modifier = modifier
            .height(112.dp)
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(18.dp),
        color = backgroundColor,
        border = BorderStroke(if (selected) 3.dp else 1.dp, borderColor),
        shadowElevation = if (selected) 6.dp else if (isHovered) 4.dp else 0.dp
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(option.icon, null, tint = if (selected) Goldenrod else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                Column {
                    Text(option.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text(option.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (selected) {
                Surface(shape = RoundedCornerShape(999.dp), color = Goldenrod) {
                    Icon(Icons.Default.Check, null, tint = Slate900, modifier = Modifier.padding(4.dp).size(14.dp))
                }
            }
        }
    }
}

@Composable
private fun BistroPrimaryButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, tween(AppAnimations.DURATION_FAST))

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = Modifier.fillMaxWidth().height(58.dp).graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Goldenrod,
            contentColor = Slate900,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Slate900)
        } else {
            Icon(icon, null, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(text, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun BistroSecondaryButton(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, tween(AppAnimations.DURATION_FAST))

    OutlinedButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier.hoverable(interactionSource).height(46.dp).graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (isHovered) Goldenrod.copy(alpha = 0.75f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isHovered) Goldenrod else MaterialTheme.colorScheme.onSurface)
    ) {
        Icon(icon, null, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TextButtonLikeCancel(enabled: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    Text(
        text = "Cancel Order",
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .hoverable(interactionSource)
            .clickable(enabled = enabled, interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(8.dp),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = if (!enabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f) else if (isHovered) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.error.copy(alpha = 0.72f)
    )
}

@Composable
private fun PosDetailLoadingContent() {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp), color = Goldenrod)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Loading order...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PosDetailClosingContent() {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator(modifier = Modifier.size(36.dp), color = Goldenrod)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Returning to orders...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PosDetailErrorContent(onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
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
private fun PosToast(message: String, isError: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (isError) MaterialTheme.colorScheme.errorContainer else Color(0xFFDDF8E7),
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (isError) MaterialTheme.colorScheme.error else Color(0xFF15803D)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isError) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF14532D)
            )
        }
    }
}
