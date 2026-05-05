package ui.screens.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import data.model.BeverageOrderItem
import data.model.FoodOrderItem
import data.model.PosOrderDetailResponse
import data.network.ReportApiService
import kotlinx.coroutines.launch
import java.text.DecimalFormat

private val Goldenrod = Color(0xFFD4AF37)
private val PaidGreen = Color(0xFF16A34A)
private val CanceledRed = Color(0xFFDC2626)

@Composable
fun PosOrderDetailScreen(orderId: String, onNavigateBack: () -> Unit) {
    val api = remember { ReportApiService() }
    val scope = rememberCoroutineScope()
    var orderDetail by remember { mutableStateOf<PosOrderDetailResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(orderId) {
        scope.launch {
            isLoading = true
            errorMessage = null
            try { orderDetail = api.getPosOrderDetail(orderId) }
            catch (e: Exception) { errorMessage = "Failed to load order details: ${e.message}" }
            finally { isLoading = false }
        }
    }

    Scaffold(topBar = { OrderDetailTopBar(orderId = orderId, onNavigateBack = onNavigateBack) }) { paddingValues ->
        Box(Modifier.padding(paddingValues).fillMaxSize().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))))) {
            when {
                isLoading -> OrderDetailLoading()
                errorMessage != null -> OrderDetailError(errorMessage = errorMessage.orEmpty(), onNavigateBack = onNavigateBack)
                orderDetail != null -> OrderDetailContent(orderDetail = orderDetail!!)
            }
        }
    }
}

@Composable
private fun OrderDetailTopBar(orderId: String, onNavigateBack: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Goldenrod) }
            Spacer(Modifier.width(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Order Details", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                Text("Order #$orderId", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun OrderDetailContent(orderDetail: PosOrderDetailResponse) {
    val df = remember { DecimalFormat("#,##0.00") }
    val isCanceled = orderDetail.orderStatus.equals("CANCELED", ignoreCase = true)
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(top = 20.dp, bottom = 32.dp)) {
        item { ReceiptMetaBar(orderDetail) }
        if (isCanceled) item { CancelReasonCard(orderDetail.cancelReason) }
        item { OrderedItemsCard(orderDetail, df) }
        if (!isCanceled) item { FinancialSummary(orderDetail, df) }
    }
}

@Composable
private fun CancelReasonCard(cancelReason: String?) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = CanceledRed.copy(alpha = 0.08f), shadowElevation = 1.dp, border = androidx.compose.foundation.BorderStroke(1.dp, CanceledRed.copy(alpha = 0.22f))) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("CANCEL REASON", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = CanceledRed)
            Text(cancelReason?.takeIf { it.isNotBlank() } ?: "No cancel reason recorded", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun ReceiptMetaBar(orderDetail: PosOrderDetailResponse) {
    val isCanceled = orderDetail.orderStatus.equals("CANCELED", ignoreCase = true)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        MetaInfoCard(Icons.Filled.Person, "Waiter", orderDetail.waiterName, Goldenrod, Modifier.weight(1f))
        MetaInfoCard(Icons.Filled.TableRestaurant, "Table", "Table ${orderDetail.tableNumber}", Goldenrod.copy(alpha = 0.78f), Modifier.weight(1f))
        MetaInfoCard(if (isCanceled) Icons.Filled.Cancel else Icons.Filled.CheckCircle, "Status", buildStatusText(orderDetail), if (isCanceled) CanceledRed else PaidGreen, Modifier.weight(1f))
    }
}

@Composable
private fun MetaInfoCard(icon: ImageVector, label: String, value: String, tint: Color, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = RoundedCornerShape(12.dp), color = tint.copy(alpha = 0.14f)) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.padding(10.dp).size(24.dp)) }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun OrderedItemsCard(orderDetail: PosOrderDetailResponse, df: DecimalFormat) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("ORDERED ITEMS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                Text("${orderDetail.foodOrders.size + orderDetail.beverageOrders.size} items", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
            ReceiptHeaderRow()
            if (orderDetail.foodOrders.isNotEmpty()) { ReceiptSectionHeader("🍽️ FOOD"); orderDetail.foodOrders.forEach { ReceiptFoodRow(it, df) } }
            if (orderDetail.beverageOrders.isNotEmpty()) { ReceiptSectionHeader("🥤 BEVERAGES"); orderDetail.beverageOrders.forEach { ReceiptBeverageRow(it, df) } }
        }
    }
}

@Composable
private fun ReceiptHeaderRow() {
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)).padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Item", modifier = Modifier.weight(2f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text("Price", modifier = Modifier.weight(0.75f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text("Qty", modifier = Modifier.weight(0.55f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text("Subtotal", modifier = Modifier.weight(0.85f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ReceiptSectionHeader(title: String) {
    Text(title, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 13.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = Goldenrod)
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f))
}

@Composable
private fun ReceiptFoodRow(item: FoodOrderItem, df: DecimalFormat) = ReceiptLineRow(item.foodName, "Size: ${item.foodSize}", item.foodPrice, item.foodQuantity.toString(), item.foodPrice * item.foodQuantity, df)

@Composable
private fun ReceiptBeverageRow(item: BeverageOrderItem, df: DecimalFormat) = ReceiptLineRow(item.beverageName, "Volume: ${formatCompact(item.quantity)} ${item.unit}", item.price, item.amount.toString(), item.price * item.amount, df)

@Composable
private fun ReceiptLineRow(name: String, variant: String, price: Double, qty: String, subtotal: Double, df: DecimalFormat) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(2f), verticalArrangement = Arrangement.spacedBy(3.dp)) { Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface); Text(variant, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Text("৳${df.format(price)}", modifier = Modifier.weight(0.75f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(qty, modifier = Modifier.weight(0.55f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text("৳${df.format(subtotal)}", modifier = Modifier.weight(0.85f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Goldenrod)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f))
}

@Composable
private fun FinancialSummary(orderDetail: PosOrderDetailResponse, df: DecimalFormat) {
    val subtotal = remember(orderDetail) { orderDetail.foodOrders.sumOf { it.foodPrice * it.foodQuantity } + orderDetail.beverageOrders.sumOf { it.price * it.amount } }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(Modifier.width(440.dp), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, Goldenrod.copy(alpha = 0.26f)), shadowElevation = 2.dp) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("FINANCIAL SUMMARY", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                SummaryLine("Subtotal", "৳${df.format(subtotal)}")
                SummaryLine(discountLabel(orderDetail), "৳${df.format(orderDetail.discount)}")
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                SummaryLine("TOTAL PAID", "৳${df.format(orderDetail.totalAmount)}", total = true)
                SummaryLine("Payment Method", orderDetail.paymentMethod?.uppercase() ?: "N/A")
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String, total: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = if (total) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium, fontWeight = if (total) FontWeight.Black else FontWeight.Medium, color = if (total) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = if (total) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium, fontWeight = if (total) FontWeight.Black else FontWeight.SemiBold, color = if (total) Goldenrod else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun OrderDetailLoading() { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(color = Goldenrod); Spacer(Modifier.height(16.dp)); Text("Loading order details...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface) } } }

@Composable
private fun OrderDetailError(errorMessage: String, onNavigateBack: () -> Unit) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Filled.Error, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error); Spacer(Modifier.height(16.dp)); Text(errorMessage, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error); Spacer(Modifier.height(16.dp)); Button(onClick = onNavigateBack) { Text("Go Back") } } } }

private fun buildStatusText(orderDetail: PosOrderDetailResponse): String { val status = orderDetail.orderStatus.uppercase(); val payment = orderDetail.paymentMethod?.uppercase(); val isCanceled = status == "CANCELED"; return if (!isCanceled && !payment.isNullOrBlank()) "$status ($payment)" else status }
private fun discountLabel(orderDetail: PosOrderDetailResponse): String = if (orderDetail.discount <= 0.0) "Discount (0%)" else if (orderDetail.discountType == "PERCENTAGE") "Discount (${formatCompact(orderDetail.discount)}%)" else "Discount"
private fun formatCompact(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
