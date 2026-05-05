package ui.screens.takeout.editor

import data.model.FoodItemShortInfo
import data.model.FoodSize

fun buildTakeoutFoodEntries(
    input: String,
    selectedSize: FoodSize?,
    foodItems: List<FoodItemShortInfo>
): Result<List<TakeoutFoodEntry>> {
    val orders = parseTakeoutFoodInput(input)
    if (orders.isEmpty()) {
        return Result.failure(IllegalArgumentException("Invalid food pattern. Use 1 or 1*3 or 1 2*3"))
    }
    val entries = mutableListOf<TakeoutFoodEntry>()
    for (parsedOrder in orders) {
        val food = foodItems.find { it.itemNumber == parsedOrder.itemNumber }
            ?: return Result.failure(IllegalArgumentException("Food item #${parsedOrder.itemNumber} not found"))
        val priceInfo = selectedSize?.let { size -> food.foodPrices.find { it.foodSize == size } }
            ?: food.defaultPrice
            ?: food.foodPrices.firstOrNull { it.isDefault }
            ?: food.foodPrices.firstOrNull()
            ?: return Result.failure(IllegalArgumentException("${food.name} has no price configured"))
        entries += TakeoutFoodEntry(
            itemNumber = parsedOrder.itemNumber,
            foodName = food.name,
            actualFoodSize = priceInfo.foodSize,
            requestedFoodSize = selectedSize,
            quantity = parsedOrder.quantity,
            price = priceInfo.foodPrice
        )
    }
    return Result.success(entries)
}
