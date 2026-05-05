package ui.screens.takeout.editor

import data.model.FoodSize

data class TakeoutFoodEntry(
    val itemNumber: Short,
    val foodName: String,
    val actualFoodSize: FoodSize,
    val requestedFoodSize: FoodSize?,
    val quantity: Int,
    val price: Double,
    val packagingInstruction: String? = null
) {
    val lineTotal: Double get() = price * quantity
    val packageLabel: String get() = if (requestedFoodSize == null) "Default: ${actualFoodSize.name} × $quantity" else "${actualFoodSize.name} × $quantity"
}
