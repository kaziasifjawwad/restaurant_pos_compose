package ui.screens.dashboard.pro

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

object DashboardFormatters {
    private val moneyFormatter: NumberFormat = NumberFormat.getNumberInstance(Locale("en", "BD")).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 0
    }

    fun apiDate(date: LocalDate): String = date.format(DateTimeFormatter.ISO_LOCAL_DATE)

    fun date(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))

    fun money(value: Double): String = "Tk ${moneyFormatter.format(value)}"

    fun decimal(value: Double): String = if (abs(value % 1.0) < 0.0001) value.toLong().toString() else "%.2f".format(value)

    fun percent(value: Double): String = "%.1f%%".format(value)

    fun deltaLabel(value: Double?): String = when {
        value == null -> "No comparison"
        value >= 0 -> "+${percent(value)} vs previous"
        else -> "${percent(value)} vs previous"
    }

    fun deltaPercent(current: Double, previous: Double): Double? =
        if (previous == 0.0) null else ((current - previous) / previous) * 100.0

    fun humanDateTime(value: String?): String {
        if (value.isNullOrBlank()) return "-"
        return runCatching {
            OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd MMM, hh:mm a"))
        }.getOrElse {
            runCatching {
                Instant.parse(value).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd MMM, hh:mm a"))
            }.getOrElse {
                runCatching {
                    LocalDateTime.parse(value).format(DateTimeFormatter.ofPattern("dd MMM, hh:mm a"))
                }.getOrDefault(value)
            }
        }
    }

    fun timezoneLabel(): String = ZoneId.systemDefault().id
}
