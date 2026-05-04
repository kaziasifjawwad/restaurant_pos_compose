package data.model

import kotlinx.serialization.Serializable

// ==================== Enums ====================

@Serializable
enum class OrderStatus {
    ORDER_PLACED,
    BILL_PRINTED,
    PAID,
    CANCELED;

    val displayName: String get() = when (this) {
        ORDER_PLACED -> "Order Placed"
        BILL_PRINTED -> "Bill Printed"
        PAID -> "Paid"
        CANCELED -> "Canceled"
    }
}

@Serializable
enum class DiscountType {
    PERCENTAGE,
    AMOUNT
}

@Serializable
enum class PaymentMethod {
    CASH,
    CREDIT_CARD,
    BKASH,
    ROCKET,
    NAGAD;

    val displayName: String get() = when (this) {
        CASH -> "Cash"
        CREDIT_CARD -> "Credit Card"
        BKASH -> "bKash"
        ROCKET -> "Rocket"
        NAGAD -> "Nagad"
    }
}

@Serializable
data class PaymentMethodResponse(
    val id: Long,
    val methodCode: PaymentMethod,
    val displayName: String,
    val defaultMethod: Boolean = false,
    val active: Boolean = true
)

@Serializable
data class PaymentMethodRequest(
    val methodCode: PaymentMethod,
    val displayName: String,
    val defaultMethod: Boolean = false,
    val active: Boolean = true
)

// Note: Use QuantityUnit from BeverageModels.kt for liquid units

// ==================== Response Models ====================

@Serializable
data class FoodOrderShortInfo(
    val id: Long,
    val waiterName: String? = null,
    val waiterId: Long? = null,
    val totalAmount: Double = 0.0,
    val orderStatus: OrderStatus,
    val paymentMethod: PaymentMethod? = null,
    val tableId: Long? = null,
    val tableNumber: Int = 0,
    val createdDateTime: String? = null
)

@Serializable
data class FoodOrderByCustomer(
    val id: Long,
    val waiterName: String? = null,
    val waiterId: Long? = null,
    val tableNumber: Int = 0,
    val tableId: Long? = null,
    val orderStatus: OrderStatus,
    val paymentMethod: PaymentMethod? = null,
    val foodOrders: List<FoodOrder> = emptyList(),
    val beverageOrders: List<BeverageOrder> = emptyList(),
    val discountType: DiscountType? = null,
    val totalAmount: Double = 0.0,
    val discount: Double = 0.0,
    val createdDateTime: String? = null
)

@Serializable
data class FoodOrder(
    val foodName: String? = null,
    val foodPrice: Double = 0.0,
    val foodId: Long? = null,
    val foodSize: FoodSize,
    val discountType: DiscountType? = null,
    val discount: Double = 0.0,
    val foodQuantity: Int = 1,
    val itemNumber: Short = 0
)

@Serializable
data class BeverageOrder(
    val beverageName: String? = null,
    val beverageId: Long? = null,
    val price: Double = 0.0,
    val quantity: Double = 0.0,
    val amount: Int = 1,
    val unit: QuantityUnit? = null,
    val discount: Double = 0.0,
    val discountType: DiscountType? = null
)

// ==================== Request Models ====================

@Serializable
data class FoodOrderByCustomerRequest(
    val id: Long? = null,
    val waiterId: Long,
    val waiterName: String? = null,
    val orderStatus: OrderStatus = OrderStatus.ORDER_PLACED,
    val foodOrders: List<FoodOrderRequest> = emptyList(),
    val beverageOrders: List<BeverageOrderRequest> = emptyList(),
    val discountType: DiscountType = DiscountType.PERCENTAGE,
    val discount: Double = 0.0,
    val tableId: Long
)

@Serializable
data class FoodOrderRequest(
    val itemNumber: Short,
    val foodSize: FoodSize? = null,
    val foodQuantity: Int = 1,
    val discount: Double = 0.0,
    val discountType: DiscountType = DiscountType.PERCENTAGE
)

@Serializable
data class BeverageOrderRequest(
    val beverageId: Long,
    val amount: Int = 1,
    val quantity: Double,
    val unit: QuantityUnit,
    val discount: Double = 0.0,
    val discountType: DiscountType = DiscountType.PERCENTAGE
)

// ==================== Lookup Models ====================

@Serializable
data class WaiterInfo(
    val id: Long,
    val fullName: String? = null,
    val firstName: String? = null,
    val lastName: String? = null
) {
    val displayName: String get() = fullName ?: "$firstName $lastName".trim().ifEmpty { "Waiter #$id" }
}

@Serializable
data class TableInfo(
    val id: Long,
    val tableNumber: Int
)

@Serializable
data class PageTableResponse(
    val totalPages: Int = 0,
    val totalElements: Long = 0,
    val size: Int = 0,
    val content: List<TableInfo> = emptyList(),
    val number: Int = 0,
    val numberOfElements: Int = 0,
    val first: Boolean = true,
    val last: Boolean = true,
    val empty: Boolean = true
)

@Serializable
data class FoodItemShortInfo(
    val id: Long,
    val name: String,
    val itemNumber: Short,
    val foodPrices: List<FoodPriceInfo> = emptyList(),
    val defaultPrice: FoodPriceInfo? = null
)

@Serializable
data class FoodPriceInfo(
    val foodSize: FoodSize,
    val foodPrice: Double,
    val isDefault: Boolean = false
)

@Serializable
data class PageFoodItemShortInfoResponse(
    val totalPages: Int = 0,
    val totalElements: Long = 0,
    val content: List<FoodItemShortInfo> = emptyList(),
    val empty: Boolean = true
)
