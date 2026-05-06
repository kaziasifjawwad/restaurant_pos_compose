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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.Payments
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import data.model.BeverageOrder
import data.model.FoodOrder
import data.model.FoodOrderByCustomer
import data.model.OrderStatus
import data.model.PaymentMethod
import data.model.PaymentMethodResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.theme.AppAnimations
import ui.viewmodel.PosUiEvent
import ui.viewmodel.PosViewModel
import java.text.DecimalFormat

private val Goldenrod = Color(0xFFD4AF37)
private val Slate900 = Color(0xFF0F172A)
private val SuccessGreen = Color(0xFF16A34A)
private val SoftSuccess = Color(0xFFEAF7ED)

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
                    listOf(
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
                onEdit = { onNavigateToEdit(orderId) },
                onPrint = { viewModel.onEvent(PosUiEvent.PrintBill(orderId)) },
                onRefund = { viewModel.onEvent(PosUiEvent.ShowError("Refund flow is not available yet")) }
            )

            val closingAfterSuccess = uiState.successMessage != null && uiState.selectedOrder == null
            when {
                uiState.isLoadingDetail -> PosDetailLoadingContent()
                uiState.selectedOrder != null -> AnimatedVisibility(
                    visible = isContentVisible,
                    enter = fadeIn(animationSpec = tween(AppAnimations.DURATION_ENTRANCE))
                ) {
                    OrderDetailsCheckoutContent(
                        order = uiState.selectedOrder!!,
                        paymentMethods = uiState.paymentMethods,
                        viewModel = viewModel
                    )
                }
                closingAfterSuccess -> PosDetailClosingContent()
                else -> PosDetailErrorContent(onRetry = { viewModel.onEvent(PosUiEvent.LoadOrderDetail(orderId)) })
            }
        }

        AnimatedVisibility(
            visible = uiState.errorMessage != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            PosToast(message = uiState.errorMessage.orEmpty(), isError = true, modifier = Modifier.padding(16.dp))
        }

        AnimatedVisibility(
            visible = uiState.successMessage != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            PosToast(
                message = uiState.successMessage.toPremiumSuccessMessage(),
                isError = false,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
private fun CheckoutHeader(
    order: FoodOrderByCustomer?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onPrint: () -> Unit,
    onRefund: () -> Unit
) {
    val isReadOnlyReceipt = order?.orderStatus == OrderStatus.PAID
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 5.dp
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
            val titleBlock: @Composable () -> Unit = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Goldenrod)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            text = if (isReadOnlyReceipt) "Order Details" else order?.let { "Order #${it.id}" } ?: "Loading order...",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = order?.let { "Order #${it.id}" } ?: "Order loading...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            val actions: @Composable () -> Unit = {
                when {
                    isReadOnlyReceipt -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        BistroSecondaryButton(text = "Print", icon = Icons.Outlined.Print, onClick = onPrint)
                        BistroDangerButton(text = "Refund", onClick = onRefund)
                    }
                    order != null && order.orderStatus != OrderStatus.CANCELED -> BistroSecondaryButton(text = "Edit", icon = Icons.Outlined.Edit, onClick = onEdit)
                }
            }

            if (maxWidth < 620.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    titleBlock()
                    actions()
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.weight(1f)) { titleBlock() }
                    actions()
                }
            }
        }
    }
}

@Composable
private fun TableBadge(tableNumber: Int) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Goldenrod.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, Goldenrod.copy(alpha = 0.45f))
    ) {
        Text(
            text = "Table #$tableNumber",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Goldenrod,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun SoftStatusPill(status: OrderStatus) {
    val color = when (status) {
        OrderStatus.ORDER_PLACED -> Goldenrod
        OrderStatus.BILL_PRINTED -> Color(0xFF38BDF8)
        OrderStatus.PAID -> SuccessGreen
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
    if (order.orderStatus == OrderStatus.PAID) {
        DigitalReceiptContent(order = order, df = df)
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        val stacked = maxWidth < 980.dp
        if (stacked) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                orderFeedItems(order = order, df = df)
                item {
                    CheckoutEngine(
                        order = order,
                        paymentMethods = paymentMethods,
                        df = df,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxWidth(),
                        fillHeightContent = false
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                OrderFeedColumn(order = order, df = df, modifier = Modifier.weight(0.58f).fillMaxHeight())
                CheckoutEngine(
                    order = order,
                    paymentMethods = paymentMethods,
                    df = df,
                    viewModel = viewModel,
                    modifier = Modifier.weight(0.42f).fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun DigitalReceiptContent(order: FoodOrderByCustomer, df: DecimalFormat) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 32.dp)
    ) {
        item { ReceiptMetaBar(order) }
        item { ReceiptItemsCard(order, df) }
        item { ReceiptFinancialSummary(order, df) }
    }
}

@Composable
private fun ReceiptMetaBar(order: FoodOrderByCustomer) {
    BoxWithConstraints {
        if (maxWidth < 760.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ReceiptInfoCard(emoji = "👤", label = "Waiter", value = order.waiterName ?: "Unknown", modifier = Modifier.fillMaxWidth())
                ReceiptInfoCard(emoji = "🪑", label = "Table", value = "Table ${order.tableNumber}", modifier = Modifier.fillMaxWidth())
                ReceiptInfoCard(emoji = "✅", label = "Status", value = "PAID (${order.paymentMethod?.displayName ?: "N/A"})", modifier = Modifier.fillMaxWidth(), success = true)
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ReceiptInfoCard(
                    emoji = "👤",
                    label = "Waiter",
                    value = order.waiterName ?: "Unknown",
                    modifier = Modifier.weight(1f)
                )
                ReceiptInfoCard(
                    emoji = "🪑",
                    label = "Table",
                    value = "Table ${order.tableNumber}",
                    modifier = Modifier.weight(1f)
                )
                ReceiptInfoCard(
                    emoji = "✅",
                    label = "Status",
                    value = "PAID (${order.paymentMethod?.displayName ?: "N/A"})",
                    modifier = Modifier.weight(1f),
                    success = true
                )
            }
        }
    }
}

@Composable
private fun ReceiptInfoCard(
    emoji: String,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    success: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = if (success) SoftSuccess else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (success) SuccessGreen.copy(alpha = 0.16f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(shape = RoundedCornerShape(12.dp), color = if (success) SuccessGreen.copy(alpha = 0.14f) else Goldenrod.copy(alpha = 0.12f)) {
                Text(emoji, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.titleMedium)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (success) Color(0xFF14532D) else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun ReceiptItemsCard(order: FoodOrderByCustomer, df: DecimalFormat) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("ORDERED ITEMS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("${order.foodOrders.size + order.beverageOrders.size} items", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
            ReceiptHeaderRow()
            if (order.foodOrders.isNotEmpty()) {
                ReceiptSectionHeader("🍽️ FOOD")
                order.foodOrders.forEach { food -> ReceiptFoodRow(food, df) }
            }
            if (order.beverageOrders.isNotEmpty()) {
                ReceiptSectionHeader("🥤 BEVERAGES")
                order.beverageOrders.forEach { beverage -> ReceiptBeverageRow(beverage, df) }
            }
        }
    }
}

@Composable
private fun ReceiptHeaderRow() {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Item", modifier = Modifier.weight(1.9f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text("Price", modifier = Modifier.weight(0.75f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text("Qty", modifier = Modifier.weight(0.55f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text("Subtotal", modifier = Modifier.weight(0.85f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ReceiptSectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 13.dp),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Black,
        color = Goldenrod
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
}

@Composable
private fun ReceiptFoodRow(item: FoodOrder, df: DecimalFormat) {
    ReceiptLineRow(
        name = item.foodName ?: "Food #${item.itemNumber}",
        variant = "Size: ${item.foodSize.name}",
        price = item.foodPrice,
        qty = item.foodQuantity.toString(),
        subtotal = item.foodPrice * item.foodQuantity,
        df = df
    )
}

@Composable
private fun ReceiptBeverageRow(item: BeverageOrder, df: DecimalFormat) {
    ReceiptLineRow(
        name = item.beverageName ?: "Beverage #${item.beverageId}",
        variant = "Volume: ${df.format(item.quantity).trimTrailingZeros()} ${item.unit?.name ?: ""}",
        price = item.price,
        qty = item.amount.toString(),
        subtotal = item.price * item.amount,
        df = df
    )
}

@Composable
private fun ReceiptLineRow(
    name: String,
    variant: String,
    price: Double,
    qty: String,
    subtotal: Double,
    df: DecimalFormat
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.9f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(variant, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("৳${df.format(price)}", modifier = Modifier.weight(0.75f), style = MaterialTheme.typography.bodyMedium)
        Text(qty, modifier = Modifier.weight(0.55f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Text("৳${df.format(subtotal)}", modifier = Modifier.weight(0.85f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Goldenrod)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
}

@Composable
private fun ReceiptFinancialSummary(order: FoodOrderByCustomer, df: DecimalFormat) {
    val lineSubtotal = remember(order) {
        order.foodOrders.sumOf { it.foodPrice * it.foodQuantity } + order.beverageOrders.sumOf { it.price * it.amount }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFFFFF8E1),
            border = BorderStroke(1.dp, Goldenrod.copy(alpha = 0.18f)),
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("FINANCIAL SUMMARY", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                HorizontalDivider(color = Goldenrod.copy(alpha = 0.18f))
                SummaryLine("Subtotal", "৳${df.format(lineSubtotal)}")
                SummaryLine("Discount (0%)", "৳${df.format(order.discount)}")
                HorizontalDivider(color = Goldenrod.copy(alpha = 0.18f))
                SummaryLine("TOTAL PAID", "৳${df.format(order.totalAmount)}", total = true)
                SummaryLine("Payment Method", order.paymentMethod?.displayName?.uppercase() ?: "N/A")
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String, total: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = if (total) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium, fontWeight = if (total) FontWeight.Black else FontWeight.Medium)
        Text(value, style = if (total) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium, fontWeight = if (total) FontWeight.Black else FontWeight.SemiBold, color = if (total) Goldenrod else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun OrderFeedColumn(order: FoodOrderByCustomer, df: DecimalFormat, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        orderFeedItems(order = order, df = df)
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.orderFeedItems(order: FoodOrderByCustomer, df: DecimalFormat) {
    item {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Order Feed", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Grouped food and beverage items for fast review before checkout",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (order.foodOrders.isNotEmpty()) {
        item { FeedSectionTitle("Food", order.foodOrders.size, Icons.Outlined.Restaurant) }
        itemsIndexed(order.foodOrders) { index, item -> FoodItemCard(index + 1, item, df) }
    }

    if (order.beverageOrders.isNotEmpty()) {
        item { FeedSectionTitle("Beverages", order.beverageOrders.size, Icons.Outlined.LocalDrink) }
        itemsIndexed(order.beverageOrders) { index, item -> BeverageItemCard(index + 1, item, df) }
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
        meta = "${item.quantity.toString().trimTrailingZeros()} ${item.unit?.name.orEmpty()}",
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
    val scale by animateFloatAsState(
        if (isPressed) 0.98f else if (isHovered) 1.01f else 1f,
        tween(AppAnimations.DURATION_FAST)
    )
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
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("#$index", style = MaterialTheme.typography.labelSmall, color = Goldenrod, fontWeight = FontWeight.Bold)
                    Text("৳ ${df.format(unitPrice)} each", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)) {
                    Text("× $quantity", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
                Text("৳ ${df.format(lineTotal)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Goldenrod, maxLines = 1)
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
    modifier: Modifier = Modifier,
    fillHeightContent: Boolean = true
) {
    val defaultPaymentMethod = remember(paymentMethods) {
        paymentMethods.firstOrNull { it.active && it.defaultMethod }?.methodCode
            ?: paymentMethods.firstOrNull { it.active }?.methodCode
    }
    var selectedPaymentMethod by remember(order.id) { mutableStateOf(order.paymentMethod ?: defaultPaymentMethod) }
    var isCompleting by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val subtotal = remember(order) {
        order.foodOrders.sumOf { it.foodPrice * it.foodQuantity } + order.beverageOrders.sumOf { it.price * it.amount }
    }

    LaunchedEffect(order.id, defaultPaymentMethod) {
        if (selectedPaymentMethod == null) {
            selectedPaymentMethod = defaultPaymentMethod
        }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = BorderStroke(1.dp, Goldenrod.copy(alpha = 0.18f))
    ) {
        if (fillHeightContent) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                contentPadding = PaddingValues(bottom = 4.dp)
            ) {
                checkoutEngineItems(
                    order = order,
                    paymentMethods = paymentMethods,
                    selectedPaymentMethod = selectedPaymentMethod,
                    onSelectPaymentMethod = { selectedPaymentMethod = it },
                    subtotal = subtotal,
                    df = df,
                    isCompleting = isCompleting,
                    onComplete = {
                        val paymentMethod = selectedPaymentMethod ?: return@checkoutEngineItems
                        isCompleting = true
                        viewModel.onEvent(PosUiEvent.CompleteOrder(order.id, paymentMethod))
                        scope.launch {
                            delay(1200)
                            isCompleting = false
                        }
                    },
                    onPrintBill = { viewModel.onEvent(PosUiEvent.PrintBill(order.id)) },
                    onKitchenMemo = { viewModel.onEvent(PosUiEvent.PrintKitchenMemo(order.id)) },
                    onCancel = { showCancelDialog = true }
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                CheckoutEngineStaticContent(
                    order = order,
                    paymentMethods = paymentMethods,
                    selectedPaymentMethod = selectedPaymentMethod,
                    onSelectPaymentMethod = { selectedPaymentMethod = it },
                    subtotal = subtotal,
                    df = df,
                    isCompleting = isCompleting,
                    onComplete = {
                        val paymentMethod = selectedPaymentMethod ?: return@CheckoutEngineStaticContent
                        isCompleting = true
                        viewModel.onEvent(PosUiEvent.CompleteOrder(order.id, paymentMethod))
                        scope.launch {
                            delay(1200)
                            isCompleting = false
                        }
                    },
                    onPrintBill = { viewModel.onEvent(PosUiEvent.PrintBill(order.id)) },
                    onKitchenMemo = { viewModel.onEvent(PosUiEvent.PrintKitchenMemo(order.id)) },
                    onCancel = { showCancelDialog = true }
                )
            }
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel order?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Please write why this order is being canceled. This reason will be saved and shown in the single order view.")

                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        label = { Text("Cancel reason") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = cancelReason.isNotBlank(),
                    onClick = {
                        viewModel.onEvent(
                            PosUiEvent.CancelOrder(
                                orderId = order.id,
                                reason = cancelReason
                            )
                        )
                        showCancelDialog = false
                    }
                ) {
                    Text("Confirm Cancel")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCancelDialog = false }) {
                    Text("Keep Order")
                }
            }
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.checkoutEngineItems(
    order: FoodOrderByCustomer,
    paymentMethods: List<PaymentMethodResponse>,
    selectedPaymentMethod: PaymentMethod?,
    onSelectPaymentMethod: (PaymentMethod) -> Unit,
    subtotal: Double,
    df: DecimalFormat,
    isCompleting: Boolean,
    onComplete: () -> Unit,
    onPrintBill: () -> Unit,
    onKitchenMemo: () -> Unit,
    onCancel: () -> Unit
) {
    item {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Checkout Engine", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Default payment method is selected from backend configuration", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    item { FinancialBreakdown(subtotal = subtotal, tax = 0.0, discount = order.discount, grandTotal = order.totalAmount, df = df) }
    item { HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)) }

    item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Payment Method", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Payment methods are loaded from backend; the default method is preselected.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PaymentActionTiles(
                paymentMethods = paymentMethods,
                selectedPaymentMethod = selectedPaymentMethod,
                onSelect = onSelectPaymentMethod
            )
        }
    }

    item {
        BistroPrimaryButton(
            text = if (isCompleting) "Completing..." else "Complete Order",
            icon = Icons.Outlined.CheckCircle,
            enabled = selectedPaymentMethod != null && order.orderStatus != OrderStatus.PAID && order.orderStatus != OrderStatus.CANCELED && !isCompleting,
            loading = isCompleting,
            onClick = onComplete
        )
    }

    item { CheckoutSecondaryActions(onPrintBill = onPrintBill, onKitchenMemo = onKitchenMemo) }
    item {
        TextButtonLikeCancel(
            enabled = order.orderStatus != OrderStatus.PAID && order.orderStatus != OrderStatus.CANCELED,
            onClick = onCancel
        )
    }
}

@Composable
private fun CheckoutEngineStaticContent(
    order: FoodOrderByCustomer,
    paymentMethods: List<PaymentMethodResponse>,
    selectedPaymentMethod: PaymentMethod?,
    onSelectPaymentMethod: (PaymentMethod) -> Unit,
    subtotal: Double,
    df: DecimalFormat,
    isCompleting: Boolean,
    onComplete: () -> Unit,
    onPrintBill: () -> Unit,
    onKitchenMemo: () -> Unit,
    onCancel: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Checkout Engine", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Default payment method is selected from backend configuration", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    FinancialBreakdown(subtotal = subtotal, tax = 0.0, discount = order.discount, grandTotal = order.totalAmount, df = df)
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Payment Method", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Payment methods are loaded from backend; the default method is preselected.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        PaymentActionTiles(
            paymentMethods = paymentMethods,
            selectedPaymentMethod = selectedPaymentMethod,
            onSelect = onSelectPaymentMethod
        )
    }

    BistroPrimaryButton(
        text = if (isCompleting) "Completing..." else "Complete Order",
        icon = Icons.Outlined.CheckCircle,
        enabled = selectedPaymentMethod != null && order.orderStatus != OrderStatus.PAID && order.orderStatus != OrderStatus.CANCELED && !isCompleting,
        loading = isCompleting,
        onClick = onComplete
    )

    CheckoutSecondaryActions(onPrintBill = onPrintBill, onKitchenMemo = onKitchenMemo)
    TextButtonLikeCancel(
        enabled = order.orderStatus != OrderStatus.PAID && order.orderStatus != OrderStatus.CANCELED,
        onClick = onCancel
    )
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
    val options = remember(paymentMethods) {
        paymentMethods
            .filter { it.active }
            .map {
                PaymentTileOption(
                    title = it.methodCode.tileTitle(),
                    subtitle = it.displayName,
                    icon = it.methodCode.tileIcon(),
                    method = it.methodCode,
                    isDefault = it.defaultMethod
                )
            }
    }

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

    BoxWithConstraints {
        val rowSize = if (maxWidth < 520.dp) 1 else 2
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            options.chunked(rowSize).forEach { rowOptions ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowOptions.forEach { option ->
                        PaymentTile(
                            option = option,
                            selected = selectedPaymentMethod == option.method,
                            modifier = Modifier.weight(1f),
                            onClick = { onSelect(option.method) }
                        )
                    }
                    repeat(rowSize - rowOptions.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private data class PaymentTileOption(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val method: PaymentMethod,
    val isDefault: Boolean
)

private fun PaymentMethod.tileTitle(): String = when (this) {
    PaymentMethod.CASH -> "Cash"
    PaymentMethod.CREDIT_CARD -> "Card"
    PaymentMethod.BKASH,
    PaymentMethod.ROCKET,
    PaymentMethod.NAGAD -> "Mobile Pay"
    PaymentMethod.PLATFORM_ONLINE,
    PaymentMethod.PLATFORM_CASH,
    PaymentMethod.PLATFORM_SETTLEMENT,
    PaymentMethod.CASH_ON_DELIVERY -> "Takeout Pay"
}

private fun PaymentMethod.tileIcon(): ImageVector = when (this) {
    PaymentMethod.CASH -> Icons.Outlined.Payments
    PaymentMethod.CREDIT_CARD -> Icons.Outlined.CreditCard
    PaymentMethod.BKASH,
    PaymentMethod.ROCKET,
    PaymentMethod.NAGAD -> Icons.Outlined.PhoneAndroid
    PaymentMethod.PLATFORM_ONLINE,
    PaymentMethod.PLATFORM_CASH,
    PaymentMethod.PLATFORM_SETTLEMENT,
    PaymentMethod.CASH_ON_DELIVERY -> Icons.Outlined.Payments
}

@Composable
private fun PaymentTile(option: PaymentTileOption, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val borderColor by animateColorAsState(
        if (selected) Goldenrod.copy(alpha = 0.88f) else if (isHovered) Goldenrod.copy(alpha = 0.42f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
        tween(AppAnimations.DURATION_FAST)
    )
    val backgroundColor by animateColorAsState(
        if (selected) Goldenrod.copy(alpha = 0.08f) else if (isHovered) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
        tween(AppAnimations.DURATION_FAST)
    )
    val scale by animateFloatAsState(if (isPressed) 0.98f else if (isHovered) 1.01f else 1f, tween(AppAnimations.DURATION_FAST))

    Surface(
        modifier = modifier
            .height(112.dp)
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(18.dp),
        color = backgroundColor,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
        shadowElevation = if (selected) 2.dp else if (isHovered) 3.dp else 0.dp
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
                Surface(shape = RoundedCornerShape(999.dp), color = Goldenrod.copy(alpha = 0.95f), modifier = Modifier.align(Alignment.TopEnd)) {
                    Icon(Icons.Default.Check, null, tint = Slate900, modifier = Modifier.padding(3.dp).size(13.dp))
                }
            } else if (option.isDefault) {
                Surface(shape = RoundedCornerShape(999.dp), color = Goldenrod.copy(alpha = 0.14f), modifier = Modifier.align(Alignment.TopEnd)) {
                    Text(
                        "Default",
                        color = Goldenrod,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckoutSecondaryActions(
    onPrintBill: () -> Unit,
    onKitchenMemo: () -> Unit
) {
    BoxWithConstraints {
        if (maxWidth < 430.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                BistroSecondaryButton(
                    text = "Print Bill",
                    icon = Icons.Outlined.Print,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onPrintBill
                )
                BistroSecondaryButton(
                    text = "Kitchen Memo",
                    icon = Icons.Outlined.Restaurant,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onKitchenMemo
                )
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                BistroSecondaryButton(
                    text = "Print Bill",
                    icon = Icons.Outlined.Print,
                    modifier = Modifier.weight(1f),
                    onClick = onPrintBill
                )
                BistroSecondaryButton(
                    text = "Kitchen Memo",
                    icon = Icons.Outlined.Restaurant,
                    modifier = Modifier.weight(1f),
                    onClick = onKitchenMemo
                )
            }
        }
    }
}

@Composable
private fun BistroPrimaryButton(text: String, icon: ImageVector, enabled: Boolean, loading: Boolean, onClick: () -> Unit) {
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
        if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Slate900)
        else Icon(icon, null, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun BistroSecondaryButton(text: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
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
private fun BistroDangerButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val danger = Color(0xFFDC2626)
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, danger.copy(alpha = 0.40f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = danger)
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TextButtonLikeCancel(enabled: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = "Cancel Order",
            modifier = Modifier
                .hoverable(interactionSource)
                .clickable(enabled = enabled, interactionSource = interactionSource, indication = null, onClick = onClick)
                .padding(8.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (!enabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
            else if (isHovered) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.error.copy(alpha = 0.72f)
        )
    }
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
                tint = if (isError) MaterialTheme.colorScheme.error else Color(0xFF15803D),
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = message,
                color = if (isError) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF14532D),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun String?.toPremiumSuccessMessage(): String =
    if (this.equals("Order completed successfully", ignoreCase = true)) "Order Completed Successfully" else this.orEmpty()

private fun String.trimTrailingZeros(): String = removeSuffix(".00").removeSuffix(".0")
