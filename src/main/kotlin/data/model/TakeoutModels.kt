package data.model

import kotlinx.serialization.Serializable

@Serializable
enum class TakeoutOrderStatus {
    ORDER_RECEIVED, ACCEPTED, PREPARING, READY_FOR_PICKUP, PICKED_UP, COMPLETED, CANCELED, REJECTED, FAILED;
    val displayName: String get() = when (this) {
        ORDER_RECEIVED -> "Order Received"
        ACCEPTED -> "Accepted"
        PREPARING -> "Preparing"
        READY_FOR_PICKUP -> "Ready for Pickup"
        PICKED_UP -> "Picked Up"
        COMPLETED -> "Completed"
        CANCELED -> "Canceled"
        REJECTED -> "Rejected"
        FAILED -> "Failed"
    }
    val isFinal: Boolean get() = this in setOf(COMPLETED, CANCELED, REJECTED, FAILED)
}

@Serializable
enum class TakeoutPaymentStatus {
    UNPAID, PAID, SETTLEMENT_PENDING, SETTLED, REFUNDED, FAILED;
    val displayName: String get() = when (this) {
        UNPAID -> "Unpaid"
        PAID -> "Paid"
        SETTLEMENT_PENDING -> "Settlement Pending"
        SETTLED -> "Settled"
        REFUNDED -> "Refunded"
        FAILED -> "Failed"
    }
}

@Serializable
enum class TakeoutSettlementStatus {
    NOT_REQUIRED, PENDING, PARTIALLY_SETTLED, SETTLED, DISPUTED;
    val displayName: String get() = when (this) {
        NOT_REQUIRED -> "Not Required"
        PENDING -> "Pending"
        PARTIALLY_SETTLED -> "Partially Settled"
        SETTLED -> "Settled"
        DISPUTED -> "Disputed"
    }
}

@Serializable
enum class CommissionType { NONE, PERCENTAGE, FIXED_AMOUNT, MANUAL }

@Serializable
data class TakeoutMediumRequest(
    val mediumCode: String,
    val displayName: String,
    val active: Boolean = true,
    val platformBased: Boolean = false,
    val requiresExternalOrderId: Boolean = false,
    val requiresCustomerPhone: Boolean = false,
    val requiresCustomerAddress: Boolean = false,
    val requiresRiderInfo: Boolean = false,
    val commissionType: CommissionType = CommissionType.NONE,
    val commissionValue: Double = 0.0,
    val defaultPaymentMethod: PaymentMethod? = null,
    val sortOrder: Int = 0,
    val note: String? = null
)

@Serializable
data class TakeoutMediumResponse(
    val id: Long,
    val mediumCode: String,
    val displayName: String,
    val active: Boolean = true,
    val platformBased: Boolean = false,
    val requiresExternalOrderId: Boolean = false,
    val requiresCustomerPhone: Boolean = false,
    val requiresCustomerAddress: Boolean = false,
    val requiresRiderInfo: Boolean = false,
    val commissionType: CommissionType = CommissionType.NONE,
    val commissionValue: Double = 0.0,
    val defaultPaymentMethod: PaymentMethod? = null,
    val sortOrder: Int = 0,
    val note: String? = null
)

@Serializable
data class TakeoutOrderRequest(
    val mediumCode: String,
    val externalOrderId: String? = null,
    val customerName: String? = null,
    val customerPhone: String? = null,
    val customerAddress: String? = null,
    val customerNote: String? = null,
    val riderName: String? = null,
    val riderPhone: String? = null,
    val riderPickupCode: String? = null,
    val specialInstruction: String? = null,
    val estimatedPickupTime: String? = null,
    val foodOrders: List<TakeoutFoodOrderRequest> = emptyList(),
    val beverageOrders: List<TakeoutBeverageOrderRequest> = emptyList(),
    val discount: Double = 0.0,
    val discountType: DiscountType = DiscountType.PERCENTAGE,
    val packagingCharge: Double = 0.0,
    val deliveryCharge: Double = 0.0,
    val platformCommissionAmount: Double = 0.0,
    val paymentMethod: PaymentMethod? = null,
    val paymentStatus: TakeoutPaymentStatus = TakeoutPaymentStatus.UNPAID
)

@Serializable
data class TakeoutFoodOrderRequest(val itemNumber: Short, val foodSize: FoodSize? = null, val foodQuantity: Int = 1, val discount: Double = 0.0, val discountType: DiscountType = DiscountType.PERCENTAGE, val packagingInstruction: String? = null)

@Serializable
data class TakeoutBeverageOrderRequest(val beverageId: Long, val quantity: Double, val unit: QuantityUnit, val amount: Int = 1, val discount: Double = 0.0, val discountType: DiscountType = DiscountType.PERCENTAGE, val packagingInstruction: String? = null)

@Serializable
data class TakeoutStatusUpdateRequest(val orderStatus: TakeoutOrderStatus, val note: String? = null, val actualPickupTime: String? = null)

@Serializable
data class TakeoutPaymentUpdateRequest(val paymentMethod: PaymentMethod? = null, val paymentStatus: TakeoutPaymentStatus = TakeoutPaymentStatus.UNPAID, val settlementStatus: TakeoutSettlementStatus? = null, val platformCommissionAmount: Double? = null, val restaurantReceivableAmount: Double? = null, val note: String? = null)

@Serializable
data class TakeoutCancelRequest(val cancelReason: String)

@Serializable
data class TakeoutOrderShortInfo(
    val id: Long,
    val takeoutOrderNumber: String? = null,
    val mediumCode: String,
    val mediumName: String? = null,
    val externalOrderId: String? = null,
    val customerName: String? = null,
    val customerPhone: String? = null,
    val orderStatus: TakeoutOrderStatus,
    val paymentStatus: TakeoutPaymentStatus,
    val settlementStatus: TakeoutSettlementStatus,
    val paymentMethod: PaymentMethod? = null,
    val totalAmount: Double = 0.0,
    val platformCommissionAmount: Double = 0.0,
    val restaurantReceivableAmount: Double = 0.0,
    val estimatedPickupTime: String? = null,
    val createdDateTime: String? = null
)

@Serializable
data class TakeoutFoodOrderResponse(val id: Long, val foodItemId: Long? = null, val itemNumber: Short? = null, val foodName: String? = null, val foodSize: FoodSize? = null, val foodQuantity: Int = 1, val foodPrice: Double = 0.0, val discount: Double = 0.0, val discountType: DiscountType? = null, val lineSubtotal: Double = 0.0, val lineDiscountAmount: Double = 0.0, val lineTotal: Double = 0.0, val packagingInstruction: String? = null, val packed: Boolean = false)

@Serializable
data class TakeoutBeverageOrderResponse(val id: Long, val beverageId: Long? = null, val beverageName: String? = null, val quantity: Double = 0.0, val unit: QuantityUnit? = null, val amount: Int = 1, val price: Double = 0.0, val discount: Double = 0.0, val discountType: DiscountType? = null, val lineSubtotal: Double = 0.0, val lineDiscountAmount: Double = 0.0, val lineTotal: Double = 0.0, val packagingInstruction: String? = null, val packed: Boolean = false)

@Serializable
data class TakeoutOrderResponse(
    val id: Long,
    val takeoutOrderNumber: String? = null,
    val mediumCode: String,
    val mediumName: String? = null,
    val externalOrderId: String? = null,
    val customerName: String? = null,
    val customerPhone: String? = null,
    val customerAddress: String? = null,
    val customerNote: String? = null,
    val riderName: String? = null,
    val riderPhone: String? = null,
    val riderPickupCode: String? = null,
    val specialInstruction: String? = null,
    val orderStatus: TakeoutOrderStatus,
    val paymentStatus: TakeoutPaymentStatus,
    val settlementStatus: TakeoutSettlementStatus,
    val paymentMethod: PaymentMethod? = null,
    val subtotalAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val packagingCharge: Double = 0.0,
    val deliveryCharge: Double = 0.0,
    val platformCommissionAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val restaurantReceivableAmount: Double = 0.0,
    val estimatedPickupTime: String? = null,
    val actualPickupTime: String? = null,
    val completedAt: String? = null,
    val canceledAt: String? = null,
    val cancelReason: String? = null,
    val createdByUserId: Long? = null,
    val createdByUserName: String? = null,
    val createdDateTime: String? = null,
    val foodOrders: List<TakeoutFoodOrderResponse> = emptyList(),
    val beverageOrders: List<TakeoutBeverageOrderResponse> = emptyList()
)

@Serializable
data class PageTakeoutOrderShortInfoResponse(val totalPages: Int = 0, val totalElements: Long = 0, val size: Int = 0, val content: List<TakeoutOrderShortInfo> = emptyList(), val number: Int = 0, val numberOfElements: Int = 0, val first: Boolean = true, val last: Boolean = true, val empty: Boolean = true)
