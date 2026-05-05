package ui.screens.takeout.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.model.TakeoutMediumResponse

@Composable
fun TakeoutSourceCustomerSection(
    mediums: List<TakeoutMediumResponse>,
    mediumCode: String,
    onMediumCodeChange: (String) -> Unit,
    externalOrderId: String,
    onExternalOrderIdChange: (String) -> Unit,
    customerName: String,
    onCustomerNameChange: (String) -> Unit,
    customerPhone: String,
    onCustomerPhoneChange: (String) -> Unit,
    customerAddress: String,
    onCustomerAddressChange: (String) -> Unit
) {
    val selectedMedium = mediums.firstOrNull { it.mediumCode == mediumCode }
    TakeoutEditorSection("Source & Customer") {
        TakeoutDropdown(
            label = "Order Medium",
            selected = selectedMedium?.displayName ?: mediumCode,
            items = mediums,
            itemText = { "${it.displayName} (${it.mediumCode})" },
            onSelect = { onMediumCodeChange(it.mediumCode) },
            enabled = mediums.isNotEmpty()
        )
        if (mediums.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = mediumCode,
                onValueChange = { onMediumCodeChange(it.uppercase()) },
                label = { Text("Medium Code e.g. FOODPANDA") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = externalOrderId,
            onValueChange = onExternalOrderIdChange,
            label = { Text("External Order ID") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(customerName, onCustomerNameChange, label = { Text("Customer Name") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(customerPhone, onCustomerPhoneChange, label = { Text("Customer Phone") }, modifier = Modifier.weight(1f), singleLine = true)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            customerAddress,
            onCustomerAddressChange,
            label = { Text("Customer Address") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )
    }
}
