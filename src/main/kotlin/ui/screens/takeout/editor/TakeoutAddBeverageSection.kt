package ui.screens.takeout.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.model.BeveragePrice
import data.model.BeverageResponse

@Composable
fun TakeoutAddBeverageSection(
    beverages: List<BeverageResponse>,
    onAdd: (TakeoutBeverageEntry) -> Unit
) {
    var selectedBeverage by remember { mutableStateOf<BeverageResponse?>(null) }
    var selectedPrice by remember { mutableStateOf<BeveragePrice?>(null) }
    var amountInput by remember { mutableStateOf("1") }

    TakeoutEditorSection("Add Beverage") {
        TakeoutDropdown(
            label = "Beverage",
            selected = selectedBeverage?.name ?: "",
            items = beverages,
            itemText = { it.name },
            onSelect = { selectedBeverage = it; selectedPrice = null }
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TakeoutDropdown(
                label = "Size",
                selected = selectedPrice?.let { "${it.quantity} ${it.unit.name}" } ?: "",
                items = selectedBeverage?.prices ?: emptyList(),
                itemText = { "${it.quantity} ${it.unit.name}" },
                onSelect = { selectedPrice = it },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = amountInput,
                onValueChange = { if (it.all(Char::isDigit)) amountInput = it },
                label = { Text("Qty") },
                modifier = Modifier.width(90.dp),
                singleLine = true
            )
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                val beverage = selectedBeverage ?: return@Button
                val price = selectedPrice ?: return@Button
                val amount = amountInput.toIntOrNull() ?: 1
                onAdd(
                    TakeoutBeverageEntry(
                        beverageId = beverage.id,
                        beverageName = "${beverage.name} ${price.quantity}${price.unit.name.first()}",
                        quantity = price.quantity,
                        amount = amount,
                        unit = price.unit,
                        price = price.price
                    )
                )
                selectedPrice = null
                amountInput = "1"
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedBeverage != null && selectedPrice != null
        ) {
            Icon(Icons.Default.Add, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add Beverage")
        }
    }
}
