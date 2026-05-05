package ui.screens.takeout.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import data.model.TakeoutOrderResponse

private val Gold = Color(0xFFD4AF37)
private val Green = Color(0xFF16A34A)

@Composable
fun TakeoutDetailMetaBar(order: TakeoutOrderResponse) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        TakeoutDetailMetaCard(
            icon = Icons.Filled.DeliveryDining,
            label = "Medium",
            value = order.mediumName ?: order.mediumCode,
            tint = Gold,
            modifier = Modifier.weight(1f)
        )
        TakeoutDetailMetaCard(
            icon = Icons.Filled.ReceiptLong,
            label = "External Order",
            value = order.externalOrderId ?: "N/A",
            tint = Gold,
            modifier = Modifier.weight(1f)
        )
        TakeoutDetailMetaCard(
            icon = Icons.Filled.CheckCircle,
            label = "Status",
            value = "${order.orderStatus.displayName} • ${order.paymentStatus.displayName}",
            tint = Green,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TakeoutDetailMetaCard(
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
    ) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = RoundedCornerShape(12.dp), color = tint.copy(alpha = 0.14f)) {
                Icon(icon, null, tint = tint, modifier = Modifier.padding(10.dp).size(24.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
