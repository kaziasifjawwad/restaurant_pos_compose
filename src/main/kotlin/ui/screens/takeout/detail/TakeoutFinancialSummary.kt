package ui.screens.takeout.detail

import androidx.compose.foundation.BorderStroke
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
import data.model.TakeoutOrderResponse
import java.text.DecimalFormat

private val Gold = Color(0xFFD4AF37)

@Composable
fun TakeoutFinancialSummary(order: TakeoutOrderResponse) {
    val df = remember { DecimalFormat("#,##0.00") }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(
            modifier = Modifier.width(480.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, Gold.copy(alpha = 0.26f)),
            shadowElevation = 2.dp
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("FINANCIAL SUMMARY", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                SummaryLine("Items Subtotal", "৳${df.format(order.subtotalAmount)}")
                SummaryLine("Discount", "৳${df.format(order.discountAmount)}")
                SummaryLine("Packaging Charge", "৳${df.format(order.packagingCharge)}")
                SummaryLine("Delivery Charge", "৳${df.format(order.deliveryCharge)}")
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                SummaryLine("Customer Payable / Gross Total", "৳${df.format(order.totalAmount)}", total = true)
                SummaryLine("Platform Commission Deduction", "-৳${df.format(order.platformCommissionAmount)}")
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                SummaryLine("Restaurant Receivable", "৳${df.format(order.restaurantReceivableAmount)}", total = true)
                SummaryLine("Payment Status", order.paymentStatus.displayName)
                SummaryLine("Settlement Status", order.settlementStatus.displayName)
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String, total: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = if (total) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (total) FontWeight.Black else FontWeight.Medium,
            color = if (total) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = if (total) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium,
            fontWeight = if (total) FontWeight.Black else FontWeight.SemiBold,
            color = if (total) Gold else MaterialTheme.colorScheme.onSurface
        )
    }
}
