package data.model

import kotlinx.serialization.Serializable

// ==================== Beverage Response Models ====================

@Serializable
data class PageBeverageResponse(
    val totalPages: Int,
    val totalElements: Long,
    val size: Int,
    val content: List<BeverageResponse>,
    val number: Int,
    val numberOfElements: Int,
    val first: Boolean,
    val last: Boolean,
    val empty: Boolean
)

@Serializable
data class BeverageResponse(
    val id: Long,
    val name: String,
    val prices: List<BeveragePrice> = emptyList()
)

@Serializable
data class BeveragePrice(
    val price: Double,
    val quantity: Double,
    val unit: QuantityUnit
)

@Serializable
enum class QuantityUnit {
    LITER, MILLILITER
}

// ==================== Beverage Request Models ====================

@Serializable
data class BeverageRequest(
    val name: String,
    val prices: List<BeveragePrice> = emptyList()
)


