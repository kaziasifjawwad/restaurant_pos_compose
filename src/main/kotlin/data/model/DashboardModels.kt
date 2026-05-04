package data.model

import kotlinx.serialization.Serializable

@Serializable
data class DashboardFullResponse(
    val overview: DashboardOverviewResponse = DashboardOverviewResponse(),
    val salesTimeline: List<DashboardTimelineResponse> = emptyList(),
    val orderStatusDistribution: DashboardOrderStatusDistributionResponse = DashboardOrderStatusDistributionResponse(),
    val paymentDistribution: DashboardPaymentDistributionResponse = DashboardPaymentDistributionResponse(),
    val waiterPerformance: List<DashboardWaiterPerformanceResponse> = emptyList(),
    val topFoodItems: List<DashboardTopFoodItemResponse> = emptyList(),
    val topBeverages: List<DashboardTopBeverageResponse> = emptyList(),
    val tablePerformance: List<DashboardTablePerformanceResponse> = emptyList(),
    val peakHours: List<DashboardPeakHourResponse> = emptyList(),
    val lowStockSummary: DashboardLowStockSummaryResponse = DashboardLowStockSummaryResponse(),
    val insights: List<DashboardInsightResponse> = emptyList(),
    val recentActivity: List<DashboardRecentActivityResponse> = emptyList()
)

@Serializable
data class DashboardOverviewResponse(
    val dateFrom: String? = null,
    val dateTo: String? = null,
    val totalSales: Double = 0.0,
    val totalPaidOrders: Long = 0,
    val totalCanceledOrders: Long = 0,
    val activeOrders: Long = 0,
    val averageOrderValue: Double = 0.0,
    val cancellationRate: Double = 0.0,
    val cashTotal: Double = 0.0,
    val cardTotal: Double = 0.0,
    val bkashTotal: Double = 0.0,
    val rocketTotal: Double = 0.0,
    val nagadTotal: Double = 0.0,
    val totalTables: Long = 0,
    val occupiedTables: Long = 0,
    val freeTables: Long = 0,
    val totalFoodItems: Long = 0,
    val totalBeverages: Long = 0,
    val lowStockIngredientCount: Long = 0,
    val todaySales: Double = 0.0,
    val yesterdaySales: Double = 0.0,
    val thisWeekSales: Double = 0.0,
    val thisMonthSales: Double = 0.0
)

@Serializable
data class DashboardTimelineResponse(
    val label: String = "",
    val periodDate: String? = null,
    val periodStartDate: String? = null,
    val periodEndDate: String? = null,
    val salesAmount: Double = 0.0,
    val paidOrderCount: Long = 0,
    val averageOrderValue: Double = 0.0
)

@Serializable
data class DashboardOrderStatusDistributionResponse(
    val totalOrders: Long = 0,
    val items: List<DashboardOrderStatusItemResponse> = emptyList()
)

@Serializable
data class DashboardOrderStatusItemResponse(
    val status: String = "",
    val count: Long = 0,
    val percentage: Double = 0.0
)

@Serializable
data class DashboardPaymentDistributionResponse(
    val totalSales: Double = 0.0,
    val items: List<DashboardPaymentDistributionItemResponse> = emptyList()
)

@Serializable
data class DashboardPaymentDistributionItemResponse(
    val paymentMethod: String = "",
    val displayName: String = "",
    val orderCount: Long = 0,
    val amount: Double = 0.0,
    val percentage: Double = 0.0
)

@Serializable
data class DashboardWaiterPerformanceResponse(
    val waiterId: Long? = null,
    val waiterName: String? = null,
    val paidOrderCount: Long = 0,
    val canceledOrderCount: Long = 0,
    val totalSales: Double = 0.0,
    val averageOrderValue: Double = 0.0,
    val cancellationRate: Double = 0.0
)

@Serializable
data class DashboardTablePerformanceResponse(
    val tableId: Long? = null,
    val tableNumber: Int? = null,
    val floor: Int? = null,
    val capacity: Int? = null,
    val paidOrderCount: Long = 0,
    val canceledOrderCount: Long = 0,
    val totalSales: Double = 0.0,
    val averageOrderValue: Double = 0.0,
    val currentFreeStatus: Boolean? = null
)

@Serializable
data class DashboardTopFoodItemResponse(
    val foodId: Long? = null,
    val itemNumber: Short? = null,
    val foodName: String = "",
    val foodSize: String = "",
    val quantitySold: Double = 0.0,
    val grossSales: Double = 0.0,
    val discountAmount: Double = 0.0,
    val netSales: Double = 0.0,
    val orderCount: Long = 0
)

@Serializable
data class DashboardTopBeverageResponse(
    val beverageId: Long? = null,
    val beverageName: String = "",
    val unit: String = "",
    val quantity: Double = 0.0,
    val amount: Int = 0,
    val grossSales: Double = 0.0,
    val discountAmount: Double = 0.0,
    val netSales: Double = 0.0,
    val orderCount: Long = 0
)

@Serializable
data class DashboardPeakHourResponse(
    val hourOfDay: Int = 0,
    val label: String = "",
    val orderCount: Long = 0,
    val totalSales: Double = 0.0
)

@Serializable
data class DashboardLowStockSummaryResponse(
    val lowStockCount: Long = 0,
    val items: List<DashboardLowStockResponse> = emptyList()
)

@Serializable
data class DashboardLowStockResponse(
    val ingredientId: Long? = null,
    val ingredientName: String? = null,
    val currentAmount: Double = 0.0,
    val unit: String? = null,
    val severity: String = "WARNING"
)

@Serializable
data class DashboardInsightResponse(
    val insightType: String = "",
    val title: String = "",
    val message: String = "",
    val severity: String = "INFO",
    val metricValue: Double = 0.0,
    val recommendedAction: String? = null
)

@Serializable
data class DashboardRecentActivityResponse(
    val type: String = "",
    val title: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val createdDateTime: String? = null,
    val severity: String = "INFO",
    val referenceId: Long? = null
)
