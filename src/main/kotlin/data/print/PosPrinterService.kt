package data.print

import data.model.BeverageOrder
import data.model.DiscountType
import data.model.FoodOrder
import data.model.FoodOrderByCustomer
import data.model.OrderStatus
import data.model.TakeoutBeverageOrderResponse
import data.model.TakeoutFoodOrderResponse
import data.model.TakeoutOrderResponse
import data.model.TakeoutOrderStatus
import data.model.TakeoutPaymentStatus
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.print.DocFlavor
import javax.print.PrintService
import javax.print.PrintServiceLookup
import javax.print.SimpleDoc

object PosPrinterService {
    private const val MAX_CHARS = 42
    private val amountFormat = DecimalFormat("#,##0.00")
    private val dateTimeFormat = DateTimeFormatter.ofPattern("dd MMM yyyy  h:mm a")
    private val dateFormat = DateTimeFormatter.ofPattern("dd MMM yyyy")
    private val printerCharset: Charset = Charsets.US_ASCII

    fun printCashMemoTwice(order: FoodOrderByCustomer, printerModelName: String): Result<Unit> =
        printEscPos(
            printerModelName = printerModelName,
            jobName = "Cash Memo #${order.id}",
            copies = 2,
            lines = buildCashMemoLines(order)
        )

    fun printTakeoutCashMemo(order: TakeoutOrderResponse, printerModelName: String): Result<Unit> =
        printEscPos(
            printerModelName = printerModelName,
            jobName = "Takeout Cash Memo #${order.id}",
            copies = 2,
            lines = buildTakeoutCashMemoLines(order)
        )

    fun printBill(order: FoodOrderByCustomer, printerModelName: String): Result<Unit> =
        printEscPos(
            printerModelName = printerModelName,
            jobName = "Bill #${order.id}",
            copies = 1,
            lines = buildBillLines(order)
        )

    fun printKitchenMemo(order: FoodOrderByCustomer, printerModelName: String): Result<Unit> =
        printEscPos(
            printerModelName = printerModelName,
            jobName = "Kitchen Memo #${order.id}",
            copies = 1,
            lines = buildKitchenMemoLines(order)
        )

    fun printTakeoutKitchenMemo(order: TakeoutOrderResponse, printerModelName: String): Result<Unit> =
        printEscPos(
            printerModelName = printerModelName,
            jobName = "Takeout Kitchen Memo #${order.id}",
            copies = 1,
            lines = buildTakeoutKitchenMemoLines(order)
        )

    private fun printEscPos(
        printerModelName: String,
        jobName: String,
        copies: Int,
        lines: List<ReceiptLine>
    ): Result<Unit> = try {
        val printService = findPrinter(printerModelName)
            ?: return Result.failure(IllegalStateException("Printer not found: $printerModelName"))

        val bytes = buildEscPosPayload(lines = lines, copies = copies.coerceAtLeast(1))
        val job = printService.createPrintJob()
        val doc = SimpleDoc(bytes, DocFlavor.BYTE_ARRAY.AUTOSENSE, null)
        job.print(doc, null)
        println("[PosPrinterService] Sent $jobName to ${printService.name}")
        Result.success(Unit)
    } catch (ex: Exception) {
        Result.failure(ex)
    }

    private fun findPrinter(printerModelName: String): PrintService? {
        val target = printerModelName.trim()
        val services = PrintServiceLookup.lookupPrintServices(DocFlavor.BYTE_ARRAY.AUTOSENSE, null)
            .ifEmpty { PrintServiceLookup.lookupPrintServices(null, null) }
        return services.firstOrNull { it.name.equals(target, ignoreCase = true) }
            ?: services.firstOrNull { it.name.contains(target, ignoreCase = true) }
    }

    private fun buildEscPosPayload(lines: List<ReceiptLine>, copies: Int): ByteArray {
        val out = ByteArrayOutputStream()
        repeat(copies) { copyIndex ->
            out.write(ESC_INIT)
            out.write(ESC_ALIGN_LEFT)
            lines.forEach { line -> out.write(line.toEscPosBytes()) }
            out.write(FEED)
            out.write(PARTIAL_CUT)
            if (copyIndex < copies - 1) out.write(ESC_INIT)
        }
        return out.toByteArray()
    }

    private fun buildCashMemoLines(order: FoodOrderByCustomer): List<ReceiptLine> = buildList {
        receiptHeader(order, "CASH MEMO")
        addItemsTable(order, includePrice = true)
        addTotals(order)
        footer("Customer Copy")
    }

    private fun buildBillLines(order: FoodOrderByCustomer): List<ReceiptLine> = buildList {
        receiptHeader(order, "PRINT BILL")
        addItemsTable(order, includePrice = true)
        addTotals(order)
        add(ReceiptLine.pair("Status", order.orderStatus.displayName))
        add(ReceiptLine.pair("Payment", order.paymentMethod?.displayName ?: "N/A"))
        footer("Thank you")
    }

    private fun buildTakeoutCashMemoLines(order: TakeoutOrderResponse): List<ReceiptLine> = buildList {
        takeoutHeader(order, "TAKEOUT CASH MEMO")
        addTakeoutItemsTable(order, includePrice = true)
        addTakeoutTotals(order)
        add(ReceiptLine.pair("Status", order.orderStatus.displayName))
        add(ReceiptLine.pair("Payment", order.paymentStatus.displayName))
        add(ReceiptLine.pair("Method", order.paymentMethod?.displayName ?: "N/A"))
        footer("Customer Copy")
    }

    private fun buildKitchenMemoLines(order: FoodOrderByCustomer): List<ReceiptLine> = buildList {
        receiptHeader(order, "KITCHEN MEMO")
        add(ReceiptLine.separator())
        add(ReceiptLine.bold("FOOD"))
        val foodGroups = order.foodOrders
            .groupBy { "${safe(it.foodName, "Food #${it.itemNumber}")}|${it.foodSize.name}" }
            .values
            .map { group ->
                val first = group.first()
                val qty = group.sumOf { it.foodQuantity }
                "${qty} x ${safe(first.foodName, "Food #${first.itemNumber}")} (${first.foodSize.name})"
            }
        if (foodGroups.isEmpty()) add(ReceiptLine.text("No food items"))
        foodGroups.forEach { addWrapped(it) }

        if (order.beverageOrders.isNotEmpty()) {
            add(ReceiptLine.separator())
            add(ReceiptLine.bold("BEVERAGE"))
            val beverageGroups = order.beverageOrders
                .groupBy { "${safe(it.beverageName, "Beverage #${it.beverageId}")}|${it.quantity}|${it.unit?.name.orEmpty()}" }
                .values
                .map { group ->
                    val first = group.first()
                    val qty = group.sumOf { it.amount }
                    "${qty} x ${beverageDisplayName(first)}"
                }
            beverageGroups.forEach { addWrapped(it) }
        }
        footer("Prepare with care")
    }

    private fun buildTakeoutKitchenMemoLines(order: TakeoutOrderResponse): List<ReceiptLine> = buildList {
        takeoutHeader(order, "TAKEOUT KITCHEN MEMO")
        if (!order.specialInstruction.isNullOrBlank()) {
            add(ReceiptLine.separator())
            add(ReceiptLine.bold("SPECIAL INSTRUCTION"))
            addWrapped(order.specialInstruction)
        }
        add(ReceiptLine.separator())
        add(ReceiptLine.bold("FOOD"))
        val foodGroups = order.foodOrders
            .groupBy { "${safe(it.foodName, "Food #${it.itemNumber}")}|${it.foodSize?.name.orEmpty()}|${it.packagingInstruction.orEmpty()}" }
            .values
            .map { group ->
                val first = group.first()
                val qty = group.sumOf { it.foodQuantity }
                buildString {
                    append(qty).append(" x ").append(safe(first.foodName, "Food #${first.itemNumber}"))
                    first.foodSize?.let { append(" (").append(it.name).append(")") }
                    if (!first.packagingInstruction.isNullOrBlank()) append(" - ").append(first.packagingInstruction)
                }
            }
        if (foodGroups.isEmpty()) add(ReceiptLine.text("No food items"))
        foodGroups.forEach { addWrapped(it) }

        if (order.beverageOrders.isNotEmpty()) {
            add(ReceiptLine.separator())
            add(ReceiptLine.bold("BEVERAGE"))
            val beverageGroups = order.beverageOrders
                .groupBy { "${safe(it.beverageName, "Beverage #${it.beverageId}")}|${it.quantity}|${it.unit?.name.orEmpty()}|${it.packagingInstruction.orEmpty()}" }
                .values
                .map { group ->
                    val first = group.first()
                    val qty = group.sumOf { it.amount }
                    buildString {
                        append(qty).append(" x ").append(takeoutBeverageDisplayName(first))
                        if (!first.packagingInstruction.isNullOrBlank()) append(" - ").append(first.packagingInstruction)
                    }
                }
            beverageGroups.forEach { addWrapped(it) }
        }
        footer("Prepare with care")
    }

    private fun MutableList<ReceiptLine>.receiptHeader(order: FoodOrderByCustomer, title: String) {
        add(ReceiptLine.center("Unique Restaurant", bold = true, doubleSize = true))
        add(ReceiptLine.center("& Party Center", bold = true, doubleSize = true))
        add(ReceiptLine.center("29 Tonugonj Lane"))
        add(ReceiptLine.center("Katherpool, Sutrapur, Dhaka-1100"))
        add(ReceiptLine.center("+8801886937480 | +8801716261536"))
        add(ReceiptLine.separator())
        add(ReceiptLine.center(title, bold = true))
        add(ReceiptLine.separator())
        add(ReceiptLine.pair("Receipt No.", "#${order.id.toString().padStart(5, '0')}"))
        add(ReceiptLine.pair("Date", humanDateTime(order.createdDateTime)))
        add(ReceiptLine.pair("Table", "No. ${order.tableNumber}"))
        add(ReceiptLine.pair("Waiter", order.waiterName ?: "Unknown"))
    }

    private fun MutableList<ReceiptLine>.takeoutHeader(order: TakeoutOrderResponse, title: String) {
        add(ReceiptLine.center("Unique Restaurant", bold = true, doubleSize = true))
        add(ReceiptLine.center("& Party Center", bold = true, doubleSize = true))
        add(ReceiptLine.center("29 Tonugonj Lane"))
        add(ReceiptLine.center("Katherpool, Sutrapur, Dhaka-1100"))
        add(ReceiptLine.center("+8801886937480 | +8801716261536"))
        add(ReceiptLine.separator())
        add(ReceiptLine.center(title, bold = true))
        add(ReceiptLine.separator())
        add(ReceiptLine.pair("Order", order.takeoutOrderNumber ?: "#${order.id.toString().padStart(5, '0')}"))
        add(ReceiptLine.pair("Date", humanDateTime(order.createdDateTime)))
        add(ReceiptLine.pair("Medium", order.mediumName ?: order.mediumCode))
        if (!order.externalOrderId.isNullOrBlank()) add(ReceiptLine.pair("External", order.externalOrderId))
        if (!order.customerName.isNullOrBlank()) add(ReceiptLine.pair("Customer", order.customerName))
        if (!order.riderName.isNullOrBlank()) add(ReceiptLine.pair("Rider", order.riderName))
    }

    private fun MutableList<ReceiptLine>.addItemsTable(order: FoodOrderByCustomer, includePrice: Boolean) {
        add(ReceiptLine.separator())
        if (includePrice) {
            add(ReceiptLine.text("Item".padEnd(18) + "Qty".padStart(4) + "Price".padStart(9) + "Total".padStart(11)))
        } else {
            add(ReceiptLine.text(pair("Item", "Qty", MAX_CHARS)))
        }
        add(ReceiptLine.separator())

        order.foodOrders.sortedWith(compareBy<FoodOrder> { it.itemNumber }.thenBy { it.foodSize.name }).forEach { item ->
            val name = safe(item.foodName, "Food #${item.itemNumber}")
            val total = item.foodPrice * item.foodQuantity
            addItemLine(name, item.foodQuantity.toString(), money(item.foodPrice), money(total), includePrice)
            addWrapped("  ${item.foodSize.name}")
        }

        order.beverageOrders.sortedBy { safe(it.beverageName, "") }.forEach { item ->
            val total = beverageTotal(item)
            addItemLine(beverageDisplayName(item), item.amount.toString(), money(item.price), money(total), includePrice)
        }
    }

    private fun MutableList<ReceiptLine>.addTakeoutItemsTable(order: TakeoutOrderResponse, includePrice: Boolean) {
        add(ReceiptLine.separator())
        if (includePrice) {
            add(ReceiptLine.text("Item".padEnd(18) + "Qty".padStart(4) + "Price".padStart(9) + "Total".padStart(11)))
        } else {
            add(ReceiptLine.text(pair("Item", "Qty", MAX_CHARS)))
        }
        add(ReceiptLine.separator())

        order.foodOrders.sortedWith(compareBy<TakeoutFoodOrderResponse> { it.itemNumber ?: 0 }.thenBy { it.foodSize?.name.orEmpty() }).forEach { item ->
            val name = safe(item.foodName, "Food #${item.itemNumber}")
            val lineTotal = item.lineTotal.takeIf { it > 0.0 } ?: (item.foodPrice * item.foodQuantity)
            addItemLine(name, item.foodQuantity.toString(), money(item.foodPrice), money(lineTotal), includePrice)
            item.foodSize?.let { addWrapped("  ${it.name}") }
        }

        order.beverageOrders.sortedBy { safe(it.beverageName, "") }.forEach { item ->
            val lineTotal = item.lineTotal.takeIf { it > 0.0 } ?: (item.price * item.amount)
            addItemLine(takeoutBeverageDisplayName(item), item.amount.toString(), money(item.price), money(lineTotal), includePrice)
        }
    }

    private fun MutableList<ReceiptLine>.addItemLine(
        name: String,
        qty: String,
        price: String,
        total: String,
        includePrice: Boolean
    ) {
        val nameWidth = if (includePrice) 18 else 34
        val nameLines = wrap(name, nameWidth)
        nameLines.forEachIndexed { index, line ->
            if (index == 0) {
                add(
                    ReceiptLine.text(
                        if (includePrice) {
                            line.padEnd(nameWidth).take(nameWidth) +
                                qty.padStart(4) +
                                price.padStart(9) +
                                total.padStart(11)
                        } else {
                            line.padEnd(nameWidth).take(nameWidth) + qty.padStart(8)
                        }
                    )
                )
            } else {
                add(ReceiptLine.text(line))
            }
        }
    }

    private fun MutableList<ReceiptLine>.addTotals(order: FoodOrderByCustomer) {
        val subtotal = order.foodOrders.sumOf { it.foodPrice * it.foodQuantity } +
            order.beverageOrders.sumOf { beverageTotal(it) }
        val discountAmount = if (order.discountType == DiscountType.PERCENTAGE) {
            subtotal * order.discount / 100.0
        } else {
            order.discount
        }
        val discountLabel = if (order.discountType == DiscountType.PERCENTAGE) {
            "Discount(${formatNum(order.discount)}%)"
        } else {
            "Discount"
        }
        val grandTotal = order.totalAmount.takeIf { it > 0.0 } ?: (subtotal - discountAmount)

        add(ReceiptLine.separator())
        add(ReceiptLine.pair("Subtotal", money(subtotal)))
        add(ReceiptLine.pair(discountLabel, "-${money(discountAmount)}"))
        add(ReceiptLine.separator())
        add(ReceiptLine.pair("TOTAL", money(grandTotal), bold = true))
    }

    private fun MutableList<ReceiptLine>.addTakeoutTotals(order: TakeoutOrderResponse) {
        add(ReceiptLine.separator())
        add(ReceiptLine.pair("Subtotal", money(order.subtotalAmount)))
        add(ReceiptLine.pair("Discount", "-${money(order.discountAmount)}"))
        if (order.packagingCharge > 0.0) add(ReceiptLine.pair("Packaging", money(order.packagingCharge)))
        if (order.deliveryCharge > 0.0) add(ReceiptLine.pair("Delivery", money(order.deliveryCharge)))
        add(ReceiptLine.separator())
        add(ReceiptLine.pair("TOTAL", money(order.totalAmount), bold = true))
    }

    private fun MutableList<ReceiptLine>.footer(text: String) {
        add(ReceiptLine.separator())
        add(ReceiptLine.center(text))
        add(ReceiptLine.center("Visit Us Again Soon"))
    }

    private fun MutableList<ReceiptLine>.addWrapped(text: String) {
        wrap(text, MAX_CHARS).forEach { add(ReceiptLine.text(it)) }
    }

    private fun ReceiptLine.toEscPosBytes(): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(if (align == Align.CENTER) ESC_ALIGN_CENTER else ESC_ALIGN_LEFT)
        out.write(if (bold) ESC_BOLD_ON else ESC_BOLD_OFF)
        out.write(if (doubleSize) GS_DOUBLE_SIZE_ON else GS_DOUBLE_SIZE_OFF)
        out.write(sanitize(text).toByteArray(printerCharset))
        out.write(LF)
        out.write(GS_DOUBLE_SIZE_OFF)
        out.write(ESC_BOLD_OFF)
        out.write(ESC_ALIGN_LEFT)
        return out.toByteArray()
    }

    private data class ReceiptLine(
        val text: String,
        val align: Align = Align.LEFT,
        val bold: Boolean = false,
        val doubleSize: Boolean = false
    ) {
        companion object {
            fun text(value: String) = ReceiptLine(value)
            fun bold(value: String) = ReceiptLine(value, bold = true)
            fun center(value: String, bold: Boolean = false, doubleSize: Boolean = false) =
                ReceiptLine(value, Align.CENTER, bold, doubleSize)
            fun pair(left: String, right: String, bold: Boolean = false) = ReceiptLine(pair(left, right, MAX_CHARS), bold = bold)
            fun separator() = ReceiptLine("-".repeat(MAX_CHARS))
        }
    }

    private enum class Align { LEFT, CENTER }

    private val OrderStatus.displayName: String get() = when (this) {
        OrderStatus.ORDER_PLACED -> "Order Placed"
        OrderStatus.BILL_PRINTED -> "Bill Printed"
        OrderStatus.PAID -> "Paid"
        OrderStatus.CANCELED -> "Canceled"
    }

    private val TakeoutOrderStatus.displayName: String get() = when (this) {
        TakeoutOrderStatus.ORDER_RECEIVED -> "Order Received"
        TakeoutOrderStatus.ACCEPTED -> "Accepted"
        TakeoutOrderStatus.PREPARING -> "Preparing"
        TakeoutOrderStatus.READY_FOR_PICKUP -> "Ready for Pickup"
        TakeoutOrderStatus.PICKED_UP -> "Picked Up"
        TakeoutOrderStatus.COMPLETED -> "Completed"
        TakeoutOrderStatus.CANCELED -> "Canceled"
        TakeoutOrderStatus.REJECTED -> "Rejected"
        TakeoutOrderStatus.FAILED -> "Failed"
    }

    private val TakeoutPaymentStatus.displayName: String get() = when (this) {
        TakeoutPaymentStatus.UNPAID -> "Unpaid"
        TakeoutPaymentStatus.PAID -> "Paid"
        TakeoutPaymentStatus.SETTLEMENT_PENDING -> "Settlement Pending"
        TakeoutPaymentStatus.SETTLED -> "Settled"
        TakeoutPaymentStatus.REFUNDED -> "Refunded"
        TakeoutPaymentStatus.FAILED -> "Failed"
    }

    private fun pair(left: String, right: String, width: Int): String {
        val safeRight = right.take(width)
        val maxLeft = (width - safeRight.length - 1).coerceAtLeast(0)
        val safeLeft = left.take(maxLeft)
        if (safeLeft.length + safeRight.length >= width) return (safeLeft + safeRight).take(width)
        return safeLeft + " ".repeat(width - safeLeft.length - safeRight.length) + safeRight
    }

    private fun beverageDisplayName(item: BeverageOrder): String {
        val unit = item.unit?.name.orEmpty()
        val size = listOf(formatNum(item.quantity), unit.takeIf { it.isNotBlank() }).filterNotNull().joinToString(" ")
        return "${safe(item.beverageName, "Beverage #${item.beverageId}")} ($size)".trim()
    }

    private fun takeoutBeverageDisplayName(item: TakeoutBeverageOrderResponse): String {
        val unit = item.unit?.name.orEmpty()
        val size = listOf(formatNum(item.quantity), unit.takeIf { it.isNotBlank() }).filterNotNull().joinToString(" ")
        return "${safe(item.beverageName, "Beverage #${item.beverageId}")} ($size)".trim()
    }

    private fun beverageTotal(item: BeverageOrder): Double = item.price * item.amount
    private fun money(amount: Double): String = "Tk ${amountFormat.format(amount)}"
    private fun formatNum(value: Double): String = if (value % 1.0 == 0.0) value.toLong().toString() else DecimalFormat("#,##0.##").format(value)
    private fun safe(value: String?, fallback: String): String = value?.takeIf { it.isNotBlank() } ?: fallback
    private fun sanitize(value: String): String = value.map { if (it.code in 32..126 || it == '\n') it else '?' }.joinToString("")

    private fun humanDateTime(value: String?): String {
        val raw = value?.trim().orEmpty()
        if (raw.isBlank()) return LocalDateTime.now().format(dateTimeFormat)

        val localZone = ZoneId.systemDefault()
        return runCatching {
            OffsetDateTime.parse(raw).atZoneSameInstant(localZone).format(dateTimeFormat)
        }.getOrElse {
            runCatching {
                Instant.parse(raw).atZone(localZone).format(dateTimeFormat)
            }.getOrElse {
                runCatching {
                    LocalDateTime.parse(raw).format(dateTimeFormat)
                }.getOrElse {
                    runCatching {
                        LocalDate.parse(raw).format(dateFormat)
                    }.getOrDefault(raw)
                }
            }
        }
    }

    private fun wrap(value: String, width: Int): List<String> {
        val words = value.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return listOf("")
        val lines = mutableListOf<String>()
        var current = ""
        words.forEach { word ->
            if (word.length > width) {
                if (current.isNotBlank()) {
                    lines += current
                    current = ""
                }
                lines += word.chunked(width)
            } else {
                val candidate = if (current.isBlank()) word else "$current $word"
                if (candidate.length <= width) {
                    current = candidate
                } else {
                    lines += current
                    current = word
                }
            }
        }
        if (current.isNotBlank()) lines += current
        return lines
    }

    private val ESC_INIT = byteArrayOf(0x1B, 0x40)
    private val ESC_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
    private val ESC_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
    private val ESC_BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
    private val ESC_BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)
    private val GS_DOUBLE_SIZE_ON = byteArrayOf(0x1D, 0x21, 0x11)
    private val GS_DOUBLE_SIZE_OFF = byteArrayOf(0x1D, 0x21, 0x00)
    private val LF = byteArrayOf(0x0A)
    private val FEED = byteArrayOf(0x0A, 0x0A, 0x0A, 0x0A)
    private val PARTIAL_CUT = byteArrayOf(0x1D, 0x56, 0x01)
}
