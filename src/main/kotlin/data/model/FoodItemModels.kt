package data.model

import kotlinx.serialization.Serializable

// ==================== Response Models ====================

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
    val defaultPrice: FoodPrice? = null,
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
    val isDefault: Boolean = false,
    val ingredientAmountRequest: List<IngredientAmountRequest>? = null
) {
    /** Get ingredients as non-null list */
    fun getIngredients(): List<IngredientAmountRequest> = ingredientAmountRequest ?: emptyList()
}

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

// ==================== Request Models ====================

@Serializable
data class FoodItemRequest(
    val name: String,
    val description: String? = null,
    val foodCategorySet: List<Long> = emptyList(),
    val foodPriceSet: List<FoodPriceRequest> = emptyList(),
    val itemNumber: Int
)

@Serializable
data class FoodPriceRequest(
    val foodPrice: Double,
    val foodSize: FoodSize,
    val isDefault: Boolean = false,
    val ingredientAmountRequest: List<IngredientAmountRequest> = emptyList()
)

// ==================== Ingredient Models ====================

@Serializable
data class Ingredient(
    val id: Long,
    val name: String,
    val description: String? = null
)

@Serializable
data class IngredientRequest(
    val name: String,
    val description: String
)

@Serializable
data class CategoryRequest(
    val name: String,
    val description: String
)

@Serializable
data class PageIngredientResponse(
    val totalPages: Int,
    val totalElements: Long,
    val size: Int,
    val content: List<Ingredient>,
    val number: Int,
    val numberOfElements: Int,
    val first: Boolean,
    val last: Boolean,
    val empty: Boolean
)

// ==================== Category Models ====================

@Serializable
data class PageCategoryResponse(
    val totalPages: Int,
    val totalElements: Long,
    val size: Int,
    val content: List<FoodCategory>,
    val number: Int,
    val numberOfElements: Int,
    val first: Boolean,
    val last: Boolean,
    val empty: Boolean
)
