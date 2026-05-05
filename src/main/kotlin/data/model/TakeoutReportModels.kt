package data.model

import kotlinx.serialization.Serializable

@Serializable
data class TakeoutReportDashboardResponse(
    val totalTakeoutOrders: Long = 0,
    val totalCompletedOrders: Long = 0,
    val totalCanceledOrders: Long = 0,
    val totalSales: Double = 0.0,
    val totalPlatformCommission: Double = 0.0,
    val totalRestaurantReceivable: Double = 0.0,
    val pendingSettlementAmount: Double = 0.0,
    val averagePreparationMinutes: Double = 0.0,
    val mediumWiseSales: List<TakeoutMediumSalesRow> = emptyList(),
    val paymentWiseSales: List<TakeoutPaymentSalesRow> = emptyList(),
    val orders: PageTakeoutOrderShortInfoResponse? = null
)

@Serializable
data class TakeoutMediumSalesRow(
    val mediumCode: String,
    val mediumName: String? = null,
    val orderCount: Long = 0,
    val totalSales: Double = 0.0,
    val commissionAmount: Double = 0.0,
    val receivableAmount: Double = 0.0,
    val canceledCount: Long = 0
)

@Serializable
data class TakeoutPaymentSalesRow(
    val paymentMethod: PaymentMethod? = null,
    val orderCount: Long = 0,
    val totalSales: Double = 0.0
)
