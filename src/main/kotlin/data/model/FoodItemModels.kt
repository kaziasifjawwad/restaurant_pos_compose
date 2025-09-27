package data.model

import kotlinx.serialization.Serializable

@Serializable
data class PageFoodItemResponse(
    val totalPages: Int,
    val totalElements: Long,
    val size: Int,
    val content: List<FoodItemResponse>,
    val number: Int,
    val numberOfElements: Int,
    val first: Boolean,
    val last: Boolean,
    val empty: Boolean
)

@Serializable
data class FoodItemResponse(
    val id: Long,
    val name: String,
    val description: String? = null,
    val foodCategories: List<FoodCategory> = emptyList(),
    val foodPrices: List<FoodPrice> = emptyList(),
    val itemNumber: Int
)

@Serializable
data class FoodCategory(
    val id: Long,
    val name: String,
    val description: String? = null
)

@Serializable
data class FoodPrice(
    val foodPrice: Double,
    val foodSize: FoodSize,
    val ingredientAmountRequest: List<IngredientAmountRequest> = emptyList()
)

@Serializable
data class IngredientAmountRequest(
    val ingredientId: Long,
    val ingredientName: String,
    val amount: Double,
    val unitOfMeasurement: UnitOfMeasurement
)

@Serializable
enum class FoodSize {
    INDIVIDUAL, SMALL, REGULAR, LARGE, PARTY
}

@Serializable
enum class UnitOfMeasurement {
    KG, GRAM, MILLIGRAM, LITER, MILLILITER
}
