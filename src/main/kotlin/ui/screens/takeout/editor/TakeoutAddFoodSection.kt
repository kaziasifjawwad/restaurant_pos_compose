package ui.screens.takeout.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import data.model.FoodItemShortInfo
import data.model.FoodSize

@Composable
fun TakeoutAddFoodSection(
    foodItems: List<FoodItemShortInfo>,
    onAdd: (TakeoutFoodEntry) -> Unit,
    onError: (String) -> Unit
) {
    var itemInput by remember { mutableStateOf("") }
    var selectedSize by remember { mutableStateOf<FoodSize?>(null) }
    val parsed = remember(itemInput) { parseTakeoutFoodInput(itemInput) }
    val selectedFood = remember(parsed, foodItems) { parsed.singleOrNull()?.let { p -> foodItems.find { it.itemNumber == p.itemNumber } } }
    val sizes = remember(selectedFood) { selectedFood?.foodPrices?.map { it.foodSize }?.distinct() ?: emptyList() }

    fun addFood() {
        val result = buildTakeoutFoodEntries(itemInput, selectedSize, foodItems)
        result.onSuccess { entries ->
            entries.forEach(onAdd)
            itemInput = ""
            selectedSize = null
        }.onFailure { error ->
            onError(error.message ?: "Failed to add food")
        }
    }

    LaunchedEffect(itemInput, sizes) {
        if (selectedSize !in sizes) selectedSize = null
    }

    TakeoutEditorSection("Add Food Item") {
        BoxWithConstraints {
            val itemInputField: @Composable (Modifier) -> Unit = { modifier ->
                OutlinedTextField(
                    value = itemInput,
                    onValueChange = { itemInput = it },
                    label = { Text("Item # or 1*3") },
                    supportingText = { Text("Press Enter to add. Blank package = default") },
                    modifier = modifier.onKeyEvent { event ->
                        if (event.key == Key.Enter && itemInput.isNotBlank()) {
                            addFood()
                            true
                        } else false
                    },
                    singleLine = true
                )
            }
            val packageDropdown: @Composable (Modifier) -> Unit = { modifier ->
                TakeoutDropdown(
                    label = "Package",
                    selected = selectedSize?.name ?: "",
                    items = sizes,
                    itemText = { it.name },
                    onSelect = { selectedSize = it },
                    modifier = modifier,
                    enabled = parsed.size == 1 && sizes.isNotEmpty()
                )
            }

            if (maxWidth < 420.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemInputField(Modifier.fillMaxWidth())
                    packageDropdown(Modifier.fillMaxWidth())
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemInputField(Modifier.weight(1f))
                    packageDropdown(Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { addFood() },
            modifier = Modifier.fillMaxWidth(),
            enabled = itemInput.isNotBlank()
        ) {
            Icon(Icons.Default.Add, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add Food")
        }
    }
}
