package ui.screens.takeout.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TakeoutRiderChargeSection(
    riderName: String,
    onRiderNameChange: (String) -> Unit,
    riderPhone: String,
    onRiderPhoneChange: (String) -> Unit,
    riderPickupCode: String,
    onRiderPickupCodeChange: (String) -> Unit,
    packagingCharge: String,
    onPackagingChargeChange: (String) -> Unit,
    deliveryCharge: String,
    onDeliveryChargeChange: (String) -> Unit,
    specialInstruction: String,
    onSpecialInstructionChange: (String) -> Unit
) {
    TakeoutEditorSection("Rider, Charges & Instruction") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(riderName, onRiderNameChange, label = { Text("Rider Name") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(riderPhone, onRiderPhoneChange, label = { Text("Rider Phone") }, modifier = Modifier.weight(1f), singleLine = true)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(riderPickupCode, onRiderPickupCodeChange, label = { Text("Rider Pickup Code") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(packagingCharge, onPackagingChargeChange, label = { Text("Packaging Charge") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(deliveryCharge, onDeliveryChargeChange, label = { Text("Delivery Charge") }, modifier = Modifier.weight(1f), singleLine = true)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(specialInstruction, onSpecialInstructionChange, label = { Text("Special Instruction") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
    }
}
