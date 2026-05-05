package ui.screens.dashboard.pro

import data.model.DashboardFullResponse
import data.model.DashboardTimelineResponse
import java.time.LocalDate

data class DashboardDateRange(
    val draftFrom: LocalDate,
    val draftTo: LocalDate,
    val appliedFrom: LocalDate,
    val appliedTo: LocalDate
) {
    val hasChanges: Boolean = draftFrom != appliedFrom || draftTo != appliedTo
}

data class KpiMetric(
    val title: String,
    val value: String,
    val subtitle: String,
    val deltaPercent: Double?,
    val sparkline: List<Double>,
    val contentDescription: String
)

data class ChartEntry(
    val label: String,
    val value: Double,
    val secondary: String = "",
    val referenceId: Long? = null
)

data class DashboardAnomaly(
    val title: String,
    val message: String,
    val severity: String = "WARNING"
)

fun DashboardFullResponse.detectAnomalies(): List<DashboardAnomaly> {
    val beverageOutliers = topBeverages
        .filter { it.orderCount in 1..3 && it.netSales >= 50000.0 }
        .map {
            DashboardAnomaly(
                title = "Anomaly detected: ${it.beverageName.ifBlank { "Beverage" }}",
                message = "${it.orderCount} orders generated ${DashboardFormatters.money(it.netSales)}. Please verify price, quantity, or payment entry."
            )
        }

    val foodOutliers = topFoodItems
        .filter { it.orderCount in 1..3 && it.netSales >= 50000.0 }
        .map {
            DashboardAnomaly(
                title = "Anomaly detected: ${it.foodName.ifBlank { "Food item" }}",
                message = "${it.orderCount} orders generated ${DashboardFormatters.money(it.netSales)}. Please verify item pricing or discount data."
            )
        }

    return (beverageOutliers + foodOutliers).take(4)
}

fun timelineValues(timeline: List<DashboardTimelineResponse>): List<Double> =
    timeline.map { it.salesAmount }.ifEmpty { listOf(0.0, 0.0) }
