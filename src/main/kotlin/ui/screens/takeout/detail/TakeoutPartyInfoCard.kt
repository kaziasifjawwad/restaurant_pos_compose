package ui.screens.takeout.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import data.model.TakeoutOrderResponse

@Composable
fun TakeoutPartyInfoCard(order: TakeoutOrderResponse) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("CUSTOMER, RIDER & INSTRUCTIONS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                InfoBlock("Customer", order.customerName ?: "N/A", Modifier.weight(1f))
                InfoBlock("Phone", order.customerPhone ?: "N/A", Modifier.weight(1f))
                InfoBlock("Address", order.customerAddress ?: "N/A", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                InfoBlock("Rider", order.riderName ?: "N/A", Modifier.weight(1f))
                InfoBlock("Rider Phone", order.riderPhone ?: "N/A", Modifier.weight(1f))
                InfoBlock("Pickup Code", order.riderPickupCode ?: "N/A", Modifier.weight(1f))
            }
            InfoBlock("Instruction", order.specialInstruction ?: "N/A", Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun InfoBlock(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
