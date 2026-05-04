package data.model

import kotlinx.serialization.Serializable

@Serializable
data class PosReportDashboardResponse(
    val orders: PosReportPage,
    val totalOrders: Long = 0,
    val totalSales: Double = 0.0,
    val cashTotal: Double = 0.0,
    val bkashTotal: Double = 0.0,
    val cardTotal: Double = 0.0,
    val rocketTotal: Double = 0.0,
    val nagadTotal: Double = 0.0
)

@Serializable
data class PosReportResponse(
    val id: String,
    val waiterName: String,
    val waiterId: String,
    val totalAmount: Double,
    val orderStatus: String,
    val paymentMethod: String? = null,
    val tableId: String,
    val tableNumber: Int,
    val createdDateTime: String
)

@Serializable
data class PosReportPage(
    val content: List<PosReportResponse>,
    val pageable: Pageable? = null,
    val totalPages: Int,
    val totalElements: Int,
    val last: Boolean,
    val size: Int,
    val number: Int,
    val first: Boolean,
    val numberOfElements: Int,
    val empty: Boolean
)

@Serializable
data class Pageable(
    val sort: List<String> = emptyList(),
    val offset: Int,
    val pageNumber: Int,
    val pageSize: Int,
    val unpaged: Boolean,
    val paged: Boolean
)

@Serializable
data class PosReportWaiterResponse(
    val id: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val fullName: String,
    val mobileNumber: String? = null,
    val username: String? = null,
    val presetAddress: String? = null,
    val permanentAddress: String? = null,
    val enabled: Boolean = true
)

@Serializable
data class PosOrderDetailResponse(
    val id: String,
    val waiterName: String,
    val waiterId: String,
    val tableNumber: Int,
    val tableId: String,
    val orderStatus: String,
    val paymentMethod: String? = null,
    val foodOrders: List<FoodOrderItem> = emptyList(),
    val beverageOrders: List<BeverageOrderItem> = emptyList(),
    val discountType: String,
    val totalAmount: Double,
    val discount: Double
)

@Serializable
data class FoodOrderItem(
    val foodName: String,
    val foodPrice: Double,
    val foodId: String,
    val itemNumber: Int,
    val foodSize: String,
    val discountType: String,
    val discount: Double,
    val foodQuantity: Int
)

@Serializable
data class BeverageOrderItem(
    val beverageName: String,
    val beverageId: String,
    val price: Double,
    val quantity: Double,
    val amount: Int,
    val unit: String,
    val discount: Double,
    val discountType: String
)
