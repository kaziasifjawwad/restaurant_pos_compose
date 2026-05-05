package ui.screens.takeout.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import data.model.TakeoutBeverageOrderResponse
import data.model.TakeoutFoodOrderResponse
import data.model.TakeoutOrderResponse
import java.text.DecimalFormat

private val Gold = Color(0xFFD4AF37)

@Composable
fun TakeoutOrderedItemsTable(order: TakeoutOrderResponse) {
    val df = remember { DecimalFormat("#,##0.00") }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("ORDERED ITEMS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("${order.foodOrders.size + order.beverageOrders.size} items", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
            TakeoutReceiptHeaderRow()
            if (order.foodOrders.isNotEmpty()) {
                TakeoutSectionHeader("🍽️ FOOD")
                order.foodOrders.forEach { TakeoutFoodRow(it, df) }
            }
            if (order.beverageOrders.isNotEmpty()) {
                TakeoutSectionHeader("🥤 BEVERAGES")
                order.beverageOrders.forEach { TakeoutBeverageRow(it, df) }
            }
        }
    }
}

@Composable
private fun TakeoutReceiptHeaderRow() {
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)).padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Item", modifier = Modifier.weight(2f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text("Price", modifier = Modifier.weight(0.75f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text("Qty", modifier = Modifier.weight(0.55f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text("Subtotal", modifier = Modifier.weight(0.85f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TakeoutSectionHeader(title: String) {
    Text(title, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 13.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = Gold)
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f))
}

@Composable
private fun TakeoutFoodRow(item: TakeoutFoodOrderResponse, df: DecimalFormat) {
    TakeoutLineRow(
        name = item.foodName ?: "Food",
        variant = "Size: ${item.foodSize?.name ?: "N/A"}",
        price = item.foodPrice,
        qty = item.foodQuantity.toString(),
        subtotal = item.lineTotal,
        df = df
    )
}

@Composable
private fun TakeoutBeverageRow(item: TakeoutBeverageOrderResponse, df: DecimalFormat) {
    TakeoutLineRow(
        name = item.beverageName ?: "Beverage",
        variant = "Volume: ${formatCompact(item.quantity)} ${item.unit?.name ?: ""}",
        price = item.price,
        qty = item.amount.toString(),
        subtotal = item.lineTotal,
        df = df
    )
}

@Composable
private fun TakeoutLineRow(name: String, variant: String, price: Double, qty: String, subtotal: Double, df: DecimalFormat) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(2f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(variant, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("৳${df.format(price)}", modifier = Modifier.weight(0.75f), style = MaterialTheme.typography.bodyMedium)
        Text(qty, modifier = Modifier.weight(0.55f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Text("৳${df.format(subtotal)}", modifier = Modifier.weight(0.85f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Gold)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f))
}

private fun formatCompact(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
